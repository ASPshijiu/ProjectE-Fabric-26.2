package moze_intel.projecte.network.payloads;

import moze_intel.projecte.api.ProjectEAPI;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;

/**
 * Client-to-server payload requesting a charge adjustment on the held item. {@code negative} true
 * means discharge (shift-held on the client). The server resolves the held item in either hand,
 * checks it implements {@link moze_intel.projecte.content.items.IItemCharge}, and applies the change
 * authoritatively.
 */
public record ChargeItemPayload(InteractionHand hand, boolean negative) implements CustomPacketPayload {
    public static final Identifier ID = ProjectEAPI.id("charge_item");
    public static final Type<ChargeItemPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ChargeItemPayload> STREAM_CODEC =
          StreamCodec.composite(
                ByteBufCodecs.idMapper(i -> InteractionHand.values()[i], InteractionHand::ordinal),
                ChargeItemPayload::hand,
                ByteBufCodecs.BOOL,
                ChargeItemPayload::negative,
                ChargeItemPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
