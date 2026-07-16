package moze_intel.projecte.content.menu;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import moze_intel.projecte.content.blocks.CollectorBlockEntity;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectorMenuTest {
    private static BlockEntityType<?> collectorType;

    @BeforeAll
    @SuppressWarnings({"rawtypes", "unchecked"})
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
        collectorType = BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(
              Identifier.withDefaultNamespace("furnace"));
        CollectorBlockEntity.MK1_TYPE = (BlockEntityType) collectorType;
        CollectorBlockEntity.MK2_TYPE = (BlockEntityType) collectorType;
        CollectorBlockEntity.MK3_TYPE = (BlockEntityType) collectorType;
    }

    @Test
    void layoutsMatchAllThreeCollectorTextures() throws Exception {
        assertLayout(context(1), 47, 124, 38, 124, 153, 8);
        assertLayout(context(2), 51, 140, 54, 140, 169, 20);
        assertLayout(context(3), 55, 158, 72, 158, 187, 30);
    }

    @Test
    void slotsEnforceInputOutputAndLockRoles() throws Exception {
        TestContext context = context(1);
        CollectorMenu menu = context.menu();
        ItemStack coal = stack(Items.COAL, 1);
        ItemStack dirt = stack(Items.DIRT, 1);

        assertTrue(menu.getSlot(0).mayPlace(coal));
        assertFalse(menu.getSlot(0).mayPlace(dirt));
        assertTrue(menu.getSlot(1).mayPlace(coal));
        assertFalse(menu.getSlot(1).mayPlace(dirt));
        assertFalse(menu.getSlot(9).mayPlace(coal));
        assertTrue(menu.getSlot(10).isFake());
        assertFalse(menu.getSlot(10).mayPickup(context.player()));
    }

    @Test
    void lockSlotCopiesOneFuelWithoutConsumingAndClearsOnSecondClick() throws Exception {
        TestContext context = context(1);
        CollectorMenu menu = context.menu();
        menu.setCarried(stack(Items.DIAMOND, 32));

        menu.clicked(10, 0, ContainerInput.PICKUP, context.player());

        assertTrue(context.collector().getItem(context.inputSlots() + 2)
              .is(Items.DIAMOND));
        assertEquals(1, context.collector().getItem(
              context.inputSlots() + 2).getCount());
        assertEquals(32, menu.getCarried().getCount());

        menu.clicked(10, 0, ContainerInput.PICKUP, context.player());

        assertTrue(context.collector().getItem(
              context.inputSlots() + 2).isEmpty());
        assertEquals(32, menu.getCarried().getCount());
    }

    @Test
    void quickMoveUsesProcessingSlotsAndNeverTheOutputOrLock() throws Exception {
        TestContext context = context(1);
        CollectorMenu menu = context.menu();
        context.inventory().setItem(9, stack(Items.COAL, 16));

        ItemStack moved = menu.quickMoveStack(context.player(), 11);

        assertEquals(16, moved.getCount());
        assertTrue(context.inventory().getItem(9).isEmpty());
        assertEquals(16, context.collector().getItem(
              context.inputSlots()).getCount());
        assertTrue(context.collector().getItem(
              context.inputSlots() + 1).isEmpty());
        assertTrue(context.collector().getItem(
              context.inputSlots() + 2).isEmpty());
    }

    @Test
    void quickMoveRejectsInvalidPlayerItemsAndExtractsUpgradeOutput() throws Exception {
        TestContext context = context(1);
        CollectorMenu menu = context.menu();
        context.inventory().setItem(9, stack(Items.DIRT, 4));

        assertTrue(menu.quickMoveStack(context.player(), 11).isEmpty());
        assertEquals(4, context.inventory().getItem(9).getCount());

        context.collector().setItem(
              context.inputSlots() + 1, stack(Items.EMERALD, 3));
        ItemStack moved = menu.quickMoveStack(context.player(), 9);

        assertEquals(3, moved.getCount());
        assertTrue(context.collector().getItem(
              context.inputSlots() + 1).isEmpty());
        assertEquals(3, countItem(context.inventory(), Items.EMERALD));
    }

    @Test
    void fuelAndStorageProgressAreClamped() throws Exception {
        TestContext context = context(1);
        context.collector().setStoredEmc(192);
        context.collector().setItem(
              context.inputSlots(), stack(Items.COAL, 1));

        assertEquals(0.0192, context.menu().storageProgress(), 0.000_001);
        assertEquals(0.5, context.menu().fuelProgress(), 0.000_001);

        context.collector().setStoredEmc(10_000);
        assertEquals(1.0, context.menu().storageProgress(), 0.0);
        assertEquals(1.0, context.menu().fuelProgress(), 0.0);
    }

    @Test
    void highestFuelShowsNoProgressEvenWithLockedTarget() throws Exception {
        TestContext context = context(1);
        context.collector().setStoredEmc(10_000);
        context.collector().setItem(
              context.inputSlots(), stack(Items.DIAMOND, 1));
        context.collector().setItem(
              context.inputSlots() + 2, stack(Items.DIAMOND, 1));

        assertEquals(0.0, context.menu().fuelProgress(), 0.0);
    }

    private static void assertLayout(
          TestContext context,
          int totalSlots,
          int upgradingX,
          int firstInputX,
          int outputX,
          int lockX,
          int playerX
    ) {
        CollectorMenu menu = context.menu();
        int inputSlots = context.inputSlots();
        int outputSlot = inputSlots + 1;
        int lockSlot = inputSlots + 2;
        int firstPlayerSlot = inputSlots + 3;

        assertEquals(totalSlots, menu.slots.size());
        assertEquals(upgradingX, menu.getSlot(0).x);
        assertEquals(58, menu.getSlot(0).y);
        assertEquals(firstInputX, menu.getSlot(1).x);
        assertEquals(62, menu.getSlot(1).y);
        assertEquals(outputX, menu.getSlot(outputSlot).x);
        assertEquals(13, menu.getSlot(outputSlot).y);
        assertEquals(lockX, menu.getSlot(lockSlot).x);
        assertEquals(36, menu.getSlot(lockSlot).y);
        assertEquals(playerX, menu.getSlot(firstPlayerSlot).x);
        assertEquals(84, menu.getSlot(firstPlayerSlot).y);
    }

    private static TestContext context(int tier) throws Exception {
        ServerPlayer player = allocate(ServerPlayer.class);
        Inventory inventory = new Inventory(player, new EntityEquipment());
        CollectorBlockEntity.Base collector = switch (tier) {
            case 1 -> new CollectorBlockEntity.MK1(
                  BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
            case 2 -> new CollectorBlockEntity.MK2(
                  BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
            case 3 -> new CollectorBlockEntity.MK3(
                  BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
            default -> throw new IllegalArgumentException();
        };
        Predicate<ItemStack> inputValidator = stack -> stack.is(Items.COAL);
        Predicate<ItemStack> lockValidator = stack -> stack.is(Items.DIAMOND);
        ToLongFunction<ItemStack> emcValue = stack -> {
            if (stack.is(Items.COAL)) return 128;
            if (stack.is(Items.DIAMOND)) return 512;
            return 0;
        };
        Function<ItemStack, ItemStack> nextFuel = stack -> stack.is(Items.COAL)
              ? stack(Items.DIAMOND, 1)
              : ItemStack.EMPTY;
        CollectorMenu menu = new CollectorMenu(
              menuType(), 0, inventory, collector,
              inputValidator, lockValidator, emcValue, nextFuel);
        return new TestContext(player, inventory, collector, tier, menu);
    }

    private static MenuType<CollectorMenu> menuType() {
        return new MenuType<>((containerId, inventory) -> null, FeatureFlags.VANILLA_SET);
    }

    private static ItemStack stack(Item item, int count) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.MAX_STACK_SIZE, 64);
        stack.setCount(count);
        return stack;
    }

    private static int countItem(Inventory inventory, Item item) {
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(item)) {
                count += inventory.getItem(slot).getCount();
            }
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field field = unsafeClass.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Object unsafe = field.get(null);
        Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        return (T) allocateInstance.invoke(unsafe, type);
    }

    private record TestContext(
          ServerPlayer player,
          Inventory inventory,
          CollectorBlockEntity.Base collector,
          int tier,
          CollectorMenu menu
    ) {
        private int inputSlots() {
            return switch (tier) {
                case 1 -> 8;
                case 2 -> 12;
                case 3 -> 16;
                default -> throw new IllegalStateException();
            };
        }
    }
}
