package io.github.erf88.stackemu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.qute.Engine;
import io.quarkus.qute.HtmlEscaper;
import io.quarkus.qute.Variant;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Isolated real Qute rendering; no application endpoint or emulator operation. */
class CssPrototypeTest {
  @Test
  void rendersEscapedPrototypeFixturesWithLiteralUtilityBranches() throws Exception {
    var engine =
        Engine.builder()
            .addDefaults()
            .addResultMapper(new HtmlEscaper(List.of("text/html")))
            .build();
    var template =
        engine.parse(
            Files.readString(Path.of("src/test/resources/ux0/prototype.html")),
            Variant.forContentType("text/html"));
    var output = Path.of("work/ux0-fixtures");
    Files.createDirectories(output);
    for (String language : List.of("en", "pt-BR")) {
      boolean portuguese = language.equals("pt-BR");
      var labels =
          Map.ofEntries(
              Map.entry("title", portuguese ? "Protótipo CSS" : "CSS prototype"),
              Map.entry(
                  "notice",
                  portuguese
                      ? "Dados ilustrativos, sem operações."
                      : "Illustrative data, no operations."),
              Map.entry("actions", portuguese ? "Ações" : "Actions"),
              Map.entry("action", portuguese ? "Ação de exemplo" : "Example action"),
              Map.entry("disabled", portuguese ? "Desabilitado" : "Disabled"),
              Map.entry("objects", portuguese ? "Objetos" : "Objects"),
              Map.entry("name", portuguese ? "Nome" : "Name"),
              Map.entry("size", portuguese ? "Tamanho" : "Size"),
              Map.entry("fields", portuguese ? "Campos" : "Fields"),
              Map.entry("readonly", portuguese ? "Somente leitura" : "Read only"),
              Map.entry("file", portuguese ? "Arquivo" : "File"),
              Map.entry(
                  "confirm", portuguese ? "Confirmação ilustrativa" : "Illustrative confirmation"),
              Map.entry("feedback", "Feedback"),
              Map.entry("details", portuguese ? "Detalhes" : "Details"),
              Map.entry(
                  "dynamicNote",
                  portuguese
                      ? "Classe fragmentada: controle negativo."
                      : "Fragmented class: negative control."));
      for (String candidate : List.of("current", "tailwind", "pico")) {
        for (String theme : List.of("dark", "light", "auto")) {
          var instance = template.instance();
          labels.forEach(instance::data);
          var html =
              instance
                  .data("language", language)
                  .data("theme", theme)
                  .data("candidate", candidate)
                  .data("utility", candidate.equals("tailwind"))
                  .data("spacing", 37)
                  .data("key", "samples/ação +% <img src=x>.txt")
                  .render();
          assertTrue(html.contains("&lt;img"));
          assertFalse(html.contains("<img"));
          assertTrue(html.contains("p-37"));
          assertFalse(html.contains("{#if"));
          Files.writeString(
              output.resolve(candidate + "-" + language + "-" + theme + ".html"), html);
        }
      }
    }
  }
}
