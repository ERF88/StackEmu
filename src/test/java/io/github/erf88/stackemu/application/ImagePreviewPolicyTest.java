package io.github.erf88.stackemu.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ImagePreviewPolicyTest {
  @Test
  void metadataRequiresMatchingExtensionMimeAndIdentityEncoding() {
    assertEquals("png", ImagePreviewPolicy.format("a.PNG", "Image/PNG", null));
    assertEquals("jpeg", ImagePreviewPolicy.format("a.JPEG", "image/jpeg", "identity"));
    assertEquals("jpeg", ImagePreviewPolicy.format("a.jpg", "image/jpeg", null));
    for (String type :
        new String[] {
          "image/svg+xml", "image/jpeg", "application/octet-stream", "image/png; charset=utf-8"
        }) assertEquals("", ImagePreviewPolicy.format("a.png", type, null));
    for (String key : new String[] {"a.svg", "a.gif", "a.webp", "a.png.html", "a.apng"})
      assertEquals("", ImagePreviewPolicy.format(key, "image/png", null));
    assertEquals("", ImagePreviewPolicy.format("a.png", "image/png", "gzip"));
    assertEquals("", ImagePreviewPolicy.format(null, "image/png", null));
  }

  @Test
  void resultOwnsBytesAndRefusalCannotRetainContent() {
    byte[] bytes = {1, 2};
    var preview =
        new S3ImagePreviewPort.ImagePreview(S3ImagePreviewPort.ImageState.AVAILABLE, bytes);
    Arrays.fill(bytes, (byte) 0);
    assertArrayEquals(new byte[] {1, 2}, preview.png());
    Arrays.fill(preview.png(), (byte) 0);
    assertArrayEquals(new byte[] {1, 2}, preview.png());
    assertEquals(
        0,
        new S3ImagePreviewPort.ImagePreview(S3ImagePreviewPort.ImageState.INVALID_IMAGE, bytes)
            .png()
            .length);
  }
}
