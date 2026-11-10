package io.github.erf88.stackemu.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

/** Read-only inspection of the Docker sandbox and its persisted marker. */
final class SandboxDiagnostics {
  private static final String MARKER_PATH = "/data/stackemu/.stackemu/marker.json";
  private final CommandRunner docker;

  SandboxDiagnostics(CommandRunner docker) {
    this.docker = docker;
  }

  Report inspect(String projectId, String configurationHash, boolean detailed) {
    String container = "stackemu-" + projectId;
    String volume = container;
    var fields = new LinkedHashMap<String, Object>();
    fields.put("projectId", projectId);
    fields.put("container", container);
    fields.put("volume", volume);
    fields.put("status", "unknown");
    fields.put("cause", null);
    fields.put("imageDigest", null);
    fields.put("dockerVersion", null);
    fields.put("configurationHash", configurationHash);
    fields.put("marker", unavailable("not-inspected"));
    fields.put("health", health("unknown", "unavailable", "unavailable", "unavailable"));
    fields.put("volumeUsageBytes", null);
    fields.put("logs", logs(null, container));
    var checks = new ArrayList<Map<String, Object>>();

    Result version = execute("version", "--format", "{{.Server.Version}}");
    if (!version.succeeded()) {
      fail(fields, "unknown", "docker-unavailable", "Docker daemon is unavailable", "Start Docker Desktop and retry", 3);
      checks.add(check("docker", "unavailable", fields.get("cause")));
      return report(fields, checks, detailed);
    }
    fields.put("dockerVersion", version.output().trim());

    Result inspect = execute("inspect", "--format", "{{json .}}", container);
    if (!inspect.succeeded()) {
      if (isNotFound(inspect.output())) {
        return missingContainer(fields, checks, detailed, projectId, volume);
      } else {
        fail(fields, "unknown", "docker-inspection-failed", "Docker could not inspect the sandbox", "Check Docker permissions and retry", 3);
        checks.add(check("docker", "error", fields.get("cause")));
      }
      checks.add(check("container", "missing", fields.get("cause")));
      return report(fields, checks, detailed);
    }

    Map<?, ?> containerInfo;
    try {
      containerInfo = mapping(inspect.output(), "container inspection");
    } catch (RuntimeException failure) {
      fail(fields, "unknown", "docker-response-invalid", "Docker returned an unreadable inspection result", "Check the Docker CLI version", 3);
      checks.add(check("container", "invalid", fields.get("cause")));
      return report(fields, checks, detailed);
    }

    String imageDigest = string(containerInfo.get("Image"));
    fields.put("imageDigest", imageDigest.isBlank() ? null : imageDigest);
    String logPath = string(containerInfo.get("LogPath"));
    fields.put("logs", logs(logPath.isBlank() ? null : logPath, container));
    Map<?, ?> state = map(containerInfo.get("State"));
    String containerState = string(state.get("Status"));
    Map<?, ?> dockerHealth = map(state.get("Health"));
    String dockerHealthState = string(dockerHealth.get("Status"));
    String emulatorHealth = "unavailable";
    String consoleHealth = "unavailable";
    Map<String, Object> marker = unavailable("container-not-running");
    Long volumeUsage = null;

    Result volumeResult = execute("volume", "inspect", "--format", "{{json .Labels}}", volume);
    if (!volumeResult.succeeded()) {
      fail(fields, "unhealthy", "volume-missing", "The project volume is missing", "Inspect project state before starting or resetting it", 4);
      checks.add(check("volume", "missing", fields.get("cause")));
      return finish(fields, checks, detailed, health(containerState, dockerHealthState, emulatorHealth, consoleHealth), marker, volumeUsage);
    }
    Map<?, ?> labels;
    try {
      labels = mapping(volumeResult.output(), "volume labels");
    } catch (RuntimeException failure) {
      fail(fields, "unhealthy", "volume-labels-invalid", "The project volume labels could not be read", "Inspect the volume before reusing it", 4);
      checks.add(check("volume", "invalid", fields.get("cause")));
      return finish(fields, checks, detailed, health(containerState, dockerHealthState, emulatorHealth, consoleHealth), marker, volumeUsage);
    }
    if (!projectId.equals(string(labels.get("io.stackemu.project")))) {
      fail(fields, "unhealthy", "volume-owner-mismatch", "The volume is not labelled for this project", "Do not reuse it; inspect the volume ownership", 4);
      checks.add(check("volume", "mismatch", fields.get("cause")));
      return finish(fields, checks, detailed, health(containerState, dockerHealthState, emulatorHealth, consoleHealth), marker, volumeUsage);
    }

    if ("running".equals(containerState)) {
      Result markerResult = execute("exec", container, "cat", MARKER_PATH);
      if (markerResult.succeeded()) {
        try {
          Map<?, ?> markerValues = mapping(markerResult.output(), "marker");
          marker = marker(markerValues, projectId, configurationHash);
        } catch (RuntimeException failure) {
          marker = invalidMarker("marker-invalid");
        }
      } else {
        marker = unavailable("marker-unavailable");
      }

      emulatorHealth = probe(container, "http://127.0.0.1:4566/_ministack/health");
      consoleHealth = probe(container, "http://127.0.0.1:8080/q/health/ready");
      Result usage = execute("exec", container, "du", "-sk", "/data/stackemu");
      if (usage.succeeded()) {
        try {
          volumeUsage = Long.parseLong(usage.output().trim().split("\\s+")[0]) * 1024;
        } catch (RuntimeException ignored) {
          volumeUsage = null;
        }
      }
    } else if ("created".equals(containerState) || "restarting".equals(containerState)) {
      fail(fields, "starting", "container-starting", "The sandbox container is still starting", "Wait briefly and run status again", 6);
      marker = unavailable("container-not-running");
    } else {
      fail(fields, "stopped", "container-stopped", "The sandbox container is not running", "Run stackemu start", 6);
      marker = unavailable("container-not-running");
    }

    fields.put("health", health(containerState, dockerHealthState, emulatorHealth, consoleHealth));
    fields.put("marker", marker);
    fields.put("volumeUsageBytes", volumeUsage);

    if (fields.get("cause") == null) {
      if ("starting".equals(dockerHealthState)) {
        fail(fields, "starting", "emulator-starting", "MiniStack is still starting", "Wait briefly and run status again", 6);
      } else if ("unhealthy".equals(dockerHealthState) || !"healthy".equals(emulatorHealth)) {
        fail(fields, "unhealthy", "emulator-unhealthy", "MiniStack health check failed", "Inspect container logs and verify the emulator process", 6);
      } else if (!"healthy".equals(consoleHealth)) {
        fail(fields, "unhealthy", "console-unready", "The StackEmu console is not ready", "Inspect container logs and verify bootstrap completion", 6);
      } else if ("invalid".equals(marker.get("status"))) {
        fail(fields, "unhealthy", "marker-invalid", "The sandbox marker is unreadable", "Run reset to recreate project state", 4);
      } else if (!"available".equals(marker.get("status"))) {
        fail(fields, "unhealthy", "marker-unavailable", "The sandbox marker is not available", "Run reset to recreate and bootstrap project state", 6);
      } else if (!Boolean.TRUE.equals(marker.get("matchesProject"))) {
        fail(fields, "unhealthy", "marker-owner-mismatch", "The marker belongs to another project", "Do not reuse this volume; inspect project ownership", 4);
      } else if (!Integer.valueOf(1).equals(marker.get("schemaVersion"))) {
        fail(fields, "unhealthy", "marker-schema-incompatible", "The marker schema is incompatible", "Use a compatible StackEmu image or reset this project", 4);
      } else if (!">=0.1.0 <1.0.0".equals(marker.get("imageCompatibility"))) {
        fail(fields, "unhealthy", "marker-image-incompatible", "The marker is incompatible with this image", "Use a compatible StackEmu image or reset this project", 4);
      } else if (!Boolean.TRUE.equals(marker.get("matchesConfiguration"))) {
        fail(fields, "unhealthy", "configuration-drift", "The marker does not match the current configuration", "Run reset to apply the current configuration", 4);
      } else {
        fields.put("status", "ready");
        fields.put("exitCode", 0);
      }
    }

    Object cause = fields.get("cause");
    checks.add(check("container", containerState, "running".equals(containerState) ? null : cause));
    checks.add(
        check(
            "dockerHealth",
            dockerHealthState,
            dockerHealthState.isBlank() || "healthy".equals(dockerHealthState) ? null : cause));
    checks.add(check("emulator", emulatorHealth, "healthy".equals(emulatorHealth) ? null : cause));
    checks.add(check("console", consoleHealth, "healthy".equals(consoleHealth) ? null : cause));
    checks.add(
        check(
            "marker",
            string(marker.get("status")),
            "available".equals(marker.get("status"))
                    && Boolean.TRUE.equals(marker.get("matchesProject"))
                    && Boolean.TRUE.equals(marker.get("matchesConfiguration"))
                ? null
                : cause));
    checks.add(check("volume", "available", null));
    return report(fields, checks, detailed);
  }

