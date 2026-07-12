package moze_intel.projecte.emc.recipe;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import moze_intel.projecte.emc.NormalizedStackKey;
import net.minecraft.resources.Identifier;

public final class RecipeConversionCollector {
    private final IngredientChoiceExpander expander = new IngredientChoiceExpander();

    public List<RecipeConversion> collect(
          Identifier recipeId,
          int outputCount,
          NormalizedStackKey output,
          List<? extends List<NormalizedStackKey>> ingredientChoices,
          Map<NormalizedStackKey, Integer> returnedIngredients,
          int maximumCombinations
    ) {
        Objects.requireNonNull(recipeId, "recipeId");
        if (outputCount <= 0) {
            throw new IllegalArgumentException("outputCount must be positive");
        }
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(ingredientChoices, "ingredientChoices");
        Objects.requireNonNull(returnedIngredients, "returnedIngredients");

        List<List<NormalizedStackKey>> expanded = expander.expand(
              ingredientChoices,
              maximumCombinations,
              recipeId.toString()
        );
        Set<RecipeConversion> unique = new LinkedHashSet<>();
        for (List<NormalizedStackKey> selection : expanded) {
            TreeMap<NormalizedStackKey, Integer> amounts = new TreeMap<>();
            selection.forEach(key -> amounts.merge(key, 1, Math::addExact));
            returnedIngredients.forEach((key, amount) -> {
                Objects.requireNonNull(key, "returned ingredient key");
                Objects.requireNonNull(amount, "returned ingredient amount");
                if (amount < 0) {
                    throw new IllegalArgumentException("returned ingredient amount must not be negative");
                }
                amounts.merge(key, -amount, Math::addExact);
            });
            amounts.entrySet().removeIf(entry -> entry.getValue() == 0);
            unique.add(new RecipeConversion(recipeId, outputCount, output, amounts));
        }
        return List.copyOf(new ArrayList<>(unique));
    }
}
