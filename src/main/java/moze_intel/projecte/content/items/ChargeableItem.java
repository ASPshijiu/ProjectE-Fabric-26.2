package moze_intel.projecte.content.items;

import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Base item that renders the shared ProjectE charge level as an inventory bar. */
public abstract class ChargeableItem extends Item implements IItemCharge {
    protected ChargeableItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getCharge(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getChargePercent(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(getChargePercent(stack) / 3.0F, 1.0F, 1.0F);
    }
}
