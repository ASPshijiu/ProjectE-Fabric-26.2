package moze_intel.projecte.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class EquipmentAssetContractTest {
    @Test
    void armorAssetsProvideHumanoidAndLeggingsTextures() throws Exception {
        Path assets = Path.of("src/main/resources/assets/projecte");
        for (String armor : new String[]{"dark_matter", "red_matter", "gem"}) {
            JsonObject root = JsonParser.parseString(
                  Files.readString(assets.resolve("equipment").resolve(armor + ".json")))
                  .getAsJsonObject();
            JsonObject layers = root.getAsJsonObject("layers");
            assertTrue(layers.has("humanoid"), armor + " is missing the humanoid layer");
            assertTrue(layers.has("humanoid_leggings"), armor + " is missing the leggings layer");
            assertEquals(1, layers.getAsJsonArray("humanoid").size());
            assertEquals(1, layers.getAsJsonArray("humanoid_leggings").size());
            assertTextureExists(assets, layers, "humanoid", armor);
            assertTextureExists(assets, layers, "humanoid_leggings", armor);
        }
    }

    private void assertTextureExists(Path assets, JsonObject layers, String layer, String armor) {
        String texture = layers.getAsJsonArray(layer).get(0).getAsJsonObject()
              .get("texture").getAsString();
        String prefix = "projecte:";
        assertTrue(texture.startsWith(prefix), "unexpected equipment texture namespace: " + texture);
        Path file = assets.resolve("textures/entity/equipment")
              .resolve(layer)
              .resolve(texture.substring(prefix.length()) + ".png");
        assertTrue(Files.isRegularFile(file), armor + " is missing " + file);
    }
}
