package moze_intel.projecte.content.menu;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Shared vanilla-style bookkeeping for custom menu shift-click transfers. */
final class MenuQuickMove {
    private MenuQuickMove() {
    }

    static ItemStack move(Slot sourceSlot, Predicate<ItemStack> transfer) {
        ItemStack source = sourceSlot.getItem();
        ItemStack original = source.copy();
        if (!transfer.test(source)) {
            return ItemStack.EMPTY;
        }
        if (source.isEmpty()) {
            sourceSlot.setByPlayer(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        return original;
    }

    static int roomForOneStack(ItemStack candidate, List<Slot> destinationSlots) {
        int stackLimit = candidate.getOrDefault(DataComponents.MAX_STACK_SIZE, 1);
        int room = 0;
        for (Slot destination : destinationSlots) {
            if (!destination.mayPlace(candidate)) {
                continue;
            }
            ItemStack existing = destination.getItem();
            if (existing.isEmpty()) {
                room += destination.getMaxStackSize(candidate);
            } else if (ItemStack.isSameItemSameComponents(candidate, existing)) {
                room += Math.max(0, destination.getMaxStackSize(candidate) - existing.getCount());
            }
            if (room >= stackLimit) {
                return stackLimit;
            }
        }
        return room;
    }
}
