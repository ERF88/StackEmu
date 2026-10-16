package io.github.erf88.stackemu.adapter.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class WebAssetCatalogTest {
  @Test
  void packagedStylesContainCompiledUtilitiesAndTheirLicense() throws Exception {
    try (var stylesheet = getClass().getResourceAsStream("/META-INF/resources/app.css");
        var license =
            getClass().getResourceAsStream("/META-INF/licenses/tailwindcss-LICENSE.txt")) {
      var css = new String(stylesheet.readAllBytes(), StandardCharsets.UTF_8);
      assertTrue(css.contains(".grid{"));
      assertTrue(css.contains("--palette-dark-canvas"));
      assertTrue(!css.contains("@source") && !css.contains("@import"));
      assertTrue(
          new String(license.readAllBytes(), StandardCharsets.UTF_8).contains("MIT License"));
    }
  }

  @Test
  void bundledUrlsHaveStableContentFingerprints() {
    var assets = new WebAssetCatalog();
    assertTrue(assets.stylesheetHref().matches("/app\\.css\\?v=[a-f0-9]{64}"));
    assertTrue(assets.faviconHref().matches("/favicon\\.svg\\?v=[a-f0-9]{64}"));
    assertTrue(assets.scriptHref().matches("/app\\.js\\?v=[a-f0-9]{64}"));
    assertEquals(assets.stylesheetHref(), new WebAssetCatalog().stylesheetHref());
  }

  @Test
  void changedContentChangesFingerprintAndMatchesKnownSha256Vector() {
    assertEquals(
        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
        WebAssetCatalog.fingerprint(new byte[0]));
    assertNotEquals(
        WebAssetCatalog.fingerprint("a".getBytes(StandardCharsets.UTF_8)),
        WebAssetCatalog.fingerprint("b".getBytes(StandardCharsets.UTF_8)));
  }
}