  private Report missingContainer(
      Map<String, Object> fields,
      List<Map<String, Object>> checks,
      boolean detailed,
      String projectId,
      String volume) {
    Result image = execute("image", "inspect", "--format", "{{.Id}}", "stackemu:local");
    String imageDigest = image.succeeded() ? image.output().trim() : "";
    fields.put("imageDigest", imageDigest.isBlank() ? null : imageDigest);
    Result volumeResult = execute("volume", "inspect", "--format", "{{json .Labels}}", volume);
    if (volumeResult.succeeded()) {
      Map<?, ?> labels;
      try {
        labels = mapping(volumeResult.output(), "volume labels");
      } catch (RuntimeException failure) {
        fail(fields, "unhealthy", "volume-labels-invalid", "The project volume labels could not be read", "Inspect the volume before reusing it", 4);
        checks.add(check("volume", "invalid", fields.get("cause")));
        checks.add(check("container", "missing", fields.get("cause")));
        fields.put("marker", unavailable("container-not-running"));
        return report(fields, checks, detailed);
      }
      if (!projectId.equals(string(labels.get("io.stackemu.project")))) {
        fail(fields, "unhealthy", "volume-owner-mismatch", "The volume is not labelled for this project", "Do not reuse it; inspect the volume ownership", 4);
        checks.add(check("volume", "mismatch", fields.get("cause")));
        checks.add(check("container", "missing", fields.get("cause")));
        fields.put("marker", unavailable("container-not-running"));
        return report(fields, checks, detailed);
      }
      checks.add(check("volume", "available", null));
    } else {
      checks.add(check("volume", "missing", null));
    }
    fields.put("marker", unavailable("container-not-running"));
    if (imageDigest.isBlank()) {
      fail(fields, "missing", "image-missing", "The local StackEmu image is unavailable", "Build or load stackemu:local", 3);
      checks.add(check("image", "missing", fields.get("cause")));
    } else {
      fail(fields, "missing", "container-not-found", "Sandbox container does not exist", "Run stackemu start", 6);
      checks.add(check("image", "available", null));
    }
    checks.add(check("container", "missing", fields.get("cause")));
    return report(fields, checks, detailed);
  }

