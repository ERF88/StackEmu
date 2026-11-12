package io.github.erf88.stackemu.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URI;
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
            """.formatted(projectId, bucket, queue, topic),
        StandardCharsets.UTF_8);

    try {
      CommandResult start = cli(userHome, config, "start");
      assertEquals(0, start.exitCode(), start.output());
      Map<?, ?> firstStatus = awaitReady(userHome, config);
      assertStatus(firstStatus, projectId);
      SandboxAwsContract.verifySdk(suffix);
      SandboxAwsContract.verifyConsoleCopy(suffix);
      if ("true".equalsIgnoreCase(System.getenv("STACKEMU_AWS_CLI_INTEGRATION"))) {
        SandboxAwsContract.verifyAwsCli(suffix, temporaryDirectory);
      }
      assertConsoleContains("/s3", bucket);
      assertConsoleContains("/sqs", queue);

      var containersBeforeDiagnostics = dockerLines("ps", "--all", "--quiet");
      CommandResult status = cli(userHome, config, "status", "--json");
      CommandResult diagnose = cli(userHome, config, "diagnose", "--json");
      assertEquals(0, status.exitCode(), status.output());
      assertEquals(0, diagnose.exitCode(), diagnose.output());
      assertTrue(parse(status.output()).containsKey("health"));
      assertTrue(parse(diagnose.output()).containsKey("checks"));
      assertEquals(containersBeforeDiagnostics, dockerLines("ps", "--all", "--quiet"));

      docker("exec", container, "sh", "-c", "printf persisted > /data/stackemu/.stackemu/integration-sentinel");
      CommandResult restart = cli(userHome, config, "restart");
      assertEquals(0, restart.exitCode(), restart.output());
      Map<?, ?> afterRestart = awaitReady(userHome, config);
      assertStatus(afterRestart, projectId);
      docker("exec", container, "test", "-f", "/data/stackemu/.stackemu/integration-sentinel");
      assertConsoleContains("/s3", bucket);

      CommandResult reset = cli(userHome, config, "reset", "--yes");
      assertEquals(0, reset.exitCode(), reset.output());
      Map<?, ?> afterReset = awaitReady(userHome, config);
      assertStatus(afterReset, projectId);
      docker("exec", container, "sh", "-c", "test ! -e /data/stackemu/.stackemu/integration-sentinel");
      assertConsoleContains("/s3", bucket);

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
    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:8080" + path))
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();
    HttpResponse<String> response =
        client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains(expected), response.body());
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

  private record CommandResult(int exitCode, String output) {}
}
