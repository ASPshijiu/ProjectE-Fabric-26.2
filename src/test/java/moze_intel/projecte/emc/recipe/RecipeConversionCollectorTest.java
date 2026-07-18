package moze_intel.projecte.emc.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import java.util.Map;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.FakeStackKey;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.emc.reload.EmcReloadProcessor;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class RecipeConversionCollectorTest {
    private static final FakeStackKey A = key("a");
    private static final FakeStackKey B = key("b");
    private static final FakeStackKey C = key("c");
    private static final FakeStackKey BUCKET = key("bucket");
    private static final FakeStackKey OUTPUT = key("output");
    private static final FakeStackKey MILK_BUCKET = key("milk_bucket");
    private static final FakeStackKey DUST = key("dust");
    private final RecipeConversionCollector collector = new RecipeConversionCollector();

    @Test
    void mergesDuplicateIngredientsAndAppliesReturnedContainers() {
        List<RecipeConversion> conversions = collector.collect(
              id("duplicate"),
              2,
              OUTPUT,
              List.of(List.of(A), List.of(A), List.of(BUCKET)),
              Map.of(BUCKET, 1),
              16
        );
        assertEquals(1, conversions.size());
        assertEquals(2, conversions.getFirst().outputCount());
        assertEquals(Map.of(A, 2), conversions.getFirst().ingredients());
    }

    @Test
    void expandsAlternativesInCanonicalOrder() {
        List<RecipeConversion> conversions = collector.collect(
              id("alternatives"),
              1,
              OUTPUT,
              List.of(List.of(B, A), List.of(C)),
              Map.of(),
              16
        );
        assertEquals(List.of(
              Map.of(A, 1, C, 1),
              Map.of(B, 1, C, 1)
        ), conversions.stream().map(RecipeConversion::ingredients).toList());
    }

    @Test
    void removesPermutationDuplicatesBeforeEnforcingExpansionLimit() {
        List<NormalizedStackKey> choices = List.of(A, B, C);
        List<RecipeConversion> conversions = collector.collect(
              id("repeated_alternatives"),
              1,
              OUTPUT,
              List.of(choices, choices, choices),
              Map.of(),
              10
        );

        assertEquals(10, conversions.size());
        assertEquals(1, conversions.stream()
              .filter(conversion -> conversion.ingredients().equals(Map.of(A, 2, B, 1)))
              .count());
    }

    @Test
    void condensesLargeIndependentAlternativeGroupsWithoutChangingMinimumCost() {
        List<NormalizedStackKey> firstGroup = List.of(A, B, C);
        List<NormalizedStackKey> secondGroup = List.of(B, C);
        List<RecipeConversion> conversions = collector.collectCondensed(
              id("condensed"),
              1,
              OUTPUT,
              List.of(
                    firstGroup, firstGroup, firstGroup, firstGroup, firstGroup, firstGroup,
                    secondGroup, secondGroup),
              Map.of(),
              5
        );

        Map<NormalizedStackKey, EmcValue> values = new EmcReloadProcessor().extend(
              Map.of(A, EmcValue.of(10), B, EmcValue.of(2), C, EmcValue.of(4)), conversions);

        assertEquals(6, conversions.size());
        assertEquals(EmcValue.of(16), values.get(OUTPUT));
    }

    @Test
    void appliesRemaindersOnlyForSelectedAlternatives() {
        List<RecipeConversion> conversions = collector.collectWithRemainders(
              id("remainders"),
              1,
              OUTPUT,
              List.of(List.of(MILK_BUCKET, DUST)),
              Map.of(MILK_BUCKET, BUCKET),
              16
        );
        assertEquals(List.of(
              Map.of(DUST, 1),
              Map.of(MILK_BUCKET, 1, BUCKET, -1)
        ), conversions.stream().map(RecipeConversion::ingredients).toList());
    }

    @Test
    void ignoresRecipesWithEmptyChoicesAndEnforcesExpansionLimit() {
        assertEquals(List.of(), collector.collect(
              id("empty"), 1, OUTPUT, List.of(List.of(A), List.of()), Map.of(), 16
        ));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> collector.collect(
              id("too_many"), 1, OUTPUT,
              List.of(List.of(A, B, C), List.of(A, B, C), List.of(A, B, C)),
              Map.of(), 8
        ));
        assertEquals("projecte:too_many expands to more than 8 ingredient combinations", error.getMessage());
    }

    @Test
    void validatesOutputAndCounts() {
        assertThrows(IllegalArgumentException.class, () -> collector.collect(
              id("zero_output"), 0, OUTPUT, List.of(List.of(A)), Map.of(), 16
        ));
        assertThrows(NullPointerException.class, () -> collector.collect(
              id("null_output"), 1, null, List.of(List.of(A)), Map.of(), 16
        ));
    }

    private static FakeStackKey key(String path) {
        return new FakeStackKey(id(path));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("projecte", path);
    }
}
