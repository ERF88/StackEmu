package io.github.erf88.stackemu.adapter.web;

import io.github.erf88.stackemu.application.S3ReadPort;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class S3ViewMapper {
  private S3ViewMapper() {}

  public record Link(String label, String href) {}

  public record Tag(String key, String value) {}

  public record ObjectRow(String key, String size, String modified, String href) {}

  public record BucketPageView(
      String name,
      String region,
      List<Tag> tags,
      String tagsMessage,
      String prefix,
      List<Link> breadcrumbs,
      List<Link> folders,
      List<ObjectRow> objects,
      String nextHref,
      String rootHref,
      int pageSize,
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
      String bucket) {}

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

  public static String downloadPath(String bucket, String key) {
    return "/s3/download?bucket=" + encode(bucket) + "&key=" + encode(key);
  }

  public static BucketPageView toBucketPage(
      S3ReadPort.BucketDetails details,
      S3ReadPort.ObjectPage listing,
      String prefix,
      int pageSize,
      UiContext ui) {
    List<Link> breadcrumbs = new ArrayList<>();
    breadcrumbs.add(
        new Link(details.name(), bucketPath(details.name(), "", null) + "&" + ui.query()));
    int start = 0;
    for (int index = 0; index < prefix.length(); index++) {
      if (prefix.charAt(index) == '/') {
        breadcrumbs.add(
            new Link(
                prefix.substring(start, index + 1),
                bucketPath(details.name(), prefix.substring(0, index + 1), null)
                    + "&"
                    + ui.query()));
        start = index + 1;
      }
    }
    if (start < prefix.length()) {
      breadcrumbs.add(
          new Link(
              prefix.substring(start),
              bucketPath(details.name(), prefix, null) + "&" + ui.query()));
    }
    String tagMessage =
        switch (details.tagsState()) {
          case AVAILABLE -> "";
          case EMPTY -> ui.text("noTags");
          case UNKNOWN -> ui.text("unknownTags");
        };
    return new BucketPageView(
        details.name(),
        optional(details.reportedRegion()),
        details.tags().entrySet().stream()
            .sorted(Comparator.comparing(e -> e.getKey()))
            .map(e -> new Tag(e.getKey(), e.getValue()))
            .toList(),
        tagMessage,
        prefix,
        List.copyOf(breadcrumbs),
        listing.prefixes().stream()
            .map(p -> new Link(p, bucketPath(details.name(), p, null) + "&" + ui.query()))
            .toList(),
        listing.objects().stream()
            .map(
                o ->
                    new ObjectRow(
                        o.key(),
                        bytes(o.size(), ui),
                        date(o.modified(), ui),
                        objectPath(details.name(), o.key()) + "&" + ui.query()))
            .toList(),
        listing.nextToken() == null || listing.nextToken().isEmpty()
            ? ""
            : bucketPath(details.name(), prefix, listing.nextToken()) + "&" + ui.query(),
        bucketPath(details.name(), prefix, null) + "&" + ui.query(),
        pageSize,
        "/s3/write?action=upload&bucket="
            + encode(details.name())
            + "&prefix="
            + encode(prefix)
            + "&"
            + ui.query(),
        "/s3/write?action=folder&bucket="
            + encode(details.name())
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
        bucket);
  }

  private static String optional(String value) {
    return value == null || value.isEmpty() ? "—" : value;
  }

  private static String bytes(long size, UiContext ui) {
    if (size < 0) return "—";
    return NumberFormat.getIntegerInstance(Locale.forLanguageTag(ui.language())).format(size)
        + " B";
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
