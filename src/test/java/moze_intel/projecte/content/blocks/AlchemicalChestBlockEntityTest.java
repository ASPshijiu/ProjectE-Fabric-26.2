package moze_intel.projecte.content.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import moze_intel.projecte.content.items.RepairTalismanItem;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AlchemicalChestBlockEntityTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
    }

    @Test
    void repairTalismanRepairsDamagedChestContents() throws Exception {
        SimpleContainer chest = new SimpleContainer(2);
        chest.setItem(0, talismanStack());
        chest.setItem(1, damagedPickaxe(7));

        boolean handled = AlchemicalChestBlockEntity.repairContents(chest);

        assertTrue(handled);
        assertEquals(6, chest.getItem(1).getDamageValue());
    }

    @Test
    void damagedContentsStayUnchangedWithoutTalisman() {
        SimpleContainer chest = new SimpleContainer(1);
        chest.setItem(0, damagedPickaxe(7));

        boolean handled = AlchemicalChestBlockEntity.repairContents(chest);

        assertFalse(handled);
        assertEquals(7, chest.getItem(0).getDamageValue());
    }

    @Test
    void usesUpstreamChestCapacity() {
        assertEquals(104, AlchemicalChestBlockEntity.SLOTS);
    }

    private static ItemStack damagedPickaxe(int damage) {
        ItemStack stack = new ItemStack(Items.IRON_PICKAXE);
        stack.set(DataComponents.MAX_DAMAGE, 250);
        stack.set(DataComponents.DAMAGE, damage);
        return stack;
    }

    private static ItemStack talismanStack() throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field field = unsafeClass.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Object unsafe = field.get(null);
        Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        RepairTalismanItem item =
              (RepairTalismanItem) allocateInstance.invoke(unsafe, RepairTalismanItem.class);
        return new ItemStack(Holder.direct(item, DataComponentMap.EMPTY));
    }
}
