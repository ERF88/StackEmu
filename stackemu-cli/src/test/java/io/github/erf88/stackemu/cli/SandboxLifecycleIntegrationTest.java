package io.github.erf88.stackemu.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URI;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

/** Docker lifecycle coverage, enabled only by an explicit integration-test opt-in. */
@EnabledIfEnvironmentVariable(named = "STACKEMU_DOCKER_INTEGRATION", matches = "true")
class SandboxLifecycleIntegrationTest {
  @TempDir Path temporaryDirectory;

  @Test
  void startStatusRestartAndResetKeepTheirProjectStateContract() throws Exception {
    String suffix = UUID.randomUUID().toString().replace("-", "");
    String projectId = "it-" + suffix;
    String bucket = "bucket-" + suffix;
    String queue = "queue-" + suffix;
    String topic = "topic-" + suffix;
    String container = "stackemu-" + projectId;
    String volume = container;
    Path config = temporaryDirectory.resolve("stackemu.yaml");
    Path userHome = temporaryDirectory.resolve("user-home");
    Files.createDirectories(userHome);
    int awsPort = availablePort();
    int consolePort = availablePortExcept(awsPort);
    Files.writeString(
        config,
        """
            version: 1
            project:
              id: %s
            services: [s3, sqs, sns]
            resources:
              buckets: [%s]
              queues: [%s]
              topics: [%s]
              subscriptions: []
            persistence: enabled
            ports:
              aws: %d
              console: %d
            """.formatted(projectId, bucket, queue, topic, awsPort, consolePort),
        StandardCharsets.UTF_8);

    try {
      CommandResult start = cli(userHome, config, "start");
      assertEquals(0, start.exitCode(), start.output());
      Map<?, ?> firstStatus = awaitReady(userHome, config);
      assertStatus(firstStatus, projectId);
      URI awsEndpoint = URI.create("http://127.0.0.1:" + awsPort);
      SandboxAwsContract.verifySdk(suffix, awsEndpoint);
      SandboxAwsContract.verifyConsoleCopy(suffix, awsEndpoint, URI.create("http://127.0.0.1:" + consolePort));
      if ("true".equalsIgnoreCase(System.getenv("STACKEMU_AWS_CLI_INTEGRATION"))) {
        SandboxAwsContract.verifyAwsCli(suffix, temporaryDirectory, awsEndpoint);
      }
      assertConsoleContains(consolePort, "/s3", bucket);
      assertConsoleContains(consolePort, "/sqs", queue);

      var containersBeforeDiagnostics = dockerLines("ps", "--all", "--quiet");
      CommandResult status = cli(userHome, config, "status", "--json");
      CommandResult diagnose = cli(userHome, config, "diagnose", "--json");
      assertEquals(0, status.exitCode(), status.output());
      assertEquals(0, diagnose.exitCode(), diagnose.output());
      assertTrue(parse(status.output()).containsKey("health"));
      assertTrue(parse(diagnose.output()).containsKey("checks"));
      assertEquals(awsEndpoint.toString(), map(parse(status.output()).get("endpoints")).get("aws"));
      assertEquals("http://127.0.0.1:" + consolePort, map(parse(status.output()).get("endpoints")).get("console"));
      assertEquals(containersBeforeDiagnostics, dockerLines("ps", "--all", "--quiet"));

      docker("exec", container, "sh", "-c", "printf persisted > /data/stackemu/.stackemu/integration-sentinel");
      CommandResult restart = cli(userHome, config, "restart");
      assertEquals(0, restart.exitCode(), restart.output());
      Map<?, ?> afterRestart = awaitReady(userHome, config);
      assertStatus(afterRestart, projectId);
      docker("exec", container, "test", "-f", "/data/stackemu/.stackemu/integration-sentinel");
      assertConsoleContains(consolePort, "/s3", bucket);

      CommandResult reset = cli(userHome, config, "reset", "--yes");
      assertEquals(0, reset.exitCode(), reset.output());
      Map<?, ?> afterReset = awaitReady(userHome, config);
      assertStatus(afterReset, projectId);
      docker("exec", container, "sh", "-c", "test ! -e /data/stackemu/.stackemu/integration-sentinel");
      assertConsoleContains(consolePort, "/s3", bucket);

      Path audit = userHome.resolve(".stackemu").resolve("audit");
      try (var events = Files.list(audit)) {
        String record = Files.readString(events.findFirst().orElseThrow(), StandardCharsets.UTF_8);
        assertTrue(record.contains(projectId));
        assertTrue(record.contains("reset"));
      }
      assertFalse(dockerLines("ps", "--all", "--filter", "name=" + container, "--quiet").isEmpty());
    } finally {
      dockerIgnoreFailure("rm", "--force", container);
      dockerIgnoreFailure("volume", "rm", "--force", volume);
    }
  }

