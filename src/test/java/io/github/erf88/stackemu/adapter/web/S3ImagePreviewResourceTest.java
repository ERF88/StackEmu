package io.github.erf88.stackemu.adapter.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.erf88.stackemu.application.S3ImagePreviewPort;
import io.github.erf88.stackemu.application.S3ReadException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class S3ImagePreviewResourceTest {
  @Test
  void remoteFailuresPreserveStatusNoStoreAndPlainSafeMessages() {
    int[] statuses = {404, 403, 400, 501, 503, 502};
    S3ReadException.Kind[] kinds = {
      S3ReadException.Kind.NOT_FOUND,
      S3ReadException.Kind.FORBIDDEN,
      S3ReadException.Kind.INVALID,
      S3ReadException.Kind.UNSUPPORTED,
      S3ReadException.Kind.UNAVAILABLE,
      S3ReadException.Kind.FAILED
    };
    for (int index = 0; index < kinds.length; index++) {
      var kind = kinds[index];
      var resource =
          new S3ImagePreviewResource(
              (bucket, key) -> {
                throw new S3ReadException(kind);
              });
      try (var response = resource.imageContent("bucket", "a.png", "pt-BR", null)) {
        assertEquals(statuses[index], response.getStatus());
        assertEquals("no-store", response.getHeaderString("Cache-Control"));
        assertEquals(
            "text/plain",
            response.getMediaType().getType() + "/" + response.getMediaType().getSubtype());
        assertFalse(response.getEntity().toString().contains("Exception"));
      }
    }
  }

  @Test
  void invalidTargetNeverInvokesReadAndRefusalStatusesAreExplicit() {
    AtomicBoolean invoked = new AtomicBoolean();
    var resource =
        new S3ImagePreviewResource(
            (bucket, key) -> {
              invoked.set(true);
              return new S3ImagePreviewPort.ImagePreview(
                  S3ImagePreviewPort.ImageState.INVALID_IMAGE, new byte[0]);
            });
    try (var response = resource.imageContent("http://external.invalid", "a.png", "en", null)) {
      assertEquals(400, response.getStatus());
      assertFalse(invoked.get());
    }
    for (var state :
        new S3ImagePreviewPort.ImageState[] {
          S3ImagePreviewPort.ImageState.UNSUPPORTED,
          S3ImagePreviewPort.ImageState.TOO_LARGE,
          S3ImagePreviewPort.ImageState.INVALID_IMAGE
        }) {
      var refused =
          new S3ImagePreviewResource(
              (bucket, key) -> new S3ImagePreviewPort.ImagePreview(state, new byte[0]));
      try (var response = refused.imageContent("bucket", "a.png", "en", null)) {
        assertEquals(
            state == S3ImagePreviewPort.ImageState.UNSUPPORTED
                ? 415
                : state == S3ImagePreviewPort.ImageState.TOO_LARGE ? 413 : 422,
            response.getStatus());
      }
    }
  }
}
