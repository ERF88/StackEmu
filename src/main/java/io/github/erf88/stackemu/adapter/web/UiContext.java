package io.github.erf88.stackemu.adapter.web;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public record UiContext(String language, String theme, String page, Map<String, String> labels) {
  public static UiContext resolve(String lang, String accept, String theme, String page) {
    String language = "en";
    if ("pt-BR".equals(lang)) language = "pt-BR";
    else if (lang == null && accept != null) {
      try {
        Locale selected =
            Locale.lookup(
                Locale.LanguageRange.parse(accept),
                java.util.List.of(Locale.ENGLISH, Locale.forLanguageTag("pt-BR")));
        if (selected != null && selected.getLanguage().equals("pt")) language = "pt-BR";
      } catch (IllegalArgumentException ignored) {
        // A malformed language header retains the safe default; it is not a page error.
      }
    }
    ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag(language));
    return new UiContext(
        language,
        "light".equals(theme) ? "light" : "dark".equals(theme) ? "dark" : "auto",
        page,
        bundle.keySet().stream()
            .collect(Collectors.toUnmodifiableMap(key -> key, bundle::getString)));
  }

  public String text(String key) {
    return labels.get(key);
  }

  public String query() {
    return "lang=" + language + "&theme=" + theme;
  }

  public String english() {
    return page + separator() + "lang=en&theme=" + theme;
  }

  public String portuguese() {
    return page + separator() + "lang=pt-BR&theme=" + theme;
  }

  public String toggleTheme() {
    return page
        + separator()
        + "lang="
        + language
        + "&theme="
        + (theme.equals("auto") ? "dark" : theme.equals("dark") ? "light" : "auto");
  }

  public String bucketLink(String bucket) {
    return S3ViewMapper.bucketPath(bucket, "", null) + "&" + query();
  }

  public String themeLink(String selected) {
    if (!java.util.Set.of("auto", "dark", "light").contains(selected)) {
      throw new IllegalArgumentException("Unsupported theme preference");
    }
    return page + separator() + "lang=" + language + "&theme=" + selected;
  }

  private String separator() {
    return page.contains("?") ? "&" : "?";
  }
}
