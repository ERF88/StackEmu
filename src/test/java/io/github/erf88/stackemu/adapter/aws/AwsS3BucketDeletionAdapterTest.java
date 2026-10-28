package io.github.erf88.stackemu.adapter.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpServer;
import io.github.erf88.stackemu.application.BucketDeletionException;
import io.github.erf88.stackemu.application.BucketDeletionException.Kind;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

class AwsS3BucketDeletionAdapterTest {
  private HttpServer server;
  private S3Client client;
  private AwsS3BucketDeletionAdapter adapter;
  private final AtomicInteger deletes = new AtomicInteger();
  private String versionXml = "";
  private String listing = "<KeyCount>0</KeyCount><IsTruncated>false</IsTruncated>";
  private int readStatus = 200;
  private int deleteStatus = 204;
  private boolean dropResponse;
  private String deletedPath;

  @BeforeEach
  void startFixture() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          try {
            if (exchange.getRequestMethod().equals("DELETE")) {
              deletes.incrementAndGet();
              deletedPath = exchange.getRequestURI().getPath();
              if (dropResponse) return;
              exchange.sendResponseHeaders(deleteStatus, -1);
            } else {
              boolean version = exchange.getRequestURI().getQuery().contains("versioning");
              String root = version ? "VersioningConfiguration" : "ListBucketResult";
              byte[] xml =
                  ("<"
                          + root
                          + " xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">"
                          + (version ? versionXml : listing)
                          + "</"
                          + root
                          + ">")
                      .getBytes(StandardCharsets.UTF_8);
              exchange.getResponseHeaders().add("Content-Type", "application/xml");
              exchange.sendResponseHeaders(readStatus, xml.length);
              exchange.getResponseBody().write(xml);
            }
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
    adapter = new AwsS3BucketDeletionAdapter(client);
  }

  @AfterEach
  void closeFixture() {
    adapter.close();
    client.close();
    server.stop(0);
  }

  @Test
  void deletesOnlyReviewedBucket() {
    var target = adapter.inspectBucket("test-bucket");
    adapter.deleteBucket(target);
    assertEquals(1, deletes.get());
    assertEquals("/test-bucket", deletedPath);
  }

  @Test
  void addedObjectOrMarkerAfterReviewPreventsDelete() {
    var target = adapter.inspectBucket("test-bucket");
    listing =
        "<KeyCount>1</KeyCount><IsTruncated>false</IsTruncated><Contents><Key>folder/</Key></Contents>";
    assertEquals(
        Kind.NOT_EMPTY,
        assertThrows(BucketDeletionException.class, () -> adapter.deleteBucket(target)).kind());
    assertEquals(0, deletes.get());
  }

  @Test
  void allVersionedAndUnknownStatesFailClosed() {
    var target = adapter.inspectBucket("test-bucket");
    for (String state : new String[] {"Enabled", "Suspended", "FutureState"}) {
      versionXml = "<Status>" + state + "</Status>";
      assertEquals(
          Kind.UNSAFE,
          assertThrows(BucketDeletionException.class, () -> adapter.deleteBucket(target)).kind());
    }
    versionXml = "<MfaDelete>Enabled</MfaDelete>";
    assertEquals(
        Kind.UNSAFE,
        assertThrows(BucketDeletionException.class, () -> adapter.inspectBucket("test-bucket"))
            .kind());
    assertEquals(0, deletes.get());
  }

  @Test
  void incompleteListingIsNotProofOfEmptiness() {
    listing = "";
    assertEquals(
        Kind.UNSAFE,
        assertThrows(BucketDeletionException.class, () -> adapter.inspectBucket("test-bucket"))
            .kind());
    assertEquals(0, deletes.get());
  }

  @Test
  void readDeniedNeverSubmitsDelete() {
    var target = adapter.inspectBucket("test-bucket");
    readStatus = 403;
    assertEquals(
        Kind.FORBIDDEN,
        assertThrows(BucketDeletionException.class, () -> adapter.deleteBucket(target)).kind());
    assertEquals(0, deletes.get());
  }

  @Test
  void concurrentNonemptyServiceRefusalIsNotSuccess() {
    var target = adapter.inspectBucket("test-bucket");
    deleteStatus = 409;
    assertEquals(
        Kind.NOT_EMPTY,
        assertThrows(BucketDeletionException.class, () -> adapter.deleteBucket(target)).kind());
    assertEquals(1, deletes.get());
  }

  @Test
  void serverFailureIsUncertainAndNotRetried() {
    var target = adapter.inspectBucket("test-bucket");
    deleteStatus = 503;
    assertEquals(
        Kind.UNCERTAIN,
        assertThrows(BucketDeletionException.class, () -> adapter.deleteBucket(target)).kind());
    assertEquals(1, deletes.get());
  }

  @Test
  void lostResponseHasExactlyOneWireDelete() {
    var target = adapter.inspectBucket("test-bucket");
    dropResponse = true;
    assertEquals(
        Kind.UNCERTAIN,
        assertThrows(BucketDeletionException.class, () -> adapter.deleteBucket(target)).kind());
    assertEquals(1, deletes.get());
  }
}
