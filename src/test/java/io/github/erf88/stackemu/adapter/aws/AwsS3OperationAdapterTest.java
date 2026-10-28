package io.github.erf88.stackemu.adapter.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import io.github.erf88.stackemu.adapter.persistence.JdbcOperationStoreAdapter;
import io.github.erf88.stackemu.application.OperationException;
import io.github.erf88.stackemu.application.OperationException.Reason;
import io.github.erf88.stackemu.application.S3OperationPort.ItemState;
import io.github.erf88.stackemu.application.S3OperationPort.Kind;
import io.github.erf88.stackemu.application.S3OperationPort.Operation;
import io.github.erf88.stackemu.application.S3OperationPort.Request;
import io.github.erf88.stackemu.application.S3OperationPort.State;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

class AwsS3OperationAdapterTest {
  @TempDir Path directory;
  private HttpServer server;
  private S3Client client;
  private JdbcOperationStoreAdapter store;
  private AwsS3OperationAdapter operations;
  private final Set<String> keys = ConcurrentHashMap.newKeySet();
  private final AtomicInteger deletes = new AtomicInteger();
  private volatile boolean loseResponse;
  private volatile boolean forbidSecond;
  private volatile boolean forbidDeletes;
  private volatile boolean blockDelete;
  private final CountDownLatch entered = new CountDownLatch(1);
  private final CountDownLatch release = new CountDownLatch(1);

