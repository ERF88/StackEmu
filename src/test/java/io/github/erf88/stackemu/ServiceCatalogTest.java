package io.github.erf88.stackemu;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Local controlled HTTP responses test presentation and call ownership, not S3 compatibility. */
@QuarkusTest
@QuarkusTestResource(value = ServiceCatalogTest.Emulator.class, restrictToAnnotatedClass = true)
class ServiceCatalogTest {
  public static class Emulator implements QuarkusTestResourceLifecycleManager {
    static final AtomicInteger requests = new AtomicInteger();
    static final AtomicInteger healthChecks = new AtomicInteger();
    static volatile String buckets = "";
    static volatile boolean forbidden;
    static HttpServer server;
    static int port;

    @Override
    public Map<String, String> start() {
      listen(0);
      port = server.getAddress().getPort();
      String endpoint = "http://127.0.0.1:" + port;
      return Map.of(
          "quarkus.s3.endpoint-override", endpoint,
          "stackemu.endpoint", endpoint,
          "stackemu.public-endpoint", "http://127.0.0.1:14566");
    }

    static void listen(int selectedPort) {
      try {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", selectedPort), 0);
        server.createContext(
            "/",
            exchange -> {
              if ("/_ministack/health".equals(exchange.getRequestURI().getPath())) {
                healthChecks.incrementAndGet();
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
                return;
              }
              requests.incrementAndGet();
              String body =
                  forbidden
                      ? "<Error><Code>AccessDenied</Code><Message>Denied</Message></Error>"
                      : "<ListAllMyBucketsResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\"><Buckets>"
                          + buckets
                          + "</Buckets></ListAllMyBucketsResult>";
              byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
              exchange.getResponseHeaders().set("Content-Type", "application/xml");
              exchange.sendResponseHeaders(forbidden ? 403 : 200, bytes.length);
              try (var output = exchange.getResponseBody()) {
                output.write(bytes);
              }
            });
        server.start();
      } catch (IOException failure) {
        throw new IllegalStateException("Cannot start the owned HTTP fixture", failure);
      }
    }

    @Override
    public void stop() {
      if (server != null) server.stop(0);
    }
  }

  @Test
  void homeChecksEmulatorHealthOnceWithoutListingResources() {
    Emulator.requests.set(0);
    Emulator.healthChecks.set(0);
    for (String language : java.util.List.of("en", "pt-BR")) {
      given()
          .queryParam("lang", language)
          .get("/")
          .then()
          .statusCode(200)
          .header("Cache-Control", "no-store")
          .body(
              containsString("health-table"),
              containsString("boarding-pass"),
              containsString("service-inventory"),
              not(containsString("id=\"bucket-panel\"")),
              not(containsString("data-refresh-panel")),
              not(containsString("href=\"/components?")));
    }
    assertEquals(0, Emulator.requests.get());
    assertEquals(1, Emulator.healthChecks.get());
    Emulator.server.stop(0);
    try {
      given().get("/").then().statusCode(200).body(containsString("health-table"));
      assertEquals(0, Emulator.requests.get());
      assertEquals(1, Emulator.healthChecks.get());
    } finally {
      Emulator.listen(Emulator.port);
    }
  }

  @Test
  void serviceHandlesEmptyPopulatedFailureAndRecoveryWithoutInventingZero() {
    Emulator.forbidden = false;
    Emulator.buckets = "";
    given()
        .get("/s3")
        .then()
        .statusCode(200)
        .body(containsString("data-bucket-count=\"0\""), not(containsString("service-connection")));
    Emulator.buckets = "<Bucket><Name>catalog-example</Name></Bucket>";
    given()
        .get("/s3")
        .then()
        .statusCode(200)
        .body(containsString("catalog-example"), containsString("data-bucket-count=\"1\""));
    Emulator.forbidden = true;
    try {
      given()
          .get("/s3")
          .then()
          .statusCode(200)
          .body(containsString("Could not verify S3"), not(containsString("data-bucket-count=")));
    } finally {
      Emulator.forbidden = false;
    }
    given()
        .header("HX-Request", "true")
        .get("/buckets")
        .then()
        .statusCode(200)
        .body(containsString("catalog-example"), not(containsString("<!doctype")));
  }

  @Test
  void publicEndpointCanDifferFromInternalEmulatorEndpoint() {
    given().get("/").then().statusCode(200).body(containsString("http://127.0.0.1:14566"));
    given()
        .get("/connection")
        .then()
        .statusCode(200)
        .body(containsString("http://127.0.0.1:14566"));
    given()
        .get("/sqs/connection")
        .then()
        .statusCode(200)
        .body(containsString("http://127.0.0.1:14566"));
  }

  @Test
  void homeLinksPreservePreferencesWithoutClaimingGlobalHealth() {
    given()
        .queryParam("lang", "pt-BR")
        .queryParam("theme", "light")
        .queryParam("density", "compact")
        .get("/")
        .then()
        .statusCode(200)
        .body(
            containsString("<title>Início · StackEmu</title>"),
            containsString("/s3?lang=pt-BR&amp;theme=light&amp;density=compact"),
            not(containsString("S3 · Não verificado")),
            containsString("id=\"action-dialog\""),
            not(containsString("id=\"bucket-panel\"")));
  }
}
