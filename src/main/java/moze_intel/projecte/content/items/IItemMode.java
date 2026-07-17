package moze_intel.projecte.content.items;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Item action invoked by the ProjectE mode-switch key. */
public interface IItemMode {
    void cycleMode(Player player, ItemStack stack);
}
