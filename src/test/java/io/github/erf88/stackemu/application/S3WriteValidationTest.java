package io.github.erf88.stackemu.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class S3WriteValidationTest {
  @Test
  void rejectsUnsafeAndReservedBucketNames() {
    for (String name :
        new String[] {
          "ab", "UPPER", "a.b", "-abc", "abc-", "xn--abc", "amzn-s3-demo-test", "a".repeat(64)
        }) {
      assertThrows(
          WriteValidationException.class, () -> S3WriteValidation.validateBucketName(name));
    }
    S3WriteValidation.validateBucketName("valid-bucket-123");
  }

  @Test
  void keyLimitsCountUtf8BytesAndRejectControlCharacters() {
    S3WriteValidation.validateObjectKey("ü".repeat(512));
    assertThrows(
        WriteValidationException.class, () -> S3WriteValidation.validateObjectKey("ü".repeat(513)));
    assertThrows(WriteValidationException.class, () -> S3WriteValidation.validateObjectKey("a\nb"));
  }

  @Test
  void markerIncludesSlashInLimitAndUploadBoundaryIsInclusive() {
    assertEquals("one/two/", S3WriteValidation.folderMarkerKey("one/two"));
    assertThrows(
        WriteValidationException.class, () -> S3WriteValidation.folderMarkerKey("a".repeat(1024)));
    S3WriteValidation.validateUploadSize(1024, 1024);
    assertThrows(
        WriteValidationException.class, () -> S3WriteValidation.validateUploadSize(1025, 1024));
  }
}
