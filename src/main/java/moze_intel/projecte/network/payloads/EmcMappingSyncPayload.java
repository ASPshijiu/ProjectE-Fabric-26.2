package moze_intel.projecte.network.payloads;

import java.util.HashMap;
import java.util.Map;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.emc.NormalizedStackKeyCodec;
import moze_intel.projecte.player.PlayerDataCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server-to-client payload carrying the full shared EMC mapping snapshot. The client receiver calls
 * {@code ProjectEEmc.service().replace(values)} to publish the server-authoritative mapping into the
 * client's display cache, enabling EMC tooltips and transmutation output computation.
 *
 * <p>The map is keyed by {@link NormalizedStackKey} (canonical string on the wire via
 * {@link NormalizedStackKeyCodec#STREAM_CODEC}) and valued by {@link EmcValue} (varlong via
 * {@link PlayerDataCodecs#EMC_STREAM_CODEC}). Sending the complete map on join and after every reload
 * matches upstream ProjectE's {@code SyncEmcPKT} semantics.
 */
public record EmcMappingSyncPayload(Map<NormalizedStackKey, EmcValue> values)
      implements CustomPacketPayload {

    public static final Identifier ID = ProjectEAPI.id("emc_mapping_sync");
    public static final CustomPacketPayload.Type<EmcMappingSyncPayload> TYPE =
          new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, EmcMappingSyncPayload> STREAM_CODEC =
          StreamCodec.composite(
                ByteBufCodecs.map(
                      HashMap::new,
                      NormalizedStackKeyCodec.STREAM_CODEC,
                      PlayerDataCodecs.EMC_STREAM_CODEC),
                EmcMappingSyncPayload::values,
                EmcMappingSyncPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
