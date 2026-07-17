package moze_intel.projecte.content.items;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Item action invoked by the ProjectE extra-function key. */
public interface IExtraFunction {
    void doExtraFunction(Player player, ItemStack stack, InteractionHand hand);
}
