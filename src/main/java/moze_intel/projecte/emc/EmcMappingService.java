package moze_intel.projecte.emc;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class EmcMappingService<K> {
    private final AtomicReference<EmcMappingSnapshot<K>> current = new AtomicReference<>(EmcMappingSnapshot.empty());

    public EmcMappingSnapshot<K> current() {
        return current.get();
    }

    public EmcMappingSnapshot<K> replace(Map<K, EmcValue> values) {
        Objects.requireNonNull(values, "values");
        while (true) {
            EmcMappingSnapshot<K> previous = current.get();
            EmcMappingSnapshot<K> next = new EmcMappingSnapshot<>(Math.incrementExact(previous.version()), values);
            if (current.compareAndSet(previous, next)) {
                return next;
            }
        }
    }

    public RebuildResult<K> rebuild(Supplier<Map<K, EmcValue>> builder) {
        Objects.requireNonNull(builder, "builder");
        EmcMappingSnapshot<K> previous = current();
        try {
            EmcMappingSnapshot<K> next = replace(Objects.requireNonNull(builder.get(), "rebuilt values"));
            return new RebuildResult<>(true, next, Optional.empty());
        } catch (RuntimeException exception) {
            return new RebuildResult<>(false, previous, Optional.of(exception));
        }
    }

    public record RebuildResult<K>(
          boolean success,
          EmcMappingSnapshot<K> snapshot,
          Optional<RuntimeException> exception
    ) {
        public RebuildResult {
            Objects.requireNonNull(snapshot, "snapshot");
            Objects.requireNonNull(exception, "exception");
            if (success == exception.isPresent()) {
                throw new IllegalArgumentException("successful rebuilds must not contain an exception");
            }
        }
    }
}