  @Test
  void twoProjectsUseSeparatePortsAndPortOnlyRestartPreservesTheVolume() throws Exception {
    String suffix = UUID.randomUUID().toString().replace("-", "");
    String firstId = "ports-a-" + suffix;
    String secondId = "ports-b-" + suffix;
    String firstContainer = "stackemu-" + firstId;
    String secondContainer = "stackemu-" + secondId;
    Path firstConfig = temporaryDirectory.resolve("first.yaml");
    Path secondConfig = temporaryDirectory.resolve("second.yaml");
    Path userHome = temporaryDirectory.resolve("ports-user-home");
    Files.createDirectories(userHome);
    int firstAws = availablePort();
    int firstConsole = availablePortExcept(firstAws);
    int secondAws = availablePortExcept(firstAws, firstConsole);
    int secondConsole = availablePortExcept(firstAws, firstConsole, secondAws);
    writePortConfig(firstConfig, firstId, firstAws, firstConsole);
    writePortConfig(secondConfig, secondId, secondAws, secondConsole);

    try {
      CommandResult firstStart = cli(userHome, firstConfig, "start");
      assertEquals(0, firstStart.exitCode(), firstStart.output());
      CommandResult secondStart = cli(userHome, secondConfig, "start");
      assertEquals(0, secondStart.exitCode(), secondStart.output());

      Map<?, ?> firstStatus = awaitReady(userHome, firstConfig);
      Map<?, ?> secondStatus = awaitReady(userHome, secondConfig);
      assertStatus(firstStatus, firstId);
      assertStatus(secondStatus, secondId);
      assertEquals("http://127.0.0.1:" + firstAws, map(firstStatus.get("endpoints")).get("aws"));
      assertEquals("http://127.0.0.1:" + secondAws, map(secondStatus.get("endpoints")).get("aws"));
      assertConsoleReady(firstConsole);
      assertConsoleReady(secondConsole);
      assertConsoleContains(firstConsole, "/", "http://127.0.0.1:" + firstAws);
      assertConsoleContains(firstConsole, "/connection", "http://127.0.0.1:" + firstAws);
      assertConsoleContains(firstConsole, "/sqs/connection", "http://127.0.0.1:" + firstAws);
      assertConsoleContains(secondConsole, "/", "http://127.0.0.1:" + secondAws);
      assertConsoleContains(secondConsole, "/connection", "http://127.0.0.1:" + secondAws);
      assertConsoleContains(secondConsole, "/sqs/connection", "http://127.0.0.1:" + secondAws);

      CommandResult firstDiagnose = cli(userHome, firstConfig, "diagnose", "--json");
      CommandResult secondDiagnose = cli(userHome, secondConfig, "diagnose", "--json");
      assertEquals(0, firstDiagnose.exitCode(), firstDiagnose.output());
      assertEquals(0, secondDiagnose.exitCode(), secondDiagnose.output());
      assertEquals("http://127.0.0.1:" + firstConsole, map(parse(firstDiagnose.output()).get("endpoints")).get("console"));
      assertEquals("http://127.0.0.1:" + secondConsole, map(parse(secondDiagnose.output()).get("endpoints")).get("console"));

      URI firstEndpoint = URI.create("http://127.0.0.1:" + firstAws);
      URI secondEndpoint = URI.create("http://127.0.0.1:" + secondAws);
      SandboxAwsContract.verifyProjectIsolation(firstEndpoint, secondEndpoint, suffix);
      SandboxAwsContract.verifySdk(suffix + "a", firstEndpoint);
      SandboxAwsContract.verifySdk(suffix + "b", secondEndpoint);

      docker("exec", firstContainer, "sh", "-c", "printf sentinel > /data/stackemu/.stackemu/port-rebind-sentinel");
      int blockedAws = availablePortExcept(firstAws, firstConsole, secondAws, secondConsole);
      int nextConsole = availablePortExcept(firstAws, firstConsole, secondAws, secondConsole, blockedAws);
      try (var occupied = new ServerSocket()) {
        occupied.bind(new InetSocketAddress("127.0.0.1", blockedAws));
        writePortConfig(firstConfig, firstId, blockedAws, nextConsole);
        CommandResult conflict = cli(userHome, firstConfig, "restart");
        assertEquals(3, conflict.exitCode(), conflict.output());
        assertTrue(conflict.output().contains("host-port-in-use"), conflict.output());
        docker("exec", firstContainer, "test", "-f", "/data/stackemu/.stackemu/port-rebind-sentinel");
        assertConsoleReady(firstConsole);
      }

      int reboundAws = availablePortExcept(firstAws, firstConsole, secondAws, secondConsole, blockedAws, nextConsole);
      int reboundConsole = availablePortExcept(firstAws, firstConsole, secondAws, secondConsole, blockedAws, nextConsole, reboundAws);
      writePortConfig(firstConfig, firstId, reboundAws, reboundConsole);
      CommandResult rebound = cli(userHome, firstConfig, "restart");
      assertEquals(0, rebound.exitCode(), rebound.output());
      Map<?, ?> reboundStatus = awaitReady(userHome, firstConfig);
      assertStatus(reboundStatus, firstId);
      assertEquals("http://127.0.0.1:" + reboundAws, map(reboundStatus.get("endpoints")).get("aws"));
      docker("exec", firstContainer, "test", "-f", "/data/stackemu/.stackemu/port-rebind-sentinel");
      assertConsoleReady(reboundConsole);
      assertStatus(awaitReady(userHome, secondConfig), secondId);
    } finally {
      dockerIgnoreFailure("rm", "--force", firstContainer);
      dockerIgnoreFailure("rm", "--force", secondContainer);
      dockerIgnoreFailure("volume", "rm", "--force", firstContainer);
      dockerIgnoreFailure("volume", "rm", "--force", secondContainer);
    }
  }

