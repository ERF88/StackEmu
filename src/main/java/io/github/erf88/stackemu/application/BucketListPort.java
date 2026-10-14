package io.github.erf88.stackemu.application;

public interface BucketListPort {
  /**
   * Performs a remote listing; a failed probe returns a disconnected snapshot, not an empty
   * success.
   */
  BucketListSnapshot listBuckets();
}
