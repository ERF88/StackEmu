package io.github.erf88.stackemu.adapter.aws.s3;

import io.github.erf88.stackemu.application.s3.BucketListPort;
import io.github.erf88.stackemu.application.s3.BucketListSnapshot;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;

@ApplicationScoped
public class AwsBucketListAdapter implements BucketListPort {
  private final S3Client client;

  public AwsBucketListAdapter(S3Client client) {
    this.client = client;
  }

  @Override
  public BucketListSnapshot listBuckets() {
    try {
      return new BucketListSnapshot(
          client.listBuckets().buckets().stream().map(b -> b.name()).sorted().toList(), true);
    } catch (SdkException ignored) {
      return new BucketListSnapshot(List.of(), false);
    }
  }
}
