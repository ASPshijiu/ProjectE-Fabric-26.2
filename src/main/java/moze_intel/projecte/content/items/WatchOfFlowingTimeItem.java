package moze_intel.projecte.content.items;

import moze_intel.projecte.content.ModDataComponents;
import moze_intel.projecte.player.PlayerDataService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

/** Watch of Flowing Time — accelerates block ticks (crops, furnaces) around the wearer. */
public class WatchOfFlowingTimeItem extends ActiveEmcItem {
    private static final int RADIUS = 8;
    /** 每刻额外触发的随机刻次数（上游同样以"额外随机刻"实现加速）。 */
    private static final int EXTRA_RANDOM_TICKS = 12;

    public WatchOfFlowingTimeItem(Properties p) { super(p); }
    @Override public long getEmcPerTick() { return 4L; }
    @Override public boolean isActive(ItemStack s) { return s.getOrDefault(ModDataComponents.STORED_EMC, 0L) == 1L; }
    @Override public void setActive(ItemStack s, boolean a) { s.set(ModDataComponents.STORED_EMC, a ? 1L : 0L); }

    @Override
    public void onTick(Player player, ItemStack stack, PlayerDataService service) {
        if (!isActive(stack)) return;
        super.onTick(player, stack, service);
        if (!isActive(stack)) return;
        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel)) return;

        BlockPos center = player.blockPosition();
        RandomSource random = serverLevel.getRandom();
        for (int attempt = 0; attempt < EXTRA_RANDOM_TICKS; attempt++) {
            BlockPos pos = center.offset(
                  random.nextInt(RADIUS * 2 + 1) - RADIUS,
                  random.nextInt(RADIUS * 2 + 1) - RADIUS,
                  random.nextInt(RADIUS * 2 + 1) - RADIUS);
            if (!serverLevel.isLoaded(pos)) continue;
            BlockState state = serverLevel.getBlockState(pos);
            // 只加速随机刻方块（作物/树苗等）。方块实体由自身 ticker 驱动，
            // 额外推进会造成产出翻倍，故不在此处干预。
            if (state.isRandomlyTicking()) {
                state.randomTick(serverLevel, pos, random);
            }
        }
    }

    @Override public InteractionResult use(Level l, Player p, net.minecraft.world.InteractionHand h) {
        ItemStack s = p.getItemInHand(h); boolean was = isActive(s); setActive(s, !was);
        if (!l.isClientSide()) p.sendSystemMessage(Component.translatable("item.projecte.watch_of_flowing_time." + (was ? "off" : "on")));
        return InteractionResult.SUCCESS;
    }
    @Override public void appendHoverText(ItemStack s, Item.TooltipContext c, TooltipDisplay d, Consumer<Component> t, TooltipFlag f) {
        super.appendHoverText(s, c, d, t, f);
        t.accept(Component.translatable("item.projecte.active_" + (isActive(s) ? "on" : "off")));
    }
}
