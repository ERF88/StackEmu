package io.github.erf88.stackemu;

import static io.restassured.RestAssured.given;
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
class S3BucketDeletionIntegrationTest {
  @Inject S3Client client;
  @Inject S3OperationPort operations;
  private String bucket;

  @BeforeEach
  void createOwnedFixture() {
    bucket = "stackemu-bucket-delete-" + UUID.randomUUID();
    client.createBucket(r -> r.bucket(bucket));
  }

  @AfterEach
  void removeOwnedFixture() {
    try {
      client.deleteObject(r -> r.bucket(bucket).key("marker/"));
      client.deleteBucket(r -> r.bucket(bucket));
    } catch (S3Exception missing) {
      if (missing.statusCode() != 404) throw missing;
    }
  }

  private OperationHttpFixtures.Form review() {
    return OperationHttpFixtures.review(operations, "/s3/delete-bucket", bucket, "");
  }

  private io.restassured.response.ValidatableResponse submit(
      OperationHttpFixtures.Form form, String name) {
    return given()
        .cookies(form.cookies())
        .formParam("csrf-token", form.csrf())
        .formParam("ticket", form.id())
        .formParam("bucketName", name)
        .formParam("lang", "en")
        .post("/s3/delete-bucket")
        .then();
  }

  @Test
  void typedNameAndCsrfAreIndependentAndMistakesCanBeCorrected() {
    var form = review();
    given()
        .cookies(form.cookies())
        .formParam("ticket", form.id())
        .formParam("bucketName", bucket)
        .post("/s3/delete-bucket")
        .then()
        .statusCode(400);
    submit(form, "wrong").statusCode(422);
    assertEquals(State.READY, operations.get(form.id()).state());
    submit(form, bucket).statusCode(200);
    assertEquals(State.SUCCEEDED, OperationHttpFixtures.await(operations, form.id()).state());
  }

  @Test
  void addedContentBlocksDeletionAndIsNotImplicitlyCleaned() {
    var form = review();
    client.putObject(r -> r.bucket(bucket).key("marker/"), RequestBody.empty());
    submit(form, bucket).statusCode(200);
    assertEquals(State.FAILED, OperationHttpFixtures.await(operations, form.id()).state());
    assertEquals(0, client.headObject(r -> r.bucket(bucket).key("marker/")).contentLength());
  }

  @Test
  void duplicateSubmissionDoesNotDeleteRecreatedBucket() {
    var form = review();
    submit(form, bucket).statusCode(200);
    assertEquals(State.SUCCEEDED, OperationHttpFixtures.await(operations, form.id()).state());
    assertEquals(
        404,
        assertThrows(S3Exception.class, () -> client.headBucket(r -> r.bucket(bucket)))
            .statusCode());
    client.createBucket(r -> r.bucket(bucket));
    submit(form, bucket).statusCode(200);
    assertEquals(200, client.headBucket(r -> r.bucket(bucket)).sdkHttpResponse().statusCode());
  }
}
