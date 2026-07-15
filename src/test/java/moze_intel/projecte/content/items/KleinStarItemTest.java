package moze_intel.projecte.content.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class KleinStarItemTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
    }

    @Test
    void addingHugeAmountFillsStarWithoutLongOverflow() throws Exception {
        KleinStarItem star = allocateKleinStar("ein");
        ItemStack stack = new ItemStack(Holder.direct(star, DataComponentMap.EMPTY));
        KleinStarItem.setStoredEmc(stack, 49_999);

        long overflow = KleinStarItem.addEmc(stack, Long.MAX_VALUE);

        assertEquals(KleinStarItem.MAX_EIN, KleinStarItem.getStoredEmc(stack));
        assertEquals(Long.MAX_VALUE - 1, overflow);
    }

    @Test
    void storedEmcControlsCapacityBarVisibility() throws Exception {
        KleinStarItem star = allocateKleinStar("ein");
        ItemStack stack = new ItemStack(Holder.direct(star, DataComponentMap.EMPTY));

        assertFalse(star.isBarVisible(stack));
        KleinStarItem.setStoredEmc(stack, 1);
        assertTrue(star.isBarVisible(stack));
    }

    @Test
    void capacityBarWidthScalesFromHalfToFull() throws Exception {
        KleinStarItem star = allocateKleinStar("ein");
        ItemStack stack = new ItemStack(Holder.direct(star, DataComponentMap.EMPTY));

        KleinStarItem.setStoredEmc(stack, KleinStarItem.MAX_EIN / 2);
        assertEquals(7, star.getBarWidth(stack));
        KleinStarItem.setStoredEmc(stack, KleinStarItem.MAX_EIN);
        assertEquals(13, star.getBarWidth(stack));
    }

    @Test
    void capacityBarColorChangesFromYellowToGreen() throws Exception {
        KleinStarItem star = allocateKleinStar("ein");
        ItemStack stack = new ItemStack(Holder.direct(star, DataComponentMap.EMPTY));

        KleinStarItem.setStoredEmc(stack, KleinStarItem.MAX_EIN / 2);
        assertEquals(Mth.hsvToRgb(1.0F / 6.0F, 1.0F, 1.0F), star.getBarColor(stack));
        KleinStarItem.setStoredEmc(stack, KleinStarItem.MAX_EIN);
        assertEquals(Mth.hsvToRgb(1.0F / 3.0F, 1.0F, 1.0F), star.getBarColor(stack));
    }

    private static KleinStarItem allocateKleinStar(String tier) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object unsafe = unsafeField.get(null);
        Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        KleinStarItem star = (KleinStarItem) allocateInstance.invoke(
              unsafe, KleinStarItem.class);
        Field tierField = KleinStarItem.class.getDeclaredField("tier");
        tierField.setAccessible(true);
        tierField.set(star, tier);
        return star;
    }
}
