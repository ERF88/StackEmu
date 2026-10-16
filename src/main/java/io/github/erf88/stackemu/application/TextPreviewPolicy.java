package io.github.erf88.stackemu.application;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Pure allowlist/encoding policy. Metadata is never proof of safe bytes. */
public final class TextPreviewPolicy {
  public static final int MAX_BYTES = 65536;

  private TextPreviewPolicy() {}

  public static boolean supports(String key, String contentType, String contentEncoding) {
    if (key == null
        || contentType == null
        || (contentEncoding != null
            && !contentEncoding.isBlank()
            && !contentEncoding.equalsIgnoreCase("identity"))) return false;
    String name = key.toLowerCase(Locale.ROOT);
    String type = contentType.toLowerCase(Locale.ROOT).trim();
    String expected =
        name.endsWith(".json")
            ? "application/json"
            : name.endsWith(".txt") || name.endsWith(".log") ? "text/plain" : "";
    return !expected.isEmpty()
        && type.matches(expected + "(?:\\s*;\\s*charset\\s*=\\s*(?:utf-8|\"utf-8\"))?");
  }

  public static S3PreviewPort.TextPreview decode(byte[] bytes) {
    if (bytes.length > MAX_BYTES) return refused(S3PreviewPort.PreviewState.TOO_LARGE);
    try {
      String text =
          StandardCharsets.UTF_8
              .newDecoder()
              .onMalformedInput(CodingErrorAction.REPORT)
              .onUnmappableCharacter(CodingErrorAction.REPORT)
              .decode(ByteBuffer.wrap(bytes))
              .toString();
      if (text.startsWith("\uFEFF")) text = text.substring(1);
      if (text.codePoints()
          .anyMatch(
              code ->
                  (code < 32 && code != 9 && code != 10 && code != 13)
                      || (code >= 127 && code <= 159)
                      || (code >= 0x202A && code <= 0x202E)
                      || (code >= 0x2066 && code <= 0x2069)))
        return refused(S3PreviewPort.PreviewState.INVALID_TEXT);
      return new S3PreviewPort.TextPreview(S3PreviewPort.PreviewState.AVAILABLE, text);
    } catch (CharacterCodingException failure) {
      return refused(S3PreviewPort.PreviewState.INVALID_TEXT);
    }
  }

  public static S3PreviewPort.TextPreview refused(S3PreviewPort.PreviewState state) {
    return new S3PreviewPort.TextPreview(state, "");
  }
}
