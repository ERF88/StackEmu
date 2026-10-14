package io.github.erf88.stackemu.adapter.aws;

import io.github.erf88.stackemu.application.S3WriteException;
import io.github.erf88.stackemu.application.S3WritePort;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.file.Path;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ApplicationScoped
public class AwsS3WriteAdapter implements S3WritePort {
  private final S3Client client;
  private final String region;

  public AwsS3WriteAdapter(
      S3Client client, @ConfigProperty(name = "quarkus.s3.aws.region") String region) {
    this.client = client;
    this.region = region;
  }

  @Override
  public void createBucket(String bucket) {
    try {
      // The probe also prevents emulator idempotency from presenting an existing bucket as new.
      try {
        client.headBucket(request -> request.bucket(bucket));
        throw new S3WriteException(S3WriteException.Kind.CONFLICT);
      } catch (S3Exception missing) {
        if (missing.statusCode() != 404) {
          throw missing;
        }
      }
      client.createBucket(
          request -> {
            request.bucket(bucket);
            if (!"us-east-1".equals(region)) {
              request.createBucketConfiguration(
                  configuration -> configuration.locationConstraint(region));
            }
          });
    } catch (SdkException failure) {
      throw translate(failure);
    }
  }

  @Override
  public void uploadObject(String bucket, String key, Path file) {
    putNewObject(bucket, key, RequestBody.fromFile(file));
  }

  @Override
  public void createFolderMarker(String bucket, String prefix) {
    putNewObject(bucket, prefix, RequestBody.empty());
  }

  private void putNewObject(String bucket, String key, RequestBody body) {
    try {
      client.putObject(
          request ->
              request
                  .bucket(bucket)
                  .key(key)
                  .contentType("application/octet-stream")
                  .ifNoneMatch("*"),
          body);
    } catch (SdkException failure) {
      throw translate(failure);
    }
  }

  private static S3WriteException translate(SdkException failure) {
    if (failure instanceof SdkClientException) {
      return new S3WriteException(S3WriteException.Kind.UNAVAILABLE);
    }
    if (failure instanceof S3Exception response) {
      return new S3WriteException(
          switch (response.statusCode()) {
            case 409, 412 -> S3WriteException.Kind.CONFLICT;
            case 404 -> S3WriteException.Kind.NOT_FOUND;
            case 403 -> S3WriteException.Kind.FORBIDDEN;
            default -> S3WriteException.Kind.FAILED;
          });
    }
    return new S3WriteException(S3WriteException.Kind.FAILED);
  }
}
