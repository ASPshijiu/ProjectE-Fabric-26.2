package moze_intel.projecte.network.payloads;

import moze_intel.projecte.api.ProjectEAPI;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ArmorTogglePayload(Action action) implements CustomPacketPayload {
    public static final Identifier ID = ProjectEAPI.id("armor_toggle");
    public static final Type<ArmorTogglePayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, ArmorTogglePayload> STREAM_CODEC =
          StreamCodec.composite(
                ByteBufCodecs.idMapper(Action::byId, Action::ordinal),
                ArmorTogglePayload::action,
                ArmorTogglePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Action {
        HELMET,
        BOOTS,
        ACTIVE,
        EXPLODE,
        ZAP;

        private static Action byId(int id) {
            return values()[Math.floorMod(id, values().length)];
        }
    }
}
