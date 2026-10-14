package io.github.erf88.stackemu.adapter.web;

import jakarta.ws.rs.core.MediaType;
import java.net.URLConnection;
import java.util.Locale;

/** Metadata hints, not content validation or malware detection. */
public final class FileContentTypes {
  private FileContentTypes() {}

  public static String safeContentType(String candidate) {
    if (candidate == null
        || candidate.length() > 256
        || candidate.codePoints().anyMatch(Character::isISOControl)) {
      return MediaType.APPLICATION_OCTET_STREAM;
    }
    try {
      MediaType type = MediaType.valueOf(candidate);
      if (!type.getType().matches("[A-Za-z0-9!#$&^_.+-]+")
          || !type.getSubtype().matches("[A-Za-z0-9!#$&^_.+-]+")) {
        return MediaType.APPLICATION_OCTET_STREAM;
      }
      return type.toString();
    } catch (IllegalArgumentException invalid) {
      return MediaType.APPLICATION_OCTET_STREAM;
    }
  }

  public static String uploadContentType(String filename, String reported) {
    String type = safeContentType(reported);
    if (!MediaType.valueOf(type).getSubtype().equalsIgnoreCase("octet-stream")) {
      return type;
    }
    String name = filename.toLowerCase(Locale.ROOT);
    String known =
        switch (name.substring(name.lastIndexOf('.') + 1)) {
          case "pdf" -> "application/pdf";
          case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
          case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
          case "pptx" ->
              "application/vnd.openxmlformats-officedocument.presentationml.presentation";
          case "csv" -> "text/csv";
          case "json" -> "application/json";
          case "svg" -> "image/svg+xml";
          case "webp" -> "image/webp";
          default -> URLConnection.guessContentTypeFromName(name);
        };
    return safeContentType(known);
  }
}
