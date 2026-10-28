package io.github.erf88.stackemu.application;

import java.time.Instant;

/** Single exact-key deletion; never traverses prefixes, buckets or version histories. */
public interface S3ObjectDeletionPort {
  /** Reads version state and object metadata; performs no mutation. */
  Target inspectObject(String bucket, String key);

  /**
   * Rechecks version state and conditionally deletes the reviewed ETag, without automatic retry.
   */
  void deleteObject(Target target);

  record Target(String bucket, String key, String etag, long size, Instant modified) {
    public Target {
      S3WriteValidation.validateBucketName(bucket);
      S3WriteValidation.validateObjectKey(key);
      if ((key.endsWith("/") && size != 0)
          || etag == null
          || !etag.matches("\"[^\"\\p{Cntrl}]{1,200}\"")) {
        throw new ObjectDeletionException(ObjectDeletionException.Kind.UNSAFE);
      }
    }
  }
}
