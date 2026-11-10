package io.github.erf88.stackemu;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

@QuarkusTest
@TestProfile(S3ReadIntegrationTest.Integration.class)
@EnabledIfEnvironmentVariable(named = "STACKEMU_INTEGRATION", matches = "true")
class S3PreviewIntegrationTest {
  @Inject S3Client client;
  private String bucket;
  private final Set<String> keys = new LinkedHashSet<>();

  @BeforeEach
  void createFixture() {
    bucket = "stackemu-preview-" + UUID.randomUUID();
    client.createBucket(request -> request.bucket(bucket));
    keys.clear();
  }

  @AfterEach
  void removeOnlyOwnFixture() {
    for (String key : keys) client.deleteObject(request -> request.bucket(bucket).key(key));
    client.deleteBucket(request -> request.bucket(bucket));
  }

  private void put(String key, byte[] bytes, String type, String encoding) {
    keys.add(key);
    client.putObject(
        request -> request.bucket(bucket).key(key).contentType(type).contentEncoding(encoding),
        RequestBody.fromBytes(bytes));
  }

  @Test
  void textAndOpaqueTargetsAreEscapedWithNativeLinksAndNoStore() {
    String key = "folder/ação +%<img src=x>.TXT";
    byte[] bytes =
        "</code></pre><script>window.previewAttack=1</script>\nação"
            .getBytes(StandardCharsets.UTF_8);
    put(key, bytes, "text/plain; charset=utf-8", null);
    given()
        .queryParam("bucket", bucket)
        .queryParam("key", key)
        .queryParam("lang", "pt-BR")
        .get("/s3/preview")
        .then()
        .statusCode(200)
        .header("Cache-Control", "no-store")
        .header("X-Content-Type-Options", "nosniff")
        .header("Referrer-Policy", "no-referrer")
        .header("X-Frame-Options", "DENY")
        .header("Content-Security-Policy", containsString("default-src 'none'"))
        .body(
            containsString("Prévia de texto"),
            containsString("&lt;script&gt;"),
            not(containsString("<script>window.previewAttack")),
            containsString("/s3/download?"),
            containsString("/s3/object?"),
            containsString("a%C3%A7%C3%A3o%20%2B%25%3Cimg"));
    assertArrayEquals(
        bytes,
        given()
            .queryParam("bucket", bucket)
            .queryParam("key", key)
            .get("/s3/download")
            .then()
            .statusCode(200)
            .header("Content-Disposition", containsString("attachment"))
            .extract()
            .asByteArray());
  }

  @Test
  void policyRefusalsContainNoObjectBodyAndRetainDownload() {
    record Fixture(String key, String type, String encoding, byte[] bytes, String message) {}
    var fixtures =
        new Fixture[] {
          new Fixture(
              "a.html", "text/plain", null, "SECRET".getBytes(StandardCharsets.UTF_8), "format"),
          new Fixture(
              "a.svg", "image/svg+xml", null, "SECRET".getBytes(StandardCharsets.UTF_8), "format"),
          new Fixture(
              "a.txt", "text/plain", "gzip", "SECRET".getBytes(StandardCharsets.UTF_8), "format"),
          new Fixture("large.txt", "text/plain", null, new byte[65537], "exceeds 64 KiB"),
          new Fixture("binary.txt", "text/plain", null, new byte[] {0}, "invalid UTF-8"),
          new Fixture(
              "malformed.txt", "text/plain", null, new byte[] {(byte) 0xC3, 0x28}, "invalid UTF-8")
        };
    for (var fixture : fixtures) {
      put(fixture.key(), fixture.bytes(), fixture.type(), fixture.encoding());
      given()
          .queryParam("bucket", bucket)
          .queryParam("key", fixture.key())
          .queryParam("lang", "en")
          .get("/s3/preview")
          .then()
          .statusCode(200)
          .header("Cache-Control", "no-store")
          .body(
              containsString(fixture.message()),
              not(containsString("<pre class=\"text-preview\"")),
              not(containsString("SECRET")),
              containsString("/s3/download?"));
    }
  }

  @Test
  void completeBoundaryEmptyAndJsonVerbatimAreSupported() {
    for (String key : new String[] {"empty.txt", "maximum.log", "verbatim.json"}) {
      String text =
          key.equals("empty.txt")
              ? ""
              : key.equals("maximum.log") ? "x".repeat(65536) : "{not parsed}";
      put(
          key,
          text.getBytes(StandardCharsets.UTF_8),
          key.endsWith("json") ? "application/json" : "text/plain",
          null);
      given()
          .queryParam("bucket", bucket)
          .queryParam("key", key)
          .queryParam("lang", "en")
          .get("/s3/preview")
          .then()
          .statusCode(200)
          .body(containsString("<pre class=\"text-preview\""), containsString(text));
    }
  }

  @Test
  void missingAndInvalidTargetsRetainRealStatusWithoutCaching() {
    given()
        .queryParam("bucket", bucket)
        .queryParam("key", "missing.txt")
        .get("/s3/preview")
        .then()
        .statusCode(404)
        .header("Cache-Control", "no-store");
    given()
        .queryParam("bucket", "http://external.invalid")
        .queryParam("key", "a.txt")
        .get("/s3/preview")
        .then()
        .statusCode(400)
        .header("Cache-Control", "no-store");
    given().queryParam("bucket", bucket).get("/s3/preview").then().statusCode(400);
  }
}
