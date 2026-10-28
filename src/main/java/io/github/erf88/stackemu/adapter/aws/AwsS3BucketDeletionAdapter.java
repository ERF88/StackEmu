package io.github.erf88.stackemu.adapter.aws;

import io.github.erf88.stackemu.application.BucketDeletionException;
import io.github.erf88.stackemu.application.BucketDeletionException.Kind;
import io.github.erf88.stackemu.application.S3BucketDeletionPort;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.util.Optional;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ApplicationScoped
public class AwsS3BucketDeletionAdapter implements S3BucketDeletionPort, AutoCloseable {
  private final S3Client client;

  public AwsS3BucketDeletionAdapter(S3Client configuredClient) {
    var settings = configuredClient.serviceClientConfiguration();
    // ADR-0026: isolate no-retry mutation transport without changing the managed client.
    client =
        S3Client.builder()
            .endpointOverride(settings.endpointOverride().orElseThrow())
            .region(settings.region())
            .credentialsProvider(settings.credentialsProvider())
            .forcePathStyle(true)
            .overrideConfiguration(
                options ->
                    options
                        .retryStrategy(AwsRetryStrategy.doNotRetry())
                        .addExecutionInterceptor(new EmptyDeleteBodyInterceptor())
                        .apiCallTimeout(Duration.ofSeconds(3))
                        .apiCallAttemptTimeout(Duration.ofSeconds(2)))
            .httpClientBuilder(
                UrlConnectionHttpClient.builder()
                    .connectionTimeout(Duration.ofSeconds(1))
                    .socketTimeout(Duration.ofSeconds(1)))
            .build();
  }

  private static class EmptyDeleteBodyInterceptor implements ExecutionInterceptor {
    @Override
    public Optional<RequestBody> modifyHttpContent(
        Context.ModifyHttpRequest context, ExecutionAttributes attributes) {
      return context.request() instanceof DeleteBucketRequest
          ? Optional.of(RequestBody.empty())
          : context.requestBody();
    }
  }

  @Override
  public Target inspectBucket(String bucket) {
    Target target = new Target(bucket);
    try {
      requireEmptyUnversioned(bucket);
      return target;
    } catch (SdkException failure) {
      throw translate(failure, false);
    }
  }

  @Override
  public void deleteBucket(Target target) {
    inspectBucket(target.bucket());
    try {
      var result =
          client.deleteBucket(
              DeleteBucketRequest.builder()
                  .bucket(target.bucket())
                  .overrideConfiguration(options -> options.putHeader("Content-Length", "0"))
                  .build());
      if (result.sdkHttpResponse().statusCode() != 204) {
        throw new BucketDeletionException(Kind.UNCERTAIN);
      }
    } catch (SdkException failure) {
      throw translate(failure, true);
    }
  }

  private void requireEmptyUnversioned(String bucket) {
    var state = client.getBucketVersioning(request -> request.bucket(bucket));
    if ((state.statusAsString() != null && !state.statusAsString().isEmpty())
        || (state.mfaDeleteAsString() != null && !state.mfaDeleteAsString().isEmpty())) {
      throw new BucketDeletionException(Kind.UNSAFE);
    }
    var listing = client.listObjectsV2(request -> request.bucket(bucket).maxKeys(1));
    if (!listing.contents().isEmpty()
        || !listing.commonPrefixes().isEmpty()
        || Boolean.TRUE.equals(listing.isTruncated())
        || (listing.keyCount() != null && listing.keyCount() > 0)) {
      throw new BucketDeletionException(Kind.NOT_EMPTY);
    }
    if (!Boolean.FALSE.equals(listing.isTruncated())
        || listing.keyCount() == null
        || listing.keyCount() != 0) {
      throw new BucketDeletionException(Kind.UNSAFE);
    }
  }

  private static BucketDeletionException translate(SdkException failure, boolean submitted) {
    if (failure instanceof S3Exception response) {
      return new BucketDeletionException(
          switch (response.statusCode()) {
            case 403 -> Kind.FORBIDDEN;
            case 404 -> Kind.NOT_FOUND;
            case 409 -> Kind.NOT_EMPTY;
            default -> submitted ? Kind.UNCERTAIN : Kind.UNAVAILABLE;
          });
    }
    return new BucketDeletionException(submitted ? Kind.UNCERTAIN : Kind.UNAVAILABLE);
  }

  @PreDestroy
  @Override
  public void close() {
    client.close();
  }
}
