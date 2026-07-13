package moze_intel.projecte.content.items;

import moze_intel.projecte.player.PlayerAttachmentKeys;
import moze_intel.projecte.player.PlayerDataService;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Tome of Knowledge — grants full transmutation knowledge while in the player's
 * inventory. EMC-based item creation is handled by the transmutation table.
 */
public class TomeOfKnowledgeItem extends Item {
    public TomeOfKnowledgeItem(Properties properties) {
        super(properties);
    }

    /**
     * Call this from a server-side inventory tick handler to sync full-knowledge
     * flag based on whether the player has a Tome in their inventory.
     */
    public static void applyFullKnowledge(Player player, boolean hasTome) {
        var access = PlayerAttachmentKeys.fabricAdapter(player);
        if (access == null) return;
        var service = new PlayerDataService(access);
        // Only modify if the state actually changed to avoid unnecessary sync
        if (service.knowledge().fullKnowledge() != hasTome) {
            service.setFullKnowledge(hasTome);
        }
    }

    /**
     * Check whether an ItemStack is a Tome of Knowledge.
     */
    public static boolean isTome(ItemStack stack) {
        return stack.getItem() instanceof TomeOfKnowledgeItem;
    }
}
