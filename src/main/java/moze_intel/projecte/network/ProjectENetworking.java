package moze_intel.projecte.network;

import java.util.Map;
import moze_intel.projecte.content.items.IItemCharge;
import moze_intel.projecte.content.items.PhilosophersStoneItem;
import moze_intel.projecte.emc.EmcMappingSnapshot;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.network.payloads.ChargeItemPayload;
import moze_intel.projecte.network.payloads.EmcMappingSyncPayload;
import moze_intel.projecte.network.payloads.PhilosophersStoneActionPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/**
 * Registers ProjectE's custom packet payloads with the Fabric networking API and provides the
 * server-side send helpers for the shared EMC mapping.
 *
 * <p>Called from {@link moze_intel.projecte.ProjectE#onInitialize()} during the common initialization
 * phase. Client-side receivers are registered in {@code ProjectEClient} because they reference
 * client-only classes ({@code Minecraft}, {@code ClientPlayNetworking}).
 */
public final class ProjectENetworking {
    private ProjectENetworking() {
    }

    private static volatile boolean initialized;

    /**
     * Registers payload types with the Fabric payload registry so both sides know the codec. The
     * receivers themselves are registered separately ({@code ServerPlayNetworking} here for C2S,
     * {@code ClientPlayNetworking} in the client source set for S2C).
     */
    public static void init() {
        if (initialized) return;
        initialized = true;

        // Server-to-client: the shared EMC mapping snapshot. Use registerLarge because the full
        // mapping (hundreds of entries) can exceed the default play-payload size budget.
        PayloadTypeRegistry.clientboundPlay().registerLarge(
              EmcMappingSyncPayload.TYPE, EmcMappingSyncPayload.STREAM_CODEC, 1_000_000);

        // Client-to-server: charge the held item.
        PayloadTypeRegistry.serverboundPlay().register(
              ChargeItemPayload.TYPE, ChargeItemPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ChargeItemPayload.TYPE,
              ProjectENetworking::handleChargeItem);

        PayloadTypeRegistry.serverboundPlay().register(
              PhilosophersStoneActionPayload.TYPE,
              PhilosophersStoneActionPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(
              PhilosophersStoneActionPayload.TYPE,
              ProjectENetworking::handlePhilosophersStoneAction);
    }

    private static void handlePhilosophersStoneAction(
          PhilosophersStoneActionPayload payload, ServerPlayNetworking.Context ctx
    ) {
        ctx.server().execute(() -> {
            ServerPlayer player = ctx.player();
            if (player.isSpectator()) {
                return;
            }
            ItemStack stack = player.getItemInHand(payload.hand());
            if (!(stack.getItem() instanceof PhilosophersStoneItem stone)) {
                return;
            }
            switch (payload.action()) {
                case MODE -> stone.cycleMode(player, stack);
                case EXTRA_FUNCTION -> stone.openPortableCrafting(player, stack);
                case PROJECTILE -> stone.shootMobRandomizer(player, stack);
            }
        });
    }

    private static void handleChargeItem(ChargeItemPayload payload, ServerPlayNetworking.Context ctx) {
        ServerPlayer player = ctx.player();
        if (player.isSpectator()) return;
        ItemStack stack = player.getItemInHand(payload.hand());
        if (stack.isEmpty() || !(stack.getItem() instanceof IItemCharge chargeable)) return;
        int delta = payload.negative() ? -1 : 1;
        int before = chargeable.getCharge(stack);
        ctx.server().execute(() -> {
            if (chargeable.changeCharge(player, stack, delta)) {
                player.playSound(payload.negative()
                      ? SoundEvents.TRIDENT_RETURN : SoundEvents.PLAYER_LEVELUP,
                      0.4F, payload.negative() ? 0.6F : 1.0F);
            }
        });
    }

    /**
     * Sends the current shared EMC mapping snapshot to a single player. Used on join so the new
     * player's client tooltip and transmutation resolver see the authoritative values immediately.
     */
    public static void sendEmcMapping(ServerPlayer player, EmcMappingSnapshot<NormalizedStackKey> snapshot) {
        Map<NormalizedStackKey, moze_intel.projecte.emc.EmcValue> values = snapshot.values();
        moze_intel.projecte.ProjectE.LOGGER.info(
              "Sending EMC mapping to {} ({} values)", player.getName().getString(), values.size());
        if (values.isEmpty()) {
            return;
        }
        ServerPlayNetworking.send(player, new EmcMappingSyncPayload(values));
    }

    /**
     * Fans the freshly reloaded shared EMC mapping out to every online player. Used as the reload
     * rebroadcast callback so {@code /reload} keeps every client's display cache current.
     */
    public static void sendEmcMappingToAll(MinecraftServer server, EmcMappingSnapshot<NormalizedStackKey> snapshot) {
        Map<NormalizedStackKey, moze_intel.projecte.emc.EmcValue> values = snapshot.values();
        if (values.isEmpty() || server == null) {
            return;
        }
        EmcMappingSyncPayload payload = new EmcMappingSyncPayload(values);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
