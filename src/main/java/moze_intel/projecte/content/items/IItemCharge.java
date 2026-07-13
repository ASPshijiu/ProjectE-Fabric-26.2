package moze_intel.projecte.content.items;

import moze_intel.projecte.content.ModDataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Mixin-style interface for items that carry a charge level (0..max) in the {@code charge} data
 * component. Default methods read and adjust the component directly so item classes only need to
 * implement {@link #getMaxCharge(ItemStack)} and opt into the interface.
 */
public interface IItemCharge {
    /**
     * @return the maximum charge the stack can hold (item-defined; e.g. DM hammer = 3).
     */
    int getMaxCharge(ItemStack stack);

    /**
     * @return the current charge (clamped to {@code [0, getMaxCharge]}). Defaults to 0 when unset.
     */
    default int getCharge(ItemStack stack) {
        return Mth.clamp(stack.getOrDefault(ModDataComponents.CHARGE, 0), 0, getMaxCharge(stack));
    }

    /**
     * @return charge as a 0..1 fraction of the maximum (for damage-bar / tooltip display).
     */
    default float getChargePercent(ItemStack stack) {
        int max = getMaxCharge(stack);
        return max <= 0 ? 0f : (float) getCharge(stack) / max;
    }

    /**
     * Adjusts the charge by {@code delta} (positive to charge, negative to discharge), clamped to
     * the valid range. Plays the vanilla anvil/level-up sound indirectly via the caller. Returns
     * true if the charge actually changed.
     */
    default boolean changeCharge(Player player, ItemStack stack, int delta) {
        int max = getMaxCharge(stack);
        int current = getCharge(stack);
        int next = Mth.clamp(current + delta, 0, max);
        if (next == current) {
            return false;
        }
        stack.set(ModDataComponents.CHARGE, next);
        return true;
    }
}
