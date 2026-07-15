package moze_intel.projecte.content.items;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
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
