package moze_intel.projecte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import moze_intel.projecte.api.ProjectEAPI;
import org.junit.jupiter.api.Test;

class ProjectEIdentityTest {
    @Test
    void keepsStableProjectEIdentity() {
        assertEquals("projecte", ProjectEAPI.MOD_ID);
        assertEquals("projecte:test", ProjectEAPI.id("test").toString());
    }

    @Test
    void fabricMetadataUsesLoaderCompatibleMixinArray() throws Exception {
        Path resources = Path.of("src/main/resources");
        JsonObject metadata = JsonParser.parseString(
              Files.readString(resources.resolve("fabric.mod.json"))).getAsJsonObject();

        assertTrue(metadata.get("mixins").isJsonArray(),
              "Fabric Loader 0.19 requires fabric.mod.json mixins to be an array");
        JsonArray mixins = metadata.getAsJsonArray("mixins");
        assertTrue(mixins.size() > 0, "at least one mixin config must be declared");
        for (var mixin : mixins) {
            assertTrue(mixin.isJsonPrimitive() && mixin.getAsJsonPrimitive().isString(),
                  "each mixin config must be declared by file name");
            assertTrue(Files.isRegularFile(resources.resolve(mixin.getAsString())),
                  "missing mixin config " + mixin.getAsString());
        }
    }
}
