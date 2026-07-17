package moze_intel.projecte.content.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MatterAxeItem extends AxeItem implements MatterTool {
    private final int maxCharge;
    private final float chargeSpeedModifier;

    public MatterAxeItem(ToolMaterial material, float attackDamage, float attackSpeed,
          Properties properties, int maxCharge, float chargeSpeedModifier) {
        super(material, attackDamage, attackSpeed, properties);
        this.maxCharge = maxCharge;
        this.chargeSpeedModifier = chargeSpeedModifier;
    }

    @Override public int getMaxCharge(ItemStack stack) { return maxCharge; }
    @Override public float getChargeSpeedModifier() { return chargeSpeedModifier; }
    @Override public float getDestroySpeed(ItemStack stack, BlockState state) {
        return chargedDestroySpeed(super.getDestroySpeed(stack, state), stack);
    }
    @Override public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos,
          LivingEntity miner) { return true; }
    @Override public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) { }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null) return InteractionResult.PASS;
        int charge = getCharge(context.getItemInHand());
        InteractionResult result = ToolHelper.useAOE(
              context, Items.NETHERITE_AXE, charge, false);
        if (result.consumesAction()) {
            return result;
        }
        return ToolHelper.clearConnected(
              context.getLevel(), context.getPlayer(), context.getItemInHand(),
              context.getClickedPos(), BlockTags.LOGS, 5 * charge);
    }
    @Override public boolean isBarVisible(ItemStack stack) { return chargeBarVisible(stack); }
    @Override public int getBarWidth(ItemStack stack) { return chargeBarWidth(stack); }
    @Override public int getBarColor(ItemStack stack) { return chargeBarColor(stack); }
}
