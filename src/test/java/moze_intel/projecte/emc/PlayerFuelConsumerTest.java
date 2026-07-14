package moze_intel.projecte.emc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import moze_intel.projecte.content.items.KleinStarItem;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PlayerFuelConsumerTest {
    @BeforeAll
    static void bootstrapRegistries() {
        MinecraftTestHarness.bootstrap();
    }

    @Test
    void consumesKleinStarBeforeLooseFuel() throws Exception {
        KleinStarItem star = allocateKleinStar("ein");
        ItemStack starStack = new ItemStack(Holder.direct(star, DataComponentMap.EMPTY));
        KleinStarItem.setStoredEmc(starStack, 1_000);
        SimpleContainer inventory = new SimpleContainer(
              stack(Items.REDSTONE, 6), starStack);

        long consumed = PlayerFuelConsumer.consume(
              inventory, 384,
              stack -> stack.is(Items.REDSTONE), ignored -> 64);

        assertEquals(384, consumed);
        assertEquals(616, KleinStarItem.getStoredEmc(starStack));
        assertEquals(6, inventory.getItem(0).getCount());
    }

    @Test
    void consumesWholeFuelItemsAndDiscardsExcessEmc() {
        SimpleContainer inventory = new SimpleContainer(stack(Items.COAL, 1));

        long consumed = PlayerFuelConsumer.consume(
              inventory, 384,
              stack -> stack.is(Items.COAL), ignored -> 512);

        assertEquals(512, consumed);
        assertTrue(inventory.getItem(0).isEmpty());
    }

    @Test
    void leavesFuelUntouchedWhenTheCombinedValueIsInsufficient() {
        SimpleContainer inventory = new SimpleContainer(stack(Items.REDSTONE, 5));

        long consumed = PlayerFuelConsumer.consume(
              inventory, 384,
              stack -> stack.is(Items.REDSTONE), ignored -> 64);

        assertEquals(-1, consumed);
        assertEquals(5, inventory.getItem(0).getCount());
    }

    private static KleinStarItem allocateKleinStar(String tier) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object unsafe = unsafeField.get(null);
        Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        KleinStarItem star = (KleinStarItem) allocateInstance.invoke(unsafe, KleinStarItem.class);
        Field tierField = KleinStarItem.class.getDeclaredField("tier");
        tierField.setAccessible(true);
        tierField.set(star, tier);
        return star;
    }

    private static ItemStack stack(net.minecraft.world.item.Item item, int count) {
        ItemStack stack = new ItemStack(item);
        stack.setCount(count);
        return stack;
    }
}
