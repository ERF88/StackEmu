package io.github.erf88.stackemu.adapter.aws;

import io.github.erf88.stackemu.application.OperationException;
import io.github.erf88.stackemu.application.OperationException.Reason;
import io.github.erf88.stackemu.application.S3ObjectToolsPort;
import io.github.erf88.stackemu.application.S3WriteValidation;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@ApplicationScoped
public class AwsS3ObjectToolsAdapter implements S3ObjectToolsPort, AutoCloseable {
  private final S3Client client;
  private final S3Presigner presigner;
  private final IdentityProvider<? extends AwsCredentialsIdentity> credentials;

  public AwsS3ObjectToolsAdapter(
      S3Client configured,
      @ConfigProperty(name = "stackemu.operations.call-timeout", defaultValue = "PT30S")
          Duration callTimeout) {
    if (callTimeout.compareTo(Duration.ofSeconds(1)) < 0
        || callTimeout.compareTo(Duration.ofMinutes(5)) > 0)
      throw new IllegalArgumentException("call timeout");
    var settings = configured.serviceClientConfiguration();
    credentials = settings.credentialsProvider();
    client =
        S3Client.builder()
            .endpointOverride(settings.endpointOverride().orElseThrow())
            .region(settings.region())
            .credentialsProvider(credentials)
            .forcePathStyle(true)
            .overrideConfiguration(
                o ->
                    o.retryStrategy(AwsRetryStrategy.doNotRetry())
                        .apiCallTimeout(callTimeout)
                        .apiCallAttemptTimeout(callTimeout)
                        .addExecutionInterceptor(new S3DeletionListingInterceptor(1048576)))
            .build();
    presigner =
        S3Presigner.builder()
            .endpointOverride(settings.endpointOverride().orElseThrow())
            .region(settings.region())
            .credentialsProvider(credentials)
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build();
  }

  private static void validate(String bucket, String key) {
    S3WriteValidation.validateBucketName(bucket);
    S3WriteValidation.validateObjectKey(key);
  }

  @Override
  public SignedDownload signDownload(String bucket, String key, int minutes, String disposition) {
    validate(bucket, key);
    if (minutes < 1 || minutes > 60) throw new IllegalArgumentException("duration");
    try {
      client.headObject(r -> r.bucket(bucket).key(key));
      Instant now = Instant.now();
      Duration lifetime = Duration.ofMinutes(minutes);
      var expires =
          credentials
              .resolveIdentity(ResolveIdentityRequest.builder().build())
              .join()
              .expirationTime();
      if (expires.isPresent()) {
        Duration remaining = Duration.between(now, expires.get()).minusSeconds(5);
        if (remaining.compareTo(lifetime) < 0) lifetime = remaining;
      }
      if (lifetime.isNegative() || lifetime.isZero())
        throw new OperationException(Reason.UNAVAILABLE);
      Duration duration = lifetime;
      var request =
          presigner.presignGetObject(
              r ->
                  r.signatureDuration(duration)
                      .getObjectRequest(
                          o -> o.bucket(bucket).key(key).responseContentDisposition(disposition)));
      return new SignedDownload(request.url().toExternalForm(), request.expiration());
    } catch (S3Exception failure) {
      throw translate(failure);
    } catch (SdkClientException failure) {
      throw new OperationException(Reason.UNAVAILABLE);
    }
  }

  @Override
  public boolean isVersionDeletionAvailable(String bucket) {
    S3WriteValidation.validateBucketName(bucket);
    try {
      String status = client.getBucketVersioning(r -> r.bucket(bucket)).statusAsString();
      return "Enabled".equals(status) || "Suspended".equals(status);
    } catch (S3Exception failure) {
      throw translate(failure);
    } catch (SdkClientException failure) {
      throw new OperationException(Reason.UNAVAILABLE);
    }
  }

  @Override
  public VersionPage versions(String bucket, String key, String keyMarker, String versionMarker) {
    S3WriteValidation.validateBucketName(bucket);
    if (key == null) key = "";
    if (!key.isEmpty()) S3WriteValidation.validateObjectKey(key);
    if (keyMarker != null && keyMarker.length() > 2048
        || versionMarker != null && versionMarker.length() > 1024)
      throw new IllegalArgumentException("cursor");
    try {
      String prefix = key;
      var response =
          client.listObjectVersions(
              r ->
                  r.bucket(bucket)
                      .prefix(prefix)
                      .keyMarker(emptyToNull(keyMarker))
                      .versionIdMarker(emptyToNull(versionMarker))
                      .maxKeys(50));
      var entries = new ArrayList<Version>();
      for (var version : response.versions())
        if (key.isEmpty() || version.key().equals(key))
          entries.add(
              new Version(
                  version.key(),
                  version.versionId(),
                  false,
                  Boolean.TRUE.equals(version.isLatest()),
                  version.lastModified()));
      for (var version : response.deleteMarkers())
        if (key.isEmpty() || version.key().equals(key))
          entries.add(
              new Version(
                  version.key(),
                  version.versionId(),
                  true,
                  Boolean.TRUE.equals(version.isLatest()),
                  version.lastModified()));
      if (Boolean.TRUE.equals(response.isTruncated())
          && (response.nextKeyMarker() == null
              || response.nextKeyMarker().equals(keyMarker)
                  && java.util.Objects.equals(response.nextVersionIdMarker(), versionMarker)))
        throw new OperationException(Reason.UNSUPPORTED);
      return new VersionPage(
          java.util.List.copyOf(entries),
          Boolean.TRUE.equals(response.isTruncated()) ? response.nextKeyMarker() : "",
          Boolean.TRUE.equals(response.isTruncated()) && response.nextVersionIdMarker() != null
              ? response.nextVersionIdMarker()
              : "");
    } catch (S3Exception failure) {
      throw translate(failure);
    } catch (SdkClientException failure) {
      throw new OperationException(Reason.UNAVAILABLE);
    }
  }

  @Override
  public String destinationEtag(String bucket, String key) {
    validate(bucket, key);
    try {
      return client.headObject(r -> r.bucket(bucket).key(key)).eTag();
    } catch (S3Exception failure) {
      throw translate(failure);
    }
  }

  @Override
  public void replaceUpload(String bucket, String key, String etag, Path file, String contentType) {
    validate(bucket, key);
    if (etag == null || etag.isBlank()) throw new IllegalArgumentException("etag");
    try {
      client.putObject(
          r -> r.bucket(bucket).key(key).ifMatch(etag).contentType(contentType),
          RequestBody.fromFile(file));
    } catch (S3Exception failure) {
      throw translate(failure);
    } catch (SdkClientException failure) {
      throw new OperationException(Reason.UNCERTAIN);
    }
  }

  private static String emptyToNull(String value) {
    return value == null || value.isEmpty() ? null : value;
  }

  private static OperationException translate(S3Exception failure) {
    return new OperationException(
        switch (failure.statusCode()) {
          case 403 -> Reason.FORBIDDEN;
          case 404 -> Reason.MISSING;
          case 409, 412 -> Reason.CONFLICT;
          case 400, 405, 501 -> Reason.UNSUPPORTED;
          default -> Reason.UNAVAILABLE;
        });
  }

  @PreDestroy
  @Override
  public void close() {
    client.close();
    presigner.close();
  }
}
