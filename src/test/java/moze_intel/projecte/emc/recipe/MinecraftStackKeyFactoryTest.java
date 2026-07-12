package moze_intel.projecte.emc.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.google.gson.JsonPrimitive;
import java.util.Map;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MinecraftStackKeyFactoryTest {
    @BeforeAll
    static void bootstrapRegistries() {
        MinecraftTestHarness.bootstrap();
    }

    @Test
    void convertsRegisteredItemsAndCanonicalComponents() {
        MinecraftStackKeyFactory factory = new MinecraftStackKeyFactory(patch -> Map.of(
              "minecraft:damage", new JsonPrimitive(1)
        ));
        var key = factory.key(new ItemStack(Items.DIAMOND));
        assertEquals("item|minecraft:diamond|{\"minecraft:damage\":1}", key.canonicalString());
    }

    @Test
    void rejectsEmptyStacks() {
        MinecraftStackKeyFactory factory = new MinecraftStackKeyFactory(patch -> Map.of());
        assertEquals(java.util.Optional.empty(), factory.optionalKey(ItemStack.EMPTY));
    }
}
