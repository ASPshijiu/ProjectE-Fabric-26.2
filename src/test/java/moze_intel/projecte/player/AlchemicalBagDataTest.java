package moze_intel.projecte.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.List;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AlchemicalBagDataTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
    }

    @Test
    void storesIndependentInventoriesByColor() {
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 3);
        diamonds.set(DataComponents.MAX_STACK_SIZE, 64);
        ItemContainerContents whiteContents = ItemContainerContents.fromItems(List.of(diamonds));

        AlchemicalBagData updated = AlchemicalBagData.empty()
              .withContents(DyeColor.WHITE, whiteContents);
        ItemContainerContents storedWhite = updated.contents(DyeColor.WHITE);
        ItemContainerContents storedBlack = updated.contents(DyeColor.BLACK);

        NonNullList<ItemStack> white = NonNullList.withSize(104, ItemStack.EMPTY);
        storedWhite.copyInto(white);
        assertEquals(3, white.getFirst().getCount());
        assertTrue(storedBlack.nonEmptyItemCopyStream().findAny().isEmpty());
    }

    @Test
    void persistenceCodecRoundTripsColoredInventories() {
        AlchemicalBagData data = AlchemicalBagData.empty()
              .withContents(DyeColor.WHITE, contents(Items.DIAMOND, 3))
              .withContents(DyeColor.BLACK, contents(Items.COAL, 12));

        JsonElement encoded = AlchemicalBagData.CODEC.encodeStart(JsonOps.INSTANCE, data)
              .getOrThrow(IllegalStateException::new);
        AlchemicalBagData decoded = AlchemicalBagData.CODEC.parse(JsonOps.INSTANCE, encoded)
              .getOrThrow(IllegalStateException::new);

        assertEquals(data, decoded);
    }

    private static ItemContainerContents contents(net.minecraft.world.item.Item item, int count) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(DataComponents.MAX_STACK_SIZE, 64);
        return ItemContainerContents.fromItems(List.of(stack));
    }
}
