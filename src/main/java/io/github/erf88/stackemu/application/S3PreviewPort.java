package io.github.erf88.stackemu.application;

/** Explicit bounded content read; independent of attachment streams and mutations. */
public interface S3PreviewPort {
  /**
   * Reads one complete small text object or a refusal; throws S3ReadException on remote failure.
   */
  TextPreview readTextPreview(String bucket, String key);

  enum PreviewState {
    AVAILABLE,
    UNSUPPORTED,
    TOO_LARGE,
    INVALID_TEXT
  }

  record TextPreview(PreviewState state, String text) {
    public TextPreview {
      if (state != PreviewState.AVAILABLE) text = "";
    }
  }
}
