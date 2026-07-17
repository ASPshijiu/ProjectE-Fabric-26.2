package moze_intel.projecte.content.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MatterShearsItem extends ShearsItem implements MatterTool {
    private final int maxCharge;
    private final float chargeSpeedModifier;

    public MatterShearsItem(Properties properties, int maxCharge, float chargeSpeedModifier) {
        super(properties);
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
    @Override public boolean isBarVisible(ItemStack stack) { return chargeBarVisible(stack); }
    @Override public int getBarWidth(ItemStack stack) { return chargeBarWidth(stack); }
    @Override public int getBarColor(ItemStack stack) { return chargeBarColor(stack); }
}
