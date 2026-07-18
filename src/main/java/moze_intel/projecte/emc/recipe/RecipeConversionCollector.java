package moze_intel.projecte.emc.recipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import moze_intel.projecte.emc.FakeStackKey;
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
        return List.copyOf(unique);
    }

    public List<RecipeConversion> collectCondensed(
          Identifier recipeId,
          int outputCount,
          NormalizedStackKey output,
          List<? extends List<NormalizedStackKey>> ingredientChoices,
          Map<NormalizedStackKey, Integer> returnedIngredients,
          int maximumAlternatives
    ) {
        Objects.requireNonNull(recipeId, "recipeId");
        if (outputCount <= 0) {
            throw new IllegalArgumentException("outputCount must be positive");
        }
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(ingredientChoices, "ingredientChoices");
        Objects.requireNonNull(returnedIngredients, "returnedIngredients");
        if (maximumAlternatives <= 0) {
            throw new IllegalArgumentException("maximumAlternatives must be positive");
        }

        Map<List<NormalizedStackKey>, Integer> groups = new LinkedHashMap<>();
        for (List<NormalizedStackKey> slot : ingredientChoices) {
            if (slot.isEmpty()) {
                return List.of();
            }
            groups.merge(List.copyOf(new TreeSet<>(slot)), 1, Math::addExact);
        }

        List<RecipeConversion> conversions = new ArrayList<>();
        Map<NormalizedStackKey, Integer> outputIngredients = new TreeMap<>();
        int alternativeCount = 0;
        int groupIndex = 0;
        for (Map.Entry<List<NormalizedStackKey>, Integer> group : groups.entrySet()) {
            List<NormalizedStackKey> choices = group.getKey();
            if (choices.size() == 1) {
                outputIngredients.merge(choices.getFirst(), group.getValue(), Math::addExact);
            } else {
                alternativeCount = Math.addExact(alternativeCount, choices.size());
                if (alternativeCount > maximumAlternatives) {
                    throw new IllegalArgumentException(
                          recipeId + " expands to more than " + maximumAlternatives + " ingredient alternatives");
                }
                FakeStackKey groupKey = new FakeStackKey(Identifier.fromNamespaceAndPath(
                      "projecte", "recipe_group/" + recipeId.getNamespace() + "/"
                            + recipeId.getPath() + "/" + groupIndex));
                for (NormalizedStackKey choice : choices) {
                    conversions.add(new RecipeConversion(recipeId, 1, groupKey, Map.of(choice, 1)));
                }
                outputIngredients.merge(groupKey, group.getValue(), Math::addExact);
            }
            groupIndex++;
        }
        returnedIngredients.forEach((key, amount) -> {
            Objects.requireNonNull(key, "returned ingredient key");
            Objects.requireNonNull(amount, "returned ingredient amount");
            if (amount < 0) {
                throw new IllegalArgumentException("returned ingredient amount must not be negative");
            }
            outputIngredients.merge(key, -amount, Math::addExact);
        });
        outputIngredients.entrySet().removeIf(entry -> entry.getValue() == 0);
        conversions.add(new RecipeConversion(recipeId, outputCount, output, outputIngredients));
        return List.copyOf(conversions);
    }

    public List<RecipeConversion> collectWithRemainders(
          Identifier recipeId,
          int outputCount,
          NormalizedStackKey output,
          List<? extends List<NormalizedStackKey>> ingredientChoices,
          Map<NormalizedStackKey, NormalizedStackKey> remainders,
          int maximumCombinations
    ) {
        Objects.requireNonNull(recipeId, "recipeId");
        if (outputCount <= 0) {
            throw new IllegalArgumentException("outputCount must be positive");
        }
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(ingredientChoices, "ingredientChoices");
        Objects.requireNonNull(remainders, "remainders");

        List<List<NormalizedStackKey>> expanded = expander.expand(
              ingredientChoices,
              maximumCombinations,
              recipeId.toString()
        );
        Set<RecipeConversion> unique = new LinkedHashSet<>();
        for (List<NormalizedStackKey> selection : expanded) {
            TreeMap<NormalizedStackKey, Integer> amounts = new TreeMap<>();
            for (NormalizedStackKey selected : selection) {
                amounts.merge(selected, 1, Math::addExact);
                NormalizedStackKey remainder = remainders.get(selected);
                if (remainder != null) {
                    amounts.merge(remainder, -1, Math::addExact);
                }
            }
            amounts.entrySet().removeIf(entry -> entry.getValue() == 0);
            unique.add(new RecipeConversion(recipeId, outputCount, output, amounts));
        }
        return List.copyOf(unique);
    }

}
