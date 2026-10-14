package io.github.erf88.stackemu.adapter.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import org.junit.jupiter.api.Test;

class UiContextTest {
  @Test
  void malformedPreferencesFallBackAndCannotEnterLinks() {
    var ui = UiContext.resolve("<script>", "invalid;q=no", "<script>", "/connection");
    assertEquals("en", ui.language());
    assertEquals("dark", ui.theme());
    assertEquals("/connection?lang=en&theme=light", ui.toggleTheme());
  }

  @Test
  void endpointCredentialsAndTokensNeverReachView() {
    var view =
        new ConnectionSettingsView(
            URI.create("http://user:secret@localhost:4566/path?token=secret#secret"), "us-east-1");
    assertEquals("http://localhost:4566/path", view.endpoint());
  }
}
