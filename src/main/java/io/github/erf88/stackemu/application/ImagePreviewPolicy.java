package io.github.erf88.stackemu.application;

import java.util.Locale;

/** Metadata allowlist only; the isolated decoder must independently validate actual bytes. */
public final class ImagePreviewPolicy {
  public static final int MAX_BYTES = 1024 * 1024;

  private ImagePreviewPolicy() {}

  public static String format(String key, String type, String encoding) {
    if (key == null
        || type == null
        || (encoding != null && !encoding.isBlank() && !encoding.equalsIgnoreCase("identity")))
      return "";
    String name = key.toLowerCase(Locale.ROOT);
    String mime = type.trim().toLowerCase(Locale.ROOT);
    if (name.endsWith(".png") && mime.equals("image/png")) return "png";
    if ((name.endsWith(".jpg") || name.endsWith(".jpeg")) && mime.equals("image/jpeg"))
      return "jpeg";
    return "";
  }
}
