package moze_intel.projecte.content.blocks;

import java.util.Objects;
import java.util.function.Predicate;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CollectorItemStorageTest {
    private static BlockEntityType<?> collectorType;

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
        collectorType = Objects.requireNonNull(BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(
              Identifier.withDefaultNamespace("furnace")));
    }

    @Test
    void horizontalSideInsertsOnlyValidItemsIntoMainInventory() {
        TestCollector collector = new TestCollector();
        Storage<ItemVariant> storage = storage(
              collector, Direction.NORTH, variant -> variant.getItem() == Items.COAL);

        try (Transaction transaction = Transaction.openOuter()) {
            assertEquals(100, storage.insert(variant(Items.COAL), 100, transaction));
            assertEquals(0, storage.insert(variant(Items.DIAMOND), 1, transaction));
            transaction.commit();
        }

        assertEquals(100, countItem(collector, 0, collector.inputSlots, Items.COAL));
        assertEquals(0, countItem(
              collector, collector.inputSlots, collector.getContainerSize(), Items.COAL));
    }

    @Test
    void horizontalSideDoesNotExtractFromMainInventory() {
        TestCollector collector = new TestCollector();
        collector.setItem(0, stack(Items.COAL, 4));
        Storage<ItemVariant> storage = storage(
              collector, Direction.EAST, variant -> variant.getItem() == Items.COAL);

        try (Transaction transaction = Transaction.openOuter()) {
            assertEquals(0, storage.extract(variant(Items.COAL), 2, transaction));
            transaction.commit();
        }

        assertEquals(4, collector.getItem(0).getCount());
    }

    @Test
    void verticalSideExtractsOnlyFromUpgradeOutput() {
        TestCollector collector = new TestCollector();
        collector.setItem(0, stack(Items.COAL, 3));
        collector.setItem(collector.inputSlots, stack(Items.DIAMOND, 4));
        collector.setItem(collector.inputSlots + 1, stack(Items.EMERALD, 5));
        collector.setItem(collector.inputSlots + 2, stack(Items.GOLD_INGOT, 1));
        Storage<ItemVariant> storage = storage(
              collector, Direction.UP, variant -> variant.getItem() == Items.COAL);

        try (Transaction transaction = Transaction.openOuter()) {
            assertEquals(0, storage.insert(variant(Items.COAL), 1, transaction));
            assertEquals(0, storage.extract(variant(Items.COAL), 2, transaction));
            assertEquals(0, storage.extract(variant(Items.DIAMOND), 2, transaction));
            assertEquals(2, storage.extract(variant(Items.EMERALD), 2, transaction));
            assertEquals(0, storage.extract(variant(Items.GOLD_INGOT), 1, transaction));
            transaction.commit();
        }

        assertEquals(3, collector.getItem(0).getCount());
        assertEquals(4, collector.getItem(collector.inputSlots).getCount());
        assertEquals(3, collector.getItem(collector.inputSlots + 1).getCount());
        assertEquals(1, collector.getItem(collector.inputSlots + 2).getCount());
    }

    @Test
    void unsidedViewCombinesInputAndUpgradeOutput() {
        TestCollector collector = new TestCollector();
        collector.setItem(collector.inputSlots + 1, stack(Items.EMERALD, 3));
        Storage<ItemVariant> storage = storage(
              collector, null, variant -> variant.getItem() == Items.COAL);

        try (Transaction transaction = Transaction.openOuter()) {
            assertEquals(2, storage.insert(variant(Items.COAL), 2, transaction));
            assertEquals(2, storage.extract(variant(Items.EMERALD), 2, transaction));
            transaction.commit();
        }

        assertEquals(2, countItem(collector, 0, collector.inputSlots, Items.COAL));
        assertEquals(1, collector.getItem(collector.inputSlots + 1).getCount());
    }

    @Test
    void uncommittedOperationsRollBack() {
        TestCollector collector = new TestCollector();
        Storage<ItemVariant> input = storage(
              collector, Direction.SOUTH, variant -> variant.getItem() == Items.COAL);

        try (Transaction transaction = Transaction.openOuter()) {
            assertEquals(3, input.insert(variant(Items.COAL), 3, transaction));
        }
        assertEquals(0, countItem(collector, 0, collector.inputSlots, Items.COAL));

        collector.setItem(collector.inputSlots + 1, stack(Items.EMERALD, 4));
        Storage<ItemVariant> output = storage(
              collector, Direction.DOWN, variant -> variant.getItem() == Items.COAL);
        try (Transaction transaction = Transaction.openOuter()) {
            assertEquals(2, output.extract(variant(Items.EMERALD), 2, transaction));
        }
        assertEquals(4, collector.getItem(collector.inputSlots + 1).getCount());
    }

    private static Storage<ItemVariant> storage(
          TestCollector collector, Direction direction, Predicate<ItemVariant> validInput
    ) {
        return CollectorItemStorage.create(collector, direction, validInput);
    }

    private static ItemVariant variant(Item item) {
        return ItemVariant.of(stack(item, 1));
    }

    private static ItemStack stack(Item item, int count) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.MAX_STACK_SIZE, 64);
        stack.setCount(count);
        return stack;
    }

    private static int countItem(
          TestCollector collector, int fromInclusive, int toExclusive, Item item
    ) {
        int count = 0;
        for (int slot = fromInclusive; slot < toExclusive; slot++) {
            ItemStack stack = collector.getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static final class TestCollector extends CollectorBlockEntity.Base {
        private TestCollector() {
            super(collectorType, BlockPos.ZERO, Blocks.FURNACE.defaultBlockState(), 1, 4);
        }
    }
}
