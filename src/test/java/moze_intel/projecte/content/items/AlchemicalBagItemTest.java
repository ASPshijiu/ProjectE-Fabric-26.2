package moze_intel.projecte.content.items;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AlchemicalBagItemTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
    }

    @Test
    void repairTalismanRepairsDamagedBagContents() throws Exception {
        RepairTalismanItem talismanItem = allocateWithoutRegistering();
        ItemStack talisman = new ItemStack(Holder.direct(talismanItem, DataComponentMap.EMPTY));
        ItemStack damagedPickaxe = new ItemStack(Items.IRON_PICKAXE);
        damagedPickaxe.set(DataComponents.MAX_DAMAGE, 250);
        damagedPickaxe.set(DataComponents.DAMAGE, 7);
        ItemStack bag = new ItemStack(Items.SHULKER_BOX);
        bag.set(DataComponents.CONTAINER,
              ItemContainerContents.fromItems(List.of(talisman, damagedPickaxe)));

        AlchemicalBagItem.repairContents(bag);

        NonNullList<ItemStack> contents = NonNullList.withSize(27, ItemStack.EMPTY);
        bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(contents);
        assertEquals(6, contents.get(1).getDamageValue());
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
