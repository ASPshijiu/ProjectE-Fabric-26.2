package moze_intel.projecte.content.items;

import static org.junit.jupiter.api.Assertions.assertEquals;

import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RepairTalismanItemTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
    }

    @Test
    void repairsWithoutPlayerEmc() {
        ItemStack damagedPickaxe = new ItemStack(Items.IRON_PICKAXE);
        damagedPickaxe.set(DataComponents.MAX_DAMAGE, 250);
        damagedPickaxe.set(DataComponents.DAMAGE, 7);
        SimpleContainer inventory = new SimpleContainer(damagedPickaxe);

        RepairTalismanItem.tickRepair(inventory, true);

        assertEquals(6, damagedPickaxe.getDamageValue());
    }

    @Test
    void repairsEveryDamagedItemInInventory() {
        ItemStack damagedPickaxe = new ItemStack(Items.IRON_PICKAXE);
        damagedPickaxe.set(DataComponents.MAX_DAMAGE, 250);
        damagedPickaxe.set(DataComponents.DAMAGE, 7);
        ItemStack damagedAxe = new ItemStack(Items.IRON_AXE);
        damagedAxe.set(DataComponents.MAX_DAMAGE, 250);
        damagedAxe.set(DataComponents.DAMAGE, 11);
        SimpleContainer inventory = new SimpleContainer(damagedPickaxe, damagedAxe);

        RepairTalismanItem.tickRepair(inventory, true);

        assertEquals(6, damagedPickaxe.getDamageValue());
        assertEquals(10, damagedAxe.getDamageValue());
    }
}
