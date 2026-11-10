package io.github.erf88.stackemu.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SandboxDiagnosticsTest {
  private static final String PROJECT = "demo";
  private static final String HASH = "current-hash";

  @TempDir Path temporaryDirectory;

  @Test
  void cliPrintsOnlyTheJsonReportAndReturnsItsExitCode() throws Exception {
    Path config = temporaryDirectory.resolve("stackemu.yaml");
    Files.writeString(config, "version: 1\nproject:\n  id: demo\n");
    var docker = new FakeDocker();
    docker.containerMissing = true;
    var stdout = new ByteArrayOutputStream();

    int exitCode =
        StackEmuCli.run(
            new String[] {"status", "--json", "--config", config.toString()},
            docker,
            new PrintStream(stdout, true, StandardCharsets.UTF_8));

    String output = stdout.toString(StandardCharsets.UTF_8);
    assertEquals(6, exitCode);
    assertTrue(output.startsWith("{\"projectId\":\"demo\""));
    assertEquals(1, output.lines().count());
    assertFalse(output.contains("private log contents"));
  }

  @Test
  void readyReportContainsHealthMarkerImageAndUsageWithoutLogContents() {
    var docker = FakeDocker.ready();
    var report = diagnose(docker, false);

    assertEquals(0, report.exitCode(), report.json());
    assertEquals("ready", report.fields().get("status"));
    assertEquals("sha256:image-id", report.fields().get("imageDigest"));
    assertEquals(12 * 1024L, report.fields().get("volumeUsageBytes"));
    assertEquals("available", map(report.fields().get("marker")).get("status"));
    assertEquals("ready", map(report.fields().get("health")).get("readiness"));
    assertTrue(report.json().startsWith("{"));
    assertTrue(report.json().contains("docker logs --tail 100 stackemu-demo"));
    assertFalse(report.json().contains("private log contents"));
    assertTrue(docker.calls().stream().allMatch(SandboxDiagnosticsTest::isReadOnlyCommand));
  }

  @Test
  void missingContainerReportsJsonAndDoesNotReadOrCreateResources() {
    var docker = new FakeDocker();
    docker.containerMissing = true;
    var report = diagnose(docker, false);

    assertEquals(6, report.exitCode());
    assertEquals("missing", report.fields().get("status"));
    assertEquals("container-not-found", cause(report).get("code"));
    assertEquals("unavailable", map(report.fields().get("marker")).get("status"));
    assertNull(report.fields().get("volumeUsageBytes"));
    assertEquals(
        List.of("version", "inspect", "image", "volume"),
        docker.calls().stream().map(args -> args.get(0)).toList());
  }

  @Test
  void stoppedContainerDoesNotUseAnAuxiliaryContainerToReadMarker() {
    var docker = new FakeDocker();
    docker.containerState = "exited";
    var report = diagnose(docker, false);

    assertEquals(6, report.exitCode());
    assertEquals("stopped", report.fields().get("status"));
    assertEquals("container-not-running", map(report.fields().get("marker")).get("reason"));
    assertNull(report.fields().get("volumeUsageBytes"));
    assertTrue(docker.calls().stream().noneMatch(args -> args.contains("exec")));
  }

  @Test
  void restartingContainerReportsStarting() {
    var docker = new FakeDocker();
    docker.containerState = "restarting";
    var report = diagnose(docker, false);
    assertEquals(6, report.exitCode());
    assertEquals("starting", report.fields().get("status"));
  }

  @Test
  void healthStartingTakesPrecedenceOverMarkerNotYetWritten() {
    var docker = FakeDocker.ready();
    docker.dockerHealthState = "starting";
    docker.markerUnavailable = true;
    var report = diagnose(docker, false);
    assertEquals("starting", report.fields().get("status"));
    assertEquals("emulator-starting", cause(report).get("code"));
  }

  @Test
  void unavailableDockerReturnsUnknownAndPrerequisiteExitCode() {
    var docker = new FakeDocker();
    docker.daemonUnavailable = true;
    var report = diagnose(docker, false);
    assertEquals(3, report.exitCode());
    assertEquals("unknown", report.fields().get("status"));
    assertEquals("docker-unavailable", cause(report).get("code"));
    assertEquals(1, docker.calls().size());
  }

  @Test
  void missingLocalImageUsesDockerImagePrerequisiteExitCode() {
    var docker = new FakeDocker();
    docker.containerMissing = true;
    docker.imageUnavailable = true;
    var report = diagnose(docker, false);
    assertEquals(3, report.exitCode());
    assertEquals("image-missing", cause(report).get("code"));
  }

  @Test
  void missingOrMismatchedVolumeReportsOwnershipError() {
    var missing = new FakeDocker();
    missing.volumeMissing = true;
    assertEquals(4, diagnose(missing, false).exitCode());

    var mismatch = new FakeDocker();
    mismatch.volumeOwner = "someone-else";
    assertEquals("volume-owner-mismatch", cause(diagnose(mismatch, false)).get("code"));
  }

  @Test
  void unhealthyEmulatorAndConsoleHaveDistinctCauses() {
    var emulator = new FakeDocker();
    emulator.emulatorReady = false;
    assertEquals("emulator-unhealthy", cause(diagnose(emulator, false)).get("code"));

    var console = new FakeDocker();
    console.consoleReady = false;
    assertEquals("console-unready", cause(diagnose(console, false)).get("code"));
  }

  @Test
  void markerOwnerSchemaAndConfigurationDriftAreRejected() {
    var owner = new FakeDocker();
    owner.markerProject = "other";
    assertEquals("marker-owner-mismatch", cause(diagnose(owner, false)).get("code"));

    var schema = new FakeDocker();
    schema.markerSchema = 2;
    assertEquals("marker-schema-incompatible", cause(diagnose(schema, false)).get("code"));

    var image = new FakeDocker();
    image.markerCompatibility = ">=2.0.0 <3.0.0";
    assertEquals("marker-image-incompatible", cause(diagnose(image, false)).get("code"));

    var drift = new FakeDocker();
    drift.markerHash = "old-hash";
    assertEquals("configuration-drift", cause(diagnose(drift, false)).get("code"));

    var invalid = new FakeDocker();
    invalid.markerMalformed = true;
    assertEquals("marker-invalid", cause(diagnose(invalid, false)).get("code"));
  }

  @Test
  void diagnoseAddsChecksWhileStatusKeepsTheSummary() {
    var status = diagnose(FakeDocker.ready(), false);
    var unready = FakeDocker.ready();
    unready.consoleReady = false;
    var diagnose = diagnose(unready, true);
    assertFalse(status.fields().containsKey("checks"));
    assertTrue(diagnose.fields().containsKey("checks"));
    assertTrue(diagnose.json().contains("\"action\""));
  }

  private static SandboxDiagnostics.Report diagnose(FakeDocker docker, boolean detailed) {
    return new SandboxDiagnostics(docker).inspect(PROJECT, HASH, detailed);
  }

  private static boolean isReadOnlyCommand(List<String> args) {
    return List.of("version", "inspect", "image", "volume", "exec").contains(args.get(0));
  }

  private static java.util.Map<?, ?> cause(SandboxDiagnostics.Report report) {
    return map(report.fields().get("cause"));
  }

  private static java.util.Map<?, ?> map(Object value) {
    return (java.util.Map<?, ?>) value;
  }

  private static final class FakeDocker implements SandboxDiagnostics.CommandRunner {
    private final List<List<String>> invocations = new ArrayList<>();
    private boolean containerMissing;
    private boolean daemonUnavailable;
    private boolean imageUnavailable;
    private boolean volumeMissing;
    private String containerState = "running";
    private String dockerHealthState;
    private String volumeOwner = PROJECT;
    private String markerProject = PROJECT;
    private int markerSchema = 1;
    private String markerCompatibility = ">=0.1.0 <1.0.0";
    private String markerHash = HASH;
    private boolean markerUnavailable;
    private boolean markerMalformed;
    private boolean emulatorReady = true;
    private boolean consoleReady = true;

    static FakeDocker ready() {
      return new FakeDocker();
    }

    List<List<String>> calls() {
      return invocations;
    }

    @Override
    public SandboxDiagnostics.Result execute(String... raw) {
      List<String> args = List.of(raw);
      invocations.add(args);
      return switch (args.get(0)) {
        case "version" ->
            daemonUnavailable
                ? new SandboxDiagnostics.Result(1, "Cannot connect to the Docker daemon")
                : new SandboxDiagnostics.Result(0, "29.8.2");
        case "inspect" -> inspect();
        case "image" ->
            imageUnavailable
                ? new SandboxDiagnostics.Result(1, "Error: no such image")
                : new SandboxDiagnostics.Result(0, "sha256:image-id");
        case "volume" ->
            volumeMissing
                ? new SandboxDiagnostics.Result(1, "Error: no such volume")
                : new SandboxDiagnostics.Result(0, "{\"io.stackemu.project\":\"" + volumeOwner + "\"}");
        case "exec" -> exec(args);
        default -> new SandboxDiagnostics.Result(1, "unexpected command");
      };
    }

    private SandboxDiagnostics.Result inspect() {
      if (containerMissing) return new SandboxDiagnostics.Result(1, "Error: no such container");
      String health =
          dockerHealthState == null ? (containerState.equals("running") ? "healthy" : "starting") : dockerHealthState;
      String json =
          "{\"Image\":\"sha256:image-id\",\"LogPath\":\"C:\\\\docker\\\\container.log\",\"State\":{\"Status\":\""
              + containerState
              + "\",\"ExitCode\":0,\"OOMKilled\":false,\"Error\":\"\",\"Health\":{\"Status\":\""
              + health
              + "\"}}}";
      return new SandboxDiagnostics.Result(0, json);
    }

    private SandboxDiagnostics.Result exec(List<String> args) {
      if (args.contains("cat")) {
        if (markerUnavailable) return new SandboxDiagnostics.Result(1, "marker unavailable");
        if (markerMalformed) return new SandboxDiagnostics.Result(0, "not a JSON object");
        return new SandboxDiagnostics.Result(
            0,
            "{\"projectId\":\""
                + markerProject
                + "\",\"schemaVersion\":"
                + markerSchema
                + ",\"imageCompatibility\":\""
                + markerCompatibility
                + "\",\"configHash\":\""
                + markerHash
                + "\"}");
      }
      if (args.contains("du")) return new SandboxDiagnostics.Result(0, "12\t/data/stackemu");
      if (args.stream().anyMatch(value -> value.contains("4566")))
        return new SandboxDiagnostics.Result(emulatorReady ? 0 : 1, "private log contents");
      if (args.stream().anyMatch(value -> value.contains("8080")))
        return new SandboxDiagnostics.Result(consoleReady ? 0 : 1, "private log contents");
      return new SandboxDiagnostics.Result(1, "unexpected exec command");
    }
  }
}
