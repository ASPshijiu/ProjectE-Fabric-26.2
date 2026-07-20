package moze_intel.projecte.content.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

class AlchemicalBagMenuTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
    }

    @Test
    void repairsTheLiveMenuInventory() throws Exception {
        RepairTalismanItem talismanItem = allocateWithoutRegistering();
        ItemStack talisman = new ItemStack(Holder.direct(talismanItem, DataComponentMap.EMPTY));
        ItemStack damagedPickaxe = new ItemStack(Items.IRON_PICKAXE);
        damagedPickaxe.set(DataComponents.MAX_DAMAGE, 250);
        damagedPickaxe.set(DataComponents.DAMAGE, 7);
        SimpleContainer contents = new SimpleContainer(talisman, damagedPickaxe);

        assertTrue(AlchemicalBagMenu.repairContents(contents));
        assertEquals(6, contents.getItem(1).getDamageValue());
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
