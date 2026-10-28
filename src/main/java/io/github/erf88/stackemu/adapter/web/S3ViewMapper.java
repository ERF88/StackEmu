package io.github.erf88.stackemu.adapter.web;

import io.github.erf88.stackemu.application.ImagePreviewPolicy;
import io.github.erf88.stackemu.application.S3ReadPort;
import io.github.erf88.stackemu.application.TextPreviewPolicy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class S3ViewMapper {
  private static final List<String> BYTE_UNITS =
      List.of("B", "KiB", "MiB", "GiB", "TiB", "PiB", "EiB");

  private S3ViewMapper() {}

  public record Link(String label, String href) {}

  public record FolderRow(String label, String href, String deleteHref) {}

  public record ObjectRow(
      String key,
      String size,
      String modified,
      String href,
      String downloadHref,
      String kind,
      String previewHref,
      String deleteHref) {}

  public record BucketPageView(
      String name,
      String prefix,
      List<Link> breadcrumbs,
      List<FolderRow> folders,
      List<ObjectRow> objects,
      String nextHref,
      String rootHref,
      boolean previousPage,
      String uploadHref,
      String folderHref) {}

  public record ObjectPageView(
      String key,
      String size,
      String contentType,
      String etag,
      String modified,
      String downloadHref,
      String parentHref,
      String bucket,
      String previewHref,
      String imagePreviewHref,
      String deleteHref,
      String displayName,
      String href) {
    public String metadataHref() {
      return href.replace("/s3/object?", "/s3/object-metadata?");
    }

    public String transferHref() {
      return href.replace("/s3/object?", "/s3/transfer?");
    }

    public String shareHref() {
      return href.replace("/s3/object?", "/s3/share?");
    }

    public String versionsHref() {
      return href.replace("/s3/object?", "/s3/versions?");
    }
  }

  public static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  public static String bucketPath(String bucket, String prefix, String token) {
    String path = "/s3/bucket?bucket=" + encode(bucket) + "&prefix=" + encode(prefix);
    return token == null || token.isEmpty() ? path : path + "&token=" + encode(token);
  }

  public static String objectPath(String bucket, String key) {
    return "/s3/object?bucket=" + encode(bucket) + "&key=" + encode(key);
  }

  public static String deletionPath(String bucket, String key) {
    return "/s3/delete-object?bucket=" + encode(bucket) + "&key=" + encode(key);
  }

  public static String downloadPath(String bucket, String key) {
    return "/s3/download?bucket=" + encode(bucket) + "&key=" + encode(key);
  }

  public static String previewPath(String bucket, String key) {
    return "/s3/preview?bucket=" + encode(bucket) + "&key=" + encode(key);
  }

  public static String imagePreviewPath(String bucket, String key) {
    return "/s3/preview/image?bucket=" + encode(bucket) + "&key=" + encode(key);
  }

  public static String imagePreviewContentPath(String bucket, String key) {
    return "/s3/preview/image/content?bucket=" + encode(bucket) + "&key=" + encode(key);
  }

  public static BucketPageView toBucketPage(
      String bucket, S3ReadPort.ObjectPage listing, String prefix, String token, UiContext ui) {
    List<Link> breadcrumbs = new ArrayList<>();
    breadcrumbs.add(new Link(bucket, bucketPath(bucket, "", null) + "&" + ui.query()));
    int start = 0;
    for (int index = 0; index < prefix.length(); index++) {
      if (prefix.charAt(index) == '/') {
        breadcrumbs.add(
            new Link(
                prefix.substring(start, index + 1),
                bucketPath(bucket, prefix.substring(0, index + 1), null) + "&" + ui.query()));
        start = index + 1;
      }
    }
    if (start < prefix.length()) {
      breadcrumbs.add(
          new Link(prefix.substring(start), bucketPath(bucket, prefix, null) + "&" + ui.query()));
    }
    return new BucketPageView(
        bucket,
        prefix,
        List.copyOf(breadcrumbs),
        listing.prefixes().stream()
            .map(
                p ->
                    new FolderRow(
                        p.startsWith(prefix) && p.length() > prefix.length()
                            ? p.substring(prefix.length())
                            : p,
                        bucketPath(bucket, p, null) + "&" + ui.query(),
                        deletionPath(bucket, p) + "&" + ui.query()))
            .toList(),
        listing.objects().stream()
            .map(
                o ->
                    new ObjectRow(
                        o.key(),
                        bytes(o.size(), ui),
                        date(o.modified(), ui),
                        objectPath(bucket, o.key()) + "&" + ui.query(),
                        downloadPath(bucket, o.key()) + "&" + ui.query(),
                        fileKind(o.key(), ui),
                        previewPath(bucket, o.key()) + "&" + ui.query(),
                        o.key().endsWith("/") && o.size() != 0
                            ? ""
                            : deletionPath(bucket, o.key()) + "&" + ui.query()))
            .toList(),
        listing.nextToken() == null || listing.nextToken().isEmpty()
            ? ""
            : bucketPath(bucket, prefix, listing.nextToken()) + "&" + ui.query(),
        bucketPath(bucket, prefix, null) + "&" + ui.query(),
        token != null && !token.isEmpty(),
        "/s3/write?action=upload&bucket="
            + encode(bucket)
            + "&prefix="
            + encode(prefix)
            + "&"
            + ui.query(),
        "/s3/write?action=folder&bucket="
            + encode(bucket)
            + "&prefix="
            + encode(prefix)
            + "&"
            + ui.query());
  }

  public static ObjectPageView toObjectPage(
      String bucket, S3ReadPort.ObjectDetails object, UiContext ui) {
    int slash = object.key().lastIndexOf('/');
    String prefix = slash < 0 ? "" : object.key().substring(0, slash + 1);
    return new ObjectPageView(
        object.key(),
        bytes(object.size(), ui),
        optional(object.contentType()),
        optional(object.etag()),
        date(object.modified(), ui),
        downloadPath(bucket, object.key()) + "&" + ui.query(),
        bucketPath(bucket, prefix, null) + "&" + ui.query(),
        bucket,
        TextPreviewPolicy.supports(object.key(), object.contentType(), null)
            ? previewPath(bucket, object.key()) + "&" + ui.query()
            : "",
        ImagePreviewPolicy.format(object.key(), object.contentType(), null).isEmpty()
            ? ""
            : imagePreviewPath(bucket, object.key()) + "&" + ui.query(),
        object.key().endsWith("/") && object.size() != 0
            ? ""
            : deletionPath(bucket, object.key()) + "&" + ui.query(),
        slash < object.key().length() - 1 ? object.key().substring(slash + 1) : object.key(),
        objectPath(bucket, object.key()) + "&" + ui.query());
  }

  private static String optional(String value) {
    return value == null || value.isEmpty() ? "—" : value;
  }

  private static String bytes(long size, UiContext ui) {
    if (size < 0) return "—";
    Locale locale = Locale.forLanguageTag(ui.language());
    String exact = NumberFormat.getIntegerInstance(locale).format(size) + " B";
    long divisor = 1;
    int unit = 0;
    while (unit < BYTE_UNITS.size() - 1 && size / divisor >= 1024) {
      divisor *= 1024;
      unit++;
    }
    if (unit == 0) return exact;
    var rounded =
        BigDecimal.valueOf(size).divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP);
    var format = NumberFormat.getNumberInstance(locale);
    format.setMaximumFractionDigits(2);
    return format.format(rounded) + " " + BYTE_UNITS.get(unit) + " (" + exact + ")";
  }

  private static String fileKind(String key, UiContext ui) {
    int dot = key.lastIndexOf('.');
    String extension =
        dot <= key.lastIndexOf('/') + 1 ? "" : key.substring(dot + 1).toLowerCase(Locale.ROOT);
    String label =
        switch (extension) {
          case "json" -> "fileKindJson";
          case "png", "jpg", "jpeg", "gif", "webp", "avif" -> "fileKindImage";
          case "txt", "log", "md", "csv" -> "fileKindText";
          default -> "fileKindFile";
        };
    return ui.text(label);
  }

  private static String date(Instant instant, UiContext ui) {
    return instant == null
        ? "—"
        : DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                .withLocale(Locale.forLanguageTag(ui.language()))
                .withZone(ZoneOffset.UTC)
                .format(instant)
            + " UTC";
  }
}
