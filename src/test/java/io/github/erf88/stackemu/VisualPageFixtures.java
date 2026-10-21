package io.github.erf88.stackemu;

import static io.restassured.RestAssured.given;

import io.github.erf88.stackemu.adapter.web.BucketsResource;
import io.github.erf88.stackemu.adapter.web.ConnectionResource;
import io.github.erf88.stackemu.adapter.web.ConnectionSettingsView;
import io.github.erf88.stackemu.adapter.web.HomeResource;
import io.github.erf88.stackemu.adapter.web.S3Resource;
import io.github.erf88.stackemu.adapter.web.S3ViewMapper;
import io.github.erf88.stackemu.adapter.web.UiContext;
import io.github.erf88.stackemu.application.S3ReadPort;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Deterministic presentation fixtures, not SDK, connectivity or compatibility evidence. */
final class VisualPageFixtures {
  private VisualPageFixtures() {}

  static void write(String language, String theme) throws IOException {
    var directory = Path.of("work", "browser-fixtures");
    Files.createDirectories(directory);
    var settings = new ConnectionSettingsView(URI.create("http://ministack:4566"), "us-east-1");
    var ui = UiContext.resolve(language, null, theme, "/");
    save(directory, "overview", language, theme, HomeResource.Templates.index(ui).render());
    save(
        directory,
        "service",
        language,
        theme,
        BucketsResource.Templates.index(
                List.of("visual-example"),
                true,
                UiContext.resolve(language, null, theme, "/s3"),
                settings)
            .render());
    save(
        directory,
        "connection",
        language,
        theme,
        ConnectionResource.Templates.connection(
                List.of("visual-example"),
                true,
                UiContext.resolve(language, null, theme, "/connection"),
                settings)
            .render());
    var modified = Instant.parse("2026-01-01T12:00:00Z");
    var details =
        new S3ReadPort.BucketDetails(
            "visual-example",
            "us-east-1",
            Map.of("purpose", "visual-fixture"),
            S3ReadPort.TagsState.AVAILABLE);
    var listing =
        new S3ReadPort.ObjectPage(
            List.of("samples/nested/"),
            List.of(new S3ReadPort.ObjectSummary("samples/ação +%.txt", 44, modified)),
            "fixture-next");
    var bucketUi =
        UiContext.resolve(
            language, null, theme, "/s3/bucket?bucket=visual-example&prefix=samples%2F");
    save(
        directory,
        "bucket",
        language,
        theme,
        S3Resource.Templates.bucket(
                S3ViewMapper.toBucketPage(details, listing, "samples/", 50, bucketUi),
                bucketUi,
                settings)
            .render());
    var objectUi =
        UiContext.resolve(
            language,
            null,
            theme,
            "/s3/object?bucket=visual-example&key=samples%2Fa%C3%A7%C3%A3o%20%2B%25.txt");
    var object =
        new S3ReadPort.ObjectDetails(
            "samples/ação +%.txt", 44, "text/plain", "fixture-etag", modified);
    save(
        directory,
        "object",
        language,
        theme,
        S3Resource.Templates.object(
                S3ViewMapper.toObjectPage("visual-example", object, objectUi), objectUi)
            .render());
    for (var page :
        Map.of(
                "gallery",
                "/components",
                "form",
                "/s3/write?action=bucket",
                "unavailable",
                "/s3",
                "error",
                "/s3/bucket?bucket=visual-example")
            .entrySet()) {
      var response =
          given().queryParam("lang", language).queryParam("theme", theme).get(page.getValue());
      int expected = page.getKey().equals("error") ? 503 : 200;
      if (response.statusCode() != expected)
        throw new IllegalStateException("Unexpected fixture response: " + page.getKey());
      save(directory, page.getKey(), language, theme, response.asString());
    }
  }

  private static void save(Path directory, String page, String language, String theme, String html)
      throws IOException {
    if (html.contains("hidden-test-value"))
      throw new IllegalStateException("Fixture contains a test secret");
    Files.writeString(
        directory.resolve(page + "-" + language + "-" + theme + ".html"),
        html,
        StandardCharsets.UTF_8);
  }
}
