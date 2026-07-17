package moze_intel.projecte.content.items.tools;

import moze_intel.projecte.content.items.IItemCharge;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/** Shared charge, speed and inventory-bar behavior for ProjectE matter tools. */
public interface MatterTool extends IItemCharge {
    float getChargeSpeedModifier();

    default float chargedDestroySpeed(float baseSpeed, ItemStack stack) {
        return baseSpeed <= 1.0F
              ? baseSpeed
              : baseSpeed + getChargeSpeedModifier() * getCharge(stack);
    }

    default boolean chargeBarVisible(ItemStack stack) {
        return getCharge(stack) > 0;
    }

    default int chargeBarWidth(ItemStack stack) {
        return Math.round(13.0F * getChargePercent(stack));
    }

    default int chargeBarColor(ItemStack stack) {
        return Mth.hsvToRgb(getChargePercent(stack) / 3.0F, 1.0F, 1.0F);
    }
}