  @BeforeEach
  void fixture() throws Exception {
    keys.add("folder/a.txt");
    keys.add("folder/b.txt");
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          try {
            String method = exchange.getRequestMethod(),
                key = exchange.getRequestURI().getPath().replaceFirst("/owned-bucket/", "");
            if (method.equals("DELETE")) {
              int count = deletes.incrementAndGet();
              entered.countDown();
              if (blockDelete)
                try {
                  release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException interrupted) {
                  Thread.currentThread().interrupt();
                }
              if (loseResponse) return;
              if (forbidDeletes || forbidSecond && count == 2) {
                exchange.sendResponseHeaders(403, -1);
                return;
              }
              keys.remove(key);
              exchange.sendResponseHeaders(204, -1);
              return;
            }
            if (method.equals("HEAD")) {
              if (exchange.getRequestURI().getPath().equals("/owned-bucket")) {
                exchange.sendResponseHeaders(200, -1);
                return;
              }
              if (!keys.contains(key)) {
                exchange.sendResponseHeaders(404, -1);
                return;
              }
              exchange.getResponseHeaders().add("ETag", "\"reviewed\"");
              exchange.getResponseHeaders().add("Content-Length", "1");
              exchange.getResponseHeaders().add("Last-Modified", "Thu, 01 Jan 2026 00:00:00 GMT");
              exchange.sendResponseHeaders(200, -1);
              return;
            }
            String query = exchange.getRequestURI().getRawQuery();
            if (method.equals("PUT")) {
              exchange.getRequestBody().transferTo(java.io.OutputStream.nullOutputStream());
              keys.add(key);
              exchange.getResponseHeaders().add("ETag", "\"copied\"");
              exchange.sendResponseHeaders(200, -1);
              return;
            }
            if (exchange.getRequestURI().getPath().startsWith("/owned-bucket/")) {
              exchange.getResponseHeaders().add("Content-Type", "text/plain");
              exchange.getResponseHeaders().add("ETag", "\"reviewed\"");
              exchange.sendResponseHeaders(200, 1);
              exchange.getResponseBody().write('a');
              return;
            }
            String body;
            if (query != null && query.contains("list-type=2")) {
              StringBuilder xml =
                  new StringBuilder(
                      "<ListBucketResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\"><IsTruncated>false</IsTruncated><KeyCount>"
                          + keys.size()
                          + "</KeyCount>");
              keys.stream()
                  .sorted()
                  .forEach(
                      name ->
                          xml.append("<Contents><Key>")
                              .append(name)
                              .append(
                                  "</Key><ETag>&quot;reviewed&quot;</ETag><Size>1</Size><LastModified>2026-01-01T00:00:00Z</LastModified></Contents>"));
              body = xml.append("</ListBucketResult>").toString();
            } else
              body = "<VersioningConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\"/>";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/xml");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
          } finally {
            exchange.close();
          }
        });
    server.start();
    client =
        S3Client.builder()
            .endpointOverride(URI.create("http://127.0.0.1:" + server.getAddress().getPort()))
            .region(Region.US_EAST_1)
            .forcePathStyle(true)
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
            .build();
    store = new JdbcOperationStoreAdapter(directory.resolve("journal").toString(), 268435456, 30);
    operations = new AwsS3OperationAdapter(client, store, 100, 67108864, Duration.ofSeconds(30));
  }

  private Operation prepare() throws Exception {
    return await(
        operations.prepare(
            new Request(Kind.DELETE_PREFIX, "owned-bucket", "folder/", "", "", "", false)));
  }

  private Operation await(String id) throws Exception {
    long until = System.nanoTime() + Duration.ofSeconds(15).toNanos();
    do {
      var result = operations.get(id);
      if (!result.active()) return result;
      Thread.sleep(20);
    } while (System.nanoTime() < until);
    throw new AssertionError("operation timeout");
  }

  @AfterEach
  void close() {
    release.countDown();
    if (operations != null) operations.close();
    if (store != null) store.close();
    if (client != null) client.close();
    if (server != null) server.stop(0);
  }

  @Test
  void lostResponseIsRecordedOnceAndCannotBeReplayed() throws Exception {
    var prepared = prepare();
    loseResponse = true;
    operations.start(prepared.id(), null);
    var result = await(prepared.id());
    assertEquals(State.UNCERTAIN, result.state());
    assertEquals(1, deletes.get());
    assertEquals(ItemState.UNCERTAIN, operations.items(result.id(), 0).getFirst().state());
    assertThrows(OperationException.class, () -> operations.resume(result.id()));
    assertEquals(1, deletes.get());
  }

  @Test
  void knownFailureKeepsPartialResultsAndStopsFurtherWrites() throws Exception {
    var prepared = prepare();
    forbidSecond = true;
    operations.start(prepared.id(), null);
    var result = await(prepared.id());
    assertEquals(State.PARTIAL, result.state());
    assertEquals(1, result.done());
    assertEquals("FORBIDDEN", result.message());
    assertEquals(2, deletes.get());
    assertEquals(1, keys.size());
  }

  @Test
  void bucketClaimPreventsConcurrentMutationsAndInterruptResumesOnlyPendingItems()
      throws Exception {
    var first = prepare();
    var second = prepare();
    blockDelete = true;
    operations.start(first.id(), null);
    assertTrue(entered.await(3, TimeUnit.SECONDS));
    assertEquals(
        Reason.CONFLICT,
        assertThrows(OperationException.class, () -> operations.start(second.id(), null)).reason());
    operations.interrupt(first.id());
    release.countDown();
    var interrupted = await(first.id());
    assertEquals(State.INTERRUPTED, interrupted.state());
    assertEquals(1, interrupted.done());
    assertEquals(1, deletes.get());
    blockDelete = false;
    operations.resume(first.id());
    assertEquals(State.SUCCEEDED, await(first.id()).state());
    assertEquals(2, deletes.get());
  }

  @Test
  void storageFailureBeforeIntentCommitSendsNoMutation() throws Exception {
    operations.close();
    store.close();
    store =
        new JdbcOperationStoreAdapter(
            directory.resolve("failure-journal").toString(), 268435456, 30) {
          @Override
          public synchronized void itemState(
              String id, long sequence, ItemState state, String message) {
            if (state == ItemState.IN_FLIGHT) throw new OperationException(Reason.UNAVAILABLE);
            super.itemState(id, sequence, state, message);
          }
        };
    operations = new AwsS3OperationAdapter(client, store, 100, 67108864, Duration.ofSeconds(30));
    var prepared = prepare();
    operations.start(prepared.id(), null);
    assertEquals(State.FAILED, await(prepared.id()).state());
    assertEquals(0, deletes.get());
  }

  @Test
  void moveReportsCopiedDestinationAndRetainedSourceAfterKnownDeleteFailure() throws Exception {
    String id =
        operations.prepare(
            new Request(
                Kind.MOVE, "owned-bucket", "folder/a.txt", "", "owned-bucket", "moved.txt", false));
    assertEquals(State.READY, await(id).state());
    forbidDeletes = true;
    operations.start(id, null);
    var result = await(id);
    assertEquals(State.PARTIAL, result.state());
    assertEquals("COPIED_SOURCE_RETAINED", result.message());
    assertTrue(keys.contains("moved.txt"));
    assertTrue(keys.contains("folder/a.txt"));
    assertEquals(1, deletes.get());
  }

  @Test
  void presignedDownloadCannotOutliveKnownSessionCredentials() {
    Instant expiration = Instant.now().plusSeconds(45);
    var credentials =
        AwsSessionCredentials.builder()
            .accessKeyId("test")
            .secretAccessKey("test")
            .sessionToken("test-token")
            .expirationTime(expiration)
            .build();
    try (var configured =
            S3Client.builder()
                .endpointOverride(URI.create("http://127.0.0.1:" + server.getAddress().getPort()))
                .region(Region.US_EAST_1)
                .forcePathStyle(true)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
        var tools = new AwsS3ObjectToolsAdapter(configured, Duration.ofSeconds(30))) {
      var signed = tools.signDownload("owned-bucket", "folder/a.txt", 15, "attachment");
      assertTrue(signed.expires().isBefore(expiration));
      assertTrue(signed.expires().isAfter(Instant.now()));
    }
  }

  @Test
  void invalidCallDeadlinesAreRejectedBeforeCreatingClients() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new AwsS3OperationAdapter(client, store, 100, 67108864, Duration.ZERO));
    assertThrows(
        IllegalArgumentException.class,
        () -> new AwsS3ObjectToolsAdapter(client, Duration.ofMinutes(6)));
  }
}
