package moze_intel.projecte.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AlchemicalBagMigrationTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
    }

    @Test
    void mergesCompatibleStacksIntoSharedInventory() {
        ItemContainerContents shared = ItemContainerContents.fromItems(
              List.of(stack(Items.DIAMOND, 60)));
        ItemContainerContents legacy = ItemContainerContents.fromItems(
              List.of(stack(Items.DIAMOND, 10)));

        AlchemicalBagMigration.Result result = AlchemicalBagMigration.merge(shared, legacy);

        NonNullList<ItemStack> migrated = slots(result.shared());
        assertEquals(64, migrated.get(0).getCount());
        assertEquals(6, migrated.get(1).getCount());
        assertTrue(result.remaining().nonEmptyItemCopyStream().findAny().isEmpty());
    }

    @Test
    void preservesLegacyContentsWhenSharedInventoryIsFull() {
        NonNullList<ItemStack> full = NonNullList.withSize(104, ItemStack.EMPTY);
        for (int slot = 0; slot < full.size(); slot++) {
            full.set(slot, stack(Items.COBBLESTONE, 64));
        }
        ItemContainerContents legacy = ItemContainerContents.fromItems(
              List.of(stack(Items.DIAMOND, 4)));

        AlchemicalBagMigration.Result result = AlchemicalBagMigration.merge(
              ItemContainerContents.fromItems(full), legacy);

        NonNullList<ItemStack> remaining = slots(result.remaining());
        assertEquals(4, remaining.getFirst().getCount());
        assertEquals(Items.DIAMOND, remaining.getFirst().getItem());
    }

    @Test
    void preservesLegacySlotsBeyondTheVisibleBagInventory() {
        NonNullList<ItemStack> full = NonNullList.withSize(104, ItemStack.EMPTY);
        for (int slot = 0; slot < full.size(); slot++) {
            full.set(slot, stack(Items.COBBLESTONE, 64));
        }
        NonNullList<ItemStack> legacy = NonNullList.withSize(105, ItemStack.EMPTY);
        legacy.set(104, stack(Items.DIAMOND, 4));

        AlchemicalBagMigration.Result result = AlchemicalBagMigration.merge(
              ItemContainerContents.fromItems(full), ItemContainerContents.fromItems(legacy));

        List<ItemStack> remaining = result.remaining().allItemsCopyStream().toList();
        assertEquals(105, remaining.size());
        assertEquals(4, remaining.get(104).getCount());
        assertEquals(Items.DIAMOND, remaining.get(104).getItem());
    }

    private static NonNullList<ItemStack> slots(ItemContainerContents contents) {
        NonNullList<ItemStack> slots = NonNullList.withSize(104, ItemStack.EMPTY);
        contents.copyInto(slots);
        return slots;
    }

    private static ItemStack stack(Item item, int count) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(DataComponents.MAX_STACK_SIZE, 64);
        return stack;
    }
}
