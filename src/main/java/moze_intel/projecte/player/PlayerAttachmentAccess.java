package moze_intel.projecte.player;

import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Read/write boundary over a player's attachments, abstracted so service logic is unit-testable
 * without the Fabric mixin-backed attachment registry.
 *
 * <p>The key is an opaque token ({@link Object}); production wiring passes Fabric
 * {@code AttachmentType} instances, tests pass plain tokens. This keeps the service free of any
 * class-load that would trigger the mixin-only sync subsystem.
 */
@FunctionalInterface
public interface PlayerAttachmentAccess {
    <A> A modify(Object key, Class<A> type, UnaryOperator<A> modifier);

    /**
     * Convenience: read the current value of an attachment.
     */
    default <A> A get(Object key, Class<A> type) {
        return modify(key, type, UnaryOperator.identity());
    }

    /**
     * Build an in-memory access for tests. Initial values are supplied per key.
     */
    static PlayerAttachmentAccess inMemory(Function<Object, Object> initial) {
        java.util.Map<Object, Object> store = new java.util.concurrent.ConcurrentHashMap<>();
        return new PlayerAttachmentAccess() {
            @Override
            public <A> A modify(Object key, Class<A> type, UnaryOperator<A> modifier) {
                @SuppressWarnings("unchecked")
                A current = (A) store.computeIfAbsent(key, initial);
                A next = modifier.apply(current);
                store.put(key, next);
                return current;
            }
        };
    }
}
