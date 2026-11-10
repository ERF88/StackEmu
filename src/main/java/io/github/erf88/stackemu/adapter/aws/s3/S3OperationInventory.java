package io.github.erf88.stackemu.adapter.aws.s3;

import io.github.erf88.stackemu.application.s3.OperationException;
import io.github.erf88.stackemu.application.s3.OperationException.Reason;
import io.github.erf88.stackemu.application.s3.OperationStorePort;
import io.github.erf88.stackemu.application.s3.S3OperationPort.Item;
import io.github.erf88.stackemu.application.s3.S3OperationPort.ItemState;
import io.github.erf88.stackemu.application.s3.S3OperationPort.Kind;
import io.github.erf88.stackemu.application.s3.S3OperationPort.Request;
import io.github.erf88.stackemu.application.s3.S3OperationPort.State;
import io.github.erf88.stackemu.application.s3.S3WriteValidation;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

/** Builds the immutable, reviewed target inventory before an S3 mutation can start. */
final class S3OperationInventory {
  private final S3Client client;
  private final OperationStorePort store;
  private final int pageSize;
  private final long maximumTransfer;
  private final String endpointId;

  S3OperationInventory(
      S3Client client,
      OperationStorePort store,
      int pageSize,
      long maximumTransfer,
      String endpointId) {
    this.client = client;
    this.store = store;
    this.pageSize = pageSize;
    this.maximumTransfer = maximumTransfer;
    this.endpointId = endpointId;
  }

  void prepare(String id) {
    var operation = store.get(id);
    var request = operation.request();
    if (!store.matchesEndpoint(id, endpointId)) throw new OperationException(Reason.CHANGED);
    String versioning = versioning(request.bucket());
    String destinationEtag = "";
    if (request.kind() == Kind.COPY || request.kind() == Kind.MOVE) {
      client.headBucket(requestBuilder -> requestBuilder.bucket(request.destinationBucket()));
      var destination = headOrNull(request.destinationBucket(), request.destinationKey(), "");
      if (destination != null) {
        if (!request.replace()) throw new OperationException(Reason.CONFLICT);
        destinationEtag = destination.eTag();
      }
    }
    store.metadata(id, versioning, destinationEtag);
    if (request.kind() == Kind.DELETE_BUCKET) {
      if (!scopeEmpty(request.bucket(), "", versioning))
        throw new OperationException(Reason.CONFLICT);
    } else if (request.kind() == Kind.EMPTY_BUCKET || request.kind() == Kind.DELETE_PREFIX) {
      appendTargets(id, request, versioning);
    } else if (request.kind() == Kind.DELETE_VERSION) {
      appendTargets(id, request, versioning);
      if (store.get(id).total() != 1) throw new OperationException(Reason.MISSING);
    } else {
      var head =
          client.headObject(
              requestBuilder -> requestBuilder.bucket(request.bucket()).key(request.key()));
      if ((request.kind() == Kind.COPY || request.kind() == Kind.MOVE)
          && head.contentLength() > maximumTransfer) throw new OperationException(Reason.CAPACITY);
      store.append(
          id,
          List.of(
              target(
                  1,
                  request.key(),
                  value(head.versionId()),
                  head.eTag(),
                  head.contentLength(),
                  head.lastModified(),
                  false)));
    }
    if (!versioning.equals(versioning(request.bucket())))
      throw new OperationException(Reason.CHANGED);
    store.transition(id, State.PREPARING, State.READY, "");
  }

  String versioning(String bucket) {
    var result = client.getBucketVersioning(requestBuilder -> requestBuilder.bucket(bucket));
    String status = value(result.statusAsString());
    if ((!status.isEmpty() && !status.equals("Enabled") && !status.equals("Suspended"))
        || "Enabled".equals(result.mfaDeleteAsString()))
      throw new OperationException(Reason.UNSUPPORTED);
    return status;
  }

  boolean scopeEmpty(String bucket, String prefix, String versioning) {
    if (versioning.isEmpty()) {
      var result =
          client.listObjectsV2(
              requestBuilder -> requestBuilder.bucket(bucket).prefix(prefix).maxKeys(1));
      return Boolean.FALSE.equals(result.isTruncated())
          && result.contents().isEmpty()
          && result.commonPrefixes().isEmpty();
    }
    var result =
        client.listObjectVersions(
            requestBuilder -> requestBuilder.bucket(bucket).prefix(prefix).maxKeys(1));
    return Boolean.FALSE.equals(result.isTruncated())
        && result.versions().isEmpty()
        && result.deleteMarkers().isEmpty()
        && result.commonPrefixes().isEmpty();
  }

  HeadObjectResponse headOrNull(String bucket, String key, String version) {
    try {
      return client.headObject(
          requestBuilder -> {
            requestBuilder.bucket(bucket).key(key);
            if (!version.isEmpty()) requestBuilder.versionId(version);
          });
    } catch (S3Exception failure) {
      if (failure.statusCode() == 404) return null;
      throw failure;
    }
  }

  void checkTarget(String bucket, Item item) {
    if (item.marker()) {
      if (!exists(bucket, item)) throw new OperationException(Reason.CHANGED);
      return;
    }
    var head = headOrNull(bucket, item.key(), item.version());
    if (head == null
        || head.contentLength() == null
        || item.size() != head.contentLength()
        || !item.etag().equals(head.eTag())
        || !item.modified().truncatedTo(ChronoUnit.SECONDS).equals(head.lastModified()))
      throw new OperationException(Reason.CHANGED);
  }

