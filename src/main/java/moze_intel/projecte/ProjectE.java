package moze_intel.projecte;

import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.content.ModCreativeTab;
import moze_intel.projecte.content.ModDataComponents;
import moze_intel.projecte.content.ModEntityTypes;
import moze_intel.projecte.content.ModFuels;
import moze_intel.projecte.content.ModItems;
import moze_intel.projecte.content.ModMenuTypes;
import moze_intel.projecte.content.ModRecipeSerializers;
import moze_intel.projecte.content.ModBlocks;
import moze_intel.projecte.command.ProjectECommands;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.emc.ProjectEEmc;
import moze_intel.projecte.emc.reload.EmcReloadListener;
import moze_intel.projecte.emc.reload.RecipeConversionSource;
import moze_intel.projecte.emc.reload.VanillaRecipeConversionSource;
import moze_intel.projecte.event.PlayerInventoryTickHandler;
import moze_intel.projecte.event.PhilosophersStoneInteractionHandler;
import moze_intel.projecte.network.ProjectENetworking;
import moze_intel.projecte.player.PlayerAttachments;
import moze_intel.projecte.player.PlayerAttachmentKeys;
import moze_intel.projecte.player.PlayerDataService;
import moze_intel.projecte.player.PlayerSyncHandlers;
import moze_intel.projecte.transmutation.world.WorldTransmutationReloadListener;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public final class ProjectE implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(ProjectEAPI.MOD_ID);

    /** Tracks the running server so reload callbacks can fan the shared EMC mapping to players. */
    private static volatile MinecraftServer server;

    @Override
    public void onInitialize() {
        PlayerAttachments.init();
        ModDataComponents.init();
        ModRecipeSerializers.init();
        ModEntityTypes.init();
        ModItems.init();
        PhilosophersStoneInteractionHandler.register();
        ModCreativeTab.init();
        ModBlocks.init();
        ModFuels.init();
        ModMenuTypes.init();
        ProjectENetworking.init();

        PlayerSyncHandlers syncHandlers = new PlayerSyncHandlers(snapshot ->
              ProjectENetworking.sendEmcMappingToAll(server, snapshot));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, srv) -> {
            if (handler.getPlayer() == null) return;
            ServerPlayer player = handler.getPlayer();
            syncHandlers.onPlayerJoin(new PlayerDataService(
                  PlayerAttachmentKeys.fabricAdapter(player)));
            // Push the authoritative shared EMC mapping to the joining client so tooltips and the
            // transmutation resolver work immediately, before the next reload.
            ProjectENetworking.sendEmcMapping(player, ProjectEEmc.service().current());
        });

        // EMC reload from data/projecte/emc/*.json files plus pe_custom_conversions and the live
        // vanilla recipe graph (resolved from the running server on each reload).
        EmcReloadListener reloadListener = new EmcReloadListener(List.of())
              .withServerRecipeSources(ProjectE::buildServerRecipeSources)
              .withReloadCallback(syncHandlers::onEmcReloaded);
        reloadListener.register();

        // The initial data-pack reload precedes the server lifecycle events. Bind the instance at
        // SERVER_STARTING, then wait until SERVER_STARTED to extend the mapping: RecipeManager is
        // already available at that point and PlayerList is initialized before sync callbacks run.
        ServerLifecycleEvents.SERVER_STARTING.register(s -> server = s);
        ServerLifecycleEvents.SERVER_STARTED.register(s -> {
            server = s;
            reloadListener.refreshRecipeMappings();
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> server = null);

        // World transmutations
        new WorldTransmutationReloadListener().register();

        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) ->
              ProjectECommands.register(dispatcher, context));

        PlayerInventoryTickHandler.register();

        LOGGER.info("Initializing ProjectE for Fabric 26.2");
    }

    /**
     * Builds the recipe-conversion sources that need the live server context (the vanilla
     * {@link VanillaRecipeConversionSource} reads the running {@code RecipeManager} and registries).
     * Returns an empty list when the server is not yet running (e.g. during early data generation).
     */
    private static List<RecipeConversionSource> buildServerRecipeSources() {
        MinecraftServer current = server;
        if (current == null) {
            return List.of();
        }
        return List.of(new VanillaRecipeConversionSource(
              current.getRecipeManager(), current.registryAccess()));
    }

    /** Returns the active logical server when one is running. */
    public static Optional<MinecraftServer> currentServer() {
        return Optional.ofNullable(server);
    }
}
