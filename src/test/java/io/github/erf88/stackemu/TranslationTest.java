package io.github.erf88.stackemu;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Locale;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;
class TranslationTest {
    @Test void localesHaveIdenticalKeysAndNonEmptyValues() throws Exception {
        var english = new java.util.Properties();
        var portuguese = new java.util.Properties();
        try (var en = getClass().getResourceAsStream("/messages_en.properties");
             var pt = getClass().getResourceAsStream("/messages_pt_BR.properties")) {
            english.load(new java.io.InputStreamReader(en, java.nio.charset.StandardCharsets.UTF_8));
            portuguese.load(new java.io.InputStreamReader(pt, java.nio.charset.StandardCharsets.UTF_8));
        }
        assertEquals(english.keySet(), portuguese.keySet());
        for (Object key : english.keySet()) {
            assertFalse(english.getProperty(key.toString()).isBlank());
            assertFalse(portuguese.getProperty(key.toString()).isBlank());
        }
    }
}
