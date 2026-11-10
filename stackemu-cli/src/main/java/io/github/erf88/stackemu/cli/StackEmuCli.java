package io.github.erf88.stackemu.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

/** Host-side lifecycle launcher. It deliberately invokes the Docker CLI, never its socket. */
public final class StackEmuCli {
  private static final Pattern PROJECT_ID = Pattern.compile("[a-z][a-z0-9-]*");
  private static final Pattern SECRET_FIELD = Pattern.compile("(?i).*(secret|password|token|credential).*" );
  private static final String IMAGE_COMPATIBILITY = ">=0.1.0 <1.0.0";

  public static void main(String[] args) {
    try {
      int exitCode = run(args);
      if (exitCode != 0) System.exit(exitCode);
    } catch (StackEmuFailure failure) {
      System.err.println("stackemu: " + failure.getMessage());
      System.exit(failure.code());
    } catch (Exception failure) {
      System.err.println("stackemu: " + failure.getMessage());
      System.exit(3);
    }
  }

  static int run(String[] args) throws Exception {
    return run(args, new ProcessDockerCommands(), System.out);
  }

  static int run(
      String[] args, SandboxDiagnostics.CommandRunner diagnosticsDocker, PrintStream output)
      throws Exception {
    String command = args.length == 0 ? "start" : args[0];
    Path config = config(args);
    Map<?, ?> document = read(config);
    String projectId = projectId(document);
    String configurationHash = configurationHash(document);
    if ("reset".equals(command) && !confirmed(args, projectId)) fail(2, "reset requires project id or --yes");
    if ("status".equals(command) || "diagnose".equals(command)) {
      SandboxDiagnostics.Report report =
          new SandboxDiagnostics(diagnosticsDocker)
              .inspect(projectId, configurationHash, "diagnose".equals(command));
      output.println(report.json());
      return report.exitCode();
    }
    try (var lock = acquire(projectId)) {
      docker("version", "--format", "{{.Server.Version}}");
      if (!"reset".equals(command)) ensureVolume(projectId);
      if ("restart".equals(command)) docker("restart", container(projectId));
      else if ("reset".equals(command)) reset(projectId, config, configurationHash);
      else start(projectId, config, configurationHash);
    }
    return 0;
  }

  private static void start(String projectId, Path config, String configurationHash) throws Exception {
    ensureVolume(projectId);
    docker(
        "run",
        "-d",
        "--name",
        container(projectId),
        "--label",
        "io.stackemu.project=" + projectId,
        "-p",
        "127.0.0.1:4566:4566",
        "-p",
        "127.0.0.1:8080:8080",
        "-v",
        volume(projectId) + ":/data/stackemu",
        "-v",
        config.toAbsolutePath() + ":/config/stackemu.yaml:ro",
        "--read-only",
        "--tmpfs",
        "/tmp:rw,noexec,nosuid,size=64m,mode=1777",
        "--cap-drop=ALL",
        "stackemu:local");
    waitUntilHealthy(projectId);
    writeMarker(projectId, configurationHash);
  }

  private static void reset(String projectId, Path config, String configurationHash) throws Exception {
    dockerIgnoreFailure("rm", "-f", container(projectId));
    dockerIgnoreFailure("volume", "rm", volume(projectId));
    start(projectId, config, configurationHash);
    audit(projectId, "reset");
  }

  private static void audit(String projectId, String action) throws IOException {
    Path directory = Path.of(System.getProperty("user.home"), ".stackemu", "audit");
    Files.createDirectories(directory);
    java.time.Instant cutoff = java.time.Instant.now().minus(java.time.Duration.ofDays(30));
    try (var entries = Files.list(directory)) {
      entries.filter(path -> {
        try { return Files.getLastModifiedTime(path).toInstant().isBefore(cutoff); } catch (IOException ignored) { return false; }
      }).forEach(path -> { try { Files.deleteIfExists(path); } catch (IOException ignored) { } });
    }
    try (var entries = Files.list(directory)) {
      entries.sorted(java.util.Comparator.comparing(Path::toString).reversed()).skip(100).forEach(path -> {
        try { Files.deleteIfExists(path); } catch (IOException ignored) { }
      });
    }
    String event = "{\"timestamp\":\"" + java.time.Instant.now() + "\",\"action\":\"" + action + "\",\"projectId\":\"" + projectId + "\"}";
    if (event.getBytes(StandardCharsets.UTF_8).length > 1024 * 1024) fail(3, "audit record exceeds 1 MiB");
    Files.writeString(directory.resolve(java.time.Instant.now().toEpochMilli() + "-" + action + ".json"), event, StandardCharsets.UTF_8);
  }

