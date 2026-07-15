package moze_intel.projecte.content.recipe;

import com.mojang.serialization.MapCodec;
import java.util.List;
import moze_intel.projecte.content.items.KleinStarItem;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;

/**
 * Shapeless Klein Star upgrade recipe that carries stored EMC into the resulting star.
 */
public final class KleinStarRecipe implements CraftingRecipe {
    private static final MapCodec<KleinStarRecipe> MAP_CODEC =
          ShapelessRecipe.MAP_CODEC.xmap(KleinStarRecipe::new, KleinStarRecipe::internal);
    private static final StreamCodec<RegistryFriendlyByteBuf, KleinStarRecipe> STREAM_CODEC =
          ShapelessRecipe.STREAM_CODEC.map(KleinStarRecipe::new, KleinStarRecipe::internal);
    public static final RecipeSerializer<KleinStarRecipe> SERIALIZER =
          new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final ShapelessRecipe internal;

    public KleinStarRecipe(ShapelessRecipe internal) {
        this.internal = internal;
    }

    private ShapelessRecipe internal() {
        return internal;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return internal.matches(input, level);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack result = internal.assemble(input);
        if (!(result.getItem() instanceof KleinStarItem outputStar)) {
            return result;
        }

        long maxEmc = outputStar.getMaxEmc();
        long storedEmc = 0;
        for (ItemStack stack : input.items()) {
            if (stack.getItem() instanceof KleinStarItem) {
                long inputEmc = Math.max(0, KleinStarItem.getStoredEmc(stack));
                if (inputEmc >= maxEmc - storedEmc) {
                    storedEmc = maxEmc;
                    break;
                }
                storedEmc += inputEmc;
            }
        }
        KleinStarItem.setStoredEmc(result, storedEmc);
        return result;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return internal.getRemainingItems(input);
    }

    @Override
    public boolean isSpecial() {
        return internal.isSpecial();
    }

    @Override
    public boolean showNotification() {
        return internal.showNotification();
    }

    @Override
    public String group() {
        return internal.group();
    }

    @Override
    public RecipeSerializer<KleinStarRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public PlacementInfo placementInfo() {
        return internal.placementInfo();
    }

    @Override
    public List<RecipeDisplay> display() {
        return internal.display();
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return internal.recipeBookCategory();
    }

    @Override
    public CraftingBookCategory category() {
        return internal.category();
    }
}
