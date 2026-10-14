package io.github.erf88.stackemu.adapter.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.erf88.stackemu.application.S3ReadPort;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class S3ViewMapperTest {
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