  private static void waitUntilHealthy(String projectId) throws Exception {
    long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(45);
    while (System.nanoTime() < deadline) {
      String status = dockerOutput("inspect", "--format", "{{.State.Health.Status}}", container(projectId));
      if ("healthy".equals(status.trim())) return;
      if ("exited".equals(dockerOutput("inspect", "--format", "{{.State.Status}}", container(projectId)).trim()))
        fail(3, "sandbox exited before bootstrap completed");
      Thread.sleep(500);
    }
    fail(3, "sandbox did not become healthy after bootstrap");
  }

  private static void ensureVolume(String projectId) throws Exception {
    if (dockerExit("volume", "inspect", volume(projectId)) != 0)
      docker("volume", "create", "--label", "io.stackemu.project=" + projectId, volume(projectId));
  }

  private static Path config(String[] args) {
    for (int i = 0; i + 1 < args.length; i++) if ("--config".equals(args[i])) return Path.of(args[i + 1]);
    return Path.of("stackemu.yaml");
  }

  @SuppressWarnings("unchecked")
  private static Map<?, ?> read(Path config) throws IOException {
    if (!Files.isRegularFile(config)) fail(2, "missing " + config);
    Object result =
        new Load(LoadSettings.builder().setLabel("stackemu.yaml").build())
            .loadFromString(Files.readString(config, StandardCharsets.UTF_8));
    if (!(result instanceof Map<?, ?>)) fail(2, "configuration must be a mapping");
    Map<?, ?> map = (Map<?, ?>) result;
    for (Object key : map.keySet())
      if (!List.of("version", "project", "services", "resources", "persistence", "limits").contains(key))
        fail(2, "unknown configuration field: " + key);
    return map;
  }

  private static String configurationHash(Map<?, ?> document) {
    return sha256(canonical(document, ""));
  }

  private static String canonical(Object value, String field) {
    if (SECRET_FIELD.matcher(field).matches()) fail(2, "configuration must not contain secrets: " + field);
    if (value instanceof Map<?, ?> map) {
      var entries = new ArrayList<>(map.entrySet());
      entries.sort(Comparator.comparing(entry -> String.valueOf(entry.getKey())));
      var values = new StringBuilder("{");
      for (var entry : entries)
        values
            .append(String.valueOf(entry.getKey()))
            .append(':')
            .append(canonical(entry.getValue(), String.valueOf(entry.getKey())))
            .append(';');
      return values.append('}').toString();
    }
    if (value instanceof List<?> list) {
      var values = new StringBuilder("[");
      for (Object item : list) values.append(canonical(item, field)).append(';');
      return values.append(']').toString();
    }
    return String.valueOf(value);
  }

