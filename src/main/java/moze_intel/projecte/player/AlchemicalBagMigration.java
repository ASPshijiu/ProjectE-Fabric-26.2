package moze_intel.projecte.player;

import java.util.Objects;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Lossless migration from the earlier per-stack bag storage into player-scoped storage. */
public final class AlchemicalBagMigration {
    private AlchemicalBagMigration() {
    }

    public static Result merge(
          ItemContainerContents sharedContents, ItemContainerContents legacyContents
    ) {
        Objects.requireNonNull(sharedContents, "sharedContents");
        Objects.requireNonNull(legacyContents, "legacyContents");
        NonNullList<ItemStack> shared = slots(sharedContents);
        NonNullList<ItemStack> legacy = slots(legacyContents);

        for (int legacySlot = 0; legacySlot < legacy.size(); legacySlot++) {
            ItemStack remaining = legacy.get(legacySlot).copy();
            if (remaining.isEmpty()) {
                continue;
            }
            mergeIntoExistingStacks(shared, remaining);
            moveIntoEmptySlots(shared, remaining);
            legacy.set(legacySlot, remaining.isEmpty() ? ItemStack.EMPTY : remaining);
        }

        return new Result(
              ItemContainerContents.fromItems(shared),
              ItemContainerContents.fromItems(legacy));
    }

    private static void mergeIntoExistingStacks(
          NonNullList<ItemStack> shared, ItemStack remaining
    ) {
        for (ItemStack target : shared) {
            if (remaining.isEmpty()) {
                return;
            }
            if (target.isEmpty() || !ItemStack.isSameItemSameComponents(target, remaining)) {
                continue;
            }
            int moved = Math.min(remaining.getCount(),
                  Math.max(0, target.getMaxStackSize() - target.getCount()));
            if (moved > 0) {
                target.grow(moved);
                remaining.shrink(moved);
            }
        }
    }

    private static void moveIntoEmptySlots(
          NonNullList<ItemStack> shared, ItemStack remaining
    ) {
        for (int slot = 0; slot < shared.size() && !remaining.isEmpty(); slot++) {
            if (!shared.get(slot).isEmpty()) {
                continue;
            }
            int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
            shared.set(slot, remaining.copyWithCount(moved));
            remaining.shrink(moved);
        }
    }

    private static NonNullList<ItemStack> slots(ItemContainerContents contents) {
        NonNullList<ItemStack> slots = NonNullList.withSize(
              AlchemicalBagData.SLOTS, ItemStack.EMPTY);
        contents.copyInto(slots);
        return slots;
    }

    public record Result(
          ItemContainerContents shared,
          ItemContainerContents remaining
    ) {
    }
}
