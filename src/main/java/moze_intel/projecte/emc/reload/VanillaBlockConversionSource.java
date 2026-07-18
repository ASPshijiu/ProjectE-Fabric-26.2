package moze_intel.projecte.emc.reload;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import moze_intel.projecte.emc.ItemStackKey;
import moze_intel.projecte.emc.recipe.MinecraftStackKeyFactory;
import moze_intel.projecte.emc.recipe.RecipeConversion;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;

public final class VanillaBlockConversionSource implements RecipeConversionSource {
    private final MinecraftStackKeyFactory keyFactory;

    public VanillaBlockConversionSource(HolderLookup.Provider registries) {
        keyFactory = new MinecraftStackKeyFactory(registries);
    }

    @Override
    public List<RecipeConversion> conversions() {
        List<RecipeConversion> conversions = new ArrayList<>();
        WeatheringCopper.NEXT_BY_BLOCK.get().entrySet().stream()
              .sorted(Comparator.comparing(entry -> blockId(entry.getKey()).toString()))
              .forEach(entry -> addBidirectional(conversions, "oxidation", entry.getKey(), entry.getValue()));

        ItemStackKey honeycomb = keyFactory.key(new ItemStack(Items.HONEYCOMB));
        HoneycombItem.WAXABLES.get().entrySet().stream()
              .sorted(Comparator.comparing(entry -> blockId(entry.getKey()).toString()))
              .forEach(entry -> {
                  ItemStackKey base = blockKey(entry.getKey());
                  ItemStackKey waxed = blockKey(entry.getValue());
                  if (base == null || waxed == null) return;
                  Identifier id = conversionId("waxing", entry.getKey());
                  conversions.add(new RecipeConversion(id, 1, waxed, Map.of(base, 1, honeycomb, 1)));
                  conversions.add(new RecipeConversion(id, 1, base, Map.of(waxed, 1)));
              });
        return List.copyOf(conversions);
    }

    private void addBidirectional(List<RecipeConversion> conversions, String type, Block from, Block to) {
        ItemStackKey fromKey = blockKey(from);
        ItemStackKey toKey = blockKey(to);
        if (fromKey == null || toKey == null) return;
        Identifier id = conversionId(type, from);
        conversions.add(new RecipeConversion(id, 1, toKey, Map.of(fromKey, 1)));
        conversions.add(new RecipeConversion(id, 1, fromKey, Map.of(toKey, 1)));
    }

    private ItemStackKey blockKey(Block block) {
        return block.asItem() == Items.AIR ? null : keyFactory.key(new ItemStack(block));
    }

    private static Identifier conversionId(String type, Block block) {
        Identifier blockId = blockId(block);
        return Identifier.fromNamespaceAndPath(
              "projecte", type + "/" + blockId.getNamespace() + "/" + blockId.getPath());
    }

    private static Identifier blockId(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block);
    }
}
