package io.github.erf88.stackemu.adapter.web;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
public record Ui(String language, String theme, String page, Map<String,String> labels) {
    public static Ui resolve(String lang, String accept, String theme, String page) {
        String language = "en";
        if ("pt-BR".equals(lang)) language = "pt-BR";
        else if (lang == null && accept != null) {
            try {
                Locale selected = Locale.lookup(Locale.LanguageRange.parse(accept), java.util.List.of(Locale.ENGLISH, Locale.forLanguageTag("pt-BR")));
                if (selected != null && selected.getLanguage().equals("pt")) language = "pt-BR";
            } catch (IllegalArgumentException ignored) { }
        }
        ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag(language));
        return new Ui(language, "light".equals(theme) ? "light" : "dark", page,
            bundle.keySet().stream().collect(Collectors.toUnmodifiableMap(key -> key, bundle::getString)));
    }
    public String text(String key) { return labels.get(key); }
    public String query() { return "lang=" + language + "&theme=" + theme; }
    public String english() { return page + "?lang=en&theme=" + theme; }
    public String portuguese() { return page + "?lang=pt-BR&theme=" + theme; }
    public String toggleTheme() { return page + "?lang=" + language + "&theme=" + (theme.equals("dark") ? "light" : "dark"); }
}
