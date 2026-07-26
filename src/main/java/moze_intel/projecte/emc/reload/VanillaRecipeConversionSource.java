package moze_intel.projecte.emc.reload;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import moze_intel.projecte.emc.ItemStackKey;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.emc.recipe.MinecraftStackKeyFactory;
import moze_intel.projecte.emc.recipe.RecipeConversion;
import moze_intel.projecte.emc.recipe.RecipeConversionCollector;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import net.minecraft.world.item.crafting.display.StonecutterRecipeDisplay;

public class VanillaRecipeConversionSource implements RecipeConversionSource {
    private final RecipeManager recipeManager;
    private final MinecraftStackKeyFactory keyFactory;
    private final RecipeConversionCollector collector;
    private final ContextMap displayContext;
    private static final int MAX_COMBINATIONS = 100_000;

    public VanillaRecipeConversionSource(RecipeManager recipeManager, HolderLookup.Provider registries) {
        this.recipeManager = recipeManager;
        this.keyFactory = new MinecraftStackKeyFactory(registries);
        this.collector = new RecipeConversionCollector();
        this.displayContext = new ContextMap.Builder()
              .withParameter(SlotDisplayContext.REGISTRIES, registries)
              .create(SlotDisplayContext.CONTEXT);
    }

    @Override
    public List<RecipeConversion> conversions() {
        List<RecipeConversion> all = new ArrayList<>();

        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            Recipe<?> recipe = holder.value();
            Identifier recipeId = holder.id().identifier();

            try {
                List<RecipeDisplay> displays = recipe.display();
                for (int displayIndex = 0; displayIndex < displays.size(); displayIndex++) {
                    RecipeDisplay display = displays.get(displayIndex);
                    ItemStack resultStack = display.result().resolveForStacks(displayContext)
                          .stream().findFirst().orElse(ItemStack.EMPTY);
                    if (resultStack.isEmpty()) continue;

                    Optional<ItemStackKey> outputKey = keyFactory.optionalKey(resultStack);
                    if (outputKey.isEmpty()) continue;

                    List<SlotDisplay> ingredientSlots = ingredientSlots(recipe, display);
                    if (ingredientSlots.isEmpty()) continue;

                    // 合成余留物（空桶等）必须从配料成本中扣除，否则蛋糕类配方的产物
                    // EMC 会按整桶牛奶计价，形成"合成→烧掉→保留空桶"的无限刷 EMC 循环。
                    List<List<NormalizedStackKey>> ingredientChoices = new ArrayList<>();
                    Map<NormalizedStackKey, NormalizedStackKey> remainders = new LinkedHashMap<>();
                    for (SlotDisplay slot : ingredientSlots) {
                        if (slot instanceof SlotDisplay.Empty) continue;
                        List<NormalizedStackKey> choices = new ArrayList<>();
                        for (ItemStack stack : slot.resolveForStacks(displayContext)) {
                            if (stack.isEmpty()) continue;
                            Optional<? extends NormalizedStackKey> choiceKey = keyFactory.optionalKey(stack);
                            if (choiceKey.isEmpty()) continue;
                            choices.add(choiceKey.get());
                            remainderKey(stack).ifPresent(
                                  remainder -> remainders.put(choiceKey.get(), remainder));
                        }
                        if (choices.isEmpty()) {
                            ingredientChoices.clear();
                            break;
                        }
                        ingredientChoices.add(choices);
                    }

                    if (ingredientChoices.isEmpty()) continue;

                    Identifier conversionId = displays.size() == 1 ? recipeId
                          : Identifier.fromNamespaceAndPath(
                                recipeId.getNamespace(), recipeId.getPath() + "/display_" + displayIndex);
                    all.addAll(remainders.isEmpty()
                          ? collector.collectCondensed(
                                conversionId,
                                resultStack.getCount(),
                                outputKey.get(),
                                ingredientChoices,
                                Map.of(),
                                MAX_COMBINATIONS)
                          : collector.collectWithRemainders(
                                conversionId,
                                resultStack.getCount(),
                                outputKey.get(),
                                ingredientChoices,
                                remainders,
                                MAX_COMBINATIONS));
                }
            } catch (RuntimeException exception) {
                throw new IllegalStateException(
                      "Failed collecting EMC conversion for recipe " + recipeId, exception);
            }
        }
        return List.copyOf(all);
    }

    /**
     * 该配料合成后归还的余留物（如牛奶桶→空桶）的键。经 Fabric 注入的
     * {@code getCraftingRemainder(ItemStack)} 读取，兼容按堆叠决定余留物的物品。
     */
    private Optional<NormalizedStackKey> remainderKey(ItemStack stack) {
        ItemStackTemplate template = stack.getItem().getCraftingRemainder(stack);
        if (template == null) {
            return Optional.empty();
        }
        ItemStack remainder = template.create();
        if (remainder.isEmpty()) {
            return Optional.empty();
        }
        return keyFactory.optionalKey(remainder).map(key -> key);
    }

    private static List<SlotDisplay> ingredientSlots(Recipe<?> recipe, RecipeDisplay display) {
        if (display instanceof ShapedCraftingRecipeDisplay shaped) {
            return shaped.ingredients();
        }
        if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
            return shapeless.ingredients();
        }
        if (display instanceof FurnaceRecipeDisplay furnace) {
            return List.of(furnace.ingredient());
        }
        if (display instanceof StonecutterRecipeDisplay stonecutter) {
            return List.of(stonecutter.input());
        }
        if (recipe instanceof SmithingTransformRecipe && display instanceof SmithingRecipeDisplay smithing) {
            return List.of(smithing.template(), smithing.base(), smithing.addition());
        }
        return List.of();
    }
}
