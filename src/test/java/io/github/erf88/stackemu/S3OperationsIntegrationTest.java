package io.github.erf88.stackemu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.erf88.stackemu.adapter.web.S3ObjectDeletionResource;
import io.github.erf88.stackemu.adapter.web.S3VersionsResource;
import io.github.erf88.stackemu.application.OperationException;
import io.github.erf88.stackemu.application.S3ObjectToolsPort;
import io.github.erf88.stackemu.application.S3OperationPort;
import io.github.erf88.stackemu.application.S3OperationPort.Kind;
import io.github.erf88.stackemu.application.S3OperationPort.Operation;
import io.github.erf88.stackemu.application.S3OperationPort.Request;
import io.github.erf88.stackemu.application.S3OperationPort.State;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

@QuarkusTest
@EnabledIfEnvironmentVariable(named = "STACKEMU_INTEGRATION", matches = "true")
class S3OperationsIntegrationTest {
  @Inject S3Client client;
  @Inject S3OperationPort operations;
  @Inject S3ObjectToolsPort tools;
  @Inject S3VersionsResource versionsResource;
  @Inject S3ObjectDeletionResource deletionResource;
  private final Set<String> owned = new LinkedHashSet<>();

  private String bucket() {
    String bucket = "stackemu-ops-" + UUID.randomUUID();
    client.createBucket(r -> r.bucket(bucket));
    owned.add(bucket);
    return bucket;
  }

  private void put(String bucket, String key, String body) {
    client.putObject(
        r -> r.bucket(bucket).key(key).contentType("text/plain; charset=utf-8"),
        RequestBody.fromString(body));
  }

  private Request request(Kind kind, String bucket, String key) {
    return new Request(kind, bucket, key, "", "", "", false);
  }

  private Operation ready(Request request) throws InterruptedException {
    return await(operations.prepare(request), State.PREPARING);
  }

  private Operation await(String id, State active) throws InterruptedException {
    long until = System.nanoTime() + Duration.ofSeconds(40).toNanos();
    Operation result;
    do {
      result = operations.get(id);
      if (result.state() != active && result.state() != State.STOP_REQUESTED) return result;
      Thread.sleep(30);
    } while (System.nanoTime() < until);
    throw new AssertionError(
        "operation did not finish: " + result.state() + " " + result.message());
  }

  private Operation run(Operation operation) throws InterruptedException {
    assertEquals(State.READY, operation.state(), operation.message());
    operations.start(operation.id(), operation.request().bucket());
    return await(operation.id(), State.RUNNING);
  }

  @Test
  void paginatedCleanupExceedsOldLimitAndKeepsNeighbor() throws Exception {
    String bucket = bucket(), neighbor = bucket();
    put(neighbor, "guard.txt", "guard");
    for (int i = 0; i < 135; i++) put(bucket, "folder/file-" + i + ".txt", "content");
    put(bucket, "folder/", "");
    var prepared = ready(request(Kind.EMPTY_BUCKET, bucket, ""));
    assertEquals(136, prepared.total());
    assertEquals(50, operations.items(prepared.id(), 0).size());
    assertEquals(36, operations.items(prepared.id(), 2).size());
    var result = run(prepared);
    assertEquals(State.SUCCEEDED, result.state(), result.message());
    assertEquals(136, result.done());
    assertEquals(
        404,
        assertThrows(S3Exception.class, () -> client.headBucket(r -> r.bucket(bucket)))
            .statusCode());
    assertEquals(
        "guard", client.getObjectAsBytes(r -> r.bucket(neighbor).key("guard.txt")).asUtf8String());
    assertEquals(State.SUCCEEDED, operations.start(prepared.id(), bucket).state());
  }

  @Test
  void reviewedInventoryNeverAdoptsLaterObjects() throws Exception {
    String bucket = bucket();
    put(bucket, "old.txt", "old");
    var prepared = ready(request(Kind.EMPTY_BUCKET, bucket, ""));
    put(bucket, "new.txt", "new");
    var result = run(prepared);
    assertEquals(State.PARTIAL, result.state());
    assertEquals("CHANGED", result.message());
    assertEquals(
        "new", client.getObjectAsBytes(r -> r.bucket(bucket).key("new.txt")).asUtf8String());
  }

