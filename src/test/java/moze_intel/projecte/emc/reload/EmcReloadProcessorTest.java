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
