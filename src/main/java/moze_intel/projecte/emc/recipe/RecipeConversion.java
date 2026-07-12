package moze_intel.projecte.emc.recipe;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import moze_intel.projecte.emc.NormalizedStackKey;
import net.minecraft.resources.Identifier;

public record RecipeConversion(
      Identifier recipeId,
      int outputCount,
      NormalizedStackKey output,
      SortedMap<NormalizedStackKey, Integer> ingredients
) {
    public RecipeConversion(Identifier recipeId, int outputCount, NormalizedStackKey output, Map<NormalizedStackKey, Integer> ingredients) {
        this(recipeId, outputCount, output, immutableIngredients(ingredients));
    }

    public RecipeConversion {
        Objects.requireNonNull(recipeId, "recipeId");
        if (outputCount <= 0) {
            throw new IllegalArgumentException("outputCount must be positive");
        }
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(ingredients, "ingredients");
    }

    private static SortedMap<NormalizedStackKey, Integer> immutableIngredients(Map<NormalizedStackKey, Integer> ingredients) {
        Objects.requireNonNull(ingredients, "ingredients");
        TreeMap<NormalizedStackKey, Integer> copy = new TreeMap<>();
        ingredients.forEach((key, amount) -> copy.put(
              Objects.requireNonNull(key, "ingredient key"),
              Objects.requireNonNull(amount, "ingredient amount")
        ));
        return Collections.unmodifiableSortedMap(copy);
    }
}
