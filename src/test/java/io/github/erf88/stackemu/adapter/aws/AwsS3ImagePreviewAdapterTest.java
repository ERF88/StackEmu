package io.github.erf88.stackemu.adapter.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.erf88.stackemu.application.ImagePreviewPolicy;
import io.github.erf88.stackemu.application.S3ImagePreviewPort;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

class AwsS3ImagePreviewAdapterTest {
  @Test
  void unknownLengthReadsOnlyLimitPlusOneAndAborts() throws Exception {
    AtomicInteger bytesRead = new AtomicInteger();
    AtomicBoolean aborted = new AtomicBoolean();
    var source =
        new ByteArrayInputStream(new byte[ImagePreviewPolicy.MAX_BYTES + 100]) {
          @Override
          public synchronized int read(byte[] bytes, int offset, int length) {
            int read = super.read(bytes, offset, length);
            if (read > 0) bytesRead.addAndGet(read);
            return read;
          }
        };
    var result =
        AwsS3PreviewAdapter.readImageBody(
            "a.png",
            GetObjectResponse.builder().contentType("image/png").build(),
            AbortableInputStream.create(source, () -> aborted.set(true)));
    assertEquals(S3ImagePreviewPort.ImageState.TOO_LARGE, result.state());
    assertEquals(ImagePreviewPolicy.MAX_BYTES + 1, bytesRead.get());
    assertTrue(aborted.get());
    assertEquals(0, result.bytes().length);
  }

  @Test
  void rejectedMetadataDoesNotReadAndIncompleteLengthCannotSucceed() throws Exception {
    var unreadable =
        new java.io.InputStream() {
          @Override
          public int read() {
            throw new AssertionError("Must not read");
          }
        };
    for (var metadata :
        new GetObjectResponse[] {
          GetObjectResponse.builder().contentType("image/png").contentEncoding("gzip").build(),
          GetObjectResponse.builder().contentType("image/png").contentRange("bytes 0-1/10").build(),
          GetObjectResponse.builder().contentType("image/png").contentLength(1048577L).build()
        }) {
      AtomicBoolean aborted = new AtomicBoolean();
      var result =
          AwsS3PreviewAdapter.readImageBody(
              "a.png", metadata, AbortableInputStream.create(unreadable, () -> aborted.set(true)));
      assertTrue(result.state() != S3ImagePreviewPort.ImageState.AVAILABLE);
      assertTrue(aborted.get());
    }
    AtomicBoolean aborted = new AtomicBoolean();
    assertThrows(
        IOException.class,
        () ->
            AwsS3PreviewAdapter.readImageBody(
                "a.png",
                GetObjectResponse.builder().contentType("image/png").contentLength(10L).build(),
                AbortableInputStream.create(
                    new ByteArrayInputStream(new byte[1]), () -> aborted.set(true))));
    assertTrue(aborted.get());
  }
}
