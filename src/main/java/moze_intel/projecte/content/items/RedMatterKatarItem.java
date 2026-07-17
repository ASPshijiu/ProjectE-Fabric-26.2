package moze_intel.projecte.content.items;

import moze_intel.projecte.content.ModDataComponents;
import moze_intel.projecte.content.items.tools.RedMatterSwordItem.KatarMode;
import moze_intel.projecte.content.items.tools.ToolHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Red Matter Katar — a chargeable melee weapon with a key-triggered attack aura and configurable
 * hostile-only/all-target modes.
 */
public class RedMatterKatarItem extends ChargeableItem implements IItemMode, IExtraFunction {
    private static final int MAX_CHARGE = 4;

    public RedMatterKatarItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getMaxCharge(ItemStack stack) {
        return MAX_CHARGE;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        ToolHelper.attackWithCharge(stack, target, attacker);
    }

    public KatarMode getMode(ItemStack stack) {
        return KatarMode.byId(stack.getOrDefault(ModDataComponents.TOOL_MODE, 0));
    }

    @Override
    public void cycleMode(Player player, ItemStack stack) {
        var next = getMode(stack).next();
        stack.set(ModDataComponents.TOOL_MODE, next.ordinal());
        player.sendOverlayMessage(Component.translatable(
              "mode.projecte.switch",
              Component.translatable(next.translationKey())));
    }

    @Override
    public void doExtraFunction(Player player, ItemStack stack, InteractionHand hand) {
        if (player.getAttackStrengthScale(0.0F) < 1.0F) {
            return;
        }
        ToolHelper.attackAOE(
              stack, player, getMode(stack) == KatarMode.SLAY_ALL,
              1000.0F);
        player.resetAttackStrengthTicker();
    }
}
