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

class RelayItemStorageTest {
    private static BlockEntityType<?> relayType;

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
        relayType = Objects.requireNonNull(BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(
              Identifier.withDefaultNamespace("furnace")));
    }

    @Test
    void horizontalSideInsertsOnlyRelayInputsIntoBurnInventory() {
        TestRelay relay = new TestRelay();
        Storage<ItemVariant> storage = storage(relay, Direction.NORTH);

        try (Transaction transaction = Transaction.openOuter()) {
            assertEquals(100, storage.insert(variant(Items.REDSTONE), 100, transaction));
            assertEquals(0, storage.insert(variant(Items.DIRT), 1, transaction));
            transaction.commit();
        }

        assertEquals(100, countItem(relay, 0, relay.inputSlots, Items.REDSTONE));
        assertEquals(0, countItem(
              relay, relay.inputSlots, relay.getContainerSize(), Items.REDSTONE));
    }

    @Test
    void horizontalSideNeverExtractsBurnInventory() {
        TestRelay relay = new TestRelay();
        relay.setItem(0, stack(Items.REDSTONE, 4));
        Storage<ItemVariant> storage = storage(relay, Direction.WEST);

        try (Transaction transaction = Transaction.openOuter()) {
            assertEquals(0, storage.extract(variant(Items.REDSTONE), 2, transaction));
            transaction.commit();
        }

        assertEquals(4, relay.getItem(0).getCount());
    }

    @Test
    void verticalSideAcceptsOnlyChargeableItems() {
        TestRelay relay = new TestRelay();
        Storage<ItemVariant> storage = storage(relay, Direction.UP);

        try (Transaction transaction = Transaction.openOuter()) {
            assertEquals(1, storage.insert(variant(Items.DIAMOND), 1, transaction));
            assertEquals(0, storage.insert(variant(Items.REDSTONE), 1, transaction));
            transaction.commit();
        }

        assertEquals(1, relay.getItem(relay.inputSlots).getCount());
        assertEquals(0, countItem(relay, 0, relay.inputSlots, Items.DIAMOND));
    }

    @Test
    void verticalSideExtractsOnlyFullyChargedOutput() {
        TestRelay relay = new TestRelay();
        relay.setItem(0, stack(Items.REDSTONE, 3));
        relay.setItem(relay.inputSlots, stack(Items.DIAMOND, 1));
        Storage<ItemVariant> storage = storage(relay, Direction.DOWN);

        try (Transaction transaction = Transaction.openOuter()) {
            assertEquals(0, storage.extract(variant(Items.REDSTONE), 1, transaction));
            assertEquals(0, storage.extract(variant(Items.DIAMOND), 1, transaction));
            transaction.commit();
        }
        assertEquals(1, relay.getItem(relay.inputSlots).getCount());

        relay.setItem(relay.inputSlots, stack(Items.EMERALD, 1));
        try (Transaction transaction = Transaction.openOuter()) {
            assertEquals(1, storage.extract(variant(Items.EMERALD), 1, transaction));
            transaction.commit();
        }
        assertEquals(0, relay.getItem(relay.inputSlots).getCount());
    }

    @Test
    void unsidedViewCombinesBurnInputAndChargedOutput() {
        TestRelay relay = new TestRelay();
        relay.setItem(relay.inputSlots, stack(Items.EMERALD, 1));
        Storage<ItemVariant> storage = storage(relay, null);

        try (Transaction transaction = Transaction.openOuter()) {
            assertEquals(2, storage.insert(variant(Items.REDSTONE), 2, transaction));
            assertEquals(1, storage.extract(variant(Items.EMERALD), 1, transaction));
            transaction.commit();
        }

        assertEquals(2, countItem(relay, 0, relay.inputSlots, Items.REDSTONE));
        assertEquals(0, relay.getItem(relay.inputSlots).getCount());
    }

    @Test
    void uncommittedOperationsRollBack() {
        TestRelay relay = new TestRelay();
        Storage<ItemVariant> input = storage(relay, Direction.SOUTH);

        try (Transaction transaction = Transaction.openOuter()) {
            assertEquals(3, input.insert(variant(Items.REDSTONE), 3, transaction));
        }
        assertEquals(0, countItem(relay, 0, relay.inputSlots, Items.REDSTONE));

        relay.setItem(relay.inputSlots, stack(Items.EMERALD, 1));
        Storage<ItemVariant> output = storage(relay, Direction.UP);
        try (Transaction transaction = Transaction.openOuter()) {
            assertEquals(1, output.extract(variant(Items.EMERALD), 1, transaction));
        }
        assertEquals(1, relay.getItem(relay.inputSlots).getCount());
    }

    private static Storage<ItemVariant> storage(TestRelay relay, Direction direction) {
        Predicate<ItemVariant> validInput = variant -> variant.getItem() == Items.REDSTONE
              || variant.getItem() == Items.DIAMOND;
        Predicate<ItemVariant> chargeable = variant -> variant.getItem() == Items.DIAMOND
              || variant.getItem() == Items.EMERALD;
        Predicate<ItemVariant> fullyCharged = variant -> variant.getItem() == Items.EMERALD;
        return RelayItemStorage.create(
              relay, direction, validInput, chargeable, fullyCharged);
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
          TestRelay relay, int fromInclusive, int toExclusive, Item item
    ) {
        int count = 0;
        for (int slot = fromInclusive; slot < toExclusive; slot++) {
            ItemStack stack = relay.getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static final class TestRelay extends RelayBlockEntity.Base {
        private TestRelay() {
            this(BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
        }

        private TestRelay(BlockPos pos, BlockState state) {
            super(relayType, pos, state, 1, 64);
        }
    }
}
