package moze_intel.projecte.emc.reload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import java.util.List;
import java.util.Map;
import moze_intel.projecte.emc.EmcMappingService;
import moze_intel.projecte.emc.EmcMappingSnapshot;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.FakeStackKey;
import moze_intel.projecte.emc.ItemStackKey;
import moze_intel.projecte.emc.recipe.RecipeConversion;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class EmcReloadProcessorTest {
    private static final FakeStackKey A = key("a");
    private static final FakeStackKey B = key("b");
    private final EmcReloadProcessor processor = new EmcReloadProcessor();

    @Test
    void combinesExplicitValuesAndRecipeConversionsDeterministically() {
        Map<Identifier, String> explicit = Map.of(
              id("emc/values.json"), "{\"fake|projecte:a|\":{\"value\":2}}"
        );
        List<RecipeConversion> recipes = List.of(
              new RecipeConversion(id("recipe"), 1, B, Map.of(A, 3))
        );

        Map<?, EmcValue> first = processor.rebuild(explicit, recipes);
        Map<?, EmcValue> second = processor.rebuild(explicit, recipes);

        assertEquals(Map.of(A, EmcValue.of(2), B, EmcValue.of(6)), first);
        assertEquals(first, second);
    }

    @Test
    void expandsExplicitTagValuesToEveryConcreteItem() {
        Identifier ironTag = Identifier.fromNamespaceAndPath("c", "ingots/iron");
        ItemStackKey firstIngot = new ItemStackKey(
              Identifier.fromNamespaceAndPath("example", "iron_ingot"), Map.of());
        ItemStackKey secondIngot = new ItemStackKey(
              Identifier.fromNamespaceAndPath("other", "iron_ingot"), Map.of());
        Map<Identifier, String> custom = Map.of(id("pe_custom_conversions/metals.json"), """
              {"values":{"before":[
                {"type":"projecte:item","emc_value":256,"tag":"c:ingots/iron"}
              ]}}
              """);

        Map<?, EmcValue> values = processor.rebuild(
              Map.of(), custom, List.of(),
              tag -> tag.identifier().equals(ironTag) ? List.of(firstIngot, secondIngot) : List.of());

        assertEquals(EmcValue.of(256), values.get(firstIngot));
        assertEquals(EmcValue.of(256), values.get(secondIngot));
    }

    @Test
    void malformedReloadPreservesPreviousServiceSnapshot() {
        EmcMappingService<moze_intel.projecte.emc.NormalizedStackKey> service = new EmcMappingService<>();
        EmcMappingSnapshot<moze_intel.projecte.emc.NormalizedStackKey> previous = service.replace(Map.of(A, EmcValue.of(9)));

        EmcMappingService.RebuildResult<moze_intel.projecte.emc.NormalizedStackKey> result = service.rebuild(() -> processor.rebuild(
              Map.of(id("emc/broken.json"), "{\"fake|projecte:a|\":{\"value\":-1}}"),
              List.of()
        ));

        assertFalse(result.success());
        assertSame(previous, service.current());
        assertEquals(EmcValue.of(9), service.current().valueFor(A).orElseThrow());
    }

    private static FakeStackKey key(String path) {
        return new FakeStackKey(id(path));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("projecte", path);
    }
}
