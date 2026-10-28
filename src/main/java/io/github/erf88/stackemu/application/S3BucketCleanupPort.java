package io.github.erf88.stackemu.application;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/** Explicit reviewed cleanup, never an automatic fallback for empty-bucket deletion. */
public interface S3BucketCleanupPort {
  int MAX_ENTRIES = 25;

  Manifest inspectBucket(String bucket);

  Result emptyAndDelete(Manifest manifest);

  enum Failure {
    NONE,
    UNSAFE,
    LIMIT,
    CHANGED,
    FORBIDDEN,
    NOT_FOUND,
    UNAVAILABLE,
    UNCERTAIN
  }

  enum Outcome {
    DELETED,
    FAILED,
    UNCERTAIN,
    NOT_ATTEMPTED
  }

  record Manifest(String bucket, List<S3ObjectDeletionPort.Target> entries) {
    public Manifest {
      S3WriteValidation.validateBucketName(bucket);
      entries =
          entries.stream().sorted(Comparator.comparing(S3ObjectDeletionPort.Target::key)).toList();
      var keys = new HashSet<String>();
      if (entries.size() > MAX_ENTRIES) throw new BucketCleanupException(Failure.LIMIT);
      for (var entry : entries) {
        if (!entry.bucket().equals(bucket) || entry.size() < 0 || !keys.add(entry.key()))
          throw new BucketCleanupException(Failure.UNSAFE);
      }
    }
  }

  record ItemResult(String key, Outcome outcome) {}

  record Result(Manifest manifest, List<ItemResult> items, boolean bucketDeleted, Failure failure) {
    public Result {
      items = List.copyOf(items);
    }
  }
}
