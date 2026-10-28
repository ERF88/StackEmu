package io.github.erf88.stackemu.application;

public class BucketCleanupException extends RuntimeException {
  private final S3BucketCleanupPort.Failure failure;

  public BucketCleanupException(S3BucketCleanupPort.Failure failure) {
    super(failure.name());
    this.failure = failure;
  }

  public S3BucketCleanupPort.Failure failure() {
    return failure;
  }
}
