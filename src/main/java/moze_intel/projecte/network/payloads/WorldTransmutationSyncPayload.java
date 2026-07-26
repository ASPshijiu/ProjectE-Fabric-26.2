package moze_intel.projecte.network.payloads;

import java.util.List;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.transmutation.world.SimpleWorldTransmutation;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server-to-client payload carrying every loaded world transmutation.
 *
 * <p>世界转化只在服务端由数据包加载，而贤者之石的客户端交互需要判断"这个方块能不能转化"
 * 才能正确返回 SUCCESS/PASS。专用服务器上客户端拿不到这张表时会恒返回 PASS，导致同一次
 * 右键继续触发副手物品。加入游戏与数据包重载后推送完整列表可消除该预测失配。
 */
public record WorldTransmutationSyncPayload(List<SimpleWorldTransmutation> transmutations)
      implements CustomPacketPayload {

    public static final Identifier ID = ProjectEAPI.id("world_transmutation_sync");
    public static final CustomPacketPayload.Type<WorldTransmutationSyncPayload> TYPE =
          new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, WorldTransmutationSyncPayload> STREAM_CODEC =
          StreamCodec.composite(
                SimpleWorldTransmutation.STREAM_CODEC.apply(ByteBufCodecs.list()),
                WorldTransmutationSyncPayload::transmutations,
                WorldTransmutationSyncPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
