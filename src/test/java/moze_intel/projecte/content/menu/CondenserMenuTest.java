package moze_intel.projecte.content.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import moze_intel.projecte.content.blocks.CondenserBlockEntity;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CondenserMenuTest {
    private static BlockEntityType<?> condenserType;

    @BeforeAll
    @SuppressWarnings({"rawtypes", "unchecked"})
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
        condenserType = BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(
              Identifier.withDefaultNamespace("furnace"));
        CondenserBlockEntity.MK1_TYPE = (BlockEntityType) condenserType;
        CondenserBlockEntity.MK2_TYPE = (BlockEntityType) condenserType;
    }

    @Test
    void mk1LayoutContainsGhostMachineAndPlayerSlots() throws Exception {
        TestContext context = context(1, stack -> true, ignored -> 8_192);
        CondenserMenu menu = context.menu();

        assertEquals(128, menu.slots.size());
        assertEquals(12, menu.getSlot(0).x);
        assertEquals(6, menu.getSlot(0).y);
        assertEquals(12, menu.getSlot(1).x);
        assertEquals(26, menu.getSlot(1).y);
        assertEquals(228, menu.getSlot(91).x);
        assertEquals(134, menu.getSlot(91).y);
        assertEquals(48, menu.getSlot(92).x);
        assertEquals(154, menu.getSlot(92).y);
    }

    @Test
    void mk2LayoutSeparatesInputAndReadOnlyOutputSlots() throws Exception {
        TestContext context = context(2, stack -> stack.is(Items.REDSTONE), ignored -> 8_192);
        CondenserMenu menu = context.menu();
        ItemStack redstone = new ItemStack(Items.REDSTONE);

        assertEquals(121, menu.slots.size());
        assertTrue(menu.getSlot(1).mayPlace(redstone));
        assertEquals(12, menu.getSlot(1).x);
        assertEquals(26, menu.getSlot(1).y);
        assertFalse(menu.getSlot(43).mayPlace(redstone));
        assertEquals(138, menu.getSlot(43).x);
        assertEquals(26, menu.getSlot(43).y);
        assertEquals(48, menu.getSlot(85).x);
        assertEquals(154, menu.getSlot(85).y);
    }

    @Test
    void targetSlotIsNonConsumingGhostSlot() throws Exception {
        TestContext context = context(1, stack -> true, ignored -> 8_192);
        CondenserMenu menu = context.menu();
        ItemStack carried = stack(Items.DIAMOND, 32);
        menu.setCarried(carried);

        assertTrue(menu.getSlot(0).isFake());
        assertFalse(menu.getSlot(0).mayPlace(carried));
        assertFalse(menu.getSlot(0).mayPickup(context.player()));

        menu.clicked(0, 0, ContainerInput.PICKUP, context.player());

        assertTrue(context.condenser().getTarget().is(Items.DIAMOND));
        assertEquals(1, context.condenser().getTarget().getCount());
        assertEquals(32, menu.getCarried().getCount());
        assertTrue(menu.getSlot(0).getItem().is(Items.DIAMOND));
    }

    @Test
    void clickingConfiguredTargetClearsIt() throws Exception {
        TestContext context = context(1, stack -> true, ignored -> 8_192);
        CondenserMenu menu = context.menu();
        menu.setCarried(new ItemStack(Items.DIAMOND));
        menu.clicked(0, 0, ContainerInput.PICKUP, context.player());

        menu.clicked(0, 0, ContainerInput.PICKUP, context.player());

        assertTrue(context.condenser().getTarget().isEmpty());
        assertTrue(menu.getSlot(0).getItem().isEmpty());
    }

    @Test
    void playerQuickMoveUsesOnlyInputSlots() throws Exception {
        TestContext context = context(2, stack -> stack.is(Items.REDSTONE), ignored -> 64);
        CondenserMenu menu = context.menu();
        context.inventory().setItem(9, stack(Items.REDSTONE, 16));

        ItemStack original = menu.quickMoveStack(context.player(), 85);

        assertEquals(16, original.getCount());
        assertEquals(0, context.inventory().getItem(9).getCount());
        assertEquals(16, context.condenser().getItem(0).getCount());
        for (int slot = 42; slot < 84; slot++) {
            assertTrue(context.condenser().getItem(slot).isEmpty());
        }
    }

    @Test
    void progressScaleHandlesEmptyPartialAndCompleteStates() {
        assertEquals(0, CondenserMenu.progressScaled(1_000, 0));
        assertEquals(51, CondenserMenu.progressScaled(4_096, 8_192));
        assertEquals(102, CondenserMenu.progressScaled(8_192, 8_192));
        assertEquals(102, CondenserMenu.progressScaled(Long.MAX_VALUE, 1));
    }

    private static TestContext context(
          int tier,
          Predicate<ItemStack> inputValidator,
          ToLongFunction<ItemStack> emcValue
    ) throws Exception {
        ServerPlayer player = allocate(ServerPlayer.class);
        Inventory inventory = new Inventory(player, new EntityEquipment());
        CondenserBlockEntity.Base condenser = tier == 1
              ? new CondenserBlockEntity.MK1(BlockPos.ZERO, Blocks.FURNACE.defaultBlockState())
              : new CondenserBlockEntity.MK2(BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
        CondenserMenu menu = new CondenserMenu(
              menuType(), 0, inventory, condenser, inputValidator, emcValue);
        return new TestContext(player, inventory, condenser, menu);
    }

    private static MenuType<CondenserMenu> menuType() {
        return new MenuType<>((containerId, inventory) -> null, FeatureFlags.VANILLA_SET);
    }

    private static ItemStack stack(net.minecraft.world.item.Item item, int count) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.MAX_STACK_SIZE, 64);
        stack.setCount(count);
        return stack;
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
          CondenserBlockEntity.Base condenser,
          CondenserMenu menu
    ) {
    }
}
