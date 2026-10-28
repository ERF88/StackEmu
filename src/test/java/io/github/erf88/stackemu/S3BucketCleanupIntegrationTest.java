package io.github.erf88.stackemu;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.erf88.stackemu.adapter.aws.AwsS3BucketCleanupAdapter;
import io.github.erf88.stackemu.application.ObjectDeletionException;
import io.github.erf88.stackemu.application.S3BucketCleanupPort;
import io.github.erf88.stackemu.application.S3BucketCleanupPort.Failure;
import io.github.erf88.stackemu.application.S3BucketCleanupPort.Outcome;
import io.github.erf88.stackemu.application.S3BucketDeletionPort;
import io.github.erf88.stackemu.application.S3ObjectDeletionPort;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

@QuarkusTest
@EnabledIfEnvironmentVariable(named = "STACKEMU_INTEGRATION", matches = "true")
class S3BucketCleanupIntegrationTest {
  @Inject S3Client client;
  @Inject S3ObjectDeletionPort objects;
  @Inject S3BucketDeletionPort buckets;
  @Inject S3BucketCleanupPort cleanup;
  @Inject io.github.erf88.stackemu.application.S3OperationPort operations;
  private String bucket;
  private String neighbor;
  private final Set<String> keys = new LinkedHashSet<>();

  @BeforeEach
  void createOwnedFixtures() {
    bucket = "stackemu-cleanup-" + UUID.randomUUID();
    neighbor = "stackemu-cleanup-guard-" + UUID.randomUUID();
    client.createBucket(r -> r.bucket(bucket));
    client.createBucket(r -> r.bucket(neighbor));
    client.putObject(r -> r.bucket(neighbor).key("guard.txt"), RequestBody.fromString("guard"));
    put("folder/", "");
    put("folder/sub/", "");
    put("folder/sub/ação +%.txt", "original");
  }

  private void put(String key, String body) {
    keys.add(key);
    client.putObject(r -> r.bucket(bucket).key(key), RequestBody.fromString(body));
  }

  @AfterEach
  void removeOnlyExactOwnedFixtures() {
    for (String key : keys) {
      try {
        client.deleteObject(r -> r.bucket(bucket).key(key));
      } catch (S3Exception missing) {
        if (missing.statusCode() != 404) throw missing;
      }
    }
    try {
      client.deleteBucket(r -> r.bucket(bucket));
    } catch (S3Exception missing) {
      if (missing.statusCode() != 404) throw missing;
    }
    client.deleteObject(r -> r.bucket(neighbor).key("guard.txt"));
    client.deleteBucket(r -> r.bucket(neighbor));
    keys.clear();
  }

  private OperationHttpFixtures.Form review(String target) {
    return OperationHttpFixtures.review(operations, "/s3/empty-bucket", target, "");
  }

  private io.restassured.response.ValidatableResponse submit(
      OperationHttpFixtures.Form form, String name) {
    return given()
        .cookies(form.cookies())
        .formParam("csrf-token", form.csrf())
        .formParam("ticket", form.id())
        .formParam("bucketName", name)
        .formParam("bucket", neighbor)
        .formParam("key", "guard.txt")
        .formParam("lang", "en")
        .post("/s3/empty-bucket")
        .then();
  }

  private void assertNeighbor() {
    assertEquals(
        "guard", client.getObjectAsBytes(r -> r.bucket(neighbor).key("guard.txt")).asUtf8String());
  }

  @Test
  void paginatedCleanupIsIdempotentAndPreservesNeighbor() {
    for (int index = 0; index < 32; index++) put("file-" + index + ".txt", "owned");
    var form = review(bucket);
    assertEquals(35, operations.get(form.id()).total());
    submit(form, bucket)
        .statusCode(200)
        .body(containsString("data-action-accepted"), not(containsString("<details")));
    assertEquals(
        io.github.erf88.stackemu.application.S3OperationPort.State.SUCCEEDED,
        OperationHttpFixtures.await(operations, form.id()).state());
    assertNeighbor();
    client.createBucket(r -> r.bucket(bucket));
    put("recreated.txt", "must survive replay");
    submit(form, bucket).statusCode(200);
    assertEquals(
        "must survive replay",
        client.getObjectAsBytes(r -> r.bucket(bucket).key("recreated.txt")).asUtf8String());
  }

