package io.github.erf88.stackemu.adapter.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.erf88.stackemu.application.S3ReadPort;
import io.quarkus.test.junit.QuarkusTest;
import java.util.List;
import org.junit.jupiter.api.Test;

@QuarkusTest
class S3BucketPageTest {
  @Test
  void listingUsesOneReadWithoutBucketMetadataAndRetainsEmptyCursorRecovery() {
    int[] listingCalls = {0};
    var reader =
        new S3ReadPort() {
          public BucketDetails getBucketDetails(String bucket) {
            throw new AssertionError("Explorer must not fetch region or tags");
          }

          public ObjectPage listObjects(String bucket, String prefix, String token, int size) {
            assertEquals("bucket", bucket);
            assertEquals("samples/", prefix);
            assertEquals(50, size);
            if (listingCalls[0] == 0) assertEquals(null, token);
            else assertEquals("opaque+/%", token);
            listingCalls[0]++;
            return new ObjectPage(List.of(), List.of(), null);
          }

          public ObjectDetails getObjectDetails(String bucket, String key) {
            throw new AssertionError("No per-object reads for a listing");
          }

          public Download openDownload(String bucket, String key) {
            throw new AssertionError("No download for a listing");
          }
        };
    var resource = new S3Resource(reader);
    resource.configuredPageSize = 50;
    var first =
        (String)
            resource
                .showBucket("bucket", "samples/", null, "en", "dark", "compact", null)
                .getEntity();
    assertEquals(1, listingCalls[0]);
    assertFalse(first.contains("resource-pagination"));
    assertFalse(first.contains("bucket-metadata"));
    assertFalse(first.contains("explorer-help"));
    var cursor =
        (String)
            resource
                .showBucket("bucket", "samples/", "opaque+/%", "en", "dark", "compact", null)
                .getEntity();
    assertEquals(2, listingCalls[0]);
    assertTrue(cursor.contains("resource-pagination"));
    assertTrue(cursor.contains("First page"));
    assertFalse(cursor.contains("id=\"next-page\""));
  }
}
