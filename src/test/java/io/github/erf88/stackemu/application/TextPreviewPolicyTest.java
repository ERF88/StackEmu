package io.github.erf88.stackemu.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class TextPreviewPolicyTest {
  @Test
  void metadataAllowlistRejectsActiveGenericConflictingAndCompressedTypes() {
    assertTrue(TextPreviewPolicy.supports("a.TXT", "Text/Plain; charset=\"UTF-8\"", null));
    assertTrue(TextPreviewPolicy.supports("a.log", "text/plain", "identity"));
    assertTrue(TextPreviewPolicy.supports("a.json", "application/json", null));
    for (String key : new String[] {"a.html", "a.svg", "a.png", "a.pdf", "a.zip", "a.js"})
      assertFalse(TextPreviewPolicy.supports(key, "text/plain", null));
    for (String type :
        new String[] {
          "text/html",
          "image/svg+xml",
          "application/octet-stream",
          "text/plain; charset=latin1",
          "text/plain; arbitrary=true",
          "application/json",
          "text/plain; charset=\"utf-8",
          "text/plain; charset=utf-8\""
        }) assertFalse(TextPreviewPolicy.supports("a.txt", type, null), type);
    assertFalse(TextPreviewPolicy.supports("a.txt", "text/plain", "gzip"));
    assertFalse(TextPreviewPolicy.supports("a.txt", null, null));
  }

  @Test
  void actualBytesAreStrictUtf8WithExplicitControlAndSizeLimits() {
    String text = "ação\t+%\n<script>not interpreted</script>\r\n";
    assertEquals(text, TextPreviewPolicy.decode(text.getBytes(StandardCharsets.UTF_8)).text());
    assertEquals(
        "{}", TextPreviewPolicy.decode("\uFEFF{}".getBytes(StandardCharsets.UTF_8)).text());
    assertEquals(
        S3PreviewPort.PreviewState.AVAILABLE, TextPreviewPolicy.decode(new byte[0]).state());
    for (String invalid : new String[] {"\u0000", "\u001b", "\u007f", "\u0085", "\u202e", "\u2066"})
      assertEquals(
          S3PreviewPort.PreviewState.INVALID_TEXT,
          TextPreviewPolicy.decode(invalid.getBytes(StandardCharsets.UTF_8)).state());
    assertEquals(
        S3PreviewPort.PreviewState.INVALID_TEXT,
        TextPreviewPolicy.decode(new byte[] {(byte) 0xC3, (byte) 0x28}).state());
    byte[] maximum = "x".repeat(TextPreviewPolicy.MAX_BYTES).getBytes(StandardCharsets.UTF_8);
    assertEquals(S3PreviewPort.PreviewState.AVAILABLE, TextPreviewPolicy.decode(maximum).state());
    assertEquals(
        S3PreviewPort.PreviewState.TOO_LARGE,
        TextPreviewPolicy.decode(new byte[TextPreviewPolicy.MAX_BYTES + 1]).state());
    assertEquals(
        "", new S3PreviewPort.TextPreview(S3PreviewPort.PreviewState.UNSUPPORTED, "secret").text());
  }
}
