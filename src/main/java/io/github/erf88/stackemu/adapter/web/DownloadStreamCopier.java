package io.github.erf88.stackemu.adapter.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class DownloadStreamCopier {
  public static final int BUFFER_BYTES = 64 * 1024;

  private DownloadStreamCopier() {}

  /** Constant-size copy buffer; does not close either stream. The caller owns their lifetime. */
  public static void copy(InputStream input, OutputStream output) throws IOException {
    byte[] buffer = new byte[BUFFER_BYTES];
    int count;
    while ((count = input.read(buffer)) != -1) {
      output.write(buffer, 0, count);
    }
  }
}
