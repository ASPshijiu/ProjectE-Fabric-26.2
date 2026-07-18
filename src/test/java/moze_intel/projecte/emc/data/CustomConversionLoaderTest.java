package moze_intel.projecte.emc.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.FakeStackKey;
import moze_intel.projecte.emc.ItemStackKey;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.emc.recipe.RecipeConversion;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class CustomConversionLoaderTest {
    private static final Path DEFAULTS = Path.of(
          "src/main/resources/data/projecte/pe_custom_conversions/defaults.json");
    private final CustomConversionLoader loader = new CustomConversionLoader();

    @Test
    void bundledDefaultsPreserveComponentsAndSignedIngredientAmounts() throws Exception {
        CustomConversionLoader.Result result = loader.load(Map.of(
              projecteId("pe_custom_conversions/defaults.json"), Files.readString(DEFAULTS)));
        Map<NormalizedStackKey, EmcValue> values = result.explicit().stream()
              .collect(Collectors.toMap(ExplicitEmcEntry::key, ExplicitEmcEntry::value));
        RecipeConversion mud = conversionFor(result, item("mud"));
        RecipeConversion skullPattern = conversionFor(result, item("skull_banner_pattern"));

        assertEquals(208, result.explicit().size());
        assertEquals(1, result.freeKeys().size());
        assertEquals(77, result.conversions().size());
        assertTrue(result.freeKeys().contains(new FakeStackKey(projecteId("fluid/minecraft/water"))));
        assertEquals(EmcValue.of(40_960), values.get(item("heavy_core")));
        assertEquals(EmcValue.of(96), values.get(item("goat_horn", Map.of(
              "minecraft:instrument", JsonParser.parseString("\"minecraft:ponder_goat_horn\"")))));
        assertEquals(EmcValue.of(192), values.get(item("goat_horn", Map.of(
              "minecraft:instrument", JsonParser.parseString("\"minecraft:admire_goat_horn\"")))));

        assertEquals(Map.of(
              item("dirt"), 1,
              item("potion", Map.of("minecraft:potion_contents", JsonParser.parseString(
                    "{\"potion\":\"minecraft:water\"}"))), 1,
              item("glass_bottle"), -1
        ), mud.ingredients());
        assertEquals(3, skullPattern.outputCount());
        assertEquals(Map.of(
              item("paper"), 3,
              item("nether_star"), 1,
              item("soul_sand"), -4
        ), skullPattern.ingredients());
    }

    @Test
    void malformedFileDoesNotPartiallyApplyValues() {
        CustomConversionLoader.Result result = loader.load(Map.of(
              projecteId("pe_custom_conversions/good.json"), """
                    {"values":{"before":[
                      {"id":"minecraft:stone","emc_value":1}
                    ]}}
                    """,
              projecteId("pe_custom_conversions/broken.json"), """
                    {
                      "values":{"before":[
                        {"id":"minecraft:diamond","emc_value":8192}
                      ]},
                      "groups":{"broken":{"conversions":[{
                        "ingredients":[{"id":"minecraft:dirt","amount":"invalid"}],
                        "output":{"id":"minecraft:grass_block"}
                      }]}}
                    }
                    """));

        assertEquals(1, result.explicit().size());
        assertEquals(item("stone"), result.explicit().getFirst().key());
        assertTrue(result.conversions().isEmpty());
    }

    private static RecipeConversion conversionFor(
          CustomConversionLoader.Result result, NormalizedStackKey output
    ) {
        return result.conversions().stream()
              .filter(conversion -> conversion.output().equals(output))
              .findFirst()
              .orElseThrow();
    }

    private static ItemStackKey item(String path) {
        return item(path, Map.of());
    }

    private static ItemStackKey item(String path, Map<String, com.google.gson.JsonElement> components) {
        return new ItemStackKey(Identifier.fromNamespaceAndPath("minecraft", path), components);
    }

    private static Identifier projecteId(String path) {
        return Identifier.fromNamespaceAndPath("projecte", path);
    }
}
