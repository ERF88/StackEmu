package io.github.erf88.stackemu.adapter.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpServer;
import io.github.erf88.stackemu.application.BucketCleanupException;
import io.github.erf88.stackemu.application.S3BucketCleanupPort.Failure;
import io.github.erf88.stackemu.application.S3BucketDeletionPort;
import io.github.erf88.stackemu.application.S3ObjectDeletionPort;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

class AwsS3BucketCleanupAdapterTest {
  private HttpServer server;
  private S3Client client;
  private AwsS3BucketCleanupAdapter adapter;
  private String body = "<KeyCount>0</KeyCount><IsTruncated>false</IsTruncated>";
  private int listStatus = 200;

  @BeforeEach
  void startOwnedProtocolFixture() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          try {
            boolean listing = exchange.getRequestURI().getRawQuery().contains("list-type=2");
            String root = listing ? "ListBucketResult" : "VersioningConfiguration";
            byte[] xml =
                ("<"
                        + root
                        + " xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">"
                        + (listing ? body : "")
                        + "</"
                        + root
                        + ">")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/xml");
            exchange.sendResponseHeaders(listing ? listStatus : 200, xml.length);
            exchange.getResponseBody().write(xml);
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
    var objects =
        new S3ObjectDeletionPort() {
          public Target inspectObject(String bucket, String key) {
            throw new AssertionError("Unexpected object mutation path");
          }

          public void deleteObject(Target target) {
            throw new AssertionError("Unexpected DELETE");
          }
        };
    var buckets =
        new S3BucketDeletionPort() {
          public Target inspectBucket(String bucket) {
            throw new AssertionError("Unexpected bucket mutation path");
          }

          public void deleteBucket(Target target) {
            throw new AssertionError("Unexpected bucket DELETE");
          }
        };
    adapter = new AwsS3BucketCleanupAdapter(client, objects, buckets);
  }

  @AfterEach
  void closeOwnedFixture() {
    adapter.close();
    client.close();
    server.stop(0);
  }

  @Test
  void compliantEmptyInventoryIsReadOnly() {
    assertEquals(0, adapter.inspectBucket("test-bucket").entries().size());
  }

  @Test
  void ambiguousCountsPrefixesAndContinuationNeverAuthorizeCleanup() {
    for (String invalid :
        new String[] {
          "<KeyCount>1</KeyCount><IsTruncated>false</IsTruncated>",
          "<KeyCount>0</KeyCount><IsTruncated>false</IsTruncated><CommonPrefixes><Prefix>hidden/</Prefix></CommonPrefixes>",
          "<KeyCount>0</KeyCount><IsTruncated>true</IsTruncated>",
          "<KeyCount>0</KeyCount><IsTruncated>false</IsTruncated><NextContinuationToken>unexpected</NextContinuationToken>",
          "<KeyCount>0</KeyCount><IsTruncated>unknown</IsTruncated>"
        }) {
      body = invalid;
      assertThrows(BucketCleanupException.class, () -> adapter.inspectBucket("test-bucket"));
    }
  }

  @Test
  void repeatedPaginationTokenIsRefused() {
    body =
        "<KeyCount>1</KeyCount><IsTruncated>true</IsTruncated><NextContinuationToken>repeat</NextContinuationToken>"
            + "<Contents><Key>file.txt</Key><Size>1</Size><ETag>\"reviewed\"</ETag><LastModified>2026-10-22T00:00:00Z</LastModified></Contents>";
    assertEquals(
        Failure.UNSAFE,
        assertThrows(BucketCleanupException.class, () -> adapter.inspectBucket("test-bucket"))
            .failure());
  }

  @Test
  void deniedInventoryDoesNotInvokeAnyMutationPort() {
    listStatus = 403;
    assertEquals(
        Failure.FORBIDDEN,
        assertThrows(BucketCleanupException.class, () -> adapter.inspectBucket("test-bucket"))
            .failure());
  }
}