  @Test
  void csrfAndTypedNameRemainIndependentWithoutConsumingCorrectableReview() {
    var form = review(bucket);
    given()
        .cookies(form.cookies())
        .formParam("ticket", form.id())
        .formParam("bucketName", bucket)
        .post("/s3/empty-bucket")
        .then()
        .statusCode(400);
    submit(form, neighbor).statusCode(422);
    assertEquals(
        io.github.erf88.stackemu.application.S3OperationPort.State.READY,
        operations.get(form.id()).state());
    assertEquals(3, client.listObjectsV2(r -> r.bucket(bucket)).contents().size());
    assertNeighbor();
  }

  @Test
  void newObjectsAfterReviewRemainOutsideTheAuthorizedInventory() {
    var form = review(bucket);
    put("new.txt", "guard");
    submit(form, bucket).statusCode(200);
    assertEquals(
        io.github.erf88.stackemu.application.S3OperationPort.State.PARTIAL,
        OperationHttpFixtures.await(operations, form.id()).state());
    assertEquals(
        "guard", client.getObjectAsBytes(r -> r.bucket(bucket).key("new.txt")).asUtf8String());
    assertNeighbor();
  }

  @Test
  void oldTwentyFiveEntryLimitDoesNotPreventReview() {
    for (int index = 0; index < 23; index++) put("file-" + index + ".txt", "owned");
    var form = review(bucket);
    assertEquals(26, operations.get(form.id()).total());
    assertEquals(26, client.listObjectsV2(r -> r.bucket(bucket)).contents().size());
    assertNeighbor();
  }

  @Test
  void nonzeroSlashObjectIsReviewedWithoutBeingRemovedByGet() {
    put("folder/", "nonzero");
    var form = review(bucket);
    assertEquals(3, operations.get(form.id()).total());
    assertEquals(
        "nonzero", client.getObjectAsBytes(r -> r.bucket(bucket).key("folder/")).asUtf8String());
    assertNeighbor();
  }

  @Test
  void lateNewObjectStopsFurtherDeletesAndIsNeverAdopted() {
    var wrapped =
        new S3ObjectDeletionPort() {
          public Target inspectObject(String name, String key) {
            return objects.inspectObject(name, key);
          }

          public void deleteObject(Target target) {
            objects.deleteObject(target);
            put("late.txt", "new guard");
          }
        };
    try (var adapter = new AwsS3BucketCleanupAdapter(client, wrapped, buckets)) {
      var manifest = adapter.inspectBucket(bucket);
      var result = adapter.emptyAndDelete(manifest);
      assertEquals(Failure.CHANGED, result.failure());
      assertTrue(!result.bucketDeleted());
      assertEquals(
          1, result.items().stream().filter(item -> item.outcome() == Outcome.DELETED).count());
      assertEquals(
          2,
          result.items().stream().filter(item -> item.outcome() == Outcome.NOT_ATTEMPTED).count());
      assertEquals(
          "new guard",
          client.getObjectAsBytes(r -> r.bucket(bucket).key("late.txt")).asUtf8String());
      assertNeighbor();
    }
  }

  @Test
  void uncertainObjectResultStopsTheJobWithoutAutomaticRetry() {
    var calls = new AtomicInteger();
    var wrapped =
        new S3ObjectDeletionPort() {
          public Target inspectObject(String name, String key) {
            return objects.inspectObject(name, key);
          }

          public void deleteObject(Target target) {
            calls.incrementAndGet();
            throw new ObjectDeletionException(ObjectDeletionException.Kind.UNCERTAIN);
          }
        };
    try (var adapter = new AwsS3BucketCleanupAdapter(client, wrapped, buckets)) {
      var result = adapter.emptyAndDelete(adapter.inspectBucket(bucket));
      assertEquals(Failure.UNCERTAIN, result.failure());
      assertEquals(1, calls.get());
      assertEquals(
          1, result.items().stream().filter(item -> item.outcome() == Outcome.UNCERTAIN).count());
      assertEquals(
          2,
          result.items().stream().filter(item -> item.outcome() == Outcome.NOT_ATTEMPTED).count());
      assertEquals(3, client.listObjectsV2(r -> r.bucket(bucket)).contents().size());
      assertNeighbor();
    }
  }

