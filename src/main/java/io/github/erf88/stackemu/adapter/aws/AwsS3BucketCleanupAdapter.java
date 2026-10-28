package io.github.erf88.stackemu.adapter.aws;

import io.github.erf88.stackemu.application.BucketCleanupException;
import io.github.erf88.stackemu.application.BucketDeletionException;
import io.github.erf88.stackemu.application.ObjectDeletionException;
import io.github.erf88.stackemu.application.S3BucketCleanupPort;
import io.github.erf88.stackemu.application.S3BucketDeletionPort;
import io.github.erf88.stackemu.application.S3ObjectDeletionPort;
import io.github.erf88.stackemu.application.S3ObjectDeletionPort.Target;
import io.github.erf88.stackemu.application.S3WriteValidation;
import io.github.erf88.stackemu.application.WriteValidationException;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ApplicationScoped
public class AwsS3BucketCleanupAdapter implements S3BucketCleanupPort, AutoCloseable {
  private final S3Client client;
  private final S3ObjectDeletionPort objects;
  private final S3BucketDeletionPort buckets;
  private final Set<String> activeBuckets = ConcurrentHashMap.newKeySet();

  public AwsS3BucketCleanupAdapter(
      S3Client configuredClient, S3ObjectDeletionPort objects, S3BucketDeletionPort buckets) {
    this.objects = objects;
    this.buckets = buckets;
    var settings = configuredClient.serviceClientConfiguration();
    // Read-only bounded inventory client. All mutations use existing no-retry deletion ports.
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
                        .addExecutionInterceptor(new S3DeletionListingInterceptor())
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

  @Override
  public Manifest inspectBucket(String bucket) {
    S3WriteValidation.validateBucketName(bucket);
    long deadline = deadline(7);
    try {
      var manifest = inventory(bucket, deadline);
      for (var entry : manifest.entries()) {
        requireTime(deadline);
        var head = client.headObject(request -> request.bucket(bucket).key(entry.key()));
        if ((head.versionId() != null && !head.versionId().equals("null"))
            || head.contentLength() == null
            || head.contentLength() != entry.size()
            || !entry.etag().equals(head.eTag())
            || !entry.modified().truncatedTo(ChronoUnit.SECONDS).equals(head.lastModified())) {
          throw new BucketCleanupException(Failure.UNSAFE);
        }
      }
      requireTime(deadline);
      requireUnversioned(bucket);
      return manifest;
    } catch (ObjectDeletionException | WriteValidationException | SdkException failure) {
      throw new BucketCleanupException(reason(failure));
    }
  }

  @Override
  public Result emptyAndDelete(Manifest manifest) {
    Map<String, Outcome> outcomes = new LinkedHashMap<>();
    manifest.entries().forEach(entry -> outcomes.put(entry.key(), Outcome.NOT_ATTEMPTED));
    if (!activeBuckets.add(manifest.bucket()))
      return result(manifest, outcomes, false, Failure.CHANGED);
    String currentKey = null;
    long deadline = deadline(20);
    try {
      var ordered =
          manifest.entries().stream()
              .sorted(
                  Comparator.comparing((Target entry) -> entry.key().endsWith("/"))
                      .thenComparing(
                          Comparator.comparingInt((Target entry) -> entry.key().length())
                              .reversed())
                      .thenComparing(Target::key))
              .toList();
      for (var entry : ordered) {
        requireTime(deadline);
        var remaining =
            new Manifest(
                manifest.bucket(),
                manifest.entries().stream()
                    .filter(target -> outcomes.get(target.key()) != Outcome.DELETED)
                    .toList());
        if (!remaining.equals(inventory(manifest.bucket(), deadline)))
          throw new BucketCleanupException(Failure.CHANGED);
        currentKey = entry.key();
        var current = objects.inspectObject(manifest.bucket(), entry.key());
        if (!current.bucket().equals(entry.bucket())
            || !current.key().equals(entry.key())
            || !current.etag().equals(entry.etag())
            || current.size() != entry.size()
            || current.modified() == null
            || !current
                .modified()
                .truncatedTo(ChronoUnit.SECONDS)
                .equals(entry.modified().truncatedTo(ChronoUnit.SECONDS)))
          throw new BucketCleanupException(Failure.CHANGED);
        requireTime(deadline);
        objects.deleteObject(entry);
        outcomes.put(entry.key(), Outcome.DELETED);
        currentKey = null;
      }
      requireTime(deadline);
      if (!inventory(manifest.bucket(), deadline).entries().isEmpty())
        throw new BucketCleanupException(Failure.CHANGED);
      requireTime(deadline);
      buckets.deleteBucket(new S3BucketDeletionPort.Target(manifest.bucket()));
      return result(manifest, outcomes, true, Failure.NONE);
    } catch (BucketCleanupException
        | ObjectDeletionException
        | BucketDeletionException
        | WriteValidationException
        | SdkException failure) {
      Failure reason = reason(failure);
      if (currentKey != null)
        outcomes.put(currentKey, reason == Failure.UNCERTAIN ? Outcome.UNCERTAIN : Outcome.FAILED);
      return result(manifest, outcomes, false, reason);
    } finally {
      activeBuckets.remove(manifest.bucket());
    }
  }

  private Manifest inventory(String bucket, long deadline) {
    requireTime(deadline);
    requireUnversioned(bucket);
    List<Target> entries = new ArrayList<>();
    Set<String> tokens = new HashSet<>();
    String token = null;
    for (int page = 0; page < 3; page++) {
      requireTime(deadline);
      String cursor = token;
      var listing =
          client.listObjectsV2(
              request -> request.bucket(bucket).maxKeys(10).continuationToken(cursor));
      if (listing.isTruncated() == null
          || listing.keyCount() == null
          || listing.keyCount() != listing.contents().size()
          || listing.contents().size() > 10
          || !listing.commonPrefixes().isEmpty()) throw new BucketCleanupException(Failure.UNSAFE);
      for (var object : listing.contents()) {
        if (object.size() == null || object.size() < 0 || object.lastModified() == null)
          throw new BucketCleanupException(Failure.UNSAFE);
        entries.add(
            new Target(bucket, object.key(), object.eTag(), object.size(), object.lastModified()));
        if (entries.size() > MAX_ENTRIES) throw new BucketCleanupException(Failure.LIMIT);
      }
      token = listing.nextContinuationToken();
      if (!listing.isTruncated()) {
        if (token != null) throw new BucketCleanupException(Failure.UNSAFE);
        requireTime(deadline);
        requireUnversioned(bucket);
        return new Manifest(bucket, entries);
      }
      if (token == null || token.isBlank() || !tokens.add(token) || listing.contents().isEmpty())
        throw new BucketCleanupException(Failure.UNSAFE);
    }
    throw new BucketCleanupException(Failure.LIMIT);
  }

  private void requireUnversioned(String bucket) {
    var state = client.getBucketVersioning(request -> request.bucket(bucket));
    if ((state.statusAsString() != null && !state.statusAsString().isEmpty())
        || (state.mfaDeleteAsString() != null && !state.mfaDeleteAsString().isEmpty()))
      throw new BucketCleanupException(Failure.UNSAFE);
  }

  private static long deadline(int seconds) {
    return System.nanoTime() + Duration.ofSeconds(seconds).toNanos();
  }

  private static void requireTime(long deadline) {
    if (System.nanoTime() - deadline >= 0) throw new BucketCleanupException(Failure.LIMIT);
  }

  private static Result result(
      Manifest manifest, Map<String, Outcome> outcomes, boolean deleted, Failure failure) {
    return new Result(
        manifest,
        outcomes.entrySet().stream()
            .map(entry -> new ItemResult(entry.getKey(), entry.getValue()))
            .toList(),
        deleted,
        failure);
  }

  private static Failure reason(RuntimeException failure) {
    if (failure instanceof WriteValidationException) return Failure.UNSAFE;
    if (failure instanceof BucketCleanupException cleanup) return cleanup.failure();
    if (failure instanceof ObjectDeletionException object)
      return switch (object.kind()) {
        case UNSAFE -> Failure.UNSAFE;
        case CONFLICT -> Failure.CHANGED;
        case NOT_FOUND -> Failure.NOT_FOUND;
        case FORBIDDEN -> Failure.FORBIDDEN;
        case UNAVAILABLE -> Failure.UNAVAILABLE;
        case UNCERTAIN -> Failure.UNCERTAIN;
      };
    if (failure instanceof BucketDeletionException bucket)
      return switch (bucket.kind()) {
        case UNSAFE -> Failure.UNSAFE;
        case NOT_EMPTY -> Failure.CHANGED;
        case NOT_FOUND -> Failure.NOT_FOUND;
        case FORBIDDEN -> Failure.FORBIDDEN;
        case UNAVAILABLE -> Failure.UNAVAILABLE;
        case UNCERTAIN -> Failure.UNCERTAIN;
      };
    if (failure instanceof S3Exception response)
      return switch (response.statusCode()) {
        case 403 -> Failure.FORBIDDEN;
        case 404 -> Failure.NOT_FOUND;
        case 409, 412 -> Failure.CHANGED;
        default -> Failure.UNAVAILABLE;
      };
    return Failure.UNAVAILABLE;
  }
}
