package io.github.erf88.stackemu;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.erf88.stackemu.application.S3OperationPort;
import io.github.erf88.stackemu.application.S3OperationPort.Operation;
import io.github.erf88.stackemu.application.S3OperationPort.State;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;

final class OperationHttpFixtures {
  private OperationHttpFixtures() {}

  record Form(String id, String csrf, Map<String, String> cookies) {}

  static String field(String html, String name) {
    var match = Pattern.compile("name=\"" + name + "\" value=\"([^\"]+)\"").matcher(html);
    assertTrue(match.find(), name);
    return match.group(1);
  }

  static Form review(S3OperationPort operations, String route, String bucket, String key) {
    var initial =
        given()
            .queryParam("bucket", bucket)
            .queryParam("key", key)
            .queryParam("lang", "en")
            .queryParam("theme", "dark")
            .queryParam("density", "compact")
            .get(route)
            .then()
            .statusCode(200)
            .extract()
            .response();
    var match =
        Pattern.compile("data-operation-id=\"([a-f0-9-]{36})\"").matcher(initial.asString());
    assertTrue(match.find());
    String id = match.group(1);
    assertEquals(State.READY, await(operations, id).state());
    var response =
        given()
            .queryParam("lang", "en")
            .queryParam("theme", "dark")
            .queryParam("density", "compact")
            .get("/operations/" + id + "/review")
            .then()
            .statusCode(200)
            .extract()
            .response();
    return new Form(id, field(response.asString(), "csrf-token"), response.cookies());
  }

  static Operation await(S3OperationPort operations, String id) {
    long until = System.nanoTime() + Duration.ofSeconds(30).toNanos();
    do {
      var operation = operations.get(id);
      if (!operation.active()) return operation;
      try {
        Thread.sleep(20);
      } catch (InterruptedException failure) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(failure);
      }
    } while (System.nanoTime() < until);
    throw new AssertionError("Operation did not reach a stable state");
  }
}
