package moze_intel.projecte.content.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import moze_intel.projecte.content.items.tools.MatterAxeItem;
import moze_intel.projecte.content.items.tools.MatterHoeItem;
import moze_intel.projecte.content.items.tools.MatterPickaxeItem;
import moze_intel.projecte.content.items.tools.MatterShearsItem;
import moze_intel.projecte.content.items.tools.MatterShovelItem;
import moze_intel.projecte.content.items.tools.MatterToolItem;
import moze_intel.projecte.content.items.tools.MatterToolTags;
import moze_intel.projecte.content.items.tools.MatterSwordItem;
import moze_intel.projecte.content.items.tools.RedMatterSwordItem;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ChargeableItemTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
    }

    @Test
    void everyChargeableToolImplementsTheSharedChargeContract() {
        for (Class<?> itemClass : List.of(
              PhilosophersStoneItem.class,
              MatterToolItem.class,
              MatterPickaxeItem.class,
              MatterSwordItem.class,
              RedMatterSwordItem.class,
              MatterAxeItem.class,
              MatterShovelItem.class,
              MatterHoeItem.class,
              MatterShearsItem.class,
              DarkMatterHammerItem.class,
              RedMatterHammerItem.class,
              RedMatterKatarItem.class,
              RedMatterMorningStarItem.class)) {
            assertTrue(IItemCharge.class.isAssignableFrom(itemClass));
        }
    }

    @Test
    void officialMatterToolChargeTiersArePreserved() {
        assertEquals(2, allocate(DarkMatterHammerItem.class).getMaxCharge(null));
        assertEquals(3, allocate(RedMatterHammerItem.class).getMaxCharge(null));
        assertEquals(4, allocate(RedMatterKatarItem.class).getMaxCharge(null));
        assertEquals(4, allocate(RedMatterMorningStarItem.class).getMaxCharge(null));
    }

    @Test
    void matterToolModesWrapInOfficialOrder() {
        assertEquals(MatterPickaxeItem.PickaxeMode.TALLSHOT,
              MatterPickaxeItem.PickaxeMode.STANDARD.next());
        assertEquals(MatterPickaxeItem.PickaxeMode.STANDARD,
              MatterPickaxeItem.PickaxeMode.LONGSHOT.next());
        assertEquals(RedMatterSwordItem.KatarMode.SLAY_ALL,
              RedMatterSwordItem.KatarMode.SLAY_HOSTILE.next());
        assertEquals(RedMatterSwordItem.KatarMode.SLAY_HOSTILE,
              RedMatterSwordItem.KatarMode.SLAY_ALL.next());
    }

    @Test
    void everyMatterToolFamilyHasItsBlockInteractionEntryPoint() throws Exception {
        for (Class<?> itemClass : List.of(
              MatterPickaxeItem.class,
              MatterAxeItem.class,
              MatterShovelItem.class,
              MatterHoeItem.class,
              MatterShearsItem.class,
              DarkMatterHammerItem.class,
              RedMatterHammerItem.class,
              RedMatterKatarItem.class,
              RedMatterMorningStarItem.class)) {
            assertEquals(itemClass,
                  itemClass.getDeclaredMethod("useOn", UseOnContext.class).getDeclaringClass());
        }
        assertEquals("projecte:mineable/hammer", MatterToolTags.HAMMER.location().toString());
        assertEquals("projecte:mineable/katar", MatterToolTags.KATAR.location().toString());
        assertEquals("projecte:mineable/morning_star",
              MatterToolTags.MORNING_STAR.location().toString());
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
        return allocate(TestChargeableItem.class);
    }

    private static <T> T allocate(Class<T> type) {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Object unsafe = field.get(null);
            Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
            return type.cast(allocateInstance.invoke(unsafe, type));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
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
