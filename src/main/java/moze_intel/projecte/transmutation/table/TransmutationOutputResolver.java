package moze_intel.projecte.transmutation.table;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import moze_intel.projecte.emc.EmcMappingSnapshot;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.ItemStackKey;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.player.PlayerKnowledge;

/**
 * Computes the transmutation-table output candidates: every item the player knows that has a
 * positive EMC value, sorted descending by EMC, filtered to those affordable given the available
 * EMC and split into stable pages. This is the pure logic that drives the output slots; the live
 * menu wires it to slot updates.
 */
public final class TransmutationOutputResolver {
    /** Maximum number of output slots the table shows at once. */
    public static final int OUTPUT_SLOT_COUNT = 16;

    private TransmutationOutputResolver() {
    }

    /**
     * @param snapshot  the current authoritative EMC mapping.
     * @param knowledge the player's learned knowledge (or full-knowledge flag).
     * @param available the player's current EMC balance; only affordable items are returned.
     * @return the first page of affordable known items, descending by EMC.
     */
    public static List<NormalizedStackKey> resolve(
          EmcMappingSnapshot<NormalizedStackKey> snapshot,
          PlayerKnowledge knowledge,
          EmcValue available
    ) {
        return resolvePage(snapshot, knowledge, available, 0).outputs();
    }

    public static Page resolvePage(
          EmcMappingSnapshot<NormalizedStackKey> snapshot,
          PlayerKnowledge knowledge,
          EmcValue available,
          int requestedPage
    ) {
        return resolvePage(snapshot, knowledge, available, Optional.empty(), requestedPage);
    }

    /**
     * Resolves one page using the same affordability and lock-cap rules as upstream ProjectE.
     * A zero-valued lock is ignored; any positive lock caps the displayed EMC range.
     */
    public static Page resolvePage(
          EmcMappingSnapshot<NormalizedStackKey> snapshot,
          PlayerKnowledge knowledge,
          EmcValue available,
          Optional<EmcValue> lockLimit,
          int requestedPage
    ) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(knowledge, "knowledge");
        Objects.requireNonNull(available, "available");
        Objects.requireNonNull(lockLimit, "lockLimit");
        EmcValue maximum = lockLimit
              .filter(value -> value.longValue() > 0)
              .filter(value -> value.compareTo(available) < 0)
              .orElse(available);
        List<NormalizedStackKey> candidates = new ArrayList<>();
        for (NormalizedStackKey key : snapshot.values().keySet()) {
            if (!(key instanceof ItemStackKey)) {
                continue;
            }
            if (!knowledge.has(key)) {
                continue;
            }
            EmcValue value = snapshot.valueFor(key).orElse(EmcValue.ZERO);
            if (value.longValue() <= 0) {
                continue;
            }
            if (value.compareTo(maximum) > 0) {
                continue;
            }
            candidates.add(key);
        }
        candidates.sort(Comparator
              .comparing((NormalizedStackKey key) -> snapshot.valueFor(key).orElse(EmcValue.ZERO))
              .reversed()
              .thenComparing(NormalizedStackKey::canonicalString));
        int pageCount = Math.max(1, (candidates.size() + OUTPUT_SLOT_COUNT - 1) / OUTPUT_SLOT_COUNT);
        int pageIndex = Math.max(0, Math.min(requestedPage, pageCount - 1));
        int from = pageIndex * OUTPUT_SLOT_COUNT;
        int to = Math.min(from + OUTPUT_SLOT_COUNT, candidates.size());
        return new Page(candidates.subList(from, to), pageIndex, pageCount);
    }

    public record Page(List<NormalizedStackKey> outputs, int pageIndex, int pageCount) {
        public Page {
            outputs = List.copyOf(outputs);
            if (pageIndex < 0 || pageCount < 1 || pageIndex >= pageCount) {
                throw new IllegalArgumentException(
                      "invalid transmutation page " + pageIndex + " of " + pageCount);
            }
        }

        public boolean hasPrevious() {
            return pageIndex > 0;
        }

        public boolean hasNext() {
            return pageIndex + 1 < pageCount;
        }
    }
}
