package moze_intel.projecte.content.blocks;

import java.util.Objects;
import java.util.Set;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CondenserItemStorageTest {
    private static BlockEntityType<?> condenserType;

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
        condenserType = Objects.requireNonNull(BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(
              Identifier.withDefaultNamespace("furnace")));
    }

    @Test
    void mk1AcceptsOnlyPositiveEmcNonTargetInputs() {
        TestCondenser condenser = new TestCondenser(1);
        prepareTarget(condenser);
        Storage<ItemVariant> storage = storage(condenser, Set.of(Items.REDSTONE, Items.DIAMOND));

        try (Transaction transaction = Transaction.openOuter()) {
            assertEquals(4, storage.insert(variant(Items.REDSTONE), 4, transaction));
            assertEquals(0, storage.insert(variant(Items.DIAMOND), 1, transaction));
            assertEquals(0, storage.insert(variant(Items.DIRT), 1, transaction));
            transaction.commit();
        }

        assertEquals(4, countItem(condenser, 0, 91, Items.REDSTONE));
        assertEquals(0, countItem(condenser, 0, 91, Items.DIAMOND));
        assertEquals(0, countItem(condenser, 0, 91, Items.DIRT));
    }

    @Test
    void mk1ExtractsOnlyItsValidTarget() {
        TestCondenser condenser = new TestCondenser(1);
        prepareTarget(condenser);
        condenser.setItem(0, stack(Items.REDSTONE, 3));
        condenser.setItem(1, stack(Items.DIAMOND, 3));
        Storage<ItemVariant> storage = storage(condenser, Set.of(Items.REDSTONE, Items.DIAMOND));

        try (Transaction transaction = Transaction.openOuter()) {
            assertEquals(0, storage.extract(variant(Items.REDSTONE), 2, transaction));
            assertEquals(2, storage.extract(variant(Items.DIAMOND), 2, transaction));
            transaction.commit();
        }

        assertEquals(3, countItem(condenser, 0, 91, Items.REDSTONE));
        assertEquals(1, countItem(condenser, 0, 91, Items.DIAMOND));
    }

    @Test
    void mk1DoesNotExtractTargetWithoutValidEmc() {
        TestCondenser condenser = new TestCondenser(1);
        condenser.setTarget(stack(Items.DIAMOND, 1));
        condenser.setItem(0, stack(Items.DIAMOND, 3));
        Storage<ItemVariant> storage = storage(condenser, Set.of(Items.DIAMOND));

        try (Transaction transaction = Transaction.openOuter()) {
            assertEquals(0, storage.extract(variant(Items.DIAMOND), 2, transaction));
            transaction.commit();
        }

        assertEquals(3, countItem(condenser, 0, 91, Items.DIAMOND));
    }

    @Test
    void mk2InsertsOnlyIntoFirstFortyTwoSlots() {
        TestCondenser condenser = new TestCondenser(2);
        prepareTarget(condenser);
        Storage<ItemVariant> storage = storage(condenser, Set.of(Items.REDSTONE));

        try (Transaction transaction = Transaction.openOuter()) {
            assertEquals(100, storage.insert(variant(Items.REDSTONE), 100, transaction));
            transaction.commit();
        }

        assertEquals(100, countItem(condenser, 0, 42, Items.REDSTONE));
        assertEquals(0, countItem(condenser, 42, 84, Items.REDSTONE));
    }

    @Test
    void mk2ExtractsOnlyFromLastFortyTwoSlots() {
        TestCondenser condenser = new TestCondenser(2);
        prepareTarget(condenser);
        condenser.setItem(0, stack(Items.REDSTONE, 3));
        condenser.setItem(42, stack(Items.EMERALD, 4));
        Storage<ItemVariant> storage = storage(condenser, Set.of(Items.REDSTONE));

        try (Transaction transaction = Transaction.openOuter()) {
            assertEquals(0, storage.extract(variant(Items.REDSTONE), 2, transaction));
            assertEquals(2, storage.extract(variant(Items.EMERALD), 2, transaction));
            transaction.commit();
        }

        assertEquals(3, countItem(condenser, 0, 42, Items.REDSTONE));
        assertEquals(2, countItem(condenser, 42, 84, Items.EMERALD));
    }

    @Test
    void uncommittedOperationsRollBack() {
        TestCondenser condenser = new TestCondenser(2);
        prepareTarget(condenser);
        Storage<ItemVariant> storage = storage(condenser, Set.of(Items.REDSTONE));

        try (Transaction transaction = Transaction.openOuter()) {
            assertEquals(5, storage.insert(variant(Items.REDSTONE), 5, transaction));
        }
        assertEquals(0, countItem(condenser, 0, 42, Items.REDSTONE));

        condenser.setItem(42, stack(Items.EMERALD, 5));
        try (Transaction transaction = Transaction.openOuter()) {
            assertEquals(3, storage.extract(variant(Items.EMERALD), 3, transaction));
        }
        assertEquals(5, countItem(condenser, 42, 84, Items.EMERALD));
    }

    private static Storage<ItemVariant> storage(TestCondenser condenser, Set<Item> emcItems) {
        return CondenserItemStorage.create(condenser, variant -> emcItems.contains(variant.getItem()));
    }

    private static void prepareTarget(TestCondenser condenser) {
        condenser.setTarget(stack(Items.DIAMOND, 1));
        condenser.refreshTargetEmc(ignored -> 8_192);
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
          TestCondenser condenser, int fromInclusive, int toExclusive, Item item
    ) {
        int count = 0;
        for (int slot = fromInclusive; slot < toExclusive; slot++) {
            ItemStack stack = condenser.getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static final class TestCondenser extends CondenserBlockEntity.Base {
        private TestCondenser(int tier) {
            this(BlockPos.ZERO, Blocks.FURNACE.defaultBlockState(), tier);
        }

        private TestCondenser(BlockPos pos, BlockState state, int tier) {
            super(condenserType, pos, state, tier);
        }
    }
}
