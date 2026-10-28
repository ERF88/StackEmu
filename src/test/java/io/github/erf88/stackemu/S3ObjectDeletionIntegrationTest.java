package io.github.erf88.stackemu;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.erf88.stackemu.application.S3OperationPort;
import io.github.erf88.stackemu.application.S3OperationPort.State;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.UUID;
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
  @Inject S3OperationPort operations;
  private String bucket;
  private final String key = "samples/ação +% file.txt";

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
  void removeOwnedFixture() {
    for (String name : new String[] {key, "neighbor.txt", "marker/", "marker/child.txt"})
      client.deleteObject(r -> r.bucket(bucket).key(name));
    client.deleteBucket(r -> r.bucket(bucket));
  }

  private OperationHttpFixtures.Form review(String name) {
    return OperationHttpFixtures.review(operations, "/s3/delete-object", bucket, name);
  }

  private io.restassured.response.ValidatableResponse submit(OperationHttpFixtures.Form form) {
    return given()
        .cookies(form.cookies())
        .formParam("csrf-token", form.csrf())
        .formParam("ticket", form.id())
        .formParam("bucket", "unrelated-bucket")
        .formParam("key", "neighbor.txt")
        .formParam("etag", "*")
        .formParam("lang", "en")
        .formParam("theme", "dark")
        .formParam("density", "compact")
        .post("/s3/delete-object")
        .then();
  }

  @Test
  void reviewedTargetIsAuthoritativeAndReplaysReturnSameOperation() {
    var form = review(key);
    submit(form)
        .statusCode(200)
        .body(containsString("data-action-accepted"), not(containsString("<details")));
    assertEquals(State.SUCCEEDED, OperationHttpFixtures.await(operations, form.id()).state());
    assertEquals(
        404,
        assertThrows(S3Exception.class, () -> client.headObject(r -> r.bucket(bucket).key(key)))
            .statusCode());
    assertEquals(
        "guard", client.getObjectAsBytes(r -> r.bucket(bucket).key("neighbor.txt")).asUtf8String());
    put(key, "recreated");
    submit(form).statusCode(200);
    assertEquals(
        "recreated", client.getObjectAsBytes(r -> r.bucket(bucket).key(key)).asUtf8String());
  }

  @Test
  void changedObjectFailsWithoutDeletingTheReplacement() {
    var form = review(key);
    put(key, "replacement");
    submit(form).statusCode(200);
    var result = OperationHttpFixtures.await(operations, form.id());
    assertEquals(State.FAILED, result.state());
    assertEquals("CHANGED", result.message());
    assertEquals(
        "replacement", client.getObjectAsBytes(r -> r.bucket(bucket).key(key)).asUtf8String());
  }

  @Test
  void csrfAndServerOperationIdentityAreIndependent() {
    var form = review(key);
    given()
        .cookies(form.cookies())
        .formParam("ticket", form.id())
        .post("/s3/delete-object")
        .then()
        .statusCode(400);
    assertEquals(State.READY, operations.get(form.id()).state());
    given()
        .cookies(form.cookies())
        .formParam("csrf-token", form.csrf())
        .formParam("ticket", UUID.randomUUID().toString())
        .post("/s3/delete-object")
        .then()
        .statusCode(404);
    assertEquals(
        "original", client.getObjectAsBytes(r -> r.bucket(bucket).key(key)).asUtf8String());
  }

  @Test
  void folderReviewAuthorizesOnlyItsPersistedContents() {
    put("marker/", "");
    put("marker/child.txt", "child");
    var form = review("marker/");
    submit(form).statusCode(200);
    assertEquals(State.SUCCEEDED, OperationHttpFixtures.await(operations, form.id()).state());
    assertEquals(
        "guard", client.getObjectAsBytes(r -> r.bucket(bucket).key("neighbor.txt")).asUtf8String());
  }

  @Test
  void lateFolderContentsAreNotAdopted() {
    put("marker/", "");
    var form = review("marker/");
    put("marker/child.txt", "late");
    submit(form).statusCode(200);
    assertEquals(State.PARTIAL, OperationHttpFixtures.await(operations, form.id()).state());
    assertEquals(
        "late",
        client.getObjectAsBytes(r -> r.bucket(bucket).key("marker/child.txt")).asUtf8String());
  }
}
