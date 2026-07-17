package moze_intel.projecte.network.payloads;

import moze_intel.projecte.api.ProjectEAPI;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;

/** Requests a server-authoritative ProjectE item key action. */
public record PhilosophersStoneActionPayload(InteractionHand hand, Action action)
      implements CustomPacketPayload {
    public static final Identifier ID = ProjectEAPI.id("philosophers_stone_action");
    public static final Type<PhilosophersStoneActionPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, PhilosophersStoneActionPayload>
          STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.idMapper(
                      i -> InteractionHand.values()[Math.floorMod(i, InteractionHand.values().length)],
                      InteractionHand::ordinal),
                PhilosophersStoneActionPayload::hand,
                ByteBufCodecs.idMapper(Action::byId, Action::ordinal),
                PhilosophersStoneActionPayload::action,
                PhilosophersStoneActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Action {
        MODE,
        EXTRA_FUNCTION,
        PROJECTILE;

        private static Action byId(int id) {
            return values()[Math.floorMod(id, values().length)];
        }
    }
}
