package moze_intel.projecte.transmutation.world;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.world.level.block.Block;

/**
 * Immutable snapshot of all loaded world transmutations keyed by origin block.
 *
 * <p>Replaced atomically by {@link WorldTransmutationReloadListener} on every successful data
 * reload. The registry exposes only read access; the default empty snapshot has no entries.
 */
public final class WorldTransmutationRegistry {
    private static final WorldTransmutationRegistry EMPTY = new WorldTransmutationRegistry(Map.of());

    private final Map<Block, List<SimpleWorldTransmutation>> entries;

    private WorldTransmutationRegistry(Map<Block, List<SimpleWorldTransmutation>> entries) {
        Map<Block, List<SimpleWorldTransmutation>> copy = new LinkedHashMap<>();
        entries.forEach((block, list) -> copy.put(block, List.copyOf(list)));
        this.entries = Collections.unmodifiableMap(copy);
    }

    public static WorldTransmutationRegistry empty() {
        return EMPTY;
    }

    /**
     * Build a registry from the given transmutations. Duplicate origin+result pairs across all
     * inputs are rejected.
     */
    public static WorldTransmutationRegistry of(List<SimpleWorldTransmutation> transmutations) {
        Objects.requireNonNull(transmutations, "transmutations");
        Map<Block, List<SimpleWorldTransmutation>> byOrigin = new LinkedHashMap<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (SimpleWorldTransmutation entry : transmutations) {
            String signature = entry.origin().unwrapKey().orElseThrow() + "->"
                  + entry.result().unwrapKey().orElseThrow();
            if (!seen.add(signature)) {
                throw new IllegalStateException("duplicate world transmutation: " + signature);
            }
            byOrigin.computeIfAbsent(entry.origin().value(), b -> new java.util.ArrayList<>()).add(entry);
        }
        return new WorldTransmutationRegistry(byOrigin);
    }

    public List<SimpleWorldTransmutation> forOrigin(Block block) {
        Objects.requireNonNull(block, "block");
        return entries.getOrDefault(block, List.of());
    }

    public Map<Block, List<SimpleWorldTransmutation>> entries() {
        return entries;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
