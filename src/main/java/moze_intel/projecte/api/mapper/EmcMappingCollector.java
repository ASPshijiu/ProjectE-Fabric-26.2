package moze_intel.projecte.api.mapper;

import java.util.Map;
import moze_intel.projecte.emc.EmcValue;

public interface EmcMappingCollector<K> {
    void addConversion(int outputCount, K output, Map<K, Integer> ingredients);
    void setValueBefore(K key, EmcValue value);
    void setValueAfter(K key, EmcValue value);
    void setFree(K key);
    void setValueFromConversion(int outputCount, K output, Map<K, Integer> ingredients);
}