  private static String sha256(String input) {
    try {
      byte[] hash = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
      var encoded = new StringBuilder();
      for (byte value : hash) encoded.append(String.format("%02x", value));
      return encoded.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private static void writeMarker(String projectId, String configurationHash) throws Exception {
    String marker =
        "{\"projectId\":\""
            + projectId
            + "\",\"schemaVersion\":1,\"imageCompatibility\":\""
            + IMAGE_COMPATIBILITY
            + "\",\"configHash\":\""
            + configurationHash
            + "\"}";
    String encodedMarker = Base64.getEncoder().encodeToString(marker.getBytes(StandardCharsets.UTF_8));
    docker(
        "run",
        "--rm",
        "--entrypoint",
        "/bin/sh",
        "-v",
        volume(projectId) + ":/data/stackemu",
        "--env",
        "STACKEMU_MARKER_BASE64=" + encodedMarker,
        "stackemu:local",
        "-c",
        "mkdir -p /data/stackemu/.stackemu && printf %s $STACKEMU_MARKER_BASE64 | base64 -d > /data/stackemu/.stackemu/marker.json");
  }

  private static LifecycleLock acquire(String projectId) throws IOException {
    Path directory = Path.of(System.getProperty("user.home"), ".stackemu", projectId, ".stackemu");
    Files.createDirectories(directory);
    Path lock = directory.resolve("lifecycle.lock");
    if (Files.exists(lock) && !lockOwnerIsAlive(lock)) Files.delete(lock);
    try {
      Files.writeString(
          lock,
          "{\"pid\":" + ProcessHandle.current().pid() + ",\"container\":\"" + container(projectId) + "\"}",
          StandardCharsets.UTF_8,
          java.nio.file.StandardOpenOption.CREATE_NEW);
    } catch (java.nio.file.FileAlreadyExistsException exception) {
      fail(4, "lifecycle operation already in progress for " + projectId);
    }
    return new LifecycleLock(lock);
  }

  private static boolean lockOwnerIsAlive(Path lock) throws IOException {
    String content = Files.readString(lock, StandardCharsets.UTF_8);
    var matcher = Pattern.compile("\\\"pid\\\":(\\d+)").matcher(content);
    return matcher.find() && ProcessHandle.of(Long.parseLong(matcher.group(1))).map(ProcessHandle::isAlive).orElse(false);
  }

  private record LifecycleLock(Path path) implements AutoCloseable {
    @Override
    public void close() throws IOException {
      Files.deleteIfExists(path);
    }
  }

  private static String projectId(Map<?, ?> document) {
    Object project = document.get("project");
    Object id = project instanceof Map<?, ?> map ? map.get("id") : null;
    if (!(id instanceof String value) || !PROJECT_ID.matcher(value).matches())
      fail(2, "project.id must be lowercase letters, digits, and hyphens");
    return (String) id;
  }

  private static boolean confirmed(String[] args, String projectId) {
    for (String arg : args) if ("--yes".equals(arg) || projectId.equals(arg)) return true;
    return false;
  }

  private static String volume(String projectId) { return "stackemu-" + projectId; }
  private static String container(String projectId) { return "stackemu-" + projectId; }

  private static void docker(String... args) throws Exception {
    if (dockerExit(args) != 0) fail(3, "docker command failed");
  }
  private static void dockerIgnoreFailure(String... args) throws Exception { dockerExit(args); }
  private static int dockerExit(String... args) throws IOException, InterruptedException {
    String[] command = new String[args.length + 1];
    command[0] = "docker";
    System.arraycopy(args, 0, command, 1, args.length);
    return new ProcessBuilder(command).inheritIO().start().waitFor();
  }
  private static String dockerOutput(String... args) throws IOException, InterruptedException {
    String[] command = new String[args.length + 1];
    command[0] = "docker";
    System.arraycopy(args, 0, command, 1, args.length);
    Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    if (process.waitFor() != 0) fail(3, "docker command failed");
    return output;
  }
  private static void fail(int code, String message) {
    throw new StackEmuFailure(code, message);
  }

  private static final class StackEmuFailure extends RuntimeException {
    private final int code;

    private StackEmuFailure(int code, String message) {
      super(message);
      this.code = code;
    }

    private int code() {
      return code;
    }
  }

  private static final class ProcessDockerCommands implements SandboxDiagnostics.CommandRunner {
    @Override
    public SandboxDiagnostics.Result execute(String... args) throws IOException, InterruptedException {
      String[] command = new String[args.length + 1];
      command[0] = "docker";
      System.arraycopy(args, 0, command, 1, args.length);
      Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
      String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      return new SandboxDiagnostics.Result(process.waitFor(), output);
    }
  }
}
