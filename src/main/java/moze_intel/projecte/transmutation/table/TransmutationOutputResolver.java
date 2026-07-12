package moze_intel.projecte.transmutation.table;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import moze_intel.projecte.emc.EmcMappingSnapshot;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.player.PlayerKnowledge;

/**
 * Computes the transmutation-table output candidates: every item the player knows that has a
 * positive EMC value, sorted ascending by EMC, filtered to those affordable given the available
 * EMC. This is the pure logic that drives the output slots; the live menu wires it to slot updates.
 */
public final class TransmutationOutputResolver {
    /** Maximum number of output slots the table shows at once. */
    public static final int OUTPUT_SLOT_COUNT = 16;

    private TransmutationOutputResolver() {
    }

    /**
     * @param snapshot  the current authoritative EMC mapping.
     * @param knowledge the player's learned knowledge (or full-knowledge flag).
     * @param available the player's current EMC balance (output is filtered to affordable items;
     *                  pass {@link EmcValue#ZERO} to compute the full knowledge ordering without
     *                  affordability filtering for display previews).
     * @return up to {@link #OUTPUT_SLOT_COUNT} affordable known items, ascending by EMC.
     */
    public static List<NormalizedStackKey> resolve(
          EmcMappingSnapshot<NormalizedStackKey> snapshot,
          PlayerKnowledge knowledge,
          EmcValue available
    ) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(knowledge, "knowledge");
        Objects.requireNonNull(available, "available");
        List<NormalizedStackKey> candidates = new ArrayList<>();
        for (NormalizedStackKey key : snapshot.values().keySet()) {
            if (!knowledge.has(key)) {
                continue;
            }
            EmcValue value = snapshot.valueFor(key).orElse(EmcValue.ZERO);
            if (value.longValue() <= 0) {
                continue;
            }
            candidates.add(key);
        }
        candidates.sort(Comparator
              .comparing((NormalizedStackKey key) -> snapshot.valueFor(key).orElse(EmcValue.ZERO))
              .thenComparing(NormalizedStackKey::canonicalString));
        List<NormalizedStackKey> affordable = new ArrayList<>();
        for (NormalizedStackKey key : candidates) {
            EmcValue value = snapshot.valueFor(key).orElse(EmcValue.ZERO);
            if (available.compareTo(value) < 0) {
                continue;
            }
            affordable.add(key);
            if (affordable.size() >= OUTPUT_SLOT_COUNT) {
                break;
            }
        }
        return List.copyOf(affordable);
    }
}
