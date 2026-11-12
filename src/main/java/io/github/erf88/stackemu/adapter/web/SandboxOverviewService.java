package io.github.erf88.stackemu.adapter.web;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

/** Read-only, short-lived snapshot of the state that the console can actually observe. */
@Named("sandboxOverview")
@ApplicationScoped
public class SandboxOverviewService {
  private static final long CACHE_NANOS = Duration.ofSeconds(3).toNanos();
  private static final String IMAGE_COMPATIBILITY = ">=0.1.0 <1.0.0";
  private static final HttpClient HTTP =
      HttpClient.newBuilder().connectTimeout(Duration.ofMillis(250)).build();

  private final String endpoint;
  private final String publicEndpoint;
  private final Path configPath;
  private final Path markerPath;
  private volatile CachedSnapshot cached;

  public SandboxOverviewService(
      @ConfigProperty(name = "stackemu.endpoint") String endpoint,
      @ConfigProperty(name = "stackemu.public-endpoint", defaultValue = "http://127.0.0.1:4566")
          String publicEndpoint,
      @ConfigProperty(name = "stackemu.config.path") String configPath,
      @ConfigProperty(
              name = "stackemu.marker.path",
              defaultValue = "/data/stackemu/.stackemu/marker.json")
          String markerPath) {
    this.endpoint = safeEndpoint(endpoint);
    this.publicEndpoint = safeEndpoint(publicEndpoint);
    this.configPath = Path.of(configPath);
    this.markerPath = Path.of(markerPath);
  }

  public Snapshot current() {
    long now = System.nanoTime();
    CachedSnapshot previous = cached;
    if (previous != null && now - previous.createdAtNanos() < CACHE_NANOS) {
      return previous.snapshot();
    }
    synchronized (this) {
      previous = cached;
      now = System.nanoTime();
      if (previous != null && now - previous.createdAtNanos() < CACHE_NANOS) {
        return previous.snapshot();
      }
      Snapshot snapshot = inspect();
      cached = new CachedSnapshot(System.nanoTime(), snapshot);
      return snapshot;
    }
  }

  private Snapshot inspect() {
    ConfigSnapshot config = readConfig();
    MarkerSnapshot marker = readMarker(config);
    boolean emulatorHealthy = probeEmulator();

    Check configuration =
        config.available()
            ? marker.matchesConfiguration()
                ? new Check(State.READY, "configurationAligned")
                : new Check(State.ATTENTION, marker.messageKey())
            : new Check(State.UNAVAILABLE, "configurationUnavailable");
    Check container = new Check(State.READY, "containerOnline");
    Check emulator =
        emulatorHealthy
            ? new Check(State.READY, "emulatorHealthy")
            : new Check(State.ATTENTION, "emulatorUnavailable");
    Check console = new Check(State.READY, "consoleReady");

    Action action =
        configuration.state() == State.READY && emulatorHealthy ? Action.STATUS : Action.DIAGNOSE;
    return new Snapshot(
        config.projectId(),
        publicEndpoint,
        config.available(),
        config.buckets(),
        config.queues(),
        config.topics(),
        configuration,
        container,
        emulator,
        console,
        action);
  }

  private ConfigSnapshot readConfig() {
    if (!Files.isRegularFile(configPath)) return ConfigSnapshot.unavailable();
    try {
      Object document = load(configPath, Files.readString(configPath));
      if (!(document instanceof Map<?, ?> root)
          || !(root.get("project") instanceof Map<?, ?> project)
          || !(project.get("id") instanceof String projectId)
          || !(root.get("resources") instanceof Map<?, ?> resources)
          || containsSecretField(document)
          || !projectId.matches("[a-z][a-z0-9-]*")) {
        return ConfigSnapshot.unavailable();
      }
      List<String> buckets = names(resources.get("buckets"));
      List<String> queues = names(resources.get("queues"));
      List<String> topics = names(resources.get("topics"));
      if (buckets == null || queues == null || topics == null) return ConfigSnapshot.unavailable();
      String hash = sha256(canonical(document, ""));
      return new ConfigSnapshot(projectId, buckets, queues, topics, hash, true);
    } catch (IOException | RuntimeException failure) {
      return ConfigSnapshot.unavailable();
    }
  }

  private MarkerSnapshot readMarker(ConfigSnapshot config) {
    if (!config.available() || !Files.isRegularFile(markerPath)) {
      return MarkerSnapshot.unavailable("markerUnavailable");
    }
    try {
      Object document = load(markerPath, Files.readString(markerPath));
      if (!(document instanceof Map<?, ?> marker)) {
        return MarkerSnapshot.unavailable("markerInvalid");
      }
      boolean sameProject = config.projectId().equals(string(marker.get("projectId")));
      boolean compatible =
          Integer.valueOf(1).equals(number(marker.get("schemaVersion")))
              && IMAGE_COMPATIBILITY.equals(string(marker.get("imageCompatibility")));
      boolean sameConfig = config.hash().equals(string(marker.get("configHash")));
      if (!sameProject) return MarkerSnapshot.unavailable("markerProjectMismatch");
      if (!compatible) return MarkerSnapshot.unavailable("markerIncompatible");
      if (!sameConfig) return MarkerSnapshot.unavailable("configurationChanged");
      return new MarkerSnapshot(true, true, "configurationAligned");
    } catch (IOException | RuntimeException failure) {
      return MarkerSnapshot.unavailable("markerInvalid");
    }
  }

