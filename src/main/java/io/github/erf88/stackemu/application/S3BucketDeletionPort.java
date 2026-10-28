package io.github.erf88.stackemu.application;

/** Exact empty-bucket deletion; never removes contained objects or versions. */
public interface S3BucketDeletionPort {
  /** Reads version state and a bounded root listing; performs no mutation. */
  Target inspectBucket(String bucket);

  /** Rechecks safety and requests deletion once, without automatic retry. */
  void deleteBucket(Target target);

  record Target(String bucket) {
    public Target {
      S3WriteValidation.validateBucketName(bucket);
    }
  }
}
