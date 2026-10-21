package io.github.erf88.stackemu.adapter.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import org.junit.jupiter.api.Test;

class UiContextTest {
  @Test
  void contextualBucketActionsEncodeNamesAndKeepPreferences() {
    var ui = UiContext.resolve("pt-BR", null, "dark", "compact", "/s3");
    String suffix = "&bucket=example%20%2B%25&lang=pt-BR&theme=dark&density=compact";
    assertEquals("/s3/write?action=upload" + suffix, ui.bucketUploadLink("example +%"));
    assertEquals("/s3/write?action=folder" + suffix, ui.bucketFolderLink("example +%"));
  }

  @Test
  void densityDefaultsAndUntrustedValuesFallBackWithoutReflection() {
    for (String value : new String[] {null, "comfortable", "COMPACT", "<script>", ""}) {
      var ui = UiContext.resolve("en", null, "dark", value, "/connection");
      assertEquals("comfortable", ui.density());
      assertEquals("lang=en&theme=dark", ui.query());
    }
  }

  @Test
  void compactDensitySurvivesPreferencesAndOpaquePageStateWithoutDuplicates() {
    String page = "/s3/bucket?bucket=example&prefix=a%2B%26%2F&token=opaque%2B%3D";
    var ui = UiContext.resolve("pt-BR", null, "auto", "compact", page);
    assertEquals("compact", ui.density());
    assertEquals("lang=pt-BR&theme=auto&density=compact", ui.query());
    assertEquals(page + "&lang=en&theme=auto&density=compact", ui.english());
    assertEquals(page + "&lang=pt-BR&theme=auto&density=compact", ui.portuguese());
    assertEquals(page + "&lang=pt-BR&theme=dark&density=compact", ui.themeLink("dark"));
    assertEquals(page + "&lang=pt-BR&theme=dark&density=compact", ui.toggleTheme());
    assertEquals(
        page + "&lang=pt-BR&theme=auto&density=comfortable", ui.densityLink("comfortable"));
    assertThrows(IllegalArgumentException.class, () -> ui.densityLink("<script>"));
  }

  @Test
  void malformedPreferencesFallBackAndCannotEnterLinks() {
    var ui = UiContext.resolve("<script>", "invalid;q=no", "<script>", "/connection");
    assertEquals("en", ui.language());
    assertEquals("auto", ui.theme());
    assertEquals("/connection?lang=en&theme=dark", ui.toggleTheme());
  }

  @Test
  void systemThemeIsDefaultAndExplicitPreferencesRemainInLinks() {
    var automatic = UiContext.resolve("pt-BR", null, null, "/connection");
    assertEquals("auto", automatic.theme());
    assertEquals("lang=pt-BR&theme=auto", automatic.query());
    assertEquals("/connection?lang=en&theme=auto", automatic.english());
    var dark = UiContext.resolve("en", null, "dark", "/connection");
    assertEquals("dark", dark.theme());
    assertEquals("/connection?lang=en&theme=light", dark.toggleTheme());
    var light = UiContext.resolve("en", null, "light", "/connection");
    assertEquals("light", light.theme());
    assertEquals("/connection?lang=en&theme=auto", light.toggleTheme());
  }

  @Test
  void endpointCredentialsAndTokensNeverReachView() {
    var view =
        new ConnectionSettingsView(
            URI.create("http://user:secret@localhost:4566/path?token=secret#secret"), "us-east-1");
    assertEquals("http://localhost:4566/path", view.endpoint());
  }

  @Test
  void explicitThemeLinksKeepPageAndLanguageAndRejectUnknownValues() {
    var ui = UiContext.resolve("pt-BR", null, "auto", "/s3/bucket?bucket=example&prefix=folder%2F");
    assertEquals(
        "/s3/bucket?bucket=example&prefix=folder%2F&lang=pt-BR&theme=dark", ui.themeLink("dark"));
    assertEquals(
        "/s3/bucket?bucket=example&prefix=folder%2F&lang=pt-BR&theme=light", ui.themeLink("light"));
    assertEquals(
        "/s3/bucket?bucket=example&prefix=folder%2F&lang=pt-BR&theme=auto", ui.themeLink("auto"));
    assertThrows(IllegalArgumentException.class, () -> ui.themeLink("<script>"));
  }
}