  private Result execute(String... args) {
    try {
      return docker.execute(args);
    } catch (Exception failure) {
      return new Result(127, "");
    }
  }

  private String probe(String container, String endpoint) {
    Result result = execute("exec", container, "wget", "-q", "-O", "/dev/null", endpoint);
    return result.succeeded() ? "healthy" : "unhealthy";
  }

  private static Report report(Map<String, Object> fields, List<Map<String, Object>> checks, boolean detailed) {
    if (detailed) fields.put("checks", checks);
    return new Report(fields, (Integer) fields.get("exitCode"));
  }

  private static Report finish(
      Map<String, Object> fields,
      List<Map<String, Object>> checks,
      boolean detailed,
      Map<String, Object> health,
      Map<String, Object> marker,
      Long usage) {
    fields.put("health", health);
    fields.put("marker", marker);
    fields.put("volumeUsageBytes", usage);
    return report(fields, checks, detailed);
  }

  private static Map<String, Object> marker(Map<?, ?> values, String projectId, String configurationHash) {
    var result = new LinkedHashMap<String, Object>();
    result.put("status", "available");
    result.put("projectId", values.get("projectId"));
    result.put("schemaVersion", values.get("schemaVersion"));
    result.put("imageCompatibility", values.get("imageCompatibility"));
    result.put("configHash", values.get("configHash"));
    result.put("matchesConfiguration", configurationHash.equals(values.get("configHash")));
    result.put("matchesProject", projectId.equals(values.get("projectId")));
    return result;
  }

