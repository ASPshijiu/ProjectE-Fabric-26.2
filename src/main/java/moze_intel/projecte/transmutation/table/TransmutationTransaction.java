package moze_intel.projecte.transmutation.table;

import java.util.Objects;
import moze_intel.projecte.emc.EmcMappingSnapshot;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.player.PlayerDataService;

/**
 * A single server-validated transmutation-table extraction: spend EMC to materialize a known item.
 *
 * <p>Validation is total before any mutation: the item must have a positive EMC mapping, the player
 * must know it (knowledge), and the requested count must not overflow EMC or item-stack limits. On
 * success the player's EMC is reduced by the exact checked cost.
 */
public final class TransmutationTransaction {
    /** Result of an extraction attempt. */
    public record Outcome(boolean success, int producedCount, String failureReason) {
        public static Outcome success(int count) {
            return new Outcome(true, count, null);
        }

        public static Outcome failure(String reason) {
            return new Outcome(false, 0, reason);
        }
    }

    private TransmutationTransaction() {
    }

    /**
     * Attempt to extract {@code requestedCount} of {@code item} from the table.
     *
     * @param service        the player's data service (mutated on success).
     * @param snapshot       the current EMC mapping.
     * @param item           the item to extract.
     * @param requestedCount the desired count (clamped to the stack max on success).
     * @param maxCount       the per-stack item limit (e.g. {@code item.maxStackSize}).
     * @return the validated outcome.
     */
    public static Outcome extract(
          PlayerDataService service,
          EmcMappingSnapshot<NormalizedStackKey> snapshot,
          NormalizedStackKey item,
          int requestedCount,
          int maxCount
    ) {
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(item, "item");
        if (requestedCount <= 0 || maxCount <= 0) {
            return Outcome.failure("requested count must be positive");
        }
        EmcValue unitEmc = snapshot.valueFor(item).orElse(EmcValue.ZERO);
        if (unitEmc.longValue() <= 0) {
            return Outcome.failure("item has no EMC value");
        }
        if (!service.hasKnowledge(item)) {
            return Outcome.failure("player has not learned this item");
        }
        int count = Math.min(requestedCount, maxCount);
        EmcValue available = service.emc();
        long affordable = available.longValue() / unitEmc.longValue();
        if (affordable <= 0) {
            return Outcome.failure("insufficient EMC");
        }
        count = (int) Math.min(affordable, count);
        // count is bounded by available / unitEmc, so this checked multiplication cannot overflow.
        EmcValue totalCost = EmcValue.of(Math.multiplyExact(unitEmc.longValue(), count));
        // Final atomic deduction; tryRemoveEmc guards against races.
        if (!service.tryRemoveEmc(totalCost)) {
            return Outcome.failure("insufficient EMC");
        }
        return Outcome.success(count);
    }
}
