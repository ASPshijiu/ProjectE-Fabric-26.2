package moze_intel.projecte.porting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrozenBaselineTest {
    private static final Path BASELINE = Path.of("docs/porting/baseline/projecte-1.21.1.json");

    @Test
    void baselineMatchesFrozenProjectECommitAndMinimumCounts() throws Exception {
        JsonObject root = JsonParser.parseString(Files.readString(BASELINE)).getAsJsonObject();
        assertEquals("15d4ce65bd06eb4222709b984255fbf5080e78bc", root.get("source_commit").getAsString());
        JsonObject categories = root.getAsJsonObject("categories");
        assertEquals(156, categories.getAsJsonArray("recipes").size());
        assertEquals(173, categories.getAsJsonArray("advancements").size());
        assertEquals(21, categories.getAsJsonArray("loot_tables").size());
        assertEquals(4, categories.getAsJsonArray("world_transmutations").size());
        assertEquals(2, categories.getAsJsonArray("custom_conversions").size());
        assertEquals(170, categories.getAsJsonArray("textures").size());
        assertEquals(15, categories.getAsJsonArray("sounds").size());
    }
}
