package moze_intel.projecte.content.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import moze_intel.projecte.player.AlchemicalBagData;
import moze_intel.projecte.player.PlayerAttachmentAccess;
import moze_intel.projecte.player.PlayerAttachmentKeys;
import moze_intel.projecte.player.PlayerDataService;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AlchemicalBagSessionTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
    }

    @Test
    void openingLegacyBagMigratesAndClearsItsStackContents() {
        PlayerDataService service = service();
        ItemStack legacyBag = bagWith(stack(Items.DIAMOND, 4));

        AlchemicalBagSession session = AlchemicalBagSession.open(
              service, DyeColor.WHITE, legacyBag);

        assertEquals(4, slots(session.contents()).getFirst().getCount());
        assertTrue(legacyBag.getOrDefault(
              DataComponents.CONTAINER, ItemContainerContents.EMPTY)
              .nonEmptyItemCopyStream().findAny().isEmpty());
    }

    @Test
    void openingLegacyBagPreservesOverflowOnItsStack() {
        PlayerDataService service = service();
        NonNullList<ItemStack> full = NonNullList.withSize(
              AlchemicalBagData.SLOTS, ItemStack.EMPTY);
        for (int slot = 0; slot < full.size(); slot++) {
            full.set(slot, stack(Items.COBBLESTONE, 64));
        }
        service.setAlchemicalBagContents(
              DyeColor.WHITE, ItemContainerContents.fromItems(full));
        ItemStack legacyBag = bagWith(stack(Items.DIAMOND, 4));

        AlchemicalBagSession.open(service, DyeColor.WHITE, legacyBag);

        ItemContainerContents remaining = legacyBag.getOrDefault(
              DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        assertEquals(4, slots(remaining).getFirst().getCount());
    }

    @Test
    void repairTalismanRepairsSharedPlayerBagContents() throws Exception {
        PlayerDataService service = service();
        RepairTalismanItem talismanItem = allocateWithoutRegistering();
        ItemStack talisman = new ItemStack(Holder.direct(talismanItem, DataComponentMap.EMPTY));
        ItemStack damagedPickaxe = new ItemStack(Items.IRON_PICKAXE);
        damagedPickaxe.set(DataComponents.MAX_DAMAGE, 250);
        damagedPickaxe.set(DataComponents.DAMAGE, 7);
        service.setAlchemicalBagContents(DyeColor.WHITE,
              ItemContainerContents.fromItems(List.of(talisman, damagedPickaxe)));
        AlchemicalBagSession session = AlchemicalBagSession.open(
              service, DyeColor.WHITE, new ItemStack(Items.SHULKER_BOX));

        session.repairContents();

        assertEquals(6, slots(session.contents()).get(1).getDamageValue());
    }

    @Test
    void savingVisibleSlotsPreservesExistingOverflow() {
        PlayerDataService service = service();
        NonNullList<ItemStack> stored = NonNullList.withSize(
              AlchemicalBagData.SLOTS + 1, ItemStack.EMPTY);
        stored.set(AlchemicalBagData.SLOTS, stack(Items.DIAMOND, 4));
        service.setAlchemicalBagContents(
              DyeColor.WHITE, ItemContainerContents.fromItems(stored));
        AlchemicalBagSession session = AlchemicalBagSession.open(
              service, DyeColor.WHITE, new ItemStack(Items.SHULKER_BOX));

        session.save(ItemContainerContents.fromItems(
              List.of(stack(Items.COBBLESTONE, 1))));

        List<ItemStack> saved = session.contents().allItemsCopyStream().toList();
        assertEquals(AlchemicalBagData.SLOTS + 1, saved.size());
        assertEquals(Items.COBBLESTONE, saved.getFirst().getItem());
        assertEquals(4, saved.get(AlchemicalBagData.SLOTS).getCount());
        assertEquals(Items.DIAMOND, saved.get(AlchemicalBagData.SLOTS).getItem());
    }

    @Test
    void concurrentMovesDoNotDuplicateItems() {
        PlayerDataService service = service();
        service.setAlchemicalBagContents(DyeColor.WHITE,
              ItemContainerContents.fromItems(List.of(stack(Items.DIAMOND, 1))));
        AlchemicalBagSession firstSession = AlchemicalBagSession.open(
              service, DyeColor.WHITE, new ItemStack(Items.SHULKER_BOX));
        AlchemicalBagSession secondSession = AlchemicalBagSession.open(
              service, DyeColor.WHITE, new ItemStack(Items.SHULKER_BOX));
        NonNullList<ItemStack> firstView = slots(firstSession.contents());
        NonNullList<ItemStack> secondView = slots(secondSession.contents());

        firstView.set(0, ItemStack.EMPTY);
        firstView.set(1, stack(Items.DIAMOND, 1));
        firstSession.save(ItemContainerContents.fromItems(firstView));
        secondView.set(0, ItemStack.EMPTY);
        secondView.set(2, stack(Items.DIAMOND, 1));
        secondSession.save(ItemContainerContents.fromItems(secondView));

        long diamonds = secondSession.contents().allItemsCopyStream()
              .filter(item -> item.is(Items.DIAMOND))
              .mapToLong(ItemStack::getCount)
              .sum();
        assertEquals(1, diamonds);
        assertEquals(Items.DIAMOND, slots(secondSession.contents()).get(2).getItem());
    }

    private static PlayerDataService service() {
        return new PlayerDataService(PlayerAttachmentAccess.inMemory(
              PlayerAttachmentKeys::initialValue));
    }

    private static ItemStack bagWith(ItemStack contents) {
        ItemStack bag = new ItemStack(Items.SHULKER_BOX);
        bag.set(DataComponents.CONTAINER,
              ItemContainerContents.fromItems(List.of(contents)));
        return bag;
    }

    private static NonNullList<ItemStack> slots(ItemContainerContents contents) {
        NonNullList<ItemStack> slots = NonNullList.withSize(
              AlchemicalBagData.SLOTS, ItemStack.EMPTY);
        contents.copyInto(slots);
        return slots;
    }

    private static ItemStack stack(Item item, int count) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(DataComponents.MAX_STACK_SIZE, 64);
        return stack;
    }

    private static RepairTalismanItem allocateWithoutRegistering() throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field field = unsafeClass.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Object unsafe = field.get(null);
        Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        return (RepairTalismanItem) allocateInstance.invoke(unsafe, RepairTalismanItem.class);
    }
}
