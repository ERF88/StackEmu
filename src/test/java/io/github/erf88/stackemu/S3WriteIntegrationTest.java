package io.github.erf88.stackemu;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

@QuarkusTest
@TestProfile(S3WriteIntegrationTest.Integration.class)
@EnabledIfEnvironmentVariable(named = "STACKEMU_INTEGRATION", matches = "true")
class S3WriteIntegrationTest {
  private static final Path UPLOADS =
      Path.of(System.getProperty("java.io.tmpdir"), "stackemu-phase3-test-uploads");

  public static class Integration implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of(
          "stackemu.max-upload",
          "1024",
          "quarkus.http.limits.max-body-size",
          "6K",
          "quarkus.http.body.uploads-directory",
          UPLOADS.toString());
    }
  }

  @Inject S3Client client;
  private String bucket;
  private boolean created;

  private record FormSession(String token, Map<String, String> cookies) {}

  @BeforeEach
  void newFixtureName() {
    bucket = "stackemu-write-" + UUID.randomUUID();
    created = false;
  }

  @AfterEach
  void removeOnlyOwnFixture() {
    if (created) {
      String token = null;
      do {
        final String current = token;
        var page =
            client.listObjectsV2(request -> request.bucket(bucket).continuationToken(current));
        for (var object : page.contents()) {
          client.deleteObject(request -> request.bucket(bucket).key(object.key()));
        }
        token = page.nextContinuationToken();
      } while (token != null);
      client.deleteBucket(request -> request.bucket(bucket));
    }
  }

  private FormSession form(String action) {
    var response =
        given()
            .queryParam("action", action)
            .queryParam("bucket", bucket)
            .get("/s3/write")
            .then()
            .statusCode(200)
            .extract()
            .response();
    var token =
        Pattern.compile("name=\"csrf-token\" value=\"([^\"]+)\"").matcher(response.asString());
    assertTrue(token.find());
    return new FormSession(token.group(1), response.cookies());
  }

  private void createThroughGui() {
    var session = form("bucket");
    given()
        .cookies(session.cookies())
        .formParam("csrf-token", session.token())
        .formParam("bucket", bucket)
        .formParam("confirm", "yes")
        .post("/s3/write/bucket")
        .then()
        .statusCode(200)
        .body(containsString("Operation completed"));
    created = true;
    client.headBucket(request -> request.bucket(bucket));
  }

  private void assertUploadsRemoved() throws Exception {
    // Framework rejection cleanup runs asynchronously after response completion.
    for (int attempt = 0; attempt < 100; attempt++) {
      if (!Files.exists(UPLOADS)) {
        return;
      }
      try (var files = Files.list(UPLOADS)) {
        if (files.findAny().isEmpty()) {
          return;
        }
      }
      Thread.sleep(50);
    }
    try (var files = Files.list(UPLOADS)) {
      assertEquals(0, files.count());
    }
  }

  @Test
  void uploadWorkspaceKeepsRealSuccessConflictValidationAndCsrfStatuses() throws Exception {
    createThroughGui();
    var session = form("upload");
    for (int expected : new int[] {200, 409}) {
      given()
          .header("HX-Request", "true")
          .cookies(session.cookies())
          .multiPart("csrf-token", session.token())
          .multiPart("bucket", bucket)
          .multiPart("prefix", "enhanced/")
          .multiPart("confirm", "yes")
          .multiPart("file", "original.txt", new byte[] {1, 2, 3}, "text/plain")
          .post("/s3/write/upload")
          .then()
          .statusCode(expected)
          .header("X-StackEmu-Upload", "workspace")
          .header("Cache-Control", "no-store")
          .body(containsString("id=\"mutation-workspace\""));
    }
    given()
        .header("HX-Request", "true")
        .cookies(session.cookies())
        .multiPart("csrf-token", session.token())
        .multiPart("bucket", bucket)
        .multiPart("prefix", "enhanced/")
        .multiPart("file", "rejected.txt", new byte[] {4}, "text/plain")
        .post("/s3/write/upload")
        .then()
        .statusCode(422)
        .header("X-StackEmu-Upload", "workspace")
        .body(containsString("confirm-error"), containsString("id=\"mutation-workspace\""));
    given()
        .header("HX-Request", "true")
        .multiPart("bucket", bucket)
        .multiPart("confirm", "yes")
        .multiPart("file", "rejected.txt", new byte[] {4}, "text/plain")
        .post("/s3/write/upload")
        .then()
        .statusCode(400)
        .header("X-StackEmu-Upload", nullValue());
    assertUploadsRemoved();
  }

  @Test
  void createUploadNavigateAndDownloadEndToEnd() throws Exception {
    createThroughGui();
    String key = "one/two/ação +%?#<img src=x onerror=alert(1)>.bin";
    byte[] bytes = new byte[1024];
    new java.util.Random(42).nextBytes(bytes);
    var session = form("upload");
    given()
        .config(
            io.restassured.config.RestAssuredConfig.config()
                .multiPartConfig(
                    io.restassured.config.MultiPartConfig.multiPartConfig()
                        .defaultCharset("UTF-8")))
        .cookies(session.cookies())
        .multiPart("csrf-token", session.token())
        .multiPart("bucket", bucket)
        .multiPart(
            new io.restassured.builder.MultiPartSpecBuilder("one/two/")
                .controlName("prefix")
                .charset("UTF-8")
                .mimeType("text/plain")
                .emptyFileName()
                .build())
        .multiPart("confirm", "yes")
        .multiPart(
            new io.restassured.builder.MultiPartSpecBuilder(bytes)
                .controlName("file")
                .fileName(
                    "=?UTF-8?B?"
                        + java.util.Base64.getEncoder()
                            .encodeToString(
                                key.substring(key.lastIndexOf('/') + 1)
                                    .getBytes(java.nio.charset.StandardCharsets.UTF_8))
                        + "?=")
                .mimeType("text/html")
                .build())
        .post("/s3/write/upload")
        .then()
        .statusCode(200)
        .body(
            containsString("data-result-return"),
            not(containsString("AWS_SECRET_ACCESS_KEY")),
            not(containsString("<img")));
    assertUploadsRemoved();
    given()
        .queryParam("bucket", bucket)
        .get("/s3/bucket")
        .then()
        .statusCode(200)
        .body(containsString("one/"));
    given()
        .queryParam("bucket", bucket)
        .queryParam("prefix", "one/two/")
        .get("/s3/bucket")
        .then()
        .statusCode(200)
        .body(containsString("ação +%?#&lt;img"), not(containsString("<img")));
    given()
        .queryParam("bucket", bucket)
        .queryParam("key", key)
        .get("/s3/object")
        .then()
        .statusCode(200);
    byte[] actual =
        given()
            .queryParam("bucket", bucket)
            .queryParam("key", key)
            .get("/s3/download")
            .then()
            .statusCode(200)
            .contentType("text/html")
            .header("Content-Disposition", containsString(".bin"))
            .extract()
            .asByteArray();
    assertArrayEquals(bytes, actual);
  }

  @Test
  void csrfMissingCookieMissingTokenAndMismatchCannotCreateBucket() {
    var session = form("bucket");
    given()
        .formParam("bucket", bucket)
        .formParam("confirm", "yes")
        .post("/s3/write/bucket")
        .then()
        .statusCode(400);
    given()
        .cookies(session.cookies())
        .formParam("bucket", bucket)
        .formParam("confirm", "yes")
        .post("/s3/write/bucket")
        .then()
        .statusCode(400);
    given()
        .formParam("csrf-token", session.token())
        .formParam("bucket", bucket)
        .formParam("confirm", "yes")
        .post("/s3/write/bucket")
        .then()
        .statusCode(400);
    given()
        .cookies(session.cookies())
        .formParam("csrf-token", "bad-token")
        .formParam("bucket", bucket)
        .formParam("confirm", "yes")
        .post("/s3/write/bucket")
        .then()
        .statusCode(400);
    assertEquals(
        404,
        assertThrows(S3Exception.class, () -> client.headBucket(request -> request.bucket(bucket)))
            .statusCode());
  }

  @Test
  void folderMarkerAppearsAndDuplicateDoesNotOverwrite() {
    createThroughGui();
    var session = form("folder");
    given()
        .cookies(session.cookies())
        .formParam("csrf-token", session.token())
        .formParam("bucket", bucket)
        .formParam("key", "nested/folder")
        .formParam("confirm", "yes")
        .post("/s3/write/folder")
        .then()
        .statusCode(200);
    assertEquals(
        0,
        client.headObject(request -> request.bucket(bucket).key("nested/folder/")).contentLength());
    given()
        .queryParam("bucket", bucket)
        .queryParam("prefix", "nested/")
        .get("/s3/bucket")
        .then()
        .statusCode(200)
        .body(containsString("<code>folder/</code>"), containsString("prefix=nested%2Ffolder%2F"));
    given()
        .cookies(session.cookies())
        .formParam("csrf-token", session.token())
        .formParam("bucket", bucket)
        .formParam("key", "nested/folder")
        .formParam("confirm", "yes")
        .post("/s3/write/folder")
        .then()
        .statusCode(409);
  }

  @Test
  void invalidBucketAndMissingConfirmationShowFieldErrorsAndEscapeValues() {
    var session = form("bucket");
    given()
        .cookies(session.cookies())
        .formParam("csrf-token", session.token())
        .formParam("bucket", bucket)
        .post("/s3/write/bucket")
        .then()
        .statusCode(422)
        .body(containsString("Confirm the action"));
    given()
        .cookies(session.cookies())
        .formParam("csrf-token", session.token())
        .formParam("bucket", "<script>alert(1)</script>")
        .formParam("confirm", "yes")
        .post("/s3/write/bucket")
        .then()
        .statusCode(422)
        .body(
            containsString("&lt;script&gt;"),
            not(containsString("<script>")),
            containsString("3–63"));
  }

  @Test
  void oversizedInvalidAndFailedUploadsRemoveTemporaryFiles() throws Exception {
    createThroughGui();
    var session = form("upload");
    given()
        .cookies(session.cookies())
        .multiPart("csrf-token", session.token())
        .multiPart("bucket", bucket)
        .multiPart("key", "too-large.bin")
        .multiPart("confirm", "yes")
        .multiPart("file", "large.bin", new byte[1025])
        .post("/s3/write/upload")
        .then()
        .statusCode(422)
        .body(containsString("exceeds"));
    assertUploadsRemoved();
    given()
        .cookies(session.cookies())
        .multiPart("csrf-token", session.token())
        .multiPart("bucket", bucket)
        .multiPart("key", "valid.bin")
        .multiPart("file", "small.bin", new byte[1])
        .post("/s3/write/upload")
        .then()
        .statusCode(422);
    assertUploadsRemoved();
    given()
        .cookies(session.cookies())
        .multiPart("csrf-token", session.token())
        .multiPart("bucket", bucket + "-missing")
        .multiPart("key", "valid.bin")
        .multiPart("confirm", "yes")
        .multiPart("file", "small.bin", new byte[1])
        .post("/s3/write/upload")
        .then()
        .statusCode(404);
    assertUploadsRemoved();
    given()
        .cookies(session.cookies())
        .multiPart("csrf-token", session.token())
        .multiPart("bucket", bucket)
        .multiPart("key", "huge.bin")
        .multiPart("confirm", "yes")
        .multiPart("file", "huge.bin", new byte[8192])
        .post("/s3/write/upload")
        .then()
        .statusCode(413);
    assertUploadsRemoved();
  }

  @Test
  void missingCsrfOnMultipartAndFolderCannotMutate() throws Exception {
    createThroughGui();
    given()
        .multiPart("bucket", bucket)
        .multiPart("key", "csrf.bin")
        .multiPart("confirm", "yes")
        .multiPart("file", "small.bin", new byte[1])
        .post("/s3/write/upload")
        .then()
        .statusCode(400);
    assertUploadsRemoved();
    given()
        .formParam("bucket", bucket)
        .formParam("key", "csrf/")
        .formParam("confirm", "yes")
        .post("/s3/write/folder")
        .then()
        .statusCode(400);
    assertTrue(client.listObjectsV2(request -> request.bucket(bucket)).contents().isEmpty());
  }

  @Test
  void existingBucketAndObjectReturnConflictWithoutChangingData() throws Exception {
    createThroughGui();
    var session = form("bucket");
    given()
        .cookies(session.cookies())
        .formParam("csrf-token", session.token())
        .formParam("bucket", bucket)
        .formParam("confirm", "yes")
        .post("/s3/write/bucket")
        .then()
        .statusCode(409);
    client.putObject(
        request -> request.bucket(bucket).key("existing.bin"), RequestBody.fromString("original"));
    session = form("upload");
    given()
        .cookies(session.cookies())
        .multiPart("csrf-token", session.token())
        .multiPart("bucket", bucket)
        .multiPart("key", "existing.bin")
        .multiPart("confirm", "yes")
        .multiPart("file", "existing.bin", new byte[1])
        .post("/s3/write/upload")
        .then()
        .statusCode(409);
    assertEquals(
        "original",
        client
            .getObjectAsBytes(request -> request.bucket(bucket).key("existing.bin"))
            .asUtf8String());
    assertUploadsRemoved();
  }

  @Test
  void originalExtensionsAndTypesRoundTripWithoutManualKey() throws Exception {
    createThroughGui();
    given()
        .queryParam("action", "upload")
        .queryParam("bucket", bucket)
        .get("/s3/write")
        .then()
        .statusCode(200)
        .body(not(containsString("name=\"key\"")), containsString("name=\"prefix\""));
    String[][] cases = {
      {"report.pdf", "application/pdf"},
      {"photo.png", "image/png"},
      {"notes.txt", "text/plain"},
      {"unknown.zzz", "application/octet-stream"}
    };
    for (String[] item : cases) {
      var session = form("upload");
      byte[] bytes = "original bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8);
      given()
          .cookies(session.cookies())
          .multiPart("csrf-token", session.token())
          .multiPart("bucket", bucket)
          .multiPart("prefix", "docs/")
          .multiPart("key", "ignored-forged-key")
          .multiPart("confirm", "yes")
          .multiPart("file", item[0], bytes, "application/octet-stream")
          .post("/s3/write/upload")
          .then()
          .statusCode(200);
      assertEquals(
          item[1],
          client
              .headObject(request -> request.bucket(bucket).key("docs/" + item[0]))
              .contentType());
      var response =
          given()
              .queryParam("bucket", bucket)
              .queryParam("key", "docs/" + item[0])
              .get("/s3/download")
              .then()
              .statusCode(200)
              .contentType(item[1])
              .header("Content-Disposition", containsString("filename=\"" + item[0] + "\""))
              .header("X-Content-Type-Options", "nosniff")
              .extract()
              .response();
      assertArrayEquals(bytes, response.asByteArray());
      assertUploadsRemoved();
    }
  }
}
