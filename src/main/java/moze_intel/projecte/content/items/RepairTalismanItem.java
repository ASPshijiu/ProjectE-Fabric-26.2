package moze_intel.projecte.content.items;

import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Repair Talisman — passively repairs damaged items in the player's inventory
 * once per second.
 */
public class RepairTalismanItem extends Item {
    public RepairTalismanItem(Properties properties) {
        super(properties);
    }

    /**
     * Call from a server-side inventory tick handler once per second.
     * Repairs every damaged, repairable item by 1 durability.
     */
    public static void tickRepair(Container inventory, boolean hasTalisman) {
        if (!hasTalisman) return;

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !stack.isDamaged()) continue;
            if (stack.getItem() instanceof RepairTalismanItem) continue; // don't repair itself

            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }

    public static boolean isTalisman(ItemStack stack) {
        return stack.getItem() instanceof RepairTalismanItem;
    }
}
