package moze_intel.projecte.content.items;

import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.player.PlayerAttachmentKeys;
import moze_intel.projecte.player.PlayerDataService;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Base class for ProjectE active items (rings, amulets, stones) that consume
 * EMC from the player's EMC pool when toggled on.
 *
 * <p>Subclasses implement {@link #onTick}, {@link #getEmcPerTick}, and any
 * active-state visual/audio effects.
 */
public abstract class ActiveEmcItem extends Item {
    protected ActiveEmcItem(Properties properties) {
        super(properties);
    }

    /**
     * @return the EMC cost per tick while this item is active (0 = free).
     */
    public abstract long getEmcPerTick();

    /**
     * Called every tick while the player has this item equipped and active.
     * The base implementation deducts EMC; subclasses should call super.
     *
     * @param player  the owning player
     * @param stack   the item stack
     * @param service the player's data service
     */
    public void onTick(Player player, ItemStack stack, PlayerDataService service) {
        long cost = getEmcPerTick();
        if (cost > 0) {
            service.tryRemoveEmc(EmcValue.of(cost));
        }
    }

    /**
     * @return true if this item is currently toggled on for the given stack.
     * The default checks a boolean component; override for custom logic.
     */
    public boolean isActive(ItemStack stack) {
        return false; // overridden by subclasses using data components
    }

    /**
     * Toggle the active state. The default no-ops; subclasses should
     * implement via a data component.
     */
    public void setActive(ItemStack stack, boolean active) {
        // overridden by subclasses
    }
}
