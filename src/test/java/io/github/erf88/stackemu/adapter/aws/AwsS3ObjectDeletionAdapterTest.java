package io.github.erf88.stackemu.adapter.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import io.github.erf88.stackemu.application.ObjectDeletionException;
import io.github.erf88.stackemu.application.ObjectDeletionException.Kind;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

class AwsS3ObjectDeletionAdapterTest {
  private HttpServer server;
  private S3Client client;
  private AwsS3ObjectDeletionAdapter adapter;
  private final AtomicInteger deletes = new AtomicInteger();
  private volatile String versionXml = "";
  private volatile int versionStatus = 200;
  private volatile int deleteStatus = 204;
  private volatile boolean marker;
  private volatile boolean dropDeleteResponse;
  private volatile String matchedEtag;
  private volatile String deletedPath;
  private volatile String headSize = "1";
  private volatile String headEtag = "\"reviewed\"";
  private volatile String folderEntries = "<Contents><Key>folder/</Key><Size>0</Size></Contents>";
  private volatile String folderCount = "1";
  private volatile String truncated = "false";
  private volatile String listQuery;

  @BeforeEach
  void startLocalProtocolFixture() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          try {
            if (exchange.getRequestMethod().equals("DELETE")) {
              deletes.incrementAndGet();
              matchedEtag = exchange.getRequestHeaders().getFirst("If-Match");
              deletedPath = exchange.getRequestURI().getPath();
              if (dropDeleteResponse) return;
              if (marker) {
                exchange.getResponseHeaders().add("x-amz-delete-marker", "true");
                exchange.getResponseHeaders().add("x-amz-version-id", "external-version");
              }
              exchange.sendResponseHeaders(deleteStatus, -1);
            } else if (exchange.getRequestMethod().equals("HEAD")) {
              exchange.getResponseHeaders().add("ETag", headEtag);
              exchange.getResponseHeaders().add("Content-Length", headSize);
              exchange.sendResponseHeaders(200, -1);
            } else if (exchange.getRequestURI().getRawQuery().contains("list-type=2")) {
              listQuery = exchange.getRequestURI().getRawQuery();
              byte[] xml =
                  ("<ListBucketResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\"><KeyCount>"
                          + folderCount
                          + "</KeyCount><IsTruncated>"
                          + truncated
                          + "</IsTruncated>"
                          + folderEntries
                          + "</ListBucketResult>")
                      .getBytes(StandardCharsets.UTF_8);
              exchange.getResponseHeaders().add("Content-Type", "application/xml");
              exchange.sendResponseHeaders(200, xml.length);
              exchange.getResponseBody().write(xml);
            } else {
              byte[] xml =
                  ("<VersioningConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">"
                          + versionXml
                          + "</VersioningConfiguration>")
                      .getBytes(StandardCharsets.UTF_8);
              exchange.getResponseHeaders().add("Content-Type", "application/xml");
              exchange.sendResponseHeaders(versionStatus, xml.length);
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
            .overrideConfiguration(c -> c.apiCallTimeout(Duration.ofSeconds(2)))
            .build();
    adapter = new AwsS3ObjectDeletionAdapter(client);
  }

  @AfterEach
  void closeOwnedClientAndServer() {
    adapter.close();
    client.close();
    server.stop(0);
  }

  @Test
  void sendsExactReviewedEtagAndTarget() {
    var target = adapter.inspectObject("test-bucket", "ação +%/file.txt");
    adapter.deleteObject(target);
    assertEquals(1, deletes.get());
    assertEquals("\"reviewed\"", matchedEtag);
    assertEquals("/test-bucket/ação +%/file.txt", deletedPath);
  }

  @Test
  void emptyFolderUsesBoundedProofAndExactConditionalDelete() {
    headSize = "0";
    var target = adapter.inspectObject("test-bucket", "folder/");
    adapter.deleteObject(target);
    assertEquals(1, deletes.get());
    assertEquals("/test-bucket/folder/", deletedPath);
    assertEquals("\"reviewed\"", matchedEtag);
    assertTrue(listQuery.contains("max-keys=2"));
    assertTrue(listQuery.contains("prefix=folder%2F"));
    assertTrue(!listQuery.contains("delimiter="));
  }

  @Test
  void changedOrNonemptyFolderNeverSubmitsDelete() {
    headSize = "0";
    var target = adapter.inspectObject("test-bucket", "folder/");
    headEtag = "\"changed\"";
    assertEquals(
        Kind.CONFLICT,
        assertThrows(ObjectDeletionException.class, () -> adapter.deleteObject(target)).kind());
    headEtag = target.etag();
    folderEntries += "<Contents><Key>folder/child</Key><Size>1</Size></Contents>";
    folderCount = "2";
    assertEquals(
        Kind.CONFLICT,
        assertThrows(ObjectDeletionException.class, () -> adapter.deleteObject(target)).kind());
    assertEquals(0, deletes.get());
  }

  @Test
  void unknownTruncatedInconsistentAndNonzeroFoldersFailClosed() {
    headSize = "0";
    truncated = "true";
    assertThrows(
        ObjectDeletionException.class, () -> adapter.inspectObject("test-bucket", "folder/"));
    truncated = "false";
    folderCount = "2";
    assertThrows(
        ObjectDeletionException.class, () -> adapter.inspectObject("test-bucket", "folder/"));
    folderCount = "1";
    headSize = "1";
    assertThrows(
        ObjectDeletionException.class, () -> adapter.inspectObject("test-bucket", "folder/"));
    headSize = "0";
    truncated = "";
    assertThrows(
        ObjectDeletionException.class, () -> adapter.inspectObject("test-bucket", "folder/"));
    assertEquals(0, deletes.get());
  }

  @Test
  void lostFolderDeleteResponseDoesNotRetry() {
    headSize = "0";
    var target = adapter.inspectObject("test-bucket", "folder/");
    dropDeleteResponse = true;
    assertEquals(
        Kind.UNCERTAIN,
        assertThrows(ObjectDeletionException.class, () -> adapter.deleteObject(target)).kind());
    assertEquals(1, deletes.get());
  }

  @Test
  void ambiguousDuplicateAndOversizedFolderResponsesFailClosed() {
    headSize = "0";
    for (String value : new String[] {"", "unknown", "FALSE"}) {
      truncated = value;
      assertThrows(
          ObjectDeletionException.class, () -> adapter.inspectObject("test-bucket", "folder/"));
    }
    truncated = "false";
    String originalEntries = folderEntries;
    folderEntries += "<IsTruncated>false</IsTruncated>";
    assertThrows(
        ObjectDeletionException.class, () -> adapter.inspectObject("test-bucket", "folder/"));
    folderEntries = originalEntries + " ".repeat(65_537);
    assertThrows(
        ObjectDeletionException.class, () -> adapter.inspectObject("test-bucket", "folder/"));
    assertEquals(0, deletes.get());
  }

  @Test
  void unknownEnabledSuspendedAndMfaStatesFailClosed() {
    for (String state : new String[] {"Enabled", "Suspended", "FutureState"}) {
      versionXml = "<Status>" + state + "</Status>";
      assertEquals(
          Kind.UNSAFE,
          assertThrows(
                  ObjectDeletionException.class,
                  () -> adapter.inspectObject("test-bucket", "file.txt"))
              .kind());
    }
    versionXml = "<MfaDelete>Enabled</MfaDelete>";
    assertEquals(
        Kind.UNSAFE,
        assertThrows(
                ObjectDeletionException.class,
                () -> adapter.inspectObject("test-bucket", "file.txt"))
            .kind());
    assertEquals(0, deletes.get());
  }

  @Test
  void versionReadDeniedAfterReviewNeverSubmitsDelete() {
    var target = adapter.inspectObject("test-bucket", "file.txt");
    versionStatus = 403;
    assertEquals(
        Kind.FORBIDDEN,
        assertThrows(ObjectDeletionException.class, () -> adapter.deleteObject(target)).kind());
    assertEquals(0, deletes.get());
  }

  @Test
  void serverFailureIsUncertainAndIsNotRetried() {
    var target = adapter.inspectObject("test-bucket", "file.txt");
    deleteStatus = 503;
    assertEquals(
        Kind.UNCERTAIN,
        assertThrows(ObjectDeletionException.class, () -> adapter.deleteObject(target)).kind());
    assertEquals(1, deletes.get());
  }

  @Test
  void lostResponseIsUncertainAndIsNotRetried() {
    var target = adapter.inspectObject("test-bucket", "file.txt");
    dropDeleteResponse = true;
    assertEquals(
        Kind.UNCERTAIN,
        assertThrows(ObjectDeletionException.class, () -> adapter.deleteObject(target)).kind());
    assertEquals(1, deletes.get());
  }

  @Test
  void closingOwnedClientLeavesManagedClientUsable() {
    adapter.close();
    assertEquals(
        "\"reviewed\"", client.headObject(r -> r.bucket("test-bucket").key("file.txt")).eTag());
  }

  @Test
  void concurrentVersionMarkerCannotBeReportedAsPermanentDeletion() {
    var target = adapter.inspectObject("test-bucket", "file.txt");
    marker = true;
    assertEquals(
        Kind.UNCERTAIN,
        assertThrows(ObjectDeletionException.class, () -> adapter.deleteObject(target)).kind());
    assertTrue(deletes.get() == 1);
  }
}
