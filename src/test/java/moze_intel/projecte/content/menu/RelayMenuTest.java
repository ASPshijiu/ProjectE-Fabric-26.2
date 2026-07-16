package moze_intel.projecte.content.menu;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import moze_intel.projecte.content.blocks.RelayBlockEntity;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
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

class RelayMenuTest {
    private static BlockEntityType<?> relayType;

    @BeforeAll
    @SuppressWarnings({"rawtypes", "unchecked"})
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
        relayType = BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(
              Identifier.withDefaultNamespace("furnace"));
        RelayBlockEntity.MK1_TYPE = (BlockEntityType) relayType;
        RelayBlockEntity.MK2_TYPE = (BlockEntityType) relayType;
        RelayBlockEntity.MK3_TYPE = (BlockEntityType) relayType;
    }

    @Test
    void layoutsMatchAllThreeRelayTextures() throws Exception {
        assertLayout(context(1), 44, 127, 67, 45, 8, 95);
        assertLayout(context(2), 50, 144, 84, 62, 16, 101);
        assertLayout(context(3), 58, 164, 104, 82, 26, 113);
    }

    @Test
    void slotsSeparateChargeableOutputAndRelayInputs() throws Exception {
        TestContext context = context(1);
        RelayMenu menu = context.menu();
        ItemStack chargeable = stack(Items.DIAMOND, 1);
        ItemStack fuel = stack(Items.REDSTONE, 1);
        ItemStack dirt = stack(Items.DIRT, 1);

        assertTrue(menu.getSlot(0).mayPlace(chargeable));
        assertFalse(menu.getSlot(0).mayPlace(fuel));
        assertTrue(menu.getSlot(1).mayPlace(chargeable));
        assertTrue(menu.getSlot(1).mayPlace(fuel));
        assertFalse(menu.getSlot(1).mayPlace(dirt));
        assertTrue(menu.getSlot(2).mayPlace(fuel));
        assertFalse(menu.getSlot(2).mayPlace(dirt));
    }

    @Test
    void quickMovePrefersChargeSlotThenBurnInventory() throws Exception {
        TestContext context = context(1);
        RelayMenu menu = context.menu();
        int firstPlayerSlot = context.inputSlots() + 1;
        context.inventory().setItem(9, stack(Items.DIAMOND, 1));
        context.inventory().setItem(10, stack(Items.REDSTONE, 16));

        assertEquals(1, menu.quickMoveStack(
              context.player(), firstPlayerSlot).getCount());
        assertEquals(16, menu.quickMoveStack(
              context.player(), firstPlayerSlot + 1).getCount());

        assertTrue(context.relay().getItem(context.inputSlots()).is(Items.DIAMOND));
        assertEquals(16, context.relay().getItem(0).getCount());
    }

    @Test
    void quickMoveRejectsInvalidItemsAndExtractsMachineContents() throws Exception {
        TestContext context = context(1);
        RelayMenu menu = context.menu();
        int firstPlayerSlot = context.inputSlots() + 1;
        context.inventory().setItem(9, stack(Items.DIRT, 4));

        assertTrue(menu.quickMoveStack(context.player(), firstPlayerSlot).isEmpty());
        assertEquals(4, context.inventory().getItem(9).getCount());

        context.relay().setItem(context.inputSlots(), stack(Items.DIAMOND, 1));
        assertEquals(1, menu.quickMoveStack(context.player(), 0).getCount());
        assertTrue(context.relay().getItem(context.inputSlots()).isEmpty());
        assertEquals(1, countItem(context.inventory(), Items.DIAMOND));
    }

    @Test
    void storageChargeAndBurnProgressAreClamped() throws Exception {
        TestContext context = context(1);
        context.relay().setStoredEmc(50_000);
        context.relay().setItem(0, stack(Items.REDSTONE, 32));
        context.relay().setItem(context.inputSlots(), stack(Items.DIAMOND, 1));

        assertEquals(0.5, context.menu().storageProgress(), 0.0);
        assertEquals(0.5, context.menu().burnProgress(), 0.0);
        assertEquals(0.75, context.menu().chargeProgress(), 0.0);

        context.relay().setStoredEmc(100_000);
        assertEquals(1.0, context.menu().storageProgress(), 0.0);
    }

    private static void assertLayout(
          TestContext context,
          int totalSlots,
          int chargeX,
          int burnX,
          int firstBufferX,
          int playerX,
          int playerY
    ) {
        RelayMenu menu = context.menu();
        int firstPlayerSlot = context.inputSlots() + 1;

        assertEquals(totalSlots, menu.slots.size());
        assertEquals(chargeX, menu.getSlot(0).x);
        assertEquals(context.tier() == 3 ? 58 : context.tier() + 42,
              menu.getSlot(0).y);
        assertEquals(burnX, menu.getSlot(1).x);
        assertEquals(menu.getSlot(0).y, menu.getSlot(1).y);
        assertEquals(firstBufferX, menu.getSlot(2).x);
        assertEquals(context.firstBufferY(), menu.getSlot(2).y);
        assertEquals(playerX, menu.getSlot(firstPlayerSlot).x);
        assertEquals(playerY, menu.getSlot(firstPlayerSlot).y);
    }

    private static TestContext context(int tier) throws Exception {
        ServerPlayer player = allocate(ServerPlayer.class);
        Inventory inventory = new Inventory(player, new EntityEquipment());
        RelayBlockEntity.Base relay = switch (tier) {
            case 1 -> new RelayBlockEntity.MK1(
                  BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
            case 2 -> new RelayBlockEntity.MK2(
                  BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
            case 3 -> new RelayBlockEntity.MK3(
                  BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
            default -> throw new IllegalArgumentException();
        };
        Predicate<ItemStack> inputValidator = stack -> stack.is(Items.REDSTONE)
              || stack.is(Items.DIAMOND);
        Predicate<ItemStack> chargeable = stack -> stack.is(Items.DIAMOND);
        ToDoubleFunction<ItemStack> chargeProgress = stack -> stack.is(Items.DIAMOND)
              ? 0.75
              : 0;
        ToDoubleFunction<ItemStack> burnProgress = stack -> stack.isEmpty()
              ? 0
              : stack.getCount() / (double) stack.getMaxStackSize();
        RelayMenu menu = new RelayMenu(
              menuType(), 0, inventory, relay,
              inputValidator, chargeable, chargeProgress, burnProgress);
        return new TestContext(player, inventory, relay, tier, menu);
    }

    private static MenuType<RelayMenu> menuType() {
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
          RelayBlockEntity.Base relay,
          int tier,
          RelayMenu menu
    ) {
        private int inputSlots() {
            return switch (tier) {
                case 1 -> 7;
                case 2 -> 13;
                case 3 -> 21;
                default -> throw new IllegalStateException();
            };
        }

        private int firstBufferY() {
            return switch (tier) {
                case 1 -> 53;
                case 2 -> 72;
                case 3 -> 90;
                default -> throw new IllegalStateException();
            };
        }
    }
}