  private boolean probeEmulator() {
    try {
      URI base = URI.create(endpoint);
      URI health =
          new URI(
              base.getScheme(),
              null,
              base.getHost(),
              base.getPort(),
              "/_ministack/health",
              null,
              null);
      HttpRequest request =
          HttpRequest.newBuilder(health).timeout(Duration.ofMillis(500)).GET().build();
      HttpResponse<Void> response = HTTP.send(request, HttpResponse.BodyHandlers.discarding());
      return response.statusCode() >= 200 && response.statusCode() < 300;
    } catch (Exception failure) {
      return false;
    }
  }

  private static Object load(Path path, String source) {
    LoadSettings settings =
        LoadSettings.builder()
            .setLabel(path.getFileName().toString())
            .setAllowDuplicateKeys(false)
            .build();
    return new Load(settings).loadFromString(source);
  }

  private static List<String> names(Object value) {
    if (value == null) return List.of();
    if (!(value instanceof List<?> values)) return null;
    var names = new ArrayList<String>();
    for (Object item : values) {
      if (item instanceof String name) names.add(name);
      else return null;
    }
    return List.copyOf(names);
  }

  private static boolean containsSecretField(Object value) {
    if (value instanceof Map<?, ?> map) {
      for (var entry : map.entrySet()) {
        String key = String.valueOf(entry.getKey());
        if (key.matches("(?i).*(secret|password|token|credential).*")
            || containsSecretField(entry.getValue())) return true;
      }
    } else if (value instanceof List<?> list) {
      for (Object item : list) if (containsSecretField(item)) return true;
    }
    return false;
  }

  private static String canonical(Object value, String field) {
    if (value instanceof Map<?, ?> map) {
      var entries = new ArrayList<>(map.entrySet());
      entries.sort(Comparator.comparing(entry -> String.valueOf(entry.getKey())));
      var result = new StringBuilder("{");
      for (var entry : entries) {
        result
            .append(String.valueOf(entry.getKey()))
            .append(':')
            .append(canonical(entry.getValue(), String.valueOf(entry.getKey())))
            .append(';');
      }
      return result.append('}').toString();
    }
    if (value instanceof List<?> list) {
      var result = new StringBuilder("[");
      for (Object item : list) result.append(canonical(item, field)).append(';');
      return result.append(']').toString();
    }
    return String.valueOf(value);
  }

  private static String sha256(String source) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
      var hash = new StringBuilder(digest.length * 2);
      for (byte value : digest) hash.append(String.format("%02x", value));
      return hash.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private static Integer number(Object value) {
    return value instanceof Number number ? number.intValue() : null;
  }

  private static String string(Object value) {
    return value instanceof String text ? text : "";
  }

  private static String safeEndpoint(String value) {
    try {
      URI uri = URI.create(value);
      if (uri.getHost() == null || uri.getScheme() == null) return "http://127.0.0.1:4566";
      return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), null, null)
          .toString();
    } catch (Exception failure) {
      return "http://127.0.0.1:4566";
    }
  }

  public enum State {
    READY,
    ATTENTION,
    UNAVAILABLE
  }

  public enum Action {
    STATUS,
    DIAGNOSE
  }

  public record Check(State state, String messageKey) {
    public String cssState() {
      return state.name().toLowerCase(java.util.Locale.ROOT);
    }
  }

  public record Snapshot(
      String projectId,
      String endpoint,
      boolean configurationAvailable,
      List<String> buckets,
      List<String> queues,
      List<String> topics,
      Check configuration,
      Check container,
      Check emulator,
      Check console,
      Action nextAction) {
    public boolean readyForUse() {
      return nextAction == Action.STATUS;
    }

    public String nextCommand() {
      return nextAction == Action.STATUS ? "stackemu status --json" : "stackemu diagnose --json";
    }

    public String nextActionKey() {
      return nextAction == Action.STATUS ? "nextActionStatus" : "nextActionDiagnose";
    }
  }

  private record ConfigSnapshot(
      String projectId,
      List<String> buckets,
      List<String> queues,
      List<String> topics,
      String hash,
      boolean available) {
    private static ConfigSnapshot unavailable() {
      return new ConfigSnapshot("", List.of(), List.of(), List.of(), "", false);
    }
  }

  private record MarkerSnapshot(
      boolean available, boolean matchesConfiguration, String messageKey) {
    private static MarkerSnapshot unavailable(String reason) {
      return new MarkerSnapshot(false, false, reason);
    }
  }

  private record CachedSnapshot(long createdAtNanos, Snapshot snapshot) {}
}
