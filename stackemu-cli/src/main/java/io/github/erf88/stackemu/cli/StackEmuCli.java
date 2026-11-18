package io.github.erf88.stackemu.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
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
  private static final Pattern SECRET_FIELD =
      Pattern.compile("(?i).*(secret|password|token|credential).*");
  private static final String IMAGE_COMPATIBILITY = ">=0.1.0 <1.0.0";
  static final int DEFAULT_AWS_PORT = 4566;
  static final int DEFAULT_CONSOLE_PORT = 8080;

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
    return run(args, diagnosticsDocker, output, StackEmuCli::portAvailable);
  }

  static int run(
      String[] args,
      SandboxDiagnostics.CommandRunner diagnosticsDocker,
      PrintStream output,
      java.util.function.IntPredicate portProbe)
      throws Exception {
    String command = args.length == 0 ? "start" : args[0];
    Path config = config(args);
    Map<?, ?> document = read(config);
    String projectId = projectId(document);
    Ports ports = ports(document);
    String configurationHash = configurationHash(document);
    String stateHash = stateConfigurationHash(document);
    if ("reset".equals(command) && !confirmed(args, projectId))
      fail(2, "reset requires project id or --yes");
    if ("status".equals(command) || "diagnose".equals(command)) {
      SandboxDiagnostics.Report report =
          new SandboxDiagnostics(diagnosticsDocker, portProbe)
              .inspect(
                  projectId,
                  configurationHash,
                  stateHash,
                  ports.aws(),
                  ports.console(),
                  "diagnose".equals(command));
      output.println(report.json());
      return report.exitCode();
    }
    try (var lock = acquire(projectId)) {
      docker("version", "--format", "{{.Server.Version}}");
      if ("restart".equals(command))
        restart(projectId, config, configurationHash, stateHash, ports, output);
      else if ("reset".equals(command))
        reset(projectId, config, configurationHash, stateHash, ports, output);
      else start(projectId, config, configurationHash, stateHash, ports, output);
    }
    return 0;
  }

  private static void start(
      String projectId,
      Path config,
      String configurationHash,
      String stateHash,
      Ports ports,
      PrintStream output)
      throws Exception {
    start(projectId, config, configurationHash, stateHash, ports, output, false);
  }

  private static void start(
      String projectId,
      Path config,
      String configurationHash,
      String stateHash,
      Ports ports,
      PrintStream output,
      boolean portsAlreadyValidated)
      throws Exception {
    if (!portsAlreadyValidated) assertPortsAvailable(ports, null);
    boolean volumeCreated = ensureVolume(projectId);
    boolean containerCreated = false;
    try {
      launchContainer(projectId, config, ports);
      containerCreated = true;
      waitUntilHealthy(projectId);
      writeMarker(projectId, configurationHash, stateHash);
    } catch (Exception failure) {
      if (containerCreated) dockerIgnoreFailure("rm", "--force", container(projectId));
      if (volumeCreated) dockerIgnoreFailure("volume", "rm", volume(projectId));
      throw failure;
    }
    printEndpoints(ports, output);
  }

  private static void launchContainer(String projectId, Path config, Ports ports) throws Exception {
    dockerOutput(containerArguments(projectId, config, ports).toArray(String[]::new));
  }

  static List<String> containerArguments(String projectId, Path config, Ports ports) {
    return List.of(
        "run",
        "-d",
        "--name",
        container(projectId),
        "--label",
        "io.stackemu.project=" + projectId,
        "-p",
        "127.0.0.1:" + ports.aws() + ":4566",
        "-p",
        "127.0.0.1:" + ports.console() + ":8080",
        "-v",
        volume(projectId) + ":/data/stackemu",
        "-v",
        config.toAbsolutePath() + ":/config/stackemu.yaml:ro",
        "--read-only",
        "--tmpfs",
        "/tmp:rw,noexec,nosuid,size=64m,mode=1777",
        "--cap-drop=ALL",
        "--env",
        "STACKEMU_PUBLIC_ENDPOINT=http://127.0.0.1:" + ports.aws(),
        "stackemu:local");
  }

  private static void restart(
      String projectId,
      Path config,
      String configurationHash,
      String stateHash,
      Ports ports,
      PrintStream output)
      throws Exception {
    Ports currentPorts = currentPorts(projectId);
    Map<?, ?> marker = mapping(readMarker(projectId), "marker");
    String previousStateHash = string(marker.get("stateHash"));
    if (previousStateHash.isBlank()) previousStateHash = string(marker.get("configHash"));
    if (!stateHash.equals(previousStateHash))
      fail(4, "configuration drift affects project state; run reset to apply it");

    String currentState =
        dockerOutput("inspect", "--format", "{{.State.Status}}", container(projectId)).trim();
    assertPortsAvailable(ports, "running".equals(currentState) ? currentPorts : null);
    if (ports.equals(currentPorts)) {
      docker("restart", container(projectId));
      waitUntilHealthy(projectId);
    } else {
      String previous =
          container(projectId)
              + "-previous-"
              + java.util.UUID.randomUUID().toString().substring(0, 8);
      String previousMarker = readMarker(projectId);
      boolean renamed = false;
      boolean replacementCreated = false;
      try {
        docker("stop", container(projectId));
        docker("rename", container(projectId), previous);
        renamed = true;
        launchContainer(projectId, config, ports);
        replacementCreated = true;
        waitUntilHealthy(projectId);
        writeMarker(projectId, configurationHash, stateHash);
        docker("rm", previous);
      } catch (Exception rebindFailure) {
        try {
          if (replacementCreated) dockerIgnoreFailure("rm", "--force", container(projectId));
          if (renamed) docker("rename", previous, container(projectId));
          dockerIgnoreFailure("start", container(projectId));
          waitUntilHealthy(projectId);
          if (renamed) restoreMarker(projectId, previousMarker);
        } catch (Exception restoreFailure) {
          fail(
              7,
              "port rebind failed; project volume is preserved but the previous container could not be restored");
        }
        throw rebindFailure;
      }
    }
    writeMarker(projectId, configurationHash, stateHash);
    printEndpoints(ports, output);
  }

  private static String readMarker(String projectId) throws Exception {
    SandboxDiagnostics.Result result =
        dockerResult("exec", container(projectId), "cat", "/data/stackemu/.stackemu/marker.json");
    if (result.succeeded()) return result.output().trim();
    return dockerOutput(
        "run",
        "--rm",
        "--network",
        "none",
        "--read-only",
        "--entrypoint",
        "/bin/sh",
        "-v",
        volume(projectId) + ":/data/stackemu:ro",
        "stackemu:local",
        "-c",
        "cat /data/stackemu/.stackemu/marker.json");
  }

  private static void restoreMarker(String projectId, String marker) throws Exception {
    String encoded = Base64.getEncoder().encodeToString(marker.getBytes(StandardCharsets.UTF_8));
    docker(
        "run",
        "--rm",
        "--network",
        "none",
        "--entrypoint",
        "/bin/sh",
        "-v",
        volume(projectId) + ":/data/stackemu",
        "--env",
        "STACKEMU_MARKER_BASE64=" + encoded,
        "stackemu:local",
        "-c",
        "printf %s $STACKEMU_MARKER_BASE64 > /data/stackemu/.stackemu/marker.json.tmp && base64 -d /data/stackemu/.stackemu/marker.json.tmp > /data/stackemu/.stackemu/marker.json.new && mv /data/stackemu/.stackemu/marker.json.new /data/stackemu/.stackemu/marker.json && rm /data/stackemu/.stackemu/marker.json.tmp");
  }

  private static Ports currentPorts(String projectId) throws Exception {
    String bindings =
        dockerOutput(
            "inspect", "--format", "{{json .HostConfig.PortBindings}}", container(projectId));
    Map<?, ?> values = mapping(bindings, "container port bindings");
    return new Ports(hostPort(values, "4566/tcp"), hostPort(values, "8080/tcp"));
  }

  private static Ports tryCurrentPorts(String projectId) throws Exception {
    SandboxDiagnostics.Result state =
        dockerResult("inspect", "--format", "{{.State.Status}}", container(projectId));
    if (!state.succeeded() || !"running".equals(state.output().trim())) return null;
    SandboxDiagnostics.Result result =
        dockerResult(
            "inspect", "--format", "{{json .HostConfig.PortBindings}}", container(projectId));
    if (!result.succeeded()) return null;
    Map<?, ?> values = mapping(result.output(), "container port bindings");
    return new Ports(hostPort(values, "4566/tcp"), hostPort(values, "8080/tcp"));
  }

  private static int hostPort(Map<?, ?> bindings, String containerPort) {
    Object binding = bindings.get(containerPort);
    if (!(binding instanceof List<?> values)
        || values.isEmpty()
        || !(values.get(0) instanceof Map<?, ?>)) {
      fail(4, "container port mapping is incomplete: " + containerPort);
      return 0;
    }
    Map<?, ?> item = (Map<?, ?>) values.get(0);
    if (!"127.0.0.1".equals(item.get("HostIp")))
      fail(4, "sandbox ports must be bound to 127.0.0.1");
    try {
      int value = Integer.parseInt(String.valueOf(item.get("HostPort")));
      if (value < 1024 || value > 65535) throw new NumberFormatException();
      return value;
    } catch (NumberFormatException invalid) {
      fail(4, "container port mapping is invalid: " + containerPort);
      return 0;
    }
  }

  private static void reset(
      String projectId,
      Path config,
      String configurationHash,
      String stateHash,
      Ports ports,
      PrintStream output)
      throws Exception {
    assertPortsAvailable(ports, tryCurrentPorts(projectId));
    dockerIgnoreFailure("rm", "-f", container(projectId));
    dockerIgnoreFailure("volume", "rm", volume(projectId));
    start(projectId, config, configurationHash, stateHash, ports, output, true);
    audit(projectId, "reset");
  }

  private static void audit(String projectId, String action) throws IOException {
    Path directory = Path.of(System.getProperty("user.home"), ".stackemu", "audit");
    Files.createDirectories(directory);
    java.time.Instant cutoff = java.time.Instant.now().minus(java.time.Duration.ofDays(30));
    try (var entries = Files.list(directory)) {
      entries
          .filter(
              path -> {
                try {
                  return Files.getLastModifiedTime(path).toInstant().isBefore(cutoff);
                } catch (IOException ignored) {
                  return false;
                }
              })
          .forEach(
              path -> {
                try {
                  Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
              });
    }
    try (var entries = Files.list(directory)) {
      entries
          .sorted(java.util.Comparator.comparing(Path::toString).reversed())
          .skip(100)
          .forEach(
              path -> {
                try {
                  Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
              });
    }
    String event =
        "{\"timestamp\":\""
            + java.time.Instant.now()
            + "\",\"action\":\""
            + action
            + "\",\"projectId\":\""
            + projectId
            + "\"}";
    if (event.getBytes(StandardCharsets.UTF_8).length > 1024 * 1024)
      fail(3, "audit record exceeds 1 MiB");
    Files.writeString(
        directory.resolve(java.time.Instant.now().toEpochMilli() + "-" + action + ".json"),
        event,
        StandardCharsets.UTF_8);
  }

  private static void waitUntilHealthy(String projectId) throws Exception {
    long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(45);
    while (System.nanoTime() < deadline) {
      String status =
          dockerOutput("inspect", "--format", "{{.State.Health.Status}}", container(projectId));
      if ("healthy".equals(status.trim())) return;
      if ("unhealthy".equals(status.trim()))
        fail(3, "sandbox health check failed before startup completed");
      if ("exited"
          .equals(
              dockerOutput("inspect", "--format", "{{.State.Status}}", container(projectId))
                  .trim())) fail(3, "sandbox exited before bootstrap completed");
      Thread.sleep(500);
    }
    fail(3, "sandbox did not become healthy after bootstrap");
  }

  private static boolean ensureVolume(String projectId) throws Exception {
    if (dockerExit("volume", "inspect", volume(projectId)) != 0) {
      docker("volume", "create", "--label", "io.stackemu.project=" + projectId, volume(projectId));
      return true;
    }
    return false;
  }

  private static Path config(String[] args) {
    for (int i = 0; i + 1 < args.length; i++)
      if ("--config".equals(args[i])) return Path.of(args[i + 1]);
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
      if (!List.of("version", "project", "services", "resources", "persistence", "limits", "ports")
          .contains(key)) fail(2, "unknown configuration field: " + key);
    return map;
  }

  static Ports ports(Map<?, ?> document) {
    Object value = document.get("ports");
    if (!document.containsKey("ports")) return new Ports(DEFAULT_AWS_PORT, DEFAULT_CONSOLE_PORT);
    if (!(value instanceof Map<?, ?>)) fail(2, "ports must be a mapping");
    Map<?, ?> mapping = (Map<?, ?>) value;
    for (Object key : mapping.keySet())
      if (!List.of("aws", "console").contains(key)) fail(2, "unknown ports field: " + key);
    if (!mapping.containsKey("aws") || !mapping.containsKey("console"))
      fail(2, "ports.aws and ports.console are required when ports is configured");
    int aws = port(mapping.get("aws"), "ports.aws");
    int console = port(mapping.get("console"), "ports.console");
    if (aws == console) fail(2, "ports.aws and ports.console must be different");
    return new Ports(aws, console);
  }

  private static int port(Object value, String field) {
    if (!(value instanceof Number number)
        || number.doubleValue() != number.longValue()
        || number.longValue() < 1024
        || number.longValue() > 65535) {
      fail(2, field + " must be an integer between 1024 and 65535");
    }
    return ((Number) value).intValue();
  }

  private static String configurationHash(Map<?, ?> document) {
    return sha256(canonical(document, ""));
  }

  static String stateConfigurationHash(Map<?, ?> document) {
    var state = new java.util.LinkedHashMap<Object, Object>();
    state.putAll(document);
    state.remove("ports");
    return sha256(canonical(state, ""));
  }

  private static void assertPortsAvailable(Ports requested, Ports ownedPorts) {
    assertPortsAvailable(requested, ownedPorts, StackEmuCli::portAvailable);
  }

  static void assertPortsAvailable(
      Ports requested, Ports ownedPorts, java.util.function.IntPredicate portProbe) {
    if (!owns(ownedPorts, requested.aws()) && !portProbe.test(requested.aws()))
      fail(
          3,
          "host-port-in-use: AWS port "
              + requested.aws()
              + " is occupied; choose a free port in stackemu.yaml");
    if (!owns(ownedPorts, requested.console()) && !portProbe.test(requested.console()))
      fail(
          3,
          "host-port-in-use: console port "
              + requested.console()
              + " is occupied; choose a free port in stackemu.yaml");
  }

  private static boolean owns(Ports ports, int candidate) {
    return ports != null && (ports.aws() == candidate || ports.console() == candidate);
  }

  static boolean portAvailable(int port) {
    try (var socket = new ServerSocket()) {
      socket.setReuseAddress(false);
      socket.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port));
      return true;
    } catch (BindException occupied) {
      return false;
    } catch (IOException unavailable) {
      return false;
    }
  }

  private static Map<?, ?> mapping(String json, String label) {
    Object parsed =
        new Load(LoadSettings.builder().setLabel(label).build()).loadFromString(json.trim());
    if (!(parsed instanceof Map<?, ?>)) fail(4, "Docker returned invalid " + label);
    return (Map<?, ?>) parsed;
  }

  private static String string(Object value) {
    return value instanceof String text ? text : "";
  }

  private static void printEndpoints(Ports ports, PrintStream output) {
    output.println("AWS endpoint: http://127.0.0.1:" + ports.aws());
    output.println("Console: http://127.0.0.1:" + ports.console());
  }

  record Ports(int aws, int console) {}

  private static String canonical(Object value, String field) {
    if (SECRET_FIELD.matcher(field).matches())
      fail(2, "configuration must not contain secrets: " + field);
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
      byte[] hash =
          MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
      var encoded = new StringBuilder();
      for (byte value : hash) encoded.append(String.format("%02x", value));
      return encoded.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private static void writeMarker(String projectId, String configurationHash, String stateHash)
      throws Exception {
    String marker =
        "{\"projectId\":\""
            + projectId
            + "\",\"schemaVersion\":1,\"imageCompatibility\":\""
            + IMAGE_COMPATIBILITY
            + "\",\"configHash\":\""
            + configurationHash
            + "\",\"stateHash\":\""
            + stateHash
            + "\"}";
    String encodedMarker =
        Base64.getEncoder().encodeToString(marker.getBytes(StandardCharsets.UTF_8));
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
        "mkdir -p /data/stackemu/.stackemu && printf %s $STACKEMU_MARKER_BASE64 > /data/stackemu/.stackemu/marker.json.tmp && base64 -d /data/stackemu/.stackemu/marker.json.tmp > /data/stackemu/.stackemu/marker.json.new && mv /data/stackemu/.stackemu/marker.json.new /data/stackemu/.stackemu/marker.json && rm /data/stackemu/.stackemu/marker.json.tmp");
  }

  private static LifecycleLock acquire(String projectId) throws IOException {
    Path directory = Path.of(System.getProperty("user.home"), ".stackemu", projectId, ".stackemu");
    Files.createDirectories(directory);
    Path lock = directory.resolve("lifecycle.lock");
    if (Files.exists(lock) && !lockOwnerIsAlive(lock)) Files.delete(lock);
    try {
      Files.writeString(
          lock,
          "{\"pid\":"
              + ProcessHandle.current().pid()
              + ",\"container\":\""
              + container(projectId)
              + "\"}",
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
    return matcher.find()
        && ProcessHandle.of(Long.parseLong(matcher.group(1)))
            .map(ProcessHandle::isAlive)
            .orElse(false);
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

  private static String volume(String projectId) {
    return "stackemu-" + projectId;
  }

  private static String container(String projectId) {
    return "stackemu-" + projectId;
  }

  private static void docker(String... args) throws Exception {
    if (dockerExit(args) != 0) fail(3, "docker command failed");
  }

  private static void dockerIgnoreFailure(String... args) throws Exception {
    dockerExit(args);
  }

  private static int dockerExit(String... args) throws IOException, InterruptedException {
    String[] command = new String[args.length + 1];
    command[0] = "docker";
    System.arraycopy(args, 0, command, 1, args.length);
    return new ProcessBuilder(command).inheritIO().start().waitFor();
  }

  private static String dockerOutput(String... args) throws IOException, InterruptedException {
    SandboxDiagnostics.Result result = dockerResult(args);
    if (!result.succeeded()) {
      if (portConflict(result.output()))
        fail(3, "host-port-in-use: choose a free AWS or console port in stackemu.yaml and retry");
      fail(3, "docker command failed");
    }
    return result.output();
  }

  private static SandboxDiagnostics.Result dockerResult(String... args)
      throws IOException, InterruptedException {
    String[] command = new String[args.length + 1];
    command[0] = "docker";
    System.arraycopy(args, 0, command, 1, args.length);
    Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    return new SandboxDiagnostics.Result(process.waitFor(), output);
  }

  private static boolean portConflict(String output) {
    String lower = output.toLowerCase(java.util.Locale.ROOT);
    return lower.contains("port is already allocated")
        || lower.contains("ports are not available")
        || lower.contains("address already in use")
        || lower.contains("bind: cannot assign requested address");
  }

  private static void fail(int code, String message) {
    throw new StackEmuFailure(code, message);
  }

  static final class StackEmuFailure extends RuntimeException {
    private final int code;

    private StackEmuFailure(int code, String message) {
      super(message);
      this.code = code;
    }

    int code() {
      return code;
    }
  }

  private static final class ProcessDockerCommands implements SandboxDiagnostics.CommandRunner {
    @Override
    public SandboxDiagnostics.Result execute(String... args)
        throws IOException, InterruptedException {
      String[] command = new String[args.length + 1];
      command[0] = "docker";
      System.arraycopy(args, 0, command, 1, args.length);
      Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
      String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      return new SandboxDiagnostics.Result(process.waitFor(), output);
    }
  }
}