  @Test
  void changedVersioningStopsPreviouslyReviewedCleanup() {
    String versionBucket = "stackemu-cleanup-version-" + UUID.randomUUID();
    client.createBucket(r -> r.bucket(versionBucket));
    try {
      var form = review(versionBucket);
      client.putBucketVersioning(
          r -> r.bucket(versionBucket).versioningConfiguration(v -> v.status("Enabled")));
      submit(form, versionBucket).statusCode(200);
      assertEquals(
          io.github.erf88.stackemu.application.S3OperationPort.State.FAILED,
          OperationHttpFixtures.await(operations, form.id()).state());
      assertEquals("CHANGED", operations.get(form.id()).message());
      for (String state : new String[] {"Enabled", "Suspended"}) {
        client.putBucketVersioning(
            r -> r.bucket(versionBucket).versioningConfiguration(v -> v.status(state)));
        var supported = review(versionBucket);
        assertEquals(
            io.github.erf88.stackemu.application.S3OperationPort.State.READY,
            operations.get(supported.id()).state());
      }
    } finally {
      client.deleteBucket(r -> r.bucket(versionBucket));
    }
    assertNeighbor();
  }

  @Test
  void knownFailureAfterOneDeletionRetainsPartialPerKeyOutcomes() {
    var calls = new AtomicInteger();
    var wrapped =
        new S3ObjectDeletionPort() {
          public Target inspectObject(String name, String key) {
            return objects.inspectObject(name, key);
          }

          public void deleteObject(Target target) {
            if (calls.incrementAndGet() == 2)
              throw new ObjectDeletionException(ObjectDeletionException.Kind.FORBIDDEN);
            objects.deleteObject(target);
          }
        };
    try (var adapter = new AwsS3BucketCleanupAdapter(client, wrapped, buckets)) {
      var result = adapter.emptyAndDelete(adapter.inspectBucket(bucket));
      assertEquals(Failure.FORBIDDEN, result.failure());
      assertTrue(!result.bucketDeleted());
      assertEquals(2, calls.get());
      for (Outcome outcome : new Outcome[] {Outcome.DELETED, Outcome.FAILED, Outcome.NOT_ATTEMPTED})
        assertEquals(1, result.items().stream().filter(item -> item.outcome() == outcome).count());
      assertEquals(2, client.listObjectsV2(r -> r.bucket(bucket)).contents().size());
      assertNeighbor();
    }
  }

  @Test
  void activeBucketGuardRefusesAnotherCleanupWithoutAdditionalMutation() {
    var active = new java.util.concurrent.atomic.AtomicReference<AwsS3BucketCleanupAdapter>();
    var reviewed = new java.util.concurrent.atomic.AtomicReference<S3BucketCleanupPort.Manifest>();
    var calls = new AtomicInteger();
    var wrapped =
        new S3ObjectDeletionPort() {
          public Target inspectObject(String name, String key) {
            return objects.inspectObject(name, key);
          }

          public void deleteObject(Target target) {
            if (calls.getAndIncrement() == 0) {
              var competing = active.get().emptyAndDelete(reviewed.get());
              assertEquals(Failure.CHANGED, competing.failure());
              assertTrue(
                  competing.items().stream()
                      .allMatch(item -> item.outcome() == Outcome.NOT_ATTEMPTED));
            }
            objects.deleteObject(target);
          }
        };
    try (var adapter = new AwsS3BucketCleanupAdapter(client, wrapped, buckets)) {
      active.set(adapter);
      reviewed.set(adapter.inspectBucket(bucket));
      assertTrue(adapter.emptyAndDelete(reviewed.get()).bucketDeleted());
      assertEquals(3, calls.get());
      assertNeighbor();
    }
  }
}
