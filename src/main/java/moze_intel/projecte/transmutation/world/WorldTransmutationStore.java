package moze_intel.projecte.transmutation.world;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Single authoritative owner of the current {@link WorldTransmutationRegistry}.
 *
 * <p>Starts empty and is replaced atomically by {@link WorldTransmutationReloadListener} on every
 * successful data reload; a failed reload preserves the prior registry.
 */
public final class WorldTransmutationStore {
    private static final AtomicReference<WorldTransmutationRegistry> CURRENT =
          new AtomicReference<>(WorldTransmutationRegistry.empty());

    private WorldTransmutationStore() {
    }

    public static WorldTransmutationRegistry current() {
        return CURRENT.get();
    }

    public static WorldTransmutationRegistry replace(WorldTransmutationRegistry next) {
        return CURRENT.getAndSet(next);
    }
}
