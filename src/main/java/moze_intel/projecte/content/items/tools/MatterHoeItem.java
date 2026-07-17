package moze_intel.projecte.content.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MatterHoeItem extends HoeItem implements MatterTool {
    private final int maxCharge;
    private final float chargeSpeedModifier;

    public MatterHoeItem(ToolMaterial material, float attackDamage, float attackSpeed,
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
    @Override public boolean isBarVisible(ItemStack stack) { return chargeBarVisible(stack); }
    @Override public int getBarWidth(ItemStack stack) { return chargeBarWidth(stack); }
    @Override public int getBarColor(ItemStack stack) { return chargeBarColor(stack); }
}
