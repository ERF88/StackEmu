package io.github.erf88.stackemu.adapter.aws;

import io.github.erf88.stackemu.application.ObjectDeletionException;
import io.github.erf88.stackemu.application.ObjectDeletionException.Kind;
import io.github.erf88.stackemu.application.S3ObjectDeletionPort;
import io.github.erf88.stackemu.application.S3WriteValidation;
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
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ApplicationScoped
public class AwsS3ObjectDeletionAdapter implements S3ObjectDeletionPort, AutoCloseable {
  private final S3Client client;

  public AwsS3ObjectDeletionAdapter(S3Client configuredClient) {
    var settings = configuredClient.serviceClientConfiguration();
    // SDK 2.54.13 has no per-request retry override. Keep destructive calls isolated.
    this.client =
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

  @PreDestroy
  @Override
  public void close() {
    client.close();
  }

  private static class EmptyDeleteBodyInterceptor implements ExecutionInterceptor {
    @Override
    public Optional<RequestBody> modifyHttpContent(
        Context.ModifyHttpRequest context, ExecutionAttributes attributes) {
      return context.request() instanceof DeleteObjectRequest
          ? Optional.of(RequestBody.empty())
          : context.requestBody();
    }
  }

  @Override
  public Target inspectObject(String bucket, String key) {
    S3WriteValidation.validateBucketName(bucket);
    S3WriteValidation.validateObjectKey(key);
    if (key.endsWith("/")) throw new ObjectDeletionException(Kind.UNSAFE);
    try {
      requireUnversioned(bucket);
      var object = client.headObject(request -> request.bucket(bucket).key(key));
      if ((object.versionId() != null && !object.versionId().equals("null"))
          || object.contentLength() == null
          || object.contentLength() < 0) {
        throw new ObjectDeletionException(Kind.UNSAFE);
      }
      return new Target(bucket, key, object.eTag(), object.contentLength(), object.lastModified());
    } catch (SdkException failure) {
      throw translate(failure, false);
    }
  }

  @Override
  public void deleteObject(Target target) {
    try {
      requireUnversioned(target.bucket());
    } catch (SdkException failure) {
      throw translate(failure, false);
    }
    try {
      var result =
          client.deleteObject(
              DeleteObjectRequest.builder()
                  .bucket(target.bucket())
                  .key(target.key())
                  .ifMatch(target.etag())
                  // URLConnection otherwise replays a bodyless DELETE after a lost response,
                  // independently of SDK retries. Explicit length enables fixed-length mode.
                  .overrideConfiguration(options -> options.putHeader("Content-Length", "0"))
                  .build());
      // Version state can change externally between the check and DELETE. Never claim rollback.
      if (Boolean.TRUE.equals(result.deleteMarker()) || result.versionId() != null) {
        throw new ObjectDeletionException(Kind.UNCERTAIN);
      }
    } catch (SdkException failure) {
      throw translate(failure, true);
    }
  }

  private void requireUnversioned(String bucket) {
    var state = client.getBucketVersioning(request -> request.bucket(bucket));
    // A successful compliant S3 response omits Status only for never-versioned buckets.
    // An unknown enum value, Enabled, Suspended or MFA deletion setting fails closed.
    if ((state.statusAsString() != null && !state.statusAsString().isEmpty())
        || (state.mfaDeleteAsString() != null && !state.mfaDeleteAsString().isEmpty())) {
      throw new ObjectDeletionException(Kind.UNSAFE);
    }
  }

  private static ObjectDeletionException translate(SdkException failure, boolean submitted) {
    if (failure instanceof S3Exception response) {
      return new ObjectDeletionException(
          switch (response.statusCode()) {
            case 403 -> Kind.FORBIDDEN;
            case 404 -> Kind.NOT_FOUND;
            case 409, 412 -> Kind.CONFLICT;
            default -> submitted ? Kind.UNCERTAIN : Kind.UNAVAILABLE;
          });
    }
    return new ObjectDeletionException(submitted ? Kind.UNCERTAIN : Kind.UNAVAILABLE);
  }
}