  @Test
  void unhealthyReplacementRestoresThePreviousContainerAndVolume() throws Exception {
    String suffix = UUID.randomUUID().toString().replace("-", "");
    String projectId = "rollback-" + suffix;
    String container = "stackemu-" + projectId;
    Path config = temporaryDirectory.resolve("rollback.yaml");
    Path userHome = temporaryDirectory.resolve("rollback-user-home");
    Path badImageContext = temporaryDirectory.resolve("unhealthy-image");
    Files.createDirectories(userHome);
    Files.createDirectories(badImageContext);
    int originalAws = availablePort();
    int originalConsole = availablePortExcept(originalAws);
    int replacementAws = availablePortExcept(originalAws, originalConsole);
    int replacementConsole = availablePortExcept(originalAws, originalConsole, replacementAws);
    String badImageTag = "stackemu:rollback-test-" + suffix;
    String prebuiltUnhealthyImage = System.getenv("STACKEMU_UNHEALTHY_TEST_IMAGE");
    String originalImageId = dockerText("image", "inspect", "--format", "{{.Id}}", "stackemu:local").trim();
    Files.writeString(
        badImageContext.resolve("Dockerfile"),
        "FROM stackemu:local\nHEALTHCHECK --interval=1s --timeout=1s --retries=1 CMD exit 1\n",
        StandardCharsets.UTF_8);
    writePortConfig(config, projectId, originalAws, originalConsole);

    try {
      if (prebuiltUnhealthyImage == null || prebuiltUnhealthyImage.isBlank()) {
        docker("build", "--network=none", "--tag", badImageTag, badImageContext.toString());
      } else {
        docker("tag", prebuiltUnhealthyImage, badImageTag);
      }
      CommandResult start = cli(userHome, config, "start");
      assertEquals(0, start.exitCode(), start.output());
      assertStatus(awaitReady(userHome, config), projectId);
      docker("exec", container, "sh", "-c", "printf rollback-sentinel > /data/stackemu/.stackemu/rollback-sentinel");

      docker("tag", badImageTag, "stackemu:local");
      writePortConfig(config, projectId, replacementAws, replacementConsole);
      CommandResult failedRebind = cli(userHome, config, "restart");
      assertEquals(3, failedRebind.exitCode(), failedRebind.output());
      assertTrue(failedRebind.output().contains("health check failed"), failedRebind.output());
      assertEquals(originalImageId, dockerText("inspect", "--format", "{{.Image}}", container).trim());
      assertEquals("healthy", dockerText("inspect", "--format", "{{.State.Health.Status}}", container).trim());
      docker("exec", container, "test", "-f", "/data/stackemu/.stackemu/rollback-sentinel");
      String bindings = dockerText("inspect", "--format", "{{json .HostConfig.PortBindings}}", container);
      assertTrue(bindings.contains("\"HostPort\":\"" + originalAws + "\""), bindings);
      assertTrue(bindings.contains("\"HostPort\":\"" + originalConsole + "\""), bindings);

      writePortConfig(config, projectId, originalAws, originalConsole);
      assertStatus(awaitReady(userHome, config), projectId);
    } finally {
      if (!originalImageId.isBlank()) dockerIgnoreFailure("tag", originalImageId, "stackemu:local");
      dockerIgnoreFailure("rm", "--force", container);
      dockerIgnoreFailure("volume", "rm", "--force", container);
      dockerIgnoreFailure("image", "rm", "--force", badImageTag);
    }
  }

