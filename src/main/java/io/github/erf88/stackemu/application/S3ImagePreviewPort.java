package io.github.erf88.stackemu.application;

/** Explicit bounded read and raster re-encoding, independent of original attachments. */
public interface S3ImagePreviewPort {
  /** Reads and sanitizes one object; remote/resource failures throw S3ReadException. */
  ImagePreview readImagePreview(String bucket, String key);

  enum ImageState {
    AVAILABLE,
    UNSUPPORTED,
    TOO_LARGE,
    INVALID_IMAGE
  }

  record ImagePreview(ImageState state, byte[] png) {
    public ImagePreview {
      png = state == ImageState.AVAILABLE ? png.clone() : new byte[0];
    }

    @Override
    public byte[] png() {
      return png.clone();
    }
  }
}
