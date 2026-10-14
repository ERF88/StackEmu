package io.github.erf88.stackemu.application;

import java.nio.charset.StandardCharsets;

/** Deliberately conservative creation policy; existing read-only names remain unaffected. */
public final class S3WriteValidation {
  private S3WriteValidation() {}

  public static void validateBucketName(String bucket) {
    if (bucket == null
        || bucket.length() < 3
        || bucket.length() > 63
        || !bucket.matches("[a-z0-9][a-z0-9-]*[a-z0-9]")
        || bucket.startsWith("xn--")
        || bucket.startsWith("sthree-")
        || bucket.startsWith("amzn-s3-demo-")
        || bucket.endsWith("-s3alias")
        || bucket.endsWith("--ol-s3")
        || bucket.endsWith("--x-s3")
        || bucket.endsWith("--table-s3")
        || bucket.endsWith("-an")) {
      throw new WriteValidationException("bucket", "invalidBucketName");
    }
  }

  public static void validateObjectKey(String key) {
    if (key == null
        || key.isBlank()
        || key.getBytes(StandardCharsets.UTF_8).length > 1024
        || key.codePoints().anyMatch(Character::isISOControl)) {
      throw new WriteValidationException("key", "invalidObjectKey");
    }
  }

  public static String folderMarkerKey(String prefix) {
    if (prefix == null || prefix.isBlank()) {
      throw new WriteValidationException("prefix", "invalidPrefix");
    }
    String marker = prefix.endsWith("/") ? prefix : prefix + "/";
    try {
      validateObjectKey(marker);
    } catch (WriteValidationException invalid) {
      throw new WriteValidationException("prefix", "invalidPrefix");
    }
    return marker;
  }

  public static void validateUploadSize(long size, long maximum) {
    if (size < 0 || maximum < 1 || size > maximum) {
      throw new WriteValidationException("file", "uploadLimit");
    }
  }

  public static String uploadObjectKey(String prefix, String filename) {
    if (filename == null) {
      throw new WriteValidationException("file", "invalidFilename");
    }
    String normalized = filename.replace('\\', '/');
    String basename = normalized.substring(normalized.lastIndexOf('/') + 1);
    if (basename.isBlank()
        || basename.equals(".")
        || basename.equals("..")
        || basename.codePoints().anyMatch(Character::isISOControl)) {
      throw new WriteValidationException("file", "invalidFilename");
    }
    String parent = prefix == null || prefix.isEmpty() ? "" : folderMarkerKey(prefix);
    String key = parent + basename;
    try {
      validateObjectKey(key);
    } catch (WriteValidationException invalid) {
      throw new WriteValidationException("file", "invalidFilename");
    }
    return key;
  }
}
