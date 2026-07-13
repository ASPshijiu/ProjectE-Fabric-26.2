package moze_intel.projecte.content.menu.slots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.concurrent.atomic.AtomicInteger;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TransmuteUnlearnSlotTest {
    @BeforeAll
    static void bootstrapRegistries() {
        MinecraftTestHarness.bootstrap();
    }

    @Test
    void unlearnsAndRetainsTheItemUntilTheMenuReturnsIt() {
        SimpleContainer container = new SimpleContainer(1);
        AtomicInteger unlearnCalls = new AtomicInteger();
        TransmuteUnlearnSlot slot = new TransmuteUnlearnSlot(
              container, 0, 0, 0, () -> true, stack -> unlearnCalls.incrementAndGet());

        slot.set(new ItemStack(Items.STONE));

        assertEquals(1, unlearnCalls.get());
        assertFalse(slot.getItem().isEmpty());
    }
}
