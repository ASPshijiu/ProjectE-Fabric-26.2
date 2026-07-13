package moze_intel.projecte.emc.reload;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import moze_intel.projecte.emc.ItemStackKey;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.emc.recipe.MinecraftStackKeyFactory;
import moze_intel.projecte.emc.recipe.RecipeConversion;
import moze_intel.projecte.emc.recipe.RecipeConversionCollector;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;

public class VanillaRecipeConversionSource implements RecipeConversionSource {
    private final RecipeManager recipeManager;
    private final MinecraftStackKeyFactory keyFactory;
    private final RecipeConversionCollector collector;
    private static final int MAX_COMBINATIONS = 100_000;
    private static final ContextMap EMPTY_CONTEXT = createEmptyContext();

    private static ContextMap createEmptyContext() {
        try {
            Constructor<ContextKeySet> csCtor = ContextKeySet.class.getDeclaredConstructor(Set.class, Set.class);
            csCtor.setAccessible(true);
            ContextKeySet empty = csCtor.newInstance(Set.of(), Set.of());
            return new ContextMap.Builder().create(empty);
        } catch (Exception e) {
            throw new RuntimeException("Cannot create empty ContextMap", e);
        }
    }

    public VanillaRecipeConversionSource(RecipeManager recipeManager, HolderLookup.Provider registries) {
        this.recipeManager = recipeManager;
        this.keyFactory = new MinecraftStackKeyFactory(registries);
        this.collector = new RecipeConversionCollector();
    }

    @Override
    public List<RecipeConversion> conversions() {
        List<RecipeConversion> all = new ArrayList<>();

        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            if (!(holder.value() instanceof CraftingRecipe craftingRecipe)) continue;
            Identifier recipeId = holder.id().identifier();

            try {
                List<RecipeDisplay> displays = craftingRecipe.display();
                for (RecipeDisplay display : displays) {
                    ItemStack resultStack = display.result().resolveForStacks(EMPTY_CONTEXT)
                          .stream().findFirst().orElse(ItemStack.EMPTY);
                    if (resultStack.isEmpty()) continue;

                    Optional<ItemStackKey> outputKey = keyFactory.optionalKey(resultStack);
                    if (outputKey.isEmpty()) continue;

                    List<SlotDisplay> ingredientSlots;
                    if (display instanceof ShapedCraftingRecipeDisplay shaped) {
                        ingredientSlots = shaped.ingredients();
                    } else if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
                        ingredientSlots = shapeless.ingredients();
                    } else {
                        continue;
                    }

                    List<List<NormalizedStackKey>> ingredientChoices = new ArrayList<>();
                    for (SlotDisplay slot : ingredientSlots) {
                        List<NormalizedStackKey> choices = new ArrayList<>();
                        for (ItemStack stack : slot.resolveForStacks(EMPTY_CONTEXT)) {
                            if (stack.isEmpty()) continue;
                            keyFactory.optionalKey(stack).ifPresent(choices::add);
                        }
                        if (choices.isEmpty()) continue;
                        ingredientChoices.add(choices);
                    }

                    if (ingredientChoices.isEmpty()) continue;

                    all.addAll(collector.collect(
                          recipeId,
                          resultStack.getCount(),
                          outputKey.get(),
                          ingredientChoices,
                          Map.of(),
                          MAX_COMBINATIONS
                    ));
                }
            } catch (Exception ignored) {}
        }
        return List.copyOf(all);
    }
}
