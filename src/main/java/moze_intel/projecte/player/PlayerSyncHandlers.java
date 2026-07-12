package moze_intel.projecte.player;

import java.util.List;
import java.util.function.Consumer;
import moze_intel.projecte.emc.EmcMappingSnapshot;
import moze_intel.projecte.emc.NormalizedStackKey;

/**
 * Server-side wiring that keeps a player's ProjectE data synced on join and rebroadcasts the
 * shared EMC mapping summary after every successful data reload.
 *
 * <p>Production registration hooks {@code ServerPlayConnectionEvents.JOIN} and the reload
 * completion callback; tests inject the lambdas directly to verify behavior without the event bus.
 */
public final class PlayerSyncHandlers {
    private final Consumer<EmcMappingSnapshot<NormalizedStackKey>> rebroadcastSharedMapping;

    public PlayerSyncHandlers(Consumer<EmcMappingSnapshot<NormalizedStackKey>> rebroadcastSharedMapping) {
        this.rebroadcastSharedMapping = rebroadcastSharedMapping;
    }

    /**
     * Called when a player joins. Touching each attachment forces initialization and the first
     * auto-sync of the player's personal EMC, knowledge, input-locks and gem-armor state to its
     * own client.
     *
     * @param access the player's data-service access boundary.
     */
    public void onPlayerJoin(PlayerDataService access) {
        // Read each attachment so Fabric initializes and syncs the default value if absent.
        access.emc();
        access.knowledge();
        access.inputLocks();
        access.gemArmorEnabled();
    }

    /**
     * Called after the EMC mapping reload completes successfully. Fans the new shared snapshot out
     * to every online player's display cache without touching their personal EMC/knowledge state.
     *
     * @param snapshot the freshly published shared EMC mapping.
     */
    public void onEmcReloaded(EmcMappingSnapshot<NormalizedStackKey> snapshot) {
        rebroadcastSharedMapping.accept(snapshot);
    }

    /**
     * Convenience for callers that need to fan out to a fixed set of players (used by tests).
     */
    public static Consumer<EmcMappingSnapshot<NormalizedStackKey>> fanOutTo(
          List<Runnable> players, Consumer<EmcMappingSnapshot<NormalizedStackKey>> perPlayer) {
        return snapshot -> players.forEach(player -> {
            player.run();
            perPlayer.accept(snapshot);
        });
    }
}
