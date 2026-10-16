package io.github.erf88.stackemu.adapter.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.erf88.stackemu.application.S3ReadPort;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class S3ViewMapperTest {
  @Test
  void binarySizesRetainExactLocalizedByteCounts() {
    var expected =
        Map.of(
            -1L,
            "—",
            0L,
            "0 B",
            1023L,
            "1,023 B",
            1024L,
            "1 KiB (1,024 B)",
            1536L,
            "1.5 KiB (1,536 B)",
            1048575L,
            "1,024 KiB (1,048,575 B)",
            1048576L,
            "1 MiB (1,048,576 B)");
    expected.forEach((size, label) -> assertEquals(label, renderedSize(size, "en")));
    assertEquals("1,5 KiB (1.536 B)", renderedSize(1536, "pt-BR"));
    assertEquals("1,01 KiB (1.034 B)", renderedSize(1034, "pt-BR"));
  }

  @Test
  void binaryUnitBoundariesAndLongMaximumRemainRepresentable() {
    long size = 1;
    for (var unit : List.of("KiB", "MiB", "GiB", "TiB", "PiB", "EiB")) {
      size = Math.multiplyExact(size, 1024);
      assertTrue(renderedSize(size, "en").startsWith("1 " + unit + " ("));
    }
    assertEquals("8 EiB (9,223,372,036,854,775,807 B)", renderedSize(Long.MAX_VALUE, "en"));
  }

  @Test
  void fileKindHintsAndQuickDownloadsPreserveOpaqueKeys() {
    var ui = UiContext.resolve("en", null, "dark", "/");
    var kinds =
        Map.of(
            "folder/CONFIG.JSON",
            "JSON",
            "folder/photo.JpG",
            "Image",
            "folder/.json",
            "File",
            "a.json/no-extension",
            "File",
            "folder/",
            "File",
            "folder/a.svg",
            "File",
            "nested/ação +%<x>.TXT",
            "Text");
    kinds.forEach(
        (key, kind) -> {
          var view =
              S3ViewMapper.toBucketPage(
                  new S3ReadPort.BucketDetails(
                      "bucket", null, Map.of(), S3ReadPort.TagsState.EMPTY),
                  new S3ReadPort.ObjectPage(
                      List.of(),
                      List.of(
                          new S3ReadPort.ObjectSummary(
                              key, 1536, Instant.parse("2026-01-01T12:00:00Z"))),
                      null),
                  "",
                  50,
                  ui);
          var row = view.objects().getFirst();
          assertEquals(key, row.key());
          assertEquals(kind, row.kind());
          assertEquals(
              "/s3/download?bucket=bucket&key=" + S3ViewMapper.encode(key) + "&lang=en&theme=dark",
              row.downloadHref());
          assertTrue(row.modified().endsWith(" UTC"));
          assertTrue(row.modified().contains("12:00"));
        });
  }

  private static String renderedSize(long size, String language) {
    return S3ViewMapper.toObjectPage(
            "bucket",
            new S3ReadPort.ObjectDetails("file", size, null, null, null),
            UiContext.resolve(language, null, "auto", "/"))
        .size();
  }

  @Test
  void missingMetadataAndUnknownTagsAreExplicit() {
    var ui = UiContext.resolve("pt-BR", null, "light", "/");
    var object =
        S3ViewMapper.toObjectPage(
            "bucket", new S3ReadPort.ObjectDetails("file", -1, null, null, null), ui);
    assertEquals("—", object.size());
    assertEquals("—", object.contentType());
    assertEquals("—", object.etag());
    assertEquals("—", object.modified());
    var bucket =
        S3ViewMapper.toBucketPage(
            new S3ReadPort.BucketDetails("bucket", null, Map.of(), S3ReadPort.TagsState.UNKNOWN),
            new S3ReadPort.ObjectPage(List.of(), List.of(), null),
            "",
            50,
            ui);
    assertEquals("—", bucket.region());
    assertTrue(bucket.tagsMessage().contains("desconhecido"));
  }

  @Test
  void changingLocaleOrThemeKeepsObjectAndPrefixContext() {
    String path = S3ViewMapper.bucketPath("bucket", "a/ü +%/", "token+/%");
    var ui = UiContext.resolve("pt-BR", null, "dark", path);
    assertEquals(path + "&lang=en&theme=dark", ui.english());
    assertEquals(path + "&lang=pt-BR&theme=light", ui.toggleTheme());
    assertTrue(path.contains("prefix=a%2F%C3%BC%20%2B%25%2F"));
    assertTrue(path.contains("token=token%2B%2F%25"));
  }
}
