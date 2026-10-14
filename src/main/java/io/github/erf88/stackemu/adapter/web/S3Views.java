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

public final class S3Views {
  private S3Views() {}

  public record Link(String label, String href) {}

  public record Tag(String key, String value) {}

  public record ObjectRow(String key, String size, String modified, String href) {}

  public record BucketPage(
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
      int pageSize) {}

  public record ObjectPage(
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

  public static BucketPage bucket(
      S3ReadPort.BucketDetails details,
      S3ReadPort.ObjectPage listing,
      String prefix,
      int pageSize,
      Ui ui) {
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
    return new BucketPage(
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
        pageSize);
  }

  public static ObjectPage object(String bucket, S3ReadPort.ObjectDetails object, Ui ui) {
    int slash = object.key().lastIndexOf('/');
    String prefix = slash < 0 ? "" : object.key().substring(0, slash + 1);
    return new ObjectPage(
        object.key(),
        bytes(object.size(), ui),
        optional(object.contentType()),
        optional(object.etag()),
        date(object.modified(), ui),
        downloadPath(bucket, object.key()) + "&" + ui.query(),
        bucketPath(bucket, prefix, null) + "&" + ui.query(),
        bucket);
  }

  public static String disposition(String key) {
    String name = key.substring(key.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "_");
    if (name.isEmpty()) name = "object";
    int[] codePoints = name.codePoints().limit(120).toArray();
    name = new String(codePoints, 0, codePoints.length);
    return "attachment; filename=\"download\"; filename*=UTF-8''"
        + encode(name).replace("*", "%2A");
  }

  private static String optional(String value) {
    return value == null || value.isEmpty() ? "—" : value;
  }

  private static String bytes(long size, Ui ui) {
    if (size < 0) return "—";
    return NumberFormat.getIntegerInstance(Locale.forLanguageTag(ui.language())).format(size)
        + " B";
  }

  private static String date(Instant instant, Ui ui) {
    return instant == null
        ? "—"
        : DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                .withLocale(Locale.forLanguageTag(ui.language()))
                .withZone(ZoneOffset.UTC)
                .format(instant)
            + " UTC";
  }
}
