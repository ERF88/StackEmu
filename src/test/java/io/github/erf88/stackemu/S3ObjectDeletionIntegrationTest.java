package io.github.erf88.stackemu;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
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
@EnabledIfEnvironmentVariable(named = "STACKEMU_INTEGRATION", matches = "true")
class S3ObjectDeletionIntegrationTest {
  @Inject S3Client client;
  private String bucket;
  private final String key = "samples/ação +% file.txt";

  private record Form(String ticket, String csrf, Map<String, String> cookies) {}

  @BeforeEach
  void createOwnedFixture() {
    bucket = "stackemu-delete-" + UUID.randomUUID();
    client.createBucket(r -> r.bucket(bucket));
    put(key, "original");
    put("neighbor.txt", "guard");
  }

  private void put(String name, String body) {
    client.putObject(
        r -> r.bucket(bucket).key(name).contentType("text/plain"), RequestBody.fromString(body));
  }

  @AfterEach
  void removeOnlyExactOwnedFixtures() {
    for (String name : new String[] {key, "neighbor.txt", "marker/"}) {
      client.deleteObject(r -> r.bucket(bucket).key(name));
    }
    client.deleteBucket(r -> r.bucket(bucket));
  }

  private Form review() {
    var response =
        given()
            .queryParam("bucket", bucket)
            .queryParam("key", key)
            .queryParam("lang", "en")
            .queryParam("theme", "dark")
            .queryParam("density", "compact")
            .get("/s3/delete-object")
            .then()
            .statusCode(200)
            .header("Cache-Control", "no-store")
            .extract()
            .response();
    return new Form(
        field(response.asString(), "ticket"),
        field(response.asString(), "csrf-token"),
        response.cookies());
  }

  private static String field(String html, String name) {
    var matcher = Pattern.compile("name=\"" + name + "\" value=\"([^\"]+)\"").matcher(html);
    assertTrue(matcher.find(), name);
    return matcher.group(1);
  }

  private io.restassured.response.ValidatableResponse submit(
      Form form, String ticket, String confirm) {
    return given()
        .cookies(form.cookies())
        .formParam("csrf-token", form.csrf())
        .formParam("ticket", ticket)
        .formParam("confirm", confirm)
        .formParam("lang", "en")
        .formParam("theme", "dark")
        .formParam("density", "compact")
        // Must not override the reviewed server-held target.
        .formParam("bucket", "unrelated-bucket")
        .formParam("key", "neighbor.txt")
        .formParam("etag", "*")
        .post("/s3/delete-object")
        .then();
  }

  private void assertNeighbor() {
    assertEquals(
        "guard", client.getObjectAsBytes(r -> r.bucket(bucket).key("neighbor.txt")).asUtf8String());
  }

  @Test
  void signedSdkDeletesOnlyReviewedExactTargetAndRejectsReplay() {
    var form = review();
    assertEquals(
        "original", client.getObjectAsBytes(r -> r.bucket(bucket).key(key)).asUtf8String());
    submit(form, form.ticket(), "yes")
        .statusCode(200)
        .body(
            containsString("acknowledged the conditional deletion"),
            containsString("density=compact"));
    assertEquals(
        404,
        assertThrows(S3Exception.class, () -> client.headObject(r -> r.bucket(bucket).key(key)))
            .statusCode());
    assertNeighbor();
    put(key, "recreated");
    submit(form, form.ticket(), "yes").statusCode(409).body(containsString("already used"));
    assertEquals(
        "recreated", client.getObjectAsBytes(r -> r.bucket(bucket).key(key)).asUtf8String());
  }

  @Test
  void replacementConflictsAndDoesNotDeleteEitherObject() {
    var form = review();
    put(key, "replacement");
    submit(form, form.ticket(), "yes").statusCode(409).body(containsString("object changed"));
    assertEquals(
        "replacement", client.getObjectAsBytes(r -> r.bucket(bucket).key(key)).asUtf8String());
    assertNeighbor();
    submit(form, form.ticket(), "yes").statusCode(409);
  }

  @Test
  void consentTicketAndCsrfAreIndependentGates() {
    var form = review();
    given()
        .cookies(form.cookies())
        .formParam("ticket", form.ticket())
        .formParam("confirm", "yes")
        .post("/s3/delete-object")
        .then()
        .statusCode(400);
    submit(form, "A".repeat(43), "yes").statusCode(409);
    submit(form, form.ticket(), "no").statusCode(422);
    submit(form, form.ticket(), "yes").statusCode(409);
    assertEquals(
        "original", client.getObjectAsBytes(r -> r.bucket(bucket).key(key)).asUtf8String());
    assertNeighbor();
  }

  @Test
  void markerAndMissingObjectDoNotProduceConfirmation() {
    put("marker/", "");
    given()
        .queryParam("bucket", bucket)
        .queryParam("key", "marker/")
        .get("/s3/delete-object")
        .then()
        .statusCode(409);
    given()
        .queryParam("bucket", bucket)
        .queryParam("key", "absent.txt")
        .get("/s3/delete-object")
        .then()
        .statusCode(404);
    assertNeighbor();
  }

  @Test
  void enabledAndSuspendedBucketsAreRefusedBeforeAndAfterConfirmation() {
    // Versioning fixture remains empty: never create/delete object versions during cleanup.
    String versionBucket = "stackemu-delete-version-" + UUID.randomUUID();
    client.createBucket(r -> r.bucket(versionBucket));
    try {
      // Obtain a ticket on a separate object, then change state without writing object versions.
      String ownKey = "never-versioned.txt";
      client.putObject(r -> r.bucket(versionBucket).key(ownKey), RequestBody.fromString("guard"));
      var response =
          given()
              .queryParam("bucket", versionBucket)
              .queryParam("key", ownKey)
              .get("/s3/delete-object")
              .then()
              .statusCode(200)
              .extract()
              .response();
      var form =
          new Form(
              field(response.asString(), "ticket"),
              field(response.asString(), "csrf-token"),
              response.cookies());
      // Remove only our null-version object before enabling; versioned bucket stays empty.
      client.deleteObject(r -> r.bucket(versionBucket).key(ownKey));
      for (String state : new String[] {"Enabled", "Suspended"}) {
        client.putBucketVersioning(
            r -> r.bucket(versionBucket).versioningConfiguration(c -> c.status(state)));
        given()
            .queryParam("bucket", versionBucket)
            .queryParam("key", ownKey)
            .get("/s3/delete-object")
            .then()
            .statusCode(409);
        if (state.equals("Enabled"))
          submit(form, form.ticket(), "yes")
              .statusCode(409)
              .body(containsString("versioning must be verifiably never enabled"));
        assertTrue(client.listObjectVersions(r -> r.bucket(versionBucket)).versions().isEmpty());
        assertTrue(
            client.listObjectVersions(r -> r.bucket(versionBucket)).deleteMarkers().isEmpty());
      }
    } finally {
      client.deleteBucket(r -> r.bucket(versionBucket));
    }
    assertNeighbor();
  }
}
