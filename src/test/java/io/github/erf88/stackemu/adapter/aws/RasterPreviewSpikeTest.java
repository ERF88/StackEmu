package io.github.erf88.stackemu.adapter.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import org.junit.jupiter.api.Test;

/** Local codec experiment only: no production endpoint or resource-isolation guarantee. */
class RasterPreviewSpikeTest {
  private static final int MAX_INPUT_BYTES = 1024 * 1024;
  private static final int MAX_DIMENSION = 4096;
  private static final long MAX_PIXELS = 4_000_000;

  @Test
  void localJdkDecodesPngAndJpegAndProducesFreshPngPixels() throws IOException {
    for (String format : new String[] {"png", "jpeg"}) {
      byte[] original = fixture(format, 3, 2, false);
      byte[] sanitized = reencode(original, format);
      assertTrue(sanitized.length < MAX_INPUT_BYTES);
      assertEquals(137, Byte.toUnsignedInt(sanitized[0]));
      try (var input = new java.io.ByteArrayInputStream(sanitized)) {
        BufferedImage image = ImageIO.read(input);
        assertNotNull(image);
        assertEquals(3, image.getWidth());
        assertEquals(2, image.getHeight());
      }
    }
  }

  @Test
  void freshPixelEncodingDropsPngTextMetadataAndTrailingBytes() throws IOException {
    byte[] original = fixture("png", 2, 2, true);
    String marker = "stackemu-untrusted-metadata";
    assertTrue(new String(original, StandardCharsets.ISO_8859_1).contains(marker));
    byte[] suffix = "<script>trailing-payload</script>".getBytes(StandardCharsets.US_ASCII);
    byte[] polyglot = Arrays.copyOf(original, original.length + suffix.length);
    System.arraycopy(suffix, 0, polyglot, original.length, suffix.length);
    byte[] sanitized = reencode(polyglot, "png");
    String output = new String(sanitized, StandardCharsets.ISO_8859_1);
    assertFalse(output.contains(marker));
    assertFalse(output.contains("trailing-payload"));
  }

  @Test
  void rejectsUnsupportedMismatchedTruncatedAndOversizedInput() throws IOException {
    assertThrows(IOException.class, () -> reencode(new byte[MAX_INPUT_BYTES + 1], "png"));
    assertThrows(IOException.class, () -> reencode(new byte[0], "png"));
    assertThrows(
        IOException.class,
        () ->
            reencode(
                "<svg xmlns='http://www.w3.org/2000/svg'/>".getBytes(StandardCharsets.UTF_8),
                "png"));
    byte[] png = fixture("png", 2, 2, false);
    assertThrows(IOException.class, () -> reencode(png, "jpeg"));
    assertThrows(IOException.class, () -> reencode(Arrays.copyOf(png, 24), "png"));
    assertThrows(IOException.class, () -> reencode(fixture("gif", 2, 2, false), "gif"));
  }

  @Test
  void rejectsDimensionAndPixelLimitsBeforeRasterDecode() throws IOException {
    assertThrows(IOException.class, () -> reencode(fixture("png", 4097, 1, false), "png"));
    assertThrows(IOException.class, () -> reencode(fixture("png", 1, 4097, false), "png"));
    assertThrows(IOException.class, () -> reencode(fixture("png", 2001, 2000, false), "png"));
  }

  private static byte[] reencode(byte[] bytes, String expectedFormat) throws IOException {
    if (bytes.length == 0 || bytes.length > MAX_INPUT_BYTES) throw new IOException("Input limit");
    if (!expectedFormat.equals("png") && !expectedFormat.equals("jpeg"))
      throw new IOException("Unsupported format");
    try (var source = input(bytes)) {
      var readers = ImageIO.getImageReaders(source);
      if (!readers.hasNext()) throw new IOException("Missing reader");
      ImageReader reader = readers.next();
      try {
        if (!reader.getFormatName().equalsIgnoreCase(expectedFormat))
          throw new IOException("Format mismatch");
        reader.setInput(source, true, true);
        int width = reader.getWidth(0);
        int height = reader.getHeight(0);
        if (width <= 0
            || height <= 0
            || width > MAX_DIMENSION
            || height > MAX_DIMENSION
            || (long) width * height > MAX_PIXELS) throw new IOException("Pixel limit");
        BufferedImage decoded = reader.read(0);
        BufferedImage pixels = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int row = 0; row < height; row++) {
          int[] colors = decoded.getRGB(0, row, width, 1, null, 0, width);
          pixels.setRGB(0, row, width, 1, colors, 0, width);
        }
        var output = new ByteArrayOutputStream();
        try (var destination = new MemoryCacheImageOutputStream(output)) {
          if (!ImageIO.write(pixels, "png", destination)) throw new IOException("Missing writer");
        }
        // This post-write check is NOT an allocation bound; production needs a bounded sink.
        if (output.size() > MAX_INPUT_BYTES) throw new IOException("Output limit");
        return output.toByteArray();
      } finally {
        reader.dispose();
      }
    }
  }

  private static MemoryCacheImageInputStream input(byte[] bytes) {
    return new MemoryCacheImageInputStream(new java.io.ByteArrayInputStream(bytes));
  }

  private static byte[] fixture(String format, int width, int height, boolean withText)
      throws IOException {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    ImageWriter writer = ImageIO.getImageWritersByFormatName(format).next();
    var bytes = new ByteArrayOutputStream();
    try (var output = new MemoryCacheImageOutputStream(bytes)) {
      writer.setOutput(output);
      IIOMetadata metadata =
          writer.getDefaultImageMetadata(
              javax.imageio.ImageTypeSpecifier.createFromRenderedImage(image),
              writer.getDefaultWriteParam());
      if (withText) {
        var root = new IIOMetadataNode("javax_imageio_png_1.0");
        var text = new IIOMetadataNode("tEXt");
        var entry = new IIOMetadataNode("tEXtEntry");
        entry.setAttribute("keyword", "Comment");
        entry.setAttribute("value", "stackemu-untrusted-metadata");
        text.appendChild(entry);
        root.appendChild(text);
        metadata.mergeTree("javax_imageio_png_1.0", root);
      }
      writer.write(null, new IIOImage(image, null, metadata), writer.getDefaultWriteParam());
    } finally {
      writer.dispose();
    }
    return bytes.toByteArray();
  }
}
