package moze_intel.projecte.emc.reload;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import moze_intel.projecte.emc.ItemStackKey;
import moze_intel.projecte.emc.recipe.MinecraftStackKeyFactory;
import moze_intel.projecte.emc.recipe.RecipeConversion;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BrewingConversionSourceTest {
    private static MinecraftStackKeyFactory keyFactory;
    private static List<RecipeConversion> conversions;

    @BeforeAll
    static void bootstrap() {
        MinecraftTestHarness.bootstrap();
        HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        keyFactory = new MinecraftStackKeyFactory(registries);
        conversions = new BrewingConversionSource(
              PotionBrewing.bootstrap(FeatureFlags.DEFAULT_FLAGS), registries).conversions();
    }

    @Test
    void mapsWaterBottleAndPotionContainerConversions() {
        ItemStackKey water = potion(Items.POTION, Potions.WATER);
        ItemStackKey awkward = potion(Items.POTION, Potions.AWKWARD);
        ItemStackKey splashAwkward = potion(Items.SPLASH_POTION, Potions.AWKWARD);
        ItemStackKey lingeringAwkward = potion(Items.LINGERING_POTION, Potions.AWKWARD);

        assertEquals(1, conversion(water, key(Items.GLASS_BOTTLE)).outputCount());
        assertEquals(3, conversion(awkward, key(Items.NETHER_WART)).outputCount());
        assertEquals(3, conversion(splashAwkward, key(Items.GUNPOWDER)).outputCount());
        assertEquals(3, conversion(lingeringAwkward, key(Items.DRAGON_BREATH)).outputCount());
    }

    @Test
    void mapsLingeringPotionsToComponentPreservingTippedArrows() {
        ItemStackKey lingering = potion(Items.LINGERING_POTION, Potions.AWKWARD);
        ItemStackKey arrows = potion(Items.TIPPED_ARROW, Potions.AWKWARD);
        RecipeConversion conversion = conversions.stream()
              .filter(candidate -> candidate.output().equals(arrows))
              .filter(candidate -> candidate.ingredients().equals(Map.of(
                    lingering, 1, key(Items.ARROW), 8)))
              .findFirst()
              .orElseThrow();

        assertEquals(8, conversion.outputCount());
    }

    private static RecipeConversion conversion(ItemStackKey output, ItemStackKey reagent) {
        return conversions.stream()
              .filter(candidate -> candidate.output().equals(output))
              .filter(candidate -> candidate.ingredients().containsKey(reagent))
              .findFirst()
              .orElseThrow();
    }

    private static ItemStackKey potion(Item item, net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> potion) {
        return keyFactory.key(PotionContents.createItemStack(item, potion));
    }

    private static ItemStackKey key(Item item) {
        return keyFactory.key(new ItemStack(item));
    }
}