  private static Map<String, Object> unavailable(String reason) {
    var marker = new LinkedHashMap<String, Object>();
    marker.put("status", "unavailable");
    marker.put("reason", reason);
    return marker;
  }

  private static Map<String, Object> invalidMarker(String reason) {
    var marker = new LinkedHashMap<String, Object>();
    marker.put("status", "invalid");
    marker.put("reason", reason);
    return marker;
  }

  private static Map<String, Object> health(String container, String docker, String emulator, String console) {
    var health = new LinkedHashMap<String, Object>();
    health.put("container", container);
    health.put("docker", docker);
    health.put("emulator", emulator);
    health.put("console", console);
    boolean dockerReady = docker.isBlank() || "none".equals(docker) || "healthy".equals(docker);
    health.put(
        "readiness",
        "running".equals(container)
                && dockerReady
                && "healthy".equals(emulator)
                && "healthy".equals(console)
            ? "ready"
            : "not-ready");
    return health;
  }

  private static Map<String, Object> logs(String path, String container) {
    var logs = new LinkedHashMap<String, Object>();
    logs.put("path", path);
    logs.put("command", "docker logs --tail 100 " + container);
    return logs;
  }

  private static void fail(
      Map<String, Object> fields, String status, String code, String message, String action, int exitCode) {
    fields.put("status", status);
    var cause = new LinkedHashMap<String, Object>();
    cause.put("code", code);
    cause.put("message", message);
    cause.put("action", action);
    fields.put("cause", cause);
    fields.put("exitCode", exitCode);
  }

  private static Map<String, Object> check(String name, String status, Object cause) {
    var check = new LinkedHashMap<String, Object>();
    check.put("name", name);
    check.put("status", status);
    check.put("cause", cause);
    if (cause instanceof Map<?, ?> value) check.put("action", value.get("action"));
    return check;
  }

  private static boolean isNotFound(String message) {
    String lower = message.toLowerCase(java.util.Locale.ROOT);
    return lower.contains("no such object") || lower.contains("no such container") || lower.contains("not found");
  }

  private static Map<?, ?> mapping(String value, String label) {
    Object parsed =
        new Load(LoadSettings.builder().setLabel(label).build()).loadFromString(value.trim());
    if (!(parsed instanceof Map<?, ?> map)) throw new IllegalArgumentException(label + " is not a mapping");
    return map;
  }

  private static Map<?, ?> map(Object value) {
    return value instanceof Map<?, ?> map ? map : Map.of();
  }

  private static String string(Object value) {
    return value == null ? "" : String.valueOf(value);
  }

  record Result(int exitCode, String output) {
    boolean succeeded() {
      return exitCode == 0;
    }
  }

  @FunctionalInterface
  interface CommandRunner {
    Result execute(String... args) throws IOException, InterruptedException;
  }

  record Report(Map<String, Object> fields, int exitCode) {
    String json() {
      return encode(fields);
    }
  }

  private static String encode(Object value) {
    if (value == null) return "null";
    if (value instanceof String text) return quote(text);
    if (value instanceof Number || value instanceof Boolean) return value.toString();
    if (value instanceof Map<?, ?> map) {
      var entries = new ArrayList<String>();
      map.forEach((key, item) -> entries.add(quote(String.valueOf(key)) + ":" + encode(item)));
      return "{" + String.join(",", entries) + "}";
    }
    if (value instanceof Iterable<?> values) {
      var entries = new ArrayList<String>();
      values.forEach(item -> entries.add(encode(item)));
      return "[" + String.join(",", entries) + "]";
    }
    return quote(String.valueOf(value));
  }

  private static String quote(String value) {
    var encoded = new StringBuilder("\"");
    for (char character : value.toCharArray()) {
      switch (character) {
        case '"' -> encoded.append("\\\"");
        case '\\' -> encoded.append("\\\\");
        case '\n' -> encoded.append("\\n");
        case '\r' -> encoded.append("\\r");
        case '\t' -> encoded.append("\\t");
        default -> {
          if (character < 0x20) encoded.append(String.format("\\u%04x", (int) character));
          else encoded.append(character);
        }
      }
    }
    return encoded.append('"').toString();
  }
}
