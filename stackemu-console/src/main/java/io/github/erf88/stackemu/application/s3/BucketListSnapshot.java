package io.github.erf88.stackemu.application.s3;

import java.util.List;

public record BucketListSnapshot(List<String> buckets, boolean connected) {
  public BucketListSnapshot {
    buckets = List.copyOf(buckets);
  }
}
