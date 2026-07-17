package moze_intel.projecte.content.items;

import moze_intel.projecte.content.ModDataComponents;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.player.PlayerDataService;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Swiftwolf's Rending Gale — grants creative-style flight while active.
 * Consumes EMC per tick from the player's EMC pool.
 * Right-click to toggle on/off.
 */
public class SwiftwolfRendingGaleItem extends ActiveEmcItem {
    private static final long EMC_PER_TICK = 2L;

    public SwiftwolfRendingGaleItem(Properties properties) {
        super(properties);
    }

    @Override
    public long getEmcPerTick() {
        return EMC_PER_TICK;
    }

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
        if (service.emc().compareTo(EmcValue.of(EMC_PER_TICK)) < 0) {
            setActive(stack, false);
            revokeFlight(player);
            return;
        }
        super.onTick(player, stack, service);
        if (!player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean wasActive = isActive(stack);
        if (!level.isClientSide() && wasActive && !player.isCreative() && !player.isSpectator()) {
            revokeFlight(player);
        }
        setActive(stack, !wasActive);
        if (!level.isClientSide()) {
            player.sendSystemMessage(
                  Component.translatable("item.projecte.swiftwolf_rending_gale."
                        + (wasActive ? "off" : "on")));
        }
        return InteractionResult.SUCCESS;
    }

    private static void revokeFlight(Player player) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        tooltip.accept(Component.translatable("item.projecte.active_" +
              (isActive(stack) ? "on" : "off")));
    }
}
