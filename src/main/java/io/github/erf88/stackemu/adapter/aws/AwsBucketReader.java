package io.github.erf88.stackemu.adapter.aws;

import io.github.erf88.stackemu.application.BucketReader;
import io.github.erf88.stackemu.application.BucketSnapshot;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;

@ApplicationScoped
public class AwsBucketReader implements BucketReader {
  @Inject S3Client client;

  public BucketSnapshot read() {
    try {
      return new BucketSnapshot(
          client.listBuckets().buckets().stream().map(b -> b.name()).sorted().toList(), true);
    } catch (SdkException ignored) {
      return new BucketSnapshot(List.of(), false);
    }
  }
}
