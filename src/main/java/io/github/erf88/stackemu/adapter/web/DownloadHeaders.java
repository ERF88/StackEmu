package io.github.erf88.stackemu.adapter.web;

/** HTTP attachment policy; never renders object content inline. */
public final class DownloadHeaders {
  private DownloadHeaders() {}

  public static String attachmentDisposition(String key) {
    String name = key.substring(key.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "_");
    if (name.isEmpty()) {
      name = "object";
    }
    String ascii = name.replaceAll("[^A-Za-z0-9 ._+-]", "_");
    return "attachment; filename=\""
        + ascii
        + "\"; filename*=UTF-8''"
        + S3ViewMapper.encode(name).replace("*", "%2A");
  }
}