  private Map<?, ?> awaitReady(Path userHome, Path config) throws Exception {
    long deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
    Map<?, ?> lastReport = Map.of();
    while (System.nanoTime() < deadline) {
      CommandResult result = cli(userHome, config, "status", "--json");
      lastReport = parse(result.output());
      if (result.exitCode() == 0) return lastReport;
      if (result.exitCode() != 6) fail("status failed with exit code " + result.exitCode() + ": " + result.output());
      Thread.sleep(500);
    }
    fail("sandbox did not become ready: " + lastReport);
    return lastReport;
  }

  private static void assertStatus(Map<?, ?> report, String projectId) {
    assertEquals(projectId, report.get("projectId"));
    assertEquals("ready", report.get("status"));
    assertEquals(0, report.get("exitCode"));
    assertEquals(true, map(report.get("marker")).get("matchesConfiguration"));
    assertTrue(report.get("imageDigest") instanceof String digest && digest.startsWith("sha256:"));
    assertTrue(report.get("volumeUsageBytes") instanceof Number);
    Map<?, ?> health = map(report.get("health"));
    assertEquals("healthy", health.get("emulator"));
    assertEquals("healthy", health.get("console"));
  }

  private void assertConsoleContains(String path, String expected) throws Exception {
    assertConsoleContains(8080, path, expected);
  }

  private void assertConsoleReady(int port) throws Exception {
    assertConsoleContains(port, "/q/health/ready", "UP");
  }

