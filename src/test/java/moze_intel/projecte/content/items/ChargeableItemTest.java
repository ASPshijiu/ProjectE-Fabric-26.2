package moze_intel.projecte.content.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ChargeableItemTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
    }

    @Test
    void everyChargeableToolUsesTheSharedChargeBar() {
        for (Class<?> itemClass : List.of(
              PhilosophersStoneItem.class,
              DarkMatterHammerItem.class,
              RedMatterHammerItem.class,
              RedMatterKatarItem.class,
              RedMatterMorningStarItem.class)) {
            assertTrue(ChargeableItem.class.isAssignableFrom(itemClass));
        }
    }

    @Test
    void chargeControlsBarVisibilityWidthAndColor() throws Exception {
        TestChargeableItem item = allocateWithoutRegistering();

        assertFalse(item.isBarVisible(null));
        item.charge = 2;
        assertTrue(item.isBarVisible(null));
        assertEquals(7, item.getBarWidth(null));
        assertEquals(Mth.hsvToRgb(1.0F / 6.0F, 1.0F, 1.0F), item.getBarColor(null));
        item.charge = 4;
        assertEquals(13, item.getBarWidth(null));
    }

    private static TestChargeableItem allocateWithoutRegistering() throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field field = unsafeClass.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Object unsafe = field.get(null);
        Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        return (TestChargeableItem) allocateInstance.invoke(unsafe, TestChargeableItem.class);
    }

    private static final class TestChargeableItem extends ChargeableItem {
        private int charge;

        private TestChargeableItem(Properties properties) {
            super(properties);
        }

        @Override
        public int getMaxCharge(ItemStack stack) {
            return 4;
        }

        @Override
        public int getCharge(ItemStack stack) {
            return charge;
        }
    }
}
