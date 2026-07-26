package moze_intel.projecte.content.items;

import moze_intel.projecte.content.ModDataComponents;
import moze_intel.projecte.player.PlayerDataService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;

import java.util.function.Consumer;

/**
 * Harvest Goddess Band — accelerates crop growth in a radius.
 * Right-click to toggle on/off. Consumes EMC while active.
 */
public class HarvestGoddessBandItem extends ActiveEmcItem {
    private static final long EMC_PER_TICK = 4L;
    private static final int RADIUS = 7;
    private static final int BONEMEAL_INTERVAL = 20;

    public HarvestGoddessBandItem(Properties properties) {
        super(properties);
    }

    @Override public long getEmcPerTick() { return EMC_PER_TICK; }

    @Override
    public boolean isActive(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.STORED_EMC, 0L) == 1L;
    }

    @Override
    public void setActive(ItemStack stack, boolean active) {
        stack.set(ModDataComponents.STORED_EMC, active ? 1L : 0L);
    }

    @Override
    public void onTick(Player player, ItemStack stack, PlayerDataService service) {
        if (!isActive(stack)) return;
        Level level = player.level();
        // 骨粉效果每 20 tick 施加一次，计费必须同频；此前每 tick 扣费，
        // 实际花费是生效频率的 20 倍。
        if (level.isClientSide() || level.getGameTime() % BONEMEAL_INTERVAL != 0) return;
        super.onTick(player, stack, service);
        if (!isActive(stack)) return;
        for (BlockPos pos : BlockPos.betweenClosed(
              BlockPos.containing(player.getBoundingBox().minX - RADIUS, player.getBoundingBox().minY - RADIUS, player.getBoundingBox().minZ - RADIUS),
              BlockPos.containing(player.getBoundingBox().maxX + RADIUS, player.getBoundingBox().maxY + RADIUS, player.getBoundingBox().maxZ + RADIUS))) {
            var state = level.getBlockState(pos);
            if (state.getBlock() instanceof BonemealableBlock boneMealable && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                if (boneMealable.isValidBonemealTarget(level, pos, state)) {
                    boneMealable.performBonemeal(serverLevel, level.getRandom(), pos, state);
                }
            }
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean wasActive = isActive(stack);
        setActive(stack, !wasActive);
        if (!level.isClientSide()) {
            player.sendSystemMessage(Component.translatable(
                  "item.projecte.harvest_goddess_band." + (wasActive ? "off" : "on")));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("item.projecte.active_" +
              (isActive(stack) ? "on" : "off")));
    }
}
