package io.github.erf88.stackemu.application;
import java.util.List;
public record BucketSnapshot(List<String> buckets, boolean connected) {
    public BucketSnapshot { buckets = List.copyOf(buckets); }
}
