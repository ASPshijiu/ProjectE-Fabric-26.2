package moze_intel.projecte.content.items;

import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.player.PlayerAttachmentKeys;
import moze_intel.projecte.player.PlayerDataService;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Repair Talisman — passively repairs damaged items in the player's inventory
 * using EMC from the player's EMC pool. Repairs one durability per 1 EMC.
 */
public class RepairTalismanItem extends Item {
    /** EMC cost per durability point repaired. */
    private static final long EMC_PER_DURABILITY = 1L;

    public RepairTalismanItem(Properties properties) {
        super(properties);
    }

    /**
     * Call from a server-side inventory tick handler once per second.
     * Finds a random damaged, repairable item and repairs 1 durability.
     */
    public static void tickRepair(Player player, boolean hasTalisman) {
        if (!hasTalisman) return;
        var access = PlayerAttachmentKeys.fabricAdapter(player);
        if (access == null) return;
        var service = new PlayerDataService(access);

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !stack.isDamaged()) continue;
            if (stack.getItem() instanceof RepairTalismanItem) continue; // don't repair itself

            long emcNeeded = EMC_PER_DURABILITY;
            if (!service.tryRemoveEmc(EmcValue.of(emcNeeded))) return;

            stack.setDamageValue(stack.getDamageValue() - 1);
            return; // one repair per tick
        }
    }

    public static boolean isTalisman(ItemStack stack) {
        return stack.getItem() instanceof RepairTalismanItem;
    }
}
