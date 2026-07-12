package moze_intel.projecte.emc;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class EmcMappingSnapshot<K> {
    private final long version;
    private final Map<K, EmcValue> values;

    public EmcMappingSnapshot(long version, Map<K, EmcValue> values) {
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
        this.version = version;
        Objects.requireNonNull(values, "values");
        LinkedHashMap<K, EmcValue> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> copy.put(
              Objects.requireNonNull(key, "EMC key"),
              Objects.requireNonNull(value, "EMC value")
        ));
        this.values = Collections.unmodifiableMap(copy);
    }

    public static <K> EmcMappingSnapshot<K> empty() {
        return new EmcMappingSnapshot<>(0, Map.of());
    }

    public long version() {
        return version;
    }

    public Map<K, EmcValue> values() {
        return values;
    }

    public Optional<EmcValue> valueFor(K key) {
        return Optional.ofNullable(values.get(key));
    }
}
