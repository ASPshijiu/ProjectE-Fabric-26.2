package moze_intel.projecte.emc.reload;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import moze_intel.projecte.emc.ItemStackKey;
import moze_intel.projecte.emc.recipe.MinecraftStackKeyFactory;
import moze_intel.projecte.emc.recipe.RecipeConversion;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class VanillaBlockConversionSourceTest {
    private static MinecraftStackKeyFactory keyFactory;
    private static List<RecipeConversion> conversions;

    @BeforeAll
    static void bootstrap() {
        MinecraftTestHarness.bootstrap();
        HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        keyFactory = new MinecraftStackKeyFactory(registries);
        conversions = new VanillaBlockConversionSource(registries).conversions();
    }

    @Test
    void mapsCopperOxidationAndWaxingInBothDirections() {
        ItemStackKey copperDoor = key(block("minecraft:copper_door"));
        ItemStackKey exposedDoor = key(block("minecraft:exposed_copper_door"));
        ItemStackKey waxedExposedDoor = key(block("minecraft:waxed_exposed_copper_door"));
        ItemStackKey honeycomb = keyFactory.key(new ItemStack(Items.HONEYCOMB));

        assertConversion(exposedDoor, Map.of(copperDoor, 1));
        assertConversion(copperDoor, Map.of(exposedDoor, 1));
        assertConversion(waxedExposedDoor, Map.of(exposedDoor, 1, honeycomb, 1));
        assertConversion(exposedDoor, Map.of(waxedExposedDoor, 1));
    }

    private static void assertConversion(ItemStackKey output, Map<ItemStackKey, Integer> ingredients) {
        assertTrue(conversions.stream().anyMatch(conversion ->
              conversion.output().equals(output) && conversion.ingredients().equals(ingredients)));
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.get(Identifier.parse(id)).orElseThrow().value();
    }

    private static ItemStackKey key(Block block) {
        return keyFactory.key(new ItemStack(block));
    }
}
