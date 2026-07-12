package moze_intel.projecte.emc.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import java.util.Map;
import moze_intel.projecte.api.mapper.EmcMappingCollector;
import moze_intel.projecte.api.mapper.EmcValueGenerator;
import moze_intel.projecte.emc.EmcValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmcGraphMapperTest {
    private EmcMappingCollector<String> collector;
    private EmcValueGenerator<String> generator;

    @BeforeEach
    void setUp() {
        EmcGraphMapper<String> mapper = EmcGraphMapper.create();
        collector = mapper.collector();
        generator = mapper.generator();
    }

    @Test
    void derivesSimpleAndMultiOutputValues() {
        collector.setValueBefore("a", EmcValue.of(1));
        collector.addConversion(1, "b", Map.of("a", 2));
        collector.addConversion(2, "c", Map.of("b", 2));
        Map<String, EmcValue> values = generator.generate();
        assertEquals(EmcValue.of(1), values.get("a"));
        assertEquals(EmcValue.of(2), values.get("b"));
        assertEquals(EmcValue.of(2), values.get("c"));
    }

    @Test
    void choosesLowestValidConversion() {
        collector.setValueBefore("a", EmcValue.of(2));
        collector.setValueBefore("b", EmcValue.of(9));
        collector.addConversion(1, "out", Map.of("a", 3));
        collector.addConversion(1, "out", Map.of("b", 1));
        assertEquals(EmcValue.of(6), generator.generate().get("out"));
    }

    @Test
    void appliesSetAfterWithoutPropagation() {
        collector.setValueBefore("a", EmcValue.of(2));
        collector.addConversion(1, "b", Map.of("a", 2));
        collector.addConversion(1, "c", Map.of("b", 2));
        collector.setValueAfter("b", EmcValue.of(100));
        Map<String, EmcValue> values = generator.generate();
        assertEquals(EmcValue.of(100), values.get("b"));
        assertEquals(EmcValue.of(8), values.get("c"));
    }

    @Test
    void supportsReturnedIngredientsAndIgnoresZeroAmounts() {
        collector.setValueBefore("container", EmcValue.of(5));
        collector.setValueBefore("content", EmcValue.of(10));
        collector.addConversion(1, "filled", Map.of("container", 1, "content", 1));
        collector.addConversion(1, "content", Map.of("filled", 1, "container", -1));
        collector.addConversion(1, "dependent", Map.of("content", 1, "missing", 0));
        Map<String, EmcValue> values = generator.generate();
        assertEquals(EmcValue.of(15), values.get("filled"));
        assertEquals(EmcValue.of(10), values.get("dependent"));
    }

    @Test
    void treatsFreeInputsAsZeroCostWithoutPublishingThem() {
        collector.setValueBefore("a", EmcValue.of(2));
        collector.setFree("catalyst");
        collector.addConversion(1, "out", Map.of("a", 1, "catalyst", 1));
        Map<String, EmcValue> values = generator.generate();
        assertEquals(EmcValue.of(2), values.get("out"));
        assertFalse(values.containsKey("catalyst"));
    }

    @Test
    void fixedConversionCannotBeUndercutByRegularRecipes() {
        collector.setValueBefore("a", EmcValue.of(1));
        collector.setValueFromConversion(1, "b", Map.of("a", 3));
        collector.addConversion(1, "b", Map.of("a", 1));
        collector.addConversion(1, "c", Map.of("b", 2));
        Map<String, EmcValue> values = generator.generate();
        assertEquals(EmcValue.of(3), values.get("b"));
        assertEquals(EmcValue.of(6), values.get("c"));
    }

    @Test
    void leavesUnresolvedCyclesAndOverflowUnmapped() {
        collector.addConversion(1, "x", Map.of("y", 1));
        collector.addConversion(1, "y", Map.of("x", 1));
        collector.setValueBefore("max", EmcValue.of(Long.MAX_VALUE));
        collector.addConversion(1, "overflow", Map.of("max", 2));
        Map<String, EmcValue> values = generator.generate();
        assertFalse(values.containsKey("x"));
        assertFalse(values.containsKey("y"));
        assertFalse(values.containsKey("overflow"));
    }
}
