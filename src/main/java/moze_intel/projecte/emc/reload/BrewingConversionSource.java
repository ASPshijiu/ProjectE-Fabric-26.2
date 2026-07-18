package moze_intel.projecte.emc.reload;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.emc.FakeStackKey;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.emc.recipe.MinecraftStackKeyFactory;
import moze_intel.projecte.emc.recipe.RecipeConversion;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;

public final class BrewingConversionSource implements RecipeConversionSource {
    private static final List<Item> CONTAINERS = List.of(
          Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION);
    private final PotionBrewing brewing;
    private final HolderLookup.Provider registries;
    private final MinecraftStackKeyFactory keyFactory;

    public BrewingConversionSource(PotionBrewing brewing, HolderLookup.Provider registries) {
        this.brewing = brewing;
        this.registries = registries;
        keyFactory = new MinecraftStackKeyFactory(registries);
    }

    @Override
    public List<RecipeConversion> conversions() {
        List<RecipeConversion> conversions = new ArrayList<>();
        ItemStack waterBottle = PotionContents.createItemStack(Items.POTION,
              net.minecraft.world.item.alchemy.Potions.WATER);
        conversions.add(new RecipeConversion(ProjectEAPI.id("brewing/water_bottle"), 1,
              keyFactory.key(waterBottle), Map.of(
                    keyFactory.key(new ItemStack(Items.GLASS_BOTTLE)), 1,
                    new FakeStackKey(ProjectEAPI.id("fluid/minecraft/water")), 333)));

        List<Holder.Reference<Potion>> potions = registries.lookupOrThrow(Registries.POTION)
              .listElements()
              .sorted(Comparator.comparing(holder -> holder.key().identifier().toString()))
              .toList();
        List<Item> reagents = BuiltInRegistries.ITEM.stream()
              .filter(item -> item != Items.AIR)
              .sorted(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()))
              .toList();

        int conversionIndex = 0;
        for (Holder<Potion> potion : potions) {
            for (Item container : CONTAINERS) {
                ItemStack input = PotionContents.createItemStack(container, potion);
                for (Item reagent : reagents) {
                    ItemStack reagentStack = new ItemStack(reagent);
                    if (!brewing.hasMix(input, reagentStack)) continue;
                    ItemStack output = brewing.mix(reagentStack, input);
                    if (output.isEmpty()) continue;
                    conversions.add(conversion(
                          ProjectEAPI.id("brewing/mix_" + conversionIndex++), output,
                          Map.of(keyFactory.key(input), 3, keyFactory.key(reagentStack), 1), 3));
                }
            }
            ItemStack lingering = PotionContents.createItemStack(Items.LINGERING_POTION, potion);
            ItemStack tippedArrows = PotionContents.createItemStack(Items.TIPPED_ARROW, potion);
            conversions.add(conversion(
                  ProjectEAPI.id("brewing/tipped_arrow_" + conversionIndex++), tippedArrows,
                  Map.of(keyFactory.key(lingering), 1,
                        keyFactory.key(new ItemStack(Items.ARROW)), 8), 8));
        }
        return List.copyOf(conversions);
    }

    private RecipeConversion conversion(
          Identifier id, ItemStack output, Map<NormalizedStackKey, Integer> ingredients,
          int outputCount
    ) {
        return new RecipeConversion(id, outputCount * output.getCount(),
              keyFactory.key(output), ingredients);
    }
}