  boolean exists(String bucket, Item item) {
    if (!item.marker()) return headOrNull(bucket, item.key(), item.version()) != null;
    String keyMarker = null;
    String versionMarker = null;
    for (int page = 0; page < 10000; page++) {
      String savedKeyMarker = keyMarker;
      String savedVersionMarker = versionMarker;
      var result =
          client.listObjectVersions(
              requestBuilder ->
                  requestBuilder
                      .bucket(bucket)
                      .prefix(item.key())
                      .keyMarker(savedKeyMarker)
                      .versionIdMarker(savedVersionMarker)
                      .maxKeys(pageSize));
      if (result.deleteMarkers().stream()
          .anyMatch(
              marker ->
                  item.key().equals(marker.key()) && item.version().equals(marker.versionId())))
        return true;
      if (Boolean.FALSE.equals(result.isTruncated())) return false;
      keyMarker = result.nextKeyMarker();
      versionMarker = result.nextVersionIdMarker();
      if (keyMarker == null
          || (keyMarker.equals(savedKeyMarker)
              && java.util.Objects.equals(savedVersionMarker, versionMarker)))
        throw new OperationException(Reason.UNSUPPORTED);
    }
    throw new OperationException(Reason.CAPACITY);
  }

  private void appendTargets(String id, Request request, String versioning) {
    String token = null;
    String keyMarker = null;
    String versionMarker = null;
    long sequence = 0;
    long deadline = System.nanoTime() + Duration.ofMinutes(10).toNanos();
    do {
      if (store.get(id).state() != State.PREPARING) return;
      if (System.nanoTime() > deadline) throw new OperationException(Reason.CAPACITY);
      List<Item> items = new ArrayList<>();
      if (versioning.isEmpty()) {
        if (request.kind() == Kind.DELETE_VERSION) throw new OperationException(Reason.UNSUPPORTED);
        String cursor = token;
        var result =
            client.listObjectsV2(
                requestBuilder ->
                    requestBuilder
                        .bucket(request.bucket())
                        .prefix(request.key())
                        .continuationToken(cursor)
                        .maxKeys(pageSize));
        if (result.isTruncated() == null
            || result.contents().size() > pageSize
            || !result.commonPrefixes().isEmpty()) throw new OperationException(Reason.UNSUPPORTED);
        for (var item : result.contents()) {
          if (!item.key().startsWith(request.key())) throw new OperationException(Reason.CHANGED);
          items.add(
              target(
                  ++sequence,
                  item.key(),
                  "",
                  item.eTag(),
                  item.size(),
                  item.lastModified(),
                  false));
        }
        token = result.nextContinuationToken();
        if (Boolean.TRUE.equals(result.isTruncated())
            && (token == null || token.isBlank() || token.equals(cursor) || items.isEmpty()))
          throw new OperationException(Reason.UNSUPPORTED);
        if (!Boolean.TRUE.equals(result.isTruncated())) {
          if (token != null) throw new OperationException(Reason.UNSUPPORTED);
          token = null;
        }
      } else {
        String savedKeyMarker = keyMarker;
        String savedVersionMarker = versionMarker;
        var result =
            client.listObjectVersions(
                requestBuilder ->
                    requestBuilder
                        .bucket(request.bucket())
                        .prefix(request.key())
                        .keyMarker(savedKeyMarker)
                        .versionIdMarker(savedVersionMarker)
                        .maxKeys(pageSize));
        if (result.isTruncated() == null
            || result.versions().size() + result.deleteMarkers().size() > pageSize
            || !result.commonPrefixes().isEmpty()) throw new OperationException(Reason.UNSUPPORTED);
        for (var item : result.versions())
          if (included(request, item.key(), item.versionId()))
            items.add(
                target(
                    ++sequence,
                    item.key(),
                    requiredVersion(item.versionId()),
                    item.eTag(),
                    item.size(),
                    item.lastModified(),
                    false));
        for (var item : result.deleteMarkers())
          if (included(request, item.key(), item.versionId()))
            items.add(
                target(
                    ++sequence,
                    item.key(),
                    requiredVersion(item.versionId()),
                    "",
                    0L,
                    item.lastModified(),
                    true));
        keyMarker = result.nextKeyMarker();
        versionMarker = result.nextVersionIdMarker();
        if (Boolean.TRUE.equals(result.isTruncated())
            && (keyMarker == null
                || (keyMarker.equals(savedKeyMarker)
                    && java.util.Objects.equals(savedVersionMarker, versionMarker))))
          throw new OperationException(Reason.UNSUPPORTED);
        token = Boolean.TRUE.equals(result.isTruncated()) ? keyMarker : null;
      }
      store.append(id, items);
    } while (token != null);
  }

  private static boolean included(Request request, String key, String version) {
    if (key == null || !key.startsWith(request.key())) throw new OperationException(Reason.CHANGED);
    return request.kind() != Kind.DELETE_VERSION
        || (key.equals(request.key()) && request.version().equals(version));
  }

  private static String requiredVersion(String version) {
    if (version == null || version.isEmpty() || version.length() > 1024)
      throw new OperationException(Reason.UNSUPPORTED);
    return version;
  }

  private static Item target(
      long sequence,
      String key,
      String version,
      String etag,
      Long size,
      Instant modified,
      boolean marker) {
    S3WriteValidation.validateObjectKey(key);
    if (size == null
        || size < 0
        || modified == null
        || (!marker && (etag == null || !etag.matches("\"[^\"\\p{Cntrl}]{1,200}\""))))
      throw new OperationException(Reason.UNSUPPORTED);
    return new Item(
        sequence, key, version, value(etag), size, modified, marker, ItemState.PENDING, "");
  }

  private static String value(String value) {
    return value == null ? "" : value;
  }
}
