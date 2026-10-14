package io.github.erf88.stackemu.adapter.web;

/** HTTP attachment policy; never renders object content inline. */
public final class DownloadHeaders {
  private DownloadHeaders() {}

  public static String attachmentDisposition(String key) {
    String name = key.substring(key.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "_");
    if (name.isEmpty()) {
      name = "object";
    }
    int[] codePoints = name.codePoints().limit(120).toArray();
    name = new String(codePoints, 0, codePoints.length);
    return "attachment; filename=\"download\"; filename*=UTF-8''"
        + S3ViewMapper.encode(name).replace("*", "%2A");
  }
}