  private void assertConsoleContains(int port, String path, String expected) throws Exception {
    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();
    HttpResponse<String> response =
        client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains(expected), response.body());
  }

  private void writePortConfig(Path config, String projectId, int awsPort, int consolePort) throws Exception {
    Files.writeString(
        config,
        """
            version: 1
            project:
              id: %s
            services: [s3, sqs, sns]
            resources:
              buckets: [bucket-%s]
              queues: [queue-%s]
              topics: [topic-%s]
              subscriptions: []
            persistence: enabled
            ports:
              aws: %d
              console: %d
            """.formatted(projectId, projectId, projectId, projectId, awsPort, consolePort),
        StandardCharsets.UTF_8);
  }

  private static int availablePort() throws IOException {
    try (var socket = new ServerSocket()) {
      socket.bind(new InetSocketAddress("127.0.0.1", 0));
      return socket.getLocalPort();
    }
  }

  private static int availablePortExcept(int... excluded) throws IOException {
    while (true) {
      int port = availablePort();
      if (java.util.Arrays.stream(excluded).noneMatch(value -> value == port)) return port;
    }
  }

  private CommandResult cli(Path userHome, Path config, String... args) throws Exception {
    Path javaExecutable =
        Path.of(
            System.getProperty("java.home"),
            "bin",
            System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java");
    String classpath =
        String.join(
            System.getProperty("path.separator"),
            Path.of(StackEmuCli.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString(),
            Path.of(org.snakeyaml.engine.v2.api.Load.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                .toString());
    var command = new java.util.ArrayList<String>();
    command.add(javaExecutable.toString());
    command.add("-Duser.home=" + userHome.toAbsolutePath());
    command.add("-cp");
    command.add(classpath);
    command.add(StackEmuCli.class.getName());
    command.addAll(Arrays.asList(args));
    command.add("--config");
    command.add(config.toAbsolutePath().toString());
    Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
    if (!process.waitFor(100, TimeUnit.SECONDS)) {
      process.destroyForcibly();
      throw new AssertionError("StackEmu CLI command timed out: " + args[0]);
    }
    return new CommandResult(
        process.exitValue(), new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
  }

  private static Map<?, ?> parse(String json) {
    Object value =
        new Load(LoadSettings.builder().setLabel("CLI JSON report").build()).loadFromString(json.trim());
    if (!(value instanceof Map<?, ?> report)) throw new AssertionError("CLI did not return a JSON mapping: " + json);
    return report;
  }

  private static Map<?, ?> map(Object value) {
    return (Map<?, ?>) value;
  }

  private static void docker(String... args) throws IOException, InterruptedException {
    int exitCode = dockerExit(args);
    if (exitCode != 0) throw new AssertionError("Docker command failed: " + String.join(" ", args));
  }

  private static void dockerIgnoreFailure(String... args) throws IOException, InterruptedException {
    dockerExit(args);
  }

  private static int dockerExit(String... args) throws IOException, InterruptedException {
    var command = new java.util.ArrayList<String>();
    command.add("docker");
    command.addAll(Arrays.asList(args));
    Process process = new ProcessBuilder(command).inheritIO().start();
    if (!process.waitFor(30, TimeUnit.SECONDS)) {
      process.destroyForcibly();
      throw new AssertionError("Docker command timed out: " + String.join(" ", args));
    }
    return process.exitValue();
  }

  private static java.util.List<String> dockerLines(String... args) throws IOException, InterruptedException {
    var command = new java.util.ArrayList<String>();
    command.add("docker");
    command.addAll(Arrays.asList(args));
    Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
    if (!process.waitFor(30, TimeUnit.SECONDS)) {
      process.destroyForcibly();
      throw new AssertionError("Docker query timed out: " + String.join(" ", args));
    }
    if (process.exitValue() != 0) throw new AssertionError("Docker query failed: " + String.join(" ", args));
    return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
        .lines()
        .sorted()
        .toList();
  }

  private static String dockerText(String... args) throws IOException, InterruptedException {
    var command = new java.util.ArrayList<String>();
    command.add("docker");
    command.addAll(Arrays.asList(args));
    Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
    if (!process.waitFor(30, TimeUnit.SECONDS)) {
      process.destroyForcibly();
      throw new AssertionError("Docker query timed out: " + String.join(" ", args));
    }
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    if (process.exitValue() != 0) throw new AssertionError("Docker query failed: " + output);
    return output;
  }

  private record CommandResult(int exitCode, String output) {}
}