  @Test
  void prefixCleanupIncludesContentsButNotSimilarPrefix() throws Exception {
    String bucket = bucket();
    put(bucket, "a/", "");
    put(bucket, "a/ação +%.txt", "delete");
    put(bucket, "ab/keep.txt", "keep");
    var result = run(ready(request(Kind.DELETE_PREFIX, bucket, "a/")));
    assertEquals(State.SUCCEEDED, result.state(), result.message());
    assertTrue(client.listObjectsV2(r -> r.bucket(bucket).prefix("a/")).contents().isEmpty());
    assertEquals(
        "keep", client.getObjectAsBytes(r -> r.bucket(bucket).key("ab/keep.txt")).asUtf8String());
  }

  @Test
  void neverVersionedBucketsDoNotOfferOrPrepareVersionDeletion() {
    String bucket = bucket();
    put(bucket, "test.txt", "one");
    assertFalse(tools.isVersionDeletionAvailable(bucket));
    var before = operations.list(0).stream().map(Operation::id).toList();
    for (String language : new String[] {"en", "pt-BR"}) {
      try (var response =
          versionsResource.list(bucket, "test.txt", null, null, language, "auto", null)) {
        assertEquals(200, response.getStatus());
        String html = (String) response.getEntity();
        assertFalse(html.contains("/s3/delete-object?"));
        assertTrue(html.contains("test.txt"));
      }
      try (var response =
          deletionResource.showConfirmation(bucket, "test.txt", "null", language, "auto", null)) {
        assertEquals(409, response.getStatus());
        String html = (String) response.getEntity();
        assertFalse(html.contains("data-operation-id"));
        assertFalse(html.contains("<form"));
        assertFalse(html.contains("/operations?"));
      }
    }
    assertEquals(before, operations.list(0).stream().map(Operation::id).toList());
    assertEquals(
        "one", client.getObjectAsBytes(r -> r.bucket(bucket).key("test.txt")).asUtf8String());
  }

  @Test
  void versionDeletionAvailabilityFollowsEnabledAndSuspendedBuckets() {
    String bucket = bucket();
    client.putBucketVersioning(
        r -> r.bucket(bucket).versioningConfiguration(v -> v.status("Enabled")));
    put(bucket, "test.txt", "one");
    assertTrue(tools.isVersionDeletionAvailable(bucket));
    try (var response =
        versionsResource.list(bucket, "test.txt", null, null, "en", "light", null)) {
      assertEquals(200, response.getStatus());
      assertTrue(((String) response.getEntity()).contains("/s3/delete-object?"));
    }
    client.putBucketVersioning(
        r -> r.bucket(bucket).versioningConfiguration(v -> v.status("Suspended")));
    assertTrue(tools.isVersionDeletionAvailable(bucket));
  }

  @Test
  void versionedCleanupRemovesVersionsAndMarkers() throws Exception {
    String bucket = bucket();
    client.putBucketVersioning(
        r -> r.bucket(bucket).versioningConfiguration(v -> v.status("Enabled")));
    put(bucket, "test.txt", "one");
    put(bucket, "test.txt", "two");
    client.deleteObject(r -> r.bucket(bucket).key("test.txt"));
    var prepared = ready(request(Kind.EMPTY_BUCKET, bucket, ""));
    assertEquals(State.READY, prepared.state(), prepared.message());
    assertEquals(3, prepared.total());
    var result = run(prepared);
    assertEquals(State.SUCCEEDED, result.state(), result.message());
  }

  @Test
  void ordinaryVersionedDeleteCreatesOneMarkerAndReplayDoesNotRepeat() throws Exception {
    String bucket = bucket();
    client.putBucketVersioning(
        r -> r.bucket(bucket).versioningConfiguration(v -> v.status("Enabled")));
    put(bucket, "test.txt", "one");
    var prepared = ready(request(Kind.DELETE_OBJECT, bucket, "test.txt"));
    assertEquals(State.SUCCEEDED, run(prepared).state());
    operations.start(prepared.id(), null);
    var versions = client.listObjectVersions(r -> r.bucket(bucket));
    assertEquals(1, versions.versions().size());
    assertEquals(1, versions.deleteMarkers().size());
    var marker = versions.deleteMarkers().getFirst();
    var removed =
        run(
            ready(
                new Request(
                    Kind.DELETE_VERSION, bucket, "test.txt", marker.versionId(), "", "", false)));
    assertEquals(State.SUCCEEDED, removed.state(), removed.message());
    assertEquals(
        "one", client.getObjectAsBytes(r -> r.bucket(bucket).key("test.txt")).asUtf8String());
  }

  @Test
  void changedReviewedObjectIsNotDeleted() throws Exception {
    String bucket = bucket();
    put(bucket, "test.txt", "one");
    var prepared = ready(request(Kind.DELETE_OBJECT, bucket, "test.txt"));
    put(bucket, "test.txt", "new");
    var result = run(prepared);
    assertEquals(State.FAILED, result.state());
    assertEquals("CHANGED", result.message());
    assertEquals(
        "new", client.getObjectAsBytes(r -> r.bucket(bucket).key("test.txt")).asUtf8String());
  }

