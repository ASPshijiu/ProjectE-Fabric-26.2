package moze_intel.projecte;

import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.content.ModItems;
import moze_intel.projecte.content.ModMenuTypes;
import moze_intel.projecte.command.ProjectECommands;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.emc.reload.EmcReloadListener;
import moze_intel.projecte.player.PlayerAttachments;
import moze_intel.projecte.player.PlayerAttachmentKeys;
import moze_intel.projecte.player.PlayerDataService;
import moze_intel.projecte.player.PlayerSyncHandlers;
import moze_intel.projecte.transmutation.world.WorldTransmutationReloadListener;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProjectE implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(ProjectEAPI.MOD_ID);

    @Override
    public void onInitialize() {
        // Register the per-player Fabric data attachments (knowledge, emc, input locks, gem armor).
        PlayerAttachments.init();
        // Register ProjectE items (Philosopher's Stone, ...).
        ModItems.init();
        // Register menu types (transmutation table, ...).
        ModMenuTypes.init();

        // Server-authoritative sync handler: initializes attachments on join and rebroadcasts the
        // shared EMC mapping to every online player after a successful data reload. The actual S2C
        // shared-mapping payload is wired by the transmutation phase; until then the callback only
        // logs the rebroadcast so the lifecycle stays exercised.
        PlayerSyncHandlers syncHandlers = new PlayerSyncHandlers(snapshot ->
              LOGGER.debug("EMC mapping rebroadcast to {} players", snapshot.values().size()));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (handler.getPlayer() == null) {
                return;
            }
            syncHandlers.onPlayerJoin(new PlayerDataService(
                  PlayerAttachmentKeys.fabricAdapter(handler.getPlayer())));
        });

        // The vanilla 26.2 RecipeManager adapter is registered as a recipe-conversion source by
        // the content/mappers phase. Until then the reload pipeline stays wired and authoritative,
        // publishing explicit EMC values and any registered source's conversions.
        EmcReloadListener reloadListener = new EmcReloadListener(java.util.List.of())
              .withReloadCallback((java.util.function.Consumer<moze_intel.projecte.emc.EmcMappingSnapshot<NormalizedStackKey>>) syncHandlers::onEmcReloaded);
        reloadListener.register();

        // World transmutations (Philosopher's-Stone block conversions) load as server data.
        new WorldTransmutationReloadListener().register();

        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) ->
              ProjectECommands.register(dispatcher, context));

        LOGGER.info("Initializing ProjectE for Fabric 26.2");
    }
}
