package io.github.erf88.stackemu.adapter.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import io.github.erf88.stackemu.application.S3PreviewPort;
import io.github.erf88.stackemu.application.S3ReadException;
import io.github.erf88.stackemu.application.TextPreviewPolicy;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

class AwsS3PreviewAdapterTest {
  @Test
  void boundedReadDoesNotTrustLengthAndAlwaysAborts() throws Exception {
    AtomicInteger bytesRead = new AtomicInteger();
    AtomicBoolean aborted = new AtomicBoolean();
    var delegate =
        new ByteArrayInputStream(new byte[TextPreviewPolicy.MAX_BYTES + 100]) {
          @Override
          public synchronized int read(byte[] bytes, int offset, int length) {
            int read = super.read(bytes, offset, length);
            if (read > 0) bytesRead.addAndGet(read);
            return read;
          }
        };
    var result =
        AwsS3PreviewAdapter.readBody(
            "a.txt",
            GetObjectResponse.builder().contentType("text/plain").build(),
            AbortableInputStream.create(delegate, () -> aborted.set(true)));
    assertEquals(S3PreviewPort.PreviewState.TOO_LARGE, result.state());
    assertEquals(TextPreviewPolicy.MAX_BYTES + 1, bytesRead.get());
    assertTrue(aborted.get());
  }

  @Test
  void rejectedMetadataReadsNothingAndIncompleteBodyCannotBecomeSuccess() throws Exception {
    AtomicBoolean aborted = new AtomicBoolean();
    var unreadable =
        new java.io.InputStream() {
          @Override
          public int read() {
            throw new AssertionError("Rejected metadata must not read body");
          }
        };
    var result =
        AwsS3PreviewAdapter.readBody(
            "a.html",
            GetObjectResponse.builder().contentType("text/html").build(),
            AbortableInputStream.create(unreadable, () -> aborted.set(true)));
    assertEquals(S3PreviewPort.PreviewState.UNSUPPORTED, result.state());
    assertTrue(aborted.get());
    aborted.set(false);
    assertThrows(
        IOException.class,
        () ->
            AwsS3PreviewAdapter.readBody(
                "a.txt",
                GetObjectResponse.builder().contentType("text/plain").contentLength(10L).build(),
                AbortableInputStream.create(
                    new ByteArrayInputStream(new byte[0]), () -> aborted.set(true))));
    assertTrue(aborted.get());
    aborted.set(false);
    result =
        AwsS3PreviewAdapter.readBody(
            "a.txt",
            GetObjectResponse.builder().contentType("text/plain").contentLength(65537L).build(),
            AbortableInputStream.create(unreadable, () -> aborted.set(true)));
    assertEquals(S3PreviewPort.PreviewState.TOO_LARGE, result.state());
    assertTrue(aborted.get());
  }

  @Test
  void transportBoundsStalledResponseBody() throws Exception {
    assertBodyDeadline(false, false);
  }

  @Test
  void monotonicDeadlineBoundsTricklingBodyBeforeSizeLimit() throws Exception {
    assertBodyDeadline(true, false);
  }

  @Test
  void imageTransportBoundsStalledResponseBody() throws Exception {
    assertBodyDeadline(false, true);
  }

  @Test
  void imageTransportBoundsTricklingResponseBody() throws Exception {
    assertBodyDeadline(true, true);
  }

  private void assertBodyDeadline(boolean trickle, boolean image) throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    var release = new CountDownLatch(1);
    var executor = Executors.newVirtualThreadPerTaskExecutor();
    server.setExecutor(executor);
    server.createContext(
        "/",
        exchange -> {
          try {
            exchange.getResponseHeaders().set("Content-Type", image ? "image/png" : "text/plain");
            exchange.sendResponseHeaders(200, 65536);
            do {
              exchange.getResponseBody().write('x');
              exchange.getResponseBody().flush();
            } while (trickle && !release.await(250, java.util.concurrent.TimeUnit.MILLISECONDS));
            if (!trickle) release.await();
          } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
          } finally {
            exchange.close();
          }
        });
    server.start();
    try (var configuredClient =
            S3Client.builder()
                .endpointOverride(URI.create("http://127.0.0.1:" + server.getAddress().getPort()))
                .region(Region.US_EAST_1)
                .forcePathStyle(true)
                .credentialsProvider(
                    StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .httpClientBuilder(
                    UrlConnectionHttpClient.builder().socketTimeout(Duration.ofSeconds(20)))
                .build();
        var adapter = new AwsS3PreviewAdapter(configuredClient, new RasterPreviewProcess())) {
      assertTimeoutPreemptively(
          Duration.ofSeconds(7),
          () -> {
            var failure =
                assertThrows(
                    S3ReadException.class,
                    () -> {
                      if (image) adapter.readImagePreview("bucket", "a.png");
                      else adapter.readTextPreview("bucket", "a.txt");
                    });
            assertEquals(S3ReadException.Kind.UNAVAILABLE, failure.kind());
          });
    } finally {
      release.countDown();
      server.stop(0);
      executor.close();
    }
  }
}
