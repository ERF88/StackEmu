package io.github.erf88.stackemu.adapter.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.erf88.stackemu.application.S3ReadPort;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.jupiter.api.Test;

class S3DownloadTest {
  private static final class GeneratedInput extends InputStream {
    long remaining = 128L * 1024 * 1024;
    int largestRead;
    long consumed;
    boolean closed;

    @Override
    public int read() {
      if (remaining == 0) return -1;
      remaining--;
      consumed++;
      return 0;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) {
      largestRead = Math.max(largestRead, length);
      if (remaining == 0) return -1;
      int count = (int) Math.min(remaining, length);
      remaining -= count;
      consumed += count;
      return count;
    }

    @Override
    public void close() {
      closed = true;
    }
  }

  private static S3Resource resource(GeneratedInput input) {
    var reader =
        new S3ReadPort() {
          public BucketDetails getBucketDetails(String bucket) {
            throw new UnsupportedOperationException();
          }

          public ObjectPage listObjects(String bucket, String prefix, String token, int size) {
            throw new UnsupportedOperationException();
          }

          public ObjectDetails getObjectDetails(String bucket, String key) {
            throw new UnsupportedOperationException();
          }

          public Download openDownload(String bucket, String key) {
            return new Download(input, input.remaining);
          }
        };
    return new S3Resource(reader);
  }

  @Test
  void largeResponseIsLazyAndUsesABoundedBuffer() throws Exception {
    var input = new GeneratedInput();
    var response = resource(input).downloadObject("bucket", "file.bin", "en", null);
    assertEquals(0, input.consumed);
    assertFalse(input.closed);
    ((StreamingOutput) response.getEntity()).write(OutputStream.nullOutputStream());
    assertEquals(128L * 1024 * 1024, input.consumed);
    assertTrue(input.largestRead <= DownloadStreamCopier.BUFFER_BYTES);
    assertTrue(input.closed);
  }

  @Test
  void cancelledClientClosesTheSdkStream() {
    var input = new GeneratedInput();
    var response = resource(input).downloadObject("bucket", "file.bin", "en", null);
    var failure =
        assertThrows(
            IOException.class,
            () ->
                ((StreamingOutput) response.getEntity())
                    .write(
                        new OutputStream() {
                          @Override
                          public void write(int value) throws IOException {
                            throw new IOException("client cancelled");
                          }

                          @Override
                          public void write(byte[] bytes, int offset, int length)
                              throws IOException {
                            throw new IOException("client cancelled");
                          }
                        }));
    assertEquals("Object transfer interrupted", failure.getMessage());
    assertTrue(input.closed);
    assertTrue(input.consumed <= DownloadStreamCopier.BUFFER_BYTES);
  }

  @Test
  void filenamesCannotInjectHeaders() {
    String header = DownloadHeaders.attachmentDisposition("prefix/a\r\nü +*.txt");
    assertTrue(header.startsWith("attachment;"));
    assertFalse(header.contains("\r"));
    assertFalse(header.contains("\n"));
    assertTrue(header.contains("%C3%BC%20%2B%2A.txt"));
  }
}
