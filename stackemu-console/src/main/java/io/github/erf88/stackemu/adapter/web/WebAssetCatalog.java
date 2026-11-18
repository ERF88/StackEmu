package io.github.erf88.stackemu.adapter.web;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Fingerprints bundled public assets once per bean lifecycle, never emulator data. */
@Named("webAssets")
@ApplicationScoped
public class WebAssetCatalog {
  private final String stylesheetHref;
  private final String faviconHref;
  private final String scriptHref;

  public WebAssetCatalog() {
    stylesheetHref = versioned("app.css");
    faviconHref = versioned("favicon.svg");
    scriptHref = versioned("app.js");
  }

  public String stylesheetHref() {
    return stylesheetHref;
  }

  public String faviconHref() {
    return faviconHref;
  }

  public String scriptHref() {
    return scriptHref;
  }

  static String fingerprint(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (NoSuchAlgorithmException failure) {
      throw new IllegalStateException("Required SHA-256 digest is unavailable", failure);
    }
  }

  private static String versioned(String filename) {
    try (var source =
        WebAssetCatalog.class.getResourceAsStream("/META-INF/resources/" + filename)) {
      if (source == null)
        throw new IllegalStateException("Missing bundled public asset: " + filename);
      return "/" + filename + "?v=" + fingerprint(source.readAllBytes());
    } catch (IOException failure) {
      throw new IllegalStateException(
          "Cannot fingerprint bundled public asset: " + filename, failure);
    }
  }
}
