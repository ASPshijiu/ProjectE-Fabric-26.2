package moze_intel.projecte.event;

import moze_intel.projecte.content.items.PhilosophersStoneItem;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/** Runs Philosopher's Stone transmutation before an interactive block consumes the click. */
public final class PhilosophersStoneInteractionHandler {
    private static boolean registered;

    private PhilosophersStoneInteractionHandler() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (player.isSpectator()) {
                return InteractionResult.PASS;
            }
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof PhilosophersStoneItem stone)) {
                return InteractionResult.PASS;
            }
            return stone.useOn(new UseOnContext(player, hand, hitResult));
        });
    }
}
