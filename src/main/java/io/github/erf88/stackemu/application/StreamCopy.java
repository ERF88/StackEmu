package io.github.erf88.stackemu.application;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class StreamCopy {
  public static final int BUFFER_BYTES = 64 * 1024;

  private StreamCopy() {}

  /** Constant-size copy buffer; never materializes the whole object. */
  public static void copy(InputStream input, OutputStream output) throws IOException {
    byte[] buffer = new byte[BUFFER_BYTES];
    int count;
    while ((count = input.read(buffer)) != -1) {
      output.write(buffer, 0, count);
    }
  }
}
