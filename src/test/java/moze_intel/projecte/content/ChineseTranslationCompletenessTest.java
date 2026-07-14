package moze_intel.projecte.content;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ChineseTranslationCompletenessTest {
    private static final Path LANG = Path.of("src/main/resources/assets/projecte/lang");

    @Test
    void everyEnglishTranslationHasReadableChineseText() throws Exception {
        JsonObject english = read("en_us.json");
        JsonObject chinese = read("zh_cn.json");

        assertTrue(english.has("tooltip.projecte.philostone"),
              "en_us.json is missing the Philosopher's Stone crafting hint");

        for (String key : english.keySet()) {
            assertTrue(chinese.has(key), "zh_cn.json is missing " + key);
            String value = chinese.get(key).getAsString();
            assertFalse(value.isBlank(), key + " has an empty Chinese translation");
            assertFalse(value.contains("\uFFFD"), key + " contains a replacement character");
        }
    }

    private JsonObject read(String file) throws Exception {
        return JsonParser.parseString(Files.readString(LANG.resolve(file))).getAsJsonObject();
    }
}
