package moze_intel.projecte.content.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PedestalBlockEntityTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
    }

    @Test
    void insertsOneItemIntoEmptyPedestal() {
        SimpleContainer pedestal = new SimpleContainer(1);
        ItemStack held = new ItemStack(Items.DIAMOND, 3);

        boolean inserted = PedestalBlockEntity.insertItem(pedestal, held);

        assertTrue(inserted);
        assertEquals(1, pedestal.getItem(0).getCount());
        assertEquals(2, held.getCount());
    }

    @Test
    void occupiedPedestalRejectsAnotherItem() {
        SimpleContainer pedestal = new SimpleContainer(new ItemStack(Items.DIAMOND));
        ItemStack held = new ItemStack(Items.EMERALD);

        boolean inserted = PedestalBlockEntity.insertItem(pedestal, held);

        assertFalse(inserted);
        assertTrue(pedestal.getItem(0).is(Items.DIAMOND));
        assertEquals(1, held.getCount());
    }

    @Test
    void takesStoredItemAndClearsPedestal() {
        SimpleContainer pedestal = new SimpleContainer(new ItemStack(Items.DIAMOND));

        ItemStack removed = PedestalBlockEntity.takeItem(pedestal);

        assertTrue(removed.is(Items.DIAMOND));
        assertTrue(pedestal.isEmpty());
    }

    @Test
    void emptyHandFallsThroughToActivationInteraction() throws Exception {
        PedestalBlock block = allocateBlockWithoutRegistering();

        InteractionResult result = block.useItemOn(
              ItemStack.EMPTY, null, null, null, null, null, null);

        assertSame(InteractionResult.TRY_WITH_EMPTY_HAND, result);
    }

    private static PedestalBlock allocateBlockWithoutRegistering() throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field field = unsafeClass.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Object unsafe = field.get(null);
        Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        return (PedestalBlock) allocateInstance.invoke(unsafe, PedestalBlock.class);
    }
}
