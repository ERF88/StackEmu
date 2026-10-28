package io.github.erf88.stackemu.adapter.aws;

import io.github.erf88.stackemu.application.OperationException;
import io.github.erf88.stackemu.application.OperationException.Reason;
import io.github.erf88.stackemu.application.OperationStorePort;
import io.github.erf88.stackemu.application.S3OperationPort;
import io.github.erf88.stackemu.application.S3WriteValidation;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

/** Owns bounded asynchronous S3 execution and its independently committed local journal. */
@ApplicationScoped
public class AwsS3OperationAdapter implements S3OperationPort, AutoCloseable {
  private final OperationStorePort store;
  private final S3Client client;
  private final int pageSize;
  private final long maximumTransfer;
  private final ExecutorService workers;
  private boolean recovered;
  private final String endpointId;

  public AwsS3OperationAdapter(
      S3Client configured,
      OperationStorePort store,
      @ConfigProperty(name = "stackemu.operations.page-size", defaultValue = "100") int pageSize,
      @ConfigProperty(name = "stackemu.operations.max-transfer", defaultValue = "67108864")
          long maximumTransfer,
      @ConfigProperty(name = "stackemu.operations.call-timeout", defaultValue = "PT30S")
          Duration callTimeout) {
    if (callTimeout.compareTo(Duration.ofSeconds(1)) < 0
        || callTimeout.compareTo(Duration.ofMinutes(5)) > 0)
      throw new IllegalArgumentException("call timeout");
    this.store = store;
    this.pageSize = Math.clamp(pageSize, 1, 100);
    this.maximumTransfer = maximumTransfer;
    if (maximumTransfer < 1) throw new IllegalArgumentException("transfer limit");
    var settings = configured.serviceClientConfiguration();
    try {
      endpointId =
          java.util.HexFormat.of()
              .formatHex(
                  java.security.MessageDigest.getInstance("SHA-256")
                      .digest(
                          (settings.endpointOverride().orElseThrow() + "|" + settings.region().id())
                              .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException unavailable) {
      throw new IllegalStateException(unavailable);
    }
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
                        .addExecutionInterceptor(new S3EmptyDeleteBodyInterceptor())
                        .addExecutionInterceptor(new S3DeletionListingInterceptor(1048576))
                        .apiCallTimeout(callTimeout)
                        .apiCallAttemptTimeout(callTimeout))
            .httpClientBuilder(
                UrlConnectionHttpClient.builder()
                    .connectionTimeout(Duration.ofSeconds(2))
                    .socketTimeout(Duration.ofSeconds(5)))
            .build();
    workers =
        new ThreadPoolExecutor(
            1,
            1,
            0,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(32),
            Thread.ofPlatform().name("stackemu-operations-", 0).factory(),
            new ThreadPoolExecutor.AbortPolicy());
  }

  private synchronized void initialize() {
    if (!recovered) {
      store.recover();
      recovered = true;
    }
  }

  @Override
  public String prepare(Request request) {
    initialize();
    String id = store.create(request);
    store.bindEndpoint(id, endpointId);
    submit(id, () -> prepareInventory(id));
    return id;
  }

  @Override
  public Operation get(String id) {
    initialize();
    return store.get(id);
  }

  @Override
  public List<Operation> list(int page) {
    initialize();
    return store.list(page);
  }

  @Override
  public List<Item> items(String id, int page) {
    initialize();
    store.get(id);
    return store.items(id, Math.max(0L, Math.min(page, 100000)) * 50, 50);
  }

  @Override
  public synchronized Operation start(String id, String bucketName) {
    var operation = get(id);
    if (!store.matchesEndpoint(id, endpointId)) throw new OperationException(Reason.CHANGED);
    if (operation.bucketConfirmation() && !operation.request().bucket().equals(bucketName))
      throw new OperationException(Reason.NAME);
    if (operation.state() != State.READY) return operation;
    if (!store.claim(id)) throw new OperationException(Reason.CONFLICT);
    if (!store.transition(id, State.READY, State.RUNNING, "")) {
      store.release(id);
      return get(id);
    }
    submit(id, () -> execute(id));
    return get(id);
  }

  @Override
  public Operation interrupt(String id) {
    var operation = get(id);
    if (operation.state() == State.PREPARING)
      store.transition(id, State.PREPARING, State.FAILED, "STOPPED");
    else store.transition(id, State.RUNNING, State.STOP_REQUESTED, "STOPPED");
    return get(id);
  }

  @Override
  public synchronized Operation resume(String id) {
    var operation = get(id);
    if (!store.matchesEndpoint(id, endpointId)) throw new OperationException(Reason.CHANGED);
    if (!operation.resumable()) return operation;
    // No ambiguous request is replayed. Reconciliation is read-only and per reviewed target.
    long cursor = 0;
    while (true) {
      var items = store.items(id, cursor, pageSize);
      if (items.isEmpty()) break;
      for (var item : items) {
        cursor = item.sequence();
        if (item.state() == ItemState.UNCERTAIN) {
          if (operation.request().kind() == Kind.COPY
              || operation.request().kind() == Kind.MOVE
              || operation.request().kind() == Kind.DELETE_OBJECT
                  && !operation.versioning().isEmpty())
            throw new OperationException(Reason.UNCERTAIN);
          if (exists(operation.request().bucket(), item))
            throw new OperationException(Reason.UNCERTAIN);
          store.itemState(id, item.sequence(), ItemState.DONE, "RECONCILED_ABSENT");
        }
      }
    }
    if ("FINALIZING".equals(operation.message())) {
      try {
        client.headBucket(r -> r.bucket(operation.request().bucket()));
        throw new OperationException(Reason.UNCERTAIN);
      } catch (S3Exception failure) {
        if (failure.statusCode() != 404) throw translate(failure);
        store.finish(id, State.SUCCEEDED, "RECONCILED_ABSENT");
        return get(id);
      }
    }
    if (!store.claim(id)) throw new OperationException(Reason.CONFLICT);
    if (store.transition(id, operation.state(), State.RUNNING, "")) submit(id, () -> execute(id));
    else store.release(id);
    return get(id);
  }

  private void submit(String id, Runnable task) {
    try {
      workers.submit(task);
    } catch (java.util.concurrent.RejectedExecutionException full) {
      store.finish(id, State.FAILED, "CAPACITY");
      store.release(id);
      throw new OperationException(Reason.CAPACITY);
    }
  }

  private void prepareInventory(String id) {
    try {
      var operation = store.get(id);
      var request = operation.request();
      if (!store.matchesEndpoint(id, endpointId)) throw new OperationException(Reason.CHANGED);
      String versioning = versioning(request.bucket());
      String destinationEtag = "";
      if (request.kind() == Kind.COPY || request.kind() == Kind.MOVE) {
        client.headBucket(r -> r.bucket(request.destinationBucket()));
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
        inventory(id, request, versioning);
      } else if (request.kind() == Kind.DELETE_VERSION) {
        inventory(id, request, versioning);
        if (store.get(id).total() != 1) throw new OperationException(Reason.MISSING);
      } else {
        var head = client.headObject(r -> r.bucket(request.bucket()).key(request.key()));
        if ((request.kind() == Kind.COPY || request.kind() == Kind.MOVE)
            && head.contentLength() > maximumTransfer)
          throw new OperationException(Reason.CAPACITY);
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
    } catch (RuntimeException failure) {
      store.finish(id, State.FAILED, translate(failure).reason().name());
    }
  }

  private void inventory(String id, Request request, String versioning) {
    String token = null, keyMarker = null, versionMarker = null;
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
                r ->
                    r.bucket(request.bucket())
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
        String km = keyMarker, vm = versionMarker;
        var result =
            client.listObjectVersions(
                r ->
                    r.bucket(request.bucket())
                        .prefix(request.key())
                        .keyMarker(km)
                        .versionIdMarker(vm)
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
                || keyMarker.equals(km) && java.util.Objects.equals(vm, versionMarker)))
          throw new OperationException(Reason.UNSUPPORTED);
        token = Boolean.TRUE.equals(result.isTruncated()) ? keyMarker : null;
      }
      store.append(id, items);
    } while (token != null);
  }

  private static boolean included(Request request, String key, String version) {
    if (key == null || !key.startsWith(request.key())) throw new OperationException(Reason.CHANGED);
    return request.kind() != Kind.DELETE_VERSION
        || key.equals(request.key()) && request.version().equals(version);
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
        || !marker && (etag == null || !etag.matches("\"[^\"\\p{Cntrl}]{1,200}\"")))
      throw new OperationException(Reason.UNSUPPORTED);
    return new Item(
        sequence, key, version, value(etag), size, modified, marker, ItemState.PENDING, "");
  }

  private void execute(String id) {
    Item current = null;
    boolean sent = false;
    boolean finalizing = false;
    try {
      var operation = store.get(id);
      var request = operation.request();
      if (!operation.versioning().equals(versioning(request.bucket())))
        throw new OperationException(Reason.CHANGED);
      long cursor = 0;
      while (true) {
        var items = store.items(id, cursor, pageSize);
        if (items.isEmpty()) break;
        for (var item : items) {
          cursor = item.sequence();
          if (item.state() == ItemState.DONE) continue;
          if (item.state() != ItemState.PENDING) throw new OperationException(Reason.UNCERTAIN);
          if (Thread.currentThread().isInterrupted()
              || store.get(id).state() == State.STOP_REQUESTED) {
            store.finish(id, State.INTERRUPTED, "STOPPED");
            return;
          }
          current = item;
          sent = false;
          if (!operation.versioning().equals(versioning(request.bucket())))
            throw new OperationException(Reason.CHANGED);
          checkTarget(request.bucket(), item);
          store.itemState(id, item.sequence(), ItemState.IN_FLIGHT, "");
          sent = true;
          if (request.kind() == Kind.COPY || request.kind() == Kind.MOVE) transfer(operation, item);
          else delete(request, item, request.kind() == Kind.DELETE_OBJECT);
          store.itemState(id, item.sequence(), ItemState.DONE, "");
          current = null;
          sent = false;
        }
      }
      if (request.kind() == Kind.DELETE_BUCKET
          || request.kind() == Kind.EMPTY_BUCKET
          || request.kind() == Kind.DELETE_PREFIX) {
        if (!scopeEmpty(request.bucket(), request.key(), operation.versioning()))
          throw new OperationException(Reason.CHANGED);
        if (request.kind() == Kind.EMPTY_BUCKET || request.kind() == Kind.DELETE_BUCKET) {
          if (!operation.versioning().equals(versioning(request.bucket())))
            throw new OperationException(Reason.CHANGED);
          if (store.get(id).state() == State.STOP_REQUESTED) {
            store.finish(id, State.INTERRUPTED, "STOPPED");
            return;
          }
          store.finish(id, State.RUNNING, "FINALIZING");
          finalizing = true;
          client.deleteBucket(
              r ->
                  r.bucket(request.bucket())
                      .overrideConfiguration(options -> options.putHeader("Content-Length", "0")));
        }
      }
      store.finish(id, State.SUCCEEDED, "");
    } catch (RuntimeException failure) {
      var reason = translate(failure).reason();
      boolean uncertain =
          (sent || finalizing)
              && (failure instanceof SdkClientException
                  || reason == Reason.UNAVAILABLE
                  || reason == Reason.UNCERTAIN);
      if (current != null)
        store.itemState(
            id,
            current.sequence(),
            uncertain ? ItemState.UNCERTAIN : ItemState.FAILED,
            uncertain ? "UNCERTAIN" : reason.name());
      store.finish(
          id,
          uncertain
              ? State.UNCERTAIN
              : store.get(id).done() > 0 || reason == Reason.COPIED_SOURCE_RETAINED
                  ? State.PARTIAL
                  : State.FAILED,
          finalizing && uncertain ? "FINALIZING" : uncertain ? "UNCERTAIN" : reason.name());
    } finally {
      store.release(id);
    }
  }

  private String versioning(String bucket) {
    var result = client.getBucketVersioning(r -> r.bucket(bucket));
    String status = value(result.statusAsString());
    if (!status.isEmpty() && !status.equals("Enabled") && !status.equals("Suspended")
        || "Enabled".equals(result.mfaDeleteAsString()))
      throw new OperationException(Reason.UNSUPPORTED);
    return status;
  }

  private boolean scopeEmpty(String bucket, String prefix, String versioning) {
    if (versioning.isEmpty()) {
      var result = client.listObjectsV2(r -> r.bucket(bucket).prefix(prefix).maxKeys(1));
      return Boolean.FALSE.equals(result.isTruncated())
          && result.contents().isEmpty()
          && result.commonPrefixes().isEmpty();
    }
    var result = client.listObjectVersions(r -> r.bucket(bucket).prefix(prefix).maxKeys(1));
    return Boolean.FALSE.equals(result.isTruncated())
        && result.versions().isEmpty()
        && result.deleteMarkers().isEmpty()
        && result.commonPrefixes().isEmpty();
  }

  private void checkTarget(String bucket, Item item) {
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

  private boolean exists(String bucket, Item item) {
    if (!item.marker()) return headOrNull(bucket, item.key(), item.version()) != null;
    String km = null, vm = null;
    for (int page = 0; page < 10000; page++) {
      String keyMarker = km, versionMarker = vm;
      var result =
          client.listObjectVersions(
              r ->
                  r.bucket(bucket)
                      .prefix(item.key())
                      .keyMarker(keyMarker)
                      .versionIdMarker(versionMarker)
                      .maxKeys(pageSize));
      if (result.deleteMarkers().stream()
          .anyMatch(v -> item.key().equals(v.key()) && item.version().equals(v.versionId())))
        return true;
      if (Boolean.FALSE.equals(result.isTruncated())) return false;
      km = result.nextKeyMarker();
      vm = result.nextVersionIdMarker();
      if (km == null || km.equals(keyMarker) && java.util.Objects.equals(vm, versionMarker))
        throw new OperationException(Reason.UNSUPPORTED);
    }
    throw new OperationException(Reason.CAPACITY);
  }

  private HeadObjectResponse headOrNull(String bucket, String key, String version) {
    try {
      return client.headObject(
          r -> {
            r.bucket(bucket).key(key);
            if (!version.isEmpty()) r.versionId(version);
          });
    } catch (S3Exception failure) {
      if (failure.statusCode() == 404) return null;
      throw failure;
    }
  }

  private void delete(Request request, Item item, boolean ordinary) {
    if (ordinary && !item.version().isEmpty()) {
      var latest = client.headObject(r -> r.bucket(request.bucket()).key(item.key()));
      if (!item.version().equals(value(latest.versionId())) || !item.etag().equals(latest.eTag()))
        throw new OperationException(Reason.CHANGED);
    }
    client.deleteObject(
        r -> {
          r.bucket(request.bucket()).key(item.key());
          r.overrideConfiguration(options -> options.putHeader("Content-Length", "0"));
          if (!ordinary && !item.version().isEmpty()) r.versionId(item.version());
          if (!item.marker()
              && (ordinary || item.version().isEmpty() || item.version().equals("null")))
            r.ifMatch(item.etag());
        });
  }

  private void transfer(Operation operation, Item item) {
    var request = operation.request();
    java.nio.file.Path temporary = null;
    try {
      temporary = Files.createTempFile("stackemu-transfer-", ".bin");
      try (var input =
              client.getObject(
                  r -> {
                    r.bucket(request.bucket()).key(item.key()).ifMatch(item.etag());
                    if (!item.version().isEmpty()) r.versionId(item.version());
                  });
          var output = Files.newOutputStream(temporary)) {
        byte[] buffer = new byte[65536];
        long copied = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
          copied += count;
          if (copied > maximumTransfer || copied > item.size())
            throw new OperationException(Reason.CHANGED);
          output.write(buffer, 0, count);
        }
        if (copied != item.size()) throw new OperationException(Reason.CHANGED);
        var metadata = input.response();
        client.putObject(
            r -> {
              r.bucket(request.destinationBucket())
                  .key(request.destinationKey())
                  .contentType(metadata.contentType())
                  .metadata(metadata.metadata())
                  .cacheControl(metadata.cacheControl())
                  .contentDisposition(metadata.contentDisposition())
                  .contentEncoding(metadata.contentEncoding())
                  .contentLanguage(metadata.contentLanguage());
              if (operation.destinationEtag().isEmpty()) r.ifNoneMatch("*");
              else r.ifMatch(operation.destinationEtag());
            },
            RequestBody.fromFile(temporary));
      }
      if (request.kind() == Kind.MOVE) {
        // A copied object is retained if source deletion fails; never roll back an unverified
        // target.
        try {
          checkTarget(request.bucket(), item);
          delete(request, item, true);
        } catch (RuntimeException failure) {
          if (failure instanceof SdkClientException
              || translate(failure).reason() == Reason.UNAVAILABLE)
            throw new OperationException(Reason.UNCERTAIN);
          throw new OperationException(Reason.COPIED_SOURCE_RETAINED);
        }
      }
    } catch (IOException failure) {
      throw new OperationException(Reason.UNAVAILABLE);
    } finally {
      if (temporary != null)
        try {
          Files.deleteIfExists(temporary);
        } catch (IOException ignored) {
          /* Owned OS temporary file; never delete user paths. */
        }
    }
  }

  private static String value(String value) {
    return value == null ? "" : value;
  }

  private static OperationException translate(RuntimeException failure) {
    if (failure instanceof OperationException operation) return operation;
    if (failure instanceof S3Exception response)
      return new OperationException(
          switch (response.statusCode()) {
            case 403 -> Reason.FORBIDDEN;
            case 404 -> Reason.MISSING;
            case 409, 412 -> Reason.CHANGED;
            case 400, 405, 501 -> Reason.UNSUPPORTED;
            default -> Reason.UNAVAILABLE;
          });
    return new OperationException(Reason.UNAVAILABLE);
  }

  @PreDestroy
  @Override
  public void close() {
    workers.shutdownNow();
    try {
      workers.awaitTermination(35, TimeUnit.SECONDS);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
    }
    client.close();
  }
}
