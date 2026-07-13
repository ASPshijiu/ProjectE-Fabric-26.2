package moze_intel.projecte.network;

import java.util.Map;
import moze_intel.projecte.emc.EmcMappingSnapshot;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.network.payloads.EmcMappingSyncPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

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

        // Server-to-client: the shared EMC mapping snapshot.
        PayloadTypeRegistry.clientboundPlay().register(
              EmcMappingSyncPayload.TYPE, EmcMappingSyncPayload.STREAM_CODEC);
    }

    /**
     * Sends the current shared EMC mapping snapshot to a single player. Used on join so the new
     * player's client tooltip and transmutation resolver see the authoritative values immediately.
     */
    public static void sendEmcMapping(ServerPlayer player, EmcMappingSnapshot<NormalizedStackKey> snapshot) {
        Map<NormalizedStackKey, moze_intel.projecte.emc.EmcValue> values = snapshot.values();
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
