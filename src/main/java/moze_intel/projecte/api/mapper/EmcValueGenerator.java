package moze_intel.projecte.api.mapper;

import java.util.Map;
import moze_intel.projecte.emc.EmcValue;

@FunctionalInterface
public interface EmcValueGenerator<K> {
    Map<K, EmcValue> generate();
}
