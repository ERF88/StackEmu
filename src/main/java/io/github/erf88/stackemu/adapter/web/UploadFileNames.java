package io.github.erf88.stackemu.adapter.web;

import io.github.erf88.stackemu.application.WriteValidationException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

/** Decode the UTF-8 encoded-word form used by .NET multipart clients; never a filesystem path. */
public final class UploadFileNames {
  private static final Pattern ENCODED_NAME =
      Pattern.compile("^=\\?UTF-8\\?B\\?([A-Za-z0-9+/=]+)\\?=$", Pattern.CASE_INSENSITIVE);

  private UploadFileNames() {}

  public static String originalFilename(String reported) {
    if (reported == null) {
      return null;
    }
    var match = ENCODED_NAME.matcher(reported);
    if (!match.matches()) {
      return reported;
    }
    try {
      return StandardCharsets.UTF_8
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(Base64.getDecoder().decode(match.group(1))))
          .toString();
    } catch (IllegalArgumentException | CharacterCodingException invalid) {
      throw new WriteValidationException("file", "invalidFilename");
    }
  }
}
