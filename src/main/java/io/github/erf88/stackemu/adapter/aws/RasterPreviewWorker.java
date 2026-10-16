package io.github.erf88.stackemu.adapter.aws;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;

/** Standalone JDK-only subprocess. Never invoke its decoder on an HTTP request thread. */
public final class RasterPreviewWorker extends OutputStream {
  static final int MAX_BYTES = 1024 * 1024;
  private final OutputStream destination;
  private int written;

  private RasterPreviewWorker(OutputStream destination) {
    this.destination = destination;
  }

  public static void main(String[] arguments) {
    try {
      if (arguments.length != 1 || (!arguments[0].equals("png") && !arguments[0].equals("jpeg")))
        throw new IOException("Format");
      byte[] bytes = System.in.readNBytes(MAX_BYTES + 1);
      if (bytes.length == 0 || bytes.length > MAX_BYTES) throw new IOException("Input limit");
      if (arguments[0].equals("png")) validatePng(bytes);
      else validateJpeg(bytes);
      ImageIO.setUseCache(false);
      try (var source = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
        var readers = ImageIO.getImageReaders(source);
        if (!readers.hasNext()) throw new IOException("Reader");
        ImageReader reader = readers.next();
        try {
          if (!reader.getFormatName().equalsIgnoreCase(arguments[0]))
            throw new IOException("Mismatch");
          reader.setInput(source, true, true);
          boolean[] warned = {false};
          reader.addIIOReadWarningListener((ignored, warning) -> warned[0] = true);
          int width = reader.getWidth(0);
          int height = reader.getHeight(0);
          if (width <= 0
              || height <= 0
              || width > 4096
              || height > 4096
              || (long) width * height > 4_000_000) throw new IOException("Pixels");
          BufferedImage decoded = reader.read(0);
          if (warned[0]) throw new IOException("Decoder warning");
          BufferedImage pixels = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
          for (int row = 0; row < height; row++) {
            int[] colors = decoded.getRGB(0, row, width, 1, null, 0, width);
            pixels.setRGB(0, row, width, 1, colors, 0, width);
          }
          if (!ImageIO.write(pixels, "png", new RasterPreviewWorker(System.out)))
            throw new IOException("Writer");
        } finally {
          reader.dispose();
        }
      }
    } catch (IOException | RuntimeException failure) {
      // Stable exit only: never emit decoder diagnostics or source content.
      System.exit(2);
    }
  }

  private static void validatePng(byte[] bytes) throws IOException {
    byte[] signature = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
    if (bytes.length < 8) throw new IOException("Signature");
    for (int index = 0; index < 8; index++)
      if (bytes[index] != signature[index]) throw new IOException("Signature");
    int offset = 8;
    while (offset <= bytes.length - 12) {
      long length = Integer.toUnsignedLong(java.nio.ByteBuffer.wrap(bytes, offset, 4).getInt());
      if (length > bytes.length - offset - 12) throw new IOException("Chunk length");
      int size = (int) length;
      String type = new String(bytes, offset + 4, 4, java.nio.charset.StandardCharsets.US_ASCII);
      if (type.equals("acTL") || type.equals("fcTL") || type.equals("fdAT"))
        throw new IOException("Animation");
      CRC32 crc = new CRC32();
      crc.update(bytes, offset + 4, size + 4);
      long expected =
          Integer.toUnsignedLong(java.nio.ByteBuffer.wrap(bytes, offset + 8 + size, 4).getInt());
      if (crc.getValue() != expected) throw new IOException("CRC");
      offset += size + 12;
      if (type.equals("IEND")) {
        if (size != 0 || offset != bytes.length) throw new IOException("Trailing data");
        return;
      }
    }
    throw new IOException("Incomplete PNG");
  }

  private static void validateJpeg(byte[] bytes) throws IOException {
    if (bytes.length < 4 || (bytes[0] & 255) != 255 || (bytes[1] & 255) != 216)
      throw new IOException("Signature");
    int offset = 2;
    boolean scanned = false;
    while (offset < bytes.length) {
      if ((bytes[offset++] & 255) != 255) throw new IOException("Marker");
      while (offset < bytes.length && (bytes[offset] & 255) == 255) offset++;
      if (offset == bytes.length) throw new IOException("Marker");
      int marker = bytes[offset++] & 255;
      if (marker == 217) {
        if (!scanned || offset != bytes.length) throw new IOException("Trailing data");
        return;
      }
      if (marker == 0
          || marker == 216
          || (marker >= 208 && marker <= 215)
          || offset > bytes.length - 2) throw new IOException("Marker");
      int length = ((bytes[offset] & 255) << 8) | (bytes[offset + 1] & 255);
      if (length < 2 || length > bytes.length - offset) throw new IOException("Segment");
      offset += length;
      if (marker == 218) {
        scanned = true;
        while (offset < bytes.length) {
          if ((bytes[offset] & 255) != 255) {
            offset++;
            continue;
          }
          int start = offset++;
          while (offset < bytes.length && (bytes[offset] & 255) == 255) offset++;
          if (offset == bytes.length) throw new IOException("Scan");
          int next = bytes[offset] & 255;
          if (next == 0 || (next >= 208 && next <= 215)) {
            offset++;
            continue;
          }
          offset = start;
          break;
        }
      }
    }
    throw new IOException("Incomplete JPEG");
  }

  @Override
  public void write(int value) throws IOException {
    if (written == MAX_BYTES) throw new IOException("Output limit");
    destination.write(value);
    written++;
  }

  @Override
  public void write(byte[] bytes, int offset, int length) throws IOException {
    if (length > MAX_BYTES - written) throw new IOException("Output limit");
    destination.write(bytes, offset, length);
    written += length;
  }
}
