package io.github.erf88.stackemu.adapter.aws.s3;

import io.github.erf88.stackemu.application.s3.OperationException;
import io.github.erf88.stackemu.application.s3.OperationException.Reason;
import io.github.erf88.stackemu.application.s3.OperationStorePort;
import io.github.erf88.stackemu.application.s3.S3OperationPort;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
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
import software.amazon.awssdk.services.s3.model.S3Exception;

/** Owns bounded asynchronous S3 execution and its independently committed local journal. */
@ApplicationScoped
public class AwsS3OperationAdapter implements S3OperationPort, AutoCloseable {
  private final OperationStorePort store;
  private final S3Client client;
  private final int pageSize;
  private final long maximumTransfer;
  private final S3OperationInventory inventory;
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
    inventory = new S3OperationInventory(client, store, this.pageSize, maximumTransfer, endpointId);
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
          if (inventory.exists(operation.request().bucket(), item))
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
      inventory.prepare(id);
    } catch (RuntimeException failure) {
      store.finish(id, State.FAILED, translate(failure).reason().name());
    }
  }

  private void execute(String id) {
    Item current = null;
    boolean sent = false;
    boolean finalizing = false;
    try {
      var operation = store.get(id);
      var request = operation.request();
      if (!operation.versioning().equals(inventory.versioning(request.bucket())))
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
          if (!operation.versioning().equals(inventory.versioning(request.bucket())))
            throw new OperationException(Reason.CHANGED);
          inventory.checkTarget(request.bucket(), item);
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
        if (!inventory.scopeEmpty(request.bucket(), request.key(), operation.versioning()))
          throw new OperationException(Reason.CHANGED);
        if (request.kind() == Kind.EMPTY_BUCKET || request.kind() == Kind.DELETE_BUCKET) {
          if (!operation.versioning().equals(inventory.versioning(request.bucket())))
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
          inventory.checkTarget(request.bucket(), item);
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