  @Test
  void copyAndMoveKeepBytesContentTypeAndRequireReplacementDecision() throws Exception {
    String source = bucket(), destination = bucket();
    put(source, "original.txt", "ação +% original");
    var copy =
        ready(new Request(Kind.COPY, source, "original.txt", "", destination, "copy.txt", false));
    assertEquals(State.SUCCEEDED, run(copy).state());
    assertEquals(
        "ação +% original",
        client.getObjectAsBytes(r -> r.bucket(destination).key("copy.txt")).asUtf8String());
    assertEquals(
        "text/plain; charset=utf-8",
        client.headObject(r -> r.bucket(destination).key("copy.txt")).contentType());
    var conflict =
        ready(new Request(Kind.COPY, source, "original.txt", "", destination, "copy.txt", false));
    assertEquals(State.FAILED, conflict.state());
    assertEquals("CONFLICT", conflict.message());
    var move =
        ready(new Request(Kind.MOVE, source, "original.txt", "", destination, "copy.txt", true));
    assertEquals(State.SUCCEEDED, run(move).state());
    assertEquals(
        404,
        assertThrows(
                S3Exception.class,
                () -> client.headObject(r -> r.bucket(source).key("original.txt")))
            .statusCode());
  }

  @Test
  void conditionalReplacementRejectsChangedDestination() throws Exception {
    String bucket = bucket();
    put(bucket, "test.txt", "old");
    String etag = tools.destinationEtag(bucket, "test.txt");
    var file = Files.createTempFile("stackemu-owned-replacement-", ".txt");
    try {
      Files.writeString(file, "new");
      tools.replaceUpload(bucket, "test.txt", etag, file, "text/plain");
      assertEquals(
          "new", client.getObjectAsBytes(r -> r.bucket(bucket).key("test.txt")).asUtf8String());
      assertThrows(
          OperationException.class,
          () -> tools.replaceUpload(bucket, "test.txt", etag, file, "text/plain"));
    } finally {
      Files.deleteIfExists(file);
    }
  }

  @Test
  void signedDownloadPreservesBodyAndHasBoundedLifetime() throws Exception {
    String bucket = bucket();
    put(bucket, "ação +%.txt", "signed body");
    var signed =
        tools.signDownload(bucket, "ação +%.txt", 15, "attachment; filename=\"download.txt\"");
    assertTrue(signed.url().contains("X-Amz-Expires=900"));
    assertFalse(signed.url().contains("secret"));
    try (var http = HttpClient.newHttpClient()) {
      var response =
          http.send(
              HttpRequest.newBuilder(URI.create(signed.url())).GET().build(),
              HttpResponse.BodyHandlers.ofString());
      assertEquals(200, response.statusCode());
      assertEquals("signed body", response.body());
    }
    assertThrows(
        IllegalArgumentException.class,
        () -> tools.signDownload(bucket, "ação +%.txt", 61, "attachment"));
  }

  @Test
  void bucketNameMismatchDoesNotStartAndEmptyDeleteDoesNotCleanContents() throws Exception {
    String bucket = bucket();
    var empty = ready(request(Kind.DELETE_BUCKET, bucket, ""));
    assertThrows(OperationException.class, () -> operations.start(empty.id(), "wrong"));
    assertEquals(State.READY, operations.get(empty.id()).state());
    put(bucket, "keep.txt", "keep");
    var result = run(empty);
    assertEquals(State.FAILED, result.state());
    assertEquals(
        "keep", client.getObjectAsBytes(r -> r.bucket(bucket).key("keep.txt")).asUtf8String());
  }

  @AfterEach
  void removeOnlyOwnedFixtures() {
    for (String bucket : owned) {
      try {
        for (int page = 0; page < 100; page++) {
          var versions = client.listObjectVersions(r -> r.bucket(bucket).maxKeys(100));
          for (var item : versions.versions())
            client.deleteObject(r -> r.bucket(bucket).key(item.key()).versionId(item.versionId()));
          for (var item : versions.deleteMarkers())
            client.deleteObject(r -> r.bucket(bucket).key(item.key()).versionId(item.versionId()));
          if (versions.versions().isEmpty() && versions.deleteMarkers().isEmpty()) break;
        }
        client.deleteBucket(r -> r.bucket(bucket));
      } catch (S3Exception missing) {
        if (missing.statusCode() != 404) throw missing;
      }
    }
  }
}
