package io.github.erf88.stackemu.adapter.web;

import io.github.erf88.stackemu.adapter.web.s3.S3ViewMapper;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public record UiContext(
    String language, String theme, String density, String page, Map<String, String> labels) {
  public static UiContext resolve(String lang, String accept, String theme, String page) {
    return resolve(lang, accept, theme, null, page);
  }

  public static UiContext resolve(
      String lang, String accept, String theme, String density, String page) {
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
        "compact".equals(density) ? "compact" : "comfortable",
        page,
        bundle.keySet().stream()
            .collect(Collectors.toUnmodifiableMap(key -> key, bundle::getString)));
  }

  public String text(String key) {
    return labels.get(key);
  }

  public String contextLabel() {
    if (page.startsWith("/operations")) return text("operations");
    if (page.startsWith("/sns")) return "SNS";
    if (page.startsWith("/sqs")) return "SQS";
    if (page.startsWith("/s3") || page.startsWith("/connection")) return "S3";
    return text(page.equals("/components") ? "gallery" : "home");
  }

  public boolean hasConnectionDetails() {
    return page.startsWith("/s3") || page.startsWith("/sqs") || page.startsWith("/connection");
  }

  public String query() {
    return "lang=" + language + "&theme=" + theme + densityQuery();
  }

  public String english() {
    return page + separator() + "lang=en&theme=" + theme + densityQuery();
  }

  public String portuguese() {
    return page + separator() + "lang=pt-BR&theme=" + theme + densityQuery();
  }

  public String toggleTheme() {
    return page
        + separator()
        + "lang="
        + language
        + "&theme="
        + (theme.equals("auto") ? "dark" : theme.equals("dark") ? "light" : "auto")
        + densityQuery();
  }

  public String bucketLink(String bucket) {
    return S3ViewMapper.bucketPath(bucket, "", null) + "&" + query();
  }

  public String objectLink(String bucket, String key) {
    return S3ViewMapper.objectPath(bucket, key) + "&" + query();
  }

  public String bucketUploadLink(String bucket) {
    return "/s3/write?action=upload&bucket=" + S3ViewMapper.encode(bucket) + "&" + query();
  }

  public String bucketFolderLink(String bucket) {
    return "/s3/write?action=folder&bucket=" + S3ViewMapper.encode(bucket) + "&" + query();
  }

  public String bucketDeletionLink(String bucket) {
    return "/s3/delete-bucket?bucket=" + S3ViewMapper.encode(bucket) + "&" + query();
  }

  public String bucketVersionsLink(String bucket) {
    return "/s3/versions?bucket=" + S3ViewMapper.encode(bucket) + "&" + query();
  }

  public String connectionDetailsLink() {
    if (page.startsWith("/sqs")) return "/sqs/connection?" + query();
    boolean s3Context = page.startsWith("/s3") || page.equals("/connection?from=s3");
    return "/connection?" + query() + (s3Context ? "&from=s3" : "");
  }

  public String themeLink(String selected) {
    if (!java.util.Set.of("auto", "dark", "light").contains(selected)) {
      throw new IllegalArgumentException("Unsupported theme preference");
    }
    return page + separator() + "lang=" + language + "&theme=" + selected + densityQuery();
  }

  public String densityLink(String selected) {
    if (!java.util.Set.of("comfortable", "compact").contains(selected)) {
      throw new IllegalArgumentException("Unsupported density preference");
    }
    return page + separator() + "lang=" + language + "&theme=" + theme + "&density=" + selected;
  }

  private String densityQuery() {
    return density.equals("compact") ? "&density=compact" : "";
  }

  private String separator() {
    return page.contains("?") ? "&" : "?";
  }
}
