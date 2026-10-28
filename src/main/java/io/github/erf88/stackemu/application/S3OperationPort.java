package io.github.erf88.stackemu.application;

import java.time.Instant;
import java.util.List;

/** Durable, reviewed S3 mutations. Preparation reads S3; only start/resume authorize writes. */
public interface S3OperationPort {
  enum Kind {
    DELETE_BUCKET,
    EMPTY_BUCKET,
    DELETE_PREFIX,
    DELETE_OBJECT,
    DELETE_VERSION,
    COPY,
    MOVE
  }

  enum State {
    PREPARING,
    READY,
    RUNNING,
    STOP_REQUESTED,
    INTERRUPTED,
    SUCCEEDED,
    PARTIAL,
    FAILED,
    UNCERTAIN
  }

  enum ItemState {
    PENDING,
    IN_FLIGHT,
    DONE,
    FAILED,
    UNCERTAIN
  }

  record Request(
      Kind kind,
      String bucket,
      String key,
      String version,
      String destinationBucket,
      String destinationKey,
      boolean replace) {
    public Request {
      S3WriteValidation.validateBucketName(bucket);
      key = key == null ? "" : key;
      version = version == null ? "" : version;
      destinationBucket = destinationBucket == null ? "" : destinationBucket;
      destinationKey = destinationKey == null ? "" : destinationKey;
      if (kind != Kind.EMPTY_BUCKET && kind != Kind.DELETE_BUCKET)
        S3WriteValidation.validateObjectKey(key);
      if (kind == Kind.DELETE_PREFIX && !key.endsWith("/"))
        throw new IllegalArgumentException("prefix");
      if (kind == Kind.DELETE_VERSION && (version.isEmpty() || version.length() > 1024))
        throw new IllegalArgumentException("version");
      if (kind == Kind.COPY || kind == Kind.MOVE) {
        S3WriteValidation.validateBucketName(destinationBucket);
        S3WriteValidation.validateObjectKey(destinationKey);
        if (bucket.equals(destinationBucket) && key.equals(destinationKey))
          throw new IllegalArgumentException("destination");
        String original = key.substring(key.lastIndexOf('/') + 1);
        int extension = original.lastIndexOf('.');
        if (extension > 0 && !destinationKey.endsWith(original.substring(extension)))
          throw new IllegalArgumentException("extension");
      }
    }
  }

  record Operation(
      String id,
      Request request,
      State state,
      Instant created,
      Instant updated,
      long total,
      long done,
      String message,
      String versioning,
      String destinationEtag) {
    public boolean active() {
      return state == State.PREPARING || state == State.RUNNING || state == State.STOP_REQUESTED;
    }

    public boolean resumable() {
      return state == State.INTERRUPTED || state == State.UNCERTAIN;
    }

    public boolean confirmable() {
      return state == State.READY;
    }

    public boolean bucketConfirmation() {
      return request.kind() == Kind.EMPTY_BUCKET || request.kind() == Kind.DELETE_BUCKET;
    }
  }

  record Item(
      long sequence,
      String key,
      String version,
      String etag,
      long size,
      Instant modified,
      boolean marker,
      ItemState state,
      String message) {}

  String prepare(Request request);

  Operation get(String id);

  List<Operation> list(int page);

  List<Item> items(String id, int page);

  Operation start(String id, String bucketName);

  Operation interrupt(String id);

  Operation resume(String id);
}
