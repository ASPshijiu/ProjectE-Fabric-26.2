package moze_intel.projecte.emc.graph;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import moze_intel.projecte.api.mapper.EmcMappingCollector;
import moze_intel.projecte.api.mapper.EmcValueGenerator;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.SimpleGraphMapper;
import moze_intel.projecte.emc.arithmetic.HiddenBigFractionArithmetic;
import moze_intel.projecte.emc.collector.LongToBigFractionCollector;
import moze_intel.projecte.emc.generator.BigFractionToLongGenerator;
import org.apache.commons.math3.fraction.BigFraction;

public final class EmcGraphMapper<K> {
    private final HiddenBigFractionArithmetic arithmetic;
    private final SimpleGraphMapper<K, BigFraction, HiddenBigFractionArithmetic> mapper;
    private final LongToBigFractionCollector<K, HiddenBigFractionArithmetic> longCollector;
    private final BigFractionToLongGenerator<K> longGenerator;
    private final EmcMappingCollector<K> collector;

    private EmcGraphMapper() {
        arithmetic = new HiddenBigFractionArithmetic();
        mapper = new SimpleGraphMapper<>(arithmetic);
        longCollector = new LongToBigFractionCollector<>(mapper);
        longGenerator = new BigFractionToLongGenerator<>(mapper);
        collector = new CollectorAdapter();
    }

    public static <K> EmcGraphMapper<K> create() {
        return new EmcGraphMapper<>();
    }

    public EmcMappingCollector<K> collector() {
        return collector;
    }

    public EmcValueGenerator<K> generator() {
        return () -> {
            Map<K, EmcValue> values = new LinkedHashMap<>();
            longGenerator.generateValues().object2LongEntrySet().stream()
                .sorted(Map.Entry.comparingByKey((left, right) -> left.toString().compareTo(right.toString())))
                .forEach(entry -> values.put(entry.getKey(), EmcValue.of(entry.getLongValue())));
            return Collections.unmodifiableMap(values);
        };
    }

    private Object2IntOpenHashMap<K> ingredientMap(Map<K, Integer> ingredients) {
        Object2IntOpenHashMap<K> result = new Object2IntOpenHashMap<>();
        ingredients.forEach((key, amount) -> result.put(key, amount.intValue()));
        return result;
    }

    private final class CollectorAdapter implements EmcMappingCollector<K> {
        @Override
        public void addConversion(int outputCount, K output, Map<K, Integer> ingredients) {
            longCollector.addConversion(outputCount, output, ingredientMap(ingredients));
        }

        @Override
        public void setValueBefore(K key, EmcValue value) {
            longCollector.setValueBefore(key, value.longValue());
        }

        @Override
        public void setValueAfter(K key, EmcValue value) {
            longCollector.setValueAfter(key, value.longValue());
        }

        @Override
        public void setFree(K key) {
            mapper.setValueBefore(key, arithmetic.getFree());
        }

        @Override
        public void setValueFromConversion(int outputCount, K output, Map<K, Integer> ingredients) {
            longCollector.setValueFromConversion(outputCount, output, ingredientMap(ingredients));
        }
    }
}
