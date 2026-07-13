package moze_intel.projecte.content.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MenuQuickMoveTest {
    @BeforeAll
    static void bootstrapRegistries() {
        MinecraftTestHarness.bootstrap();
    }

    @Test
    void partialMoveConsumesTheSourceAndReturnsItsOriginalContents() {
        SimpleContainer container = new SimpleContainer(1);
        ItemStack stack = new ItemStack(Items.STONE);
        stack.set(DataComponents.MAX_STACK_SIZE, 64);
        stack.setCount(10);
        container.setItem(0, stack);
        Slot source = new Slot(container, 0, 0, 0);

        ItemStack moved = MenuQuickMove.move(source, sourceStack -> {
            sourceStack.shrink(4);
            return true;
        });

        assertEquals(10, moved.getCount());
        assertEquals(6, source.getItem().getCount());
    }

    @Test
    void roomForOneStackCountsOnlyCompatibleDestinationSpace() {
        SimpleContainer destination = new SimpleContainer(2);
        destination.setItem(0, stack(Items.STONE, 63));
        destination.setItem(1, stack(Items.DIRT, 64));

        assertEquals(1, MenuQuickMove.roomForOneStack(
              stack(Items.STONE, 1),
              List.of(new Slot(destination, 0, 0, 0), new Slot(destination, 1, 0, 0))));
    }

    private static ItemStack stack(net.minecraft.world.item.Item item, int count) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.MAX_STACK_SIZE, 64);
        stack.setCount(count);
        return stack;
    }
}
