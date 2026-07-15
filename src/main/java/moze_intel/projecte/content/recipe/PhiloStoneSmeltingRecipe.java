package moze_intel.projecte.content.recipe;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import moze_intel.projecte.ProjectE;
import moze_intel.projecte.content.ModItems;
import moze_intel.projecte.content.ModRecipeSerializers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;

/** Smelts seven matching inputs by crafting them with coal and the Philosopher's Stone. */
public final class PhiloStoneSmeltingRecipe extends CustomRecipe {
    private static final int SMELTING_INPUT_COUNT = 7;
    private static final PhiloStoneSmeltingRecipe INSTANCE = new PhiloStoneSmeltingRecipe();
    private static final MapCodec<PhiloStoneSmeltingRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    private static final StreamCodec<RegistryFriendlyByteBuf, PhiloStoneSmeltingRecipe>
          STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<PhiloStoneSmeltingRecipe> SERIALIZER =
          new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final Predicate<ItemStack> isPhilosophersStone;
    private final Predicate<ItemStack> isCoal;
    private final SmeltingRecipeProvider smeltingRecipes;

    public PhiloStoneSmeltingRecipe() {
        this(stack -> stack.is(ModItems.PHILOSOPHERS_STONE),
              stack -> stack.is(ItemTags.COALS),
              PhiloStoneSmeltingRecipe::serverSmeltingRecipes);
    }

    PhiloStoneSmeltingRecipe(
          Predicate<ItemStack> isPhilosophersStone,
          Predicate<ItemStack> isCoal,
          SmeltingRecipeProvider smeltingRecipes
    ) {
        this.isPhilosophersStone = isPhilosophersStone;
        this.isCoal = isCoal;
        this.smeltingRecipes = smeltingRecipes;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return findMatch(input, level) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        MatchingRecipe match = findMatch(input, null);
        if (match == null) {
            return ItemStack.EMPTY;
        }
        ItemStack output = match.recipe().assemble(new SingleRecipeInput(match.input()));
        if (output.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return output.copyWithCount(output.getCount() * SMELTING_INPUT_COUNT);
    }

    @Override
    public RecipeSerializer<PhiloStoneSmeltingRecipe> getSerializer() {
        return ModRecipeSerializers.PHILO_STONE_SMELTING;
    }

    private MatchingRecipe findMatch(CraftingInput input, Level requestedLevel) {
        if (input.ingredientCount() != SMELTING_INPUT_COUNT + 2) {
            return null;
        }
        List<ItemStack> items = input.items().stream()
              .filter(stack -> !stack.isEmpty())
              .toList();
        Level recipeLevel = recipeLevel(requestedLevel);
        List<SmeltingRecipe> availableRecipes = smeltingRecipes.get(recipeLevel);
        if (availableRecipes.isEmpty()) {
            return null;
        }

        for (int stoneIndex = 0; stoneIndex < items.size(); stoneIndex++) {
            if (!isPhilosophersStone.test(items.get(stoneIndex))) {
                continue;
            }
            for (int coalIndex = 0; coalIndex < items.size(); coalIndex++) {
                if (coalIndex == stoneIndex || !isCoal.test(items.get(coalIndex))) {
                    continue;
                }
                MatchingRecipe match = findSharedRecipe(
                      items, stoneIndex, coalIndex, availableRecipes, recipeLevel);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private static MatchingRecipe findSharedRecipe(
          List<ItemStack> items,
          int stoneIndex,
          int coalIndex,
          List<SmeltingRecipe> availableRecipes,
          Level level
    ) {
        List<SmeltingRecipe> candidates = new ArrayList<>(availableRecipes);
        ItemStack firstInput = ItemStack.EMPTY;
        for (int index = 0; index < items.size(); index++) {
            if (index == stoneIndex || index == coalIndex) {
                continue;
            }
            ItemStack stack = items.get(index);
            if (firstInput.isEmpty()) {
                firstInput = stack;
            }
            SingleRecipeInput furnaceInput = new SingleRecipeInput(stack);
            candidates.removeIf(recipe -> !recipe.matches(furnaceInput, level));
            if (candidates.isEmpty()) {
                return null;
            }
        }
        return firstInput.isEmpty() ? null : new MatchingRecipe(candidates.getFirst(), firstInput);
    }

    private static Level recipeLevel(Level requestedLevel) {
        if (requestedLevel != null && requestedLevel.getServer() != null) {
            return requestedLevel;
        }
        return ProjectE.currentServer()
              .map(server -> (Level) server.overworld())
              .orElse(requestedLevel);
    }

    private static List<SmeltingRecipe> serverSmeltingRecipes(Level level) {
        if (level == null || level.getServer() == null) {
            return List.of();
        }
        return level.getServer().getRecipeManager().getRecipes().stream()
              .map(RecipeHolder::value)
              .filter(SmeltingRecipe.class::isInstance)
              .map(SmeltingRecipe.class::cast)
              .toList();
    }

    @FunctionalInterface
    interface SmeltingRecipeProvider {
        List<SmeltingRecipe> get(Level level);
    }

    private record MatchingRecipe(SmeltingRecipe recipe, ItemStack input) {
    }
}
