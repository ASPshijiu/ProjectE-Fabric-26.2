package moze_intel.projecte.emc.reload;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import moze_intel.projecte.api.mapper.EmcMappingCollector;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.ItemStackKey;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.emc.TagStackKey;
import moze_intel.projecte.emc.data.CustomConversionLoader;
import moze_intel.projecte.emc.data.ExplicitEmcEntry;
import moze_intel.projecte.emc.data.ExplicitEmcLoader;
import moze_intel.projecte.emc.graph.EmcGraphMapper;
import moze_intel.projecte.emc.recipe.RecipeConversion;
import net.minecraft.resources.Identifier;

public final class EmcReloadProcessor {
    private static final int MAX_TAG_EXPANSIONS = 100_000;
    private static final Function<TagStackKey, Collection<ItemStackKey>> NO_TAGS = tag -> List.of();
    private final ExplicitEmcLoader explicitLoader = new ExplicitEmcLoader();
    private final CustomConversionLoader customLoader = new CustomConversionLoader();

    public Map<NormalizedStackKey, EmcValue> rebuild(
          Map<Identifier, String> explicitResources,
          List<RecipeConversion> recipeConversions
    ) {
        return rebuild(explicitResources, Map.of(), recipeConversions);
    }

    /**
     * Rebuilds the EMC mapping from explicit values, custom-conversion data
     * ({@code pe_custom_conversions}) and recipe conversions. Custom-conversion explicit values are
     * applied with the explicit ones (before/after phases interleaved by source order); their free
     * keys are declared free; their recipe conversions join the recipe pool.
     */
    public Map<NormalizedStackKey, EmcValue> rebuild(
          Map<Identifier, String> explicitResources,
          Map<Identifier, String> customConversionResources,
          List<RecipeConversion> recipeConversions
    ) {
        return rebuild(explicitResources, customConversionResources, recipeConversions, NO_TAGS);
    }

    public Map<NormalizedStackKey, EmcValue> rebuild(
          Map<Identifier, String> explicitResources,
          Map<Identifier, String> customConversionResources,
          List<RecipeConversion> recipeConversions,
          Function<TagStackKey, ? extends Collection<ItemStackKey>> tagResolver
    ) {
        EmcGraphMapper<NormalizedStackKey> mapper = EmcGraphMapper.create();
        EmcMappingCollector<NormalizedStackKey> collector = mapper.collector();

        for (ExplicitEmcEntry entry : explicitLoader.load(explicitResources)) {
            setExplicitValue(collector, entry, tagResolver);
        }

        CustomConversionLoader.Result custom = customLoader.load(customConversionResources);
        for (ExplicitEmcEntry entry : custom.explicit()) {
            setExplicitValue(collector, entry, tagResolver);
        }
        for (NormalizedStackKey freeKey : custom.freeKeys()) {
            for (NormalizedStackKey expanded : expandKey(freeKey, tagResolver)) {
                collector.setFree(expanded);
            }
        }

        List<RecipeConversion> sortedRecipes = new ArrayList<>(recipeConversions);
        sortedRecipes.addAll(custom.conversions());
        sortedRecipes.sort(Comparator
              .comparing((RecipeConversion conversion) -> conversion.recipeId().toString())
              .thenComparing(conversion -> conversion.output().canonicalString())
              .thenComparing(conversion -> conversion.ingredients().toString()));
        for (RecipeConversion conversion : sortedRecipes) {
            addExpandedConversion(collector, conversion, tagResolver);
        }
        return mapper.generator().generate();
    }

    public Map<NormalizedStackKey, EmcValue> extend(
          Map<NormalizedStackKey, EmcValue> seedValues,
          List<RecipeConversion> recipeConversions
    ) {
        EmcGraphMapper<NormalizedStackKey> mapper = EmcGraphMapper.create();
        EmcMappingCollector<NormalizedStackKey> collector = mapper.collector();
        seedValues.entrySet().stream()
              .sorted(Map.Entry.comparingByKey())
              .forEach(entry -> collector.setValueBefore(entry.getKey(), entry.getValue()));
        recipeConversions.stream()
              .sorted(Comparator
                    .comparing((RecipeConversion conversion) -> conversion.recipeId().toString())
                    .thenComparing(conversion -> conversion.output().canonicalString())
                    .thenComparing(conversion -> conversion.ingredients().toString()))
              .forEach(conversion -> addExpandedConversion(collector, conversion, NO_TAGS));
        return mapper.generator().generate();
    }

    private void setExplicitValue(
          EmcMappingCollector<NormalizedStackKey> collector,
          ExplicitEmcEntry entry,
          Function<TagStackKey, ? extends Collection<ItemStackKey>> tagResolver
    ) {
        for (NormalizedStackKey key : expandKey(entry.key(), tagResolver)) {
            if (entry.phase() == ExplicitEmcEntry.Phase.BEFORE) {
                collector.setValueBefore(key, entry.value());
            } else {
                collector.setValueAfter(key, entry.value());
            }
        }
    }

    private void addExpandedConversion(
          EmcMappingCollector<NormalizedStackKey> collector,
          RecipeConversion conversion,
          Function<TagStackKey, ? extends Collection<ItemStackKey>> tagResolver
    ) {
        List<NormalizedStackKey> outputs = expandKey(conversion.output(), tagResolver);
        if (outputs.size() > MAX_TAG_EXPANSIONS) {
            return;
        }
        int maximumIngredientVariants = MAX_TAG_EXPANSIONS / outputs.size();
        List<Map<NormalizedStackKey, Integer>> ingredientVariants = List.of(new LinkedHashMap<>());
        List<Map.Entry<NormalizedStackKey, Integer>> ingredients = conversion.ingredients().entrySet().stream()
              .sorted(Map.Entry.comparingByKey())
              .toList();
        for (Map.Entry<NormalizedStackKey, Integer> ingredient : ingredients) {
            List<NormalizedStackKey> choices = expandKey(ingredient.getKey(), tagResolver);
            List<Map<NormalizedStackKey, Integer>> next = new ArrayList<>();
            for (Map<NormalizedStackKey, Integer> variant : ingredientVariants) {
                for (NormalizedStackKey choice : choices) {
                    if (next.size() >= maximumIngredientVariants) {
                        return;
                    }
                    Map<NormalizedStackKey, Integer> expanded = new LinkedHashMap<>(variant);
                    expanded.merge(choice, ingredient.getValue(), Math::addExact);
                    next.add(expanded);
                }
            }
            ingredientVariants = next;
        }
        for (NormalizedStackKey output : outputs) {
            for (Map<NormalizedStackKey, Integer> ingredientsForOutput : ingredientVariants) {
                collector.addConversion(conversion.outputCount(), output, Map.copyOf(ingredientsForOutput));
            }
        }
    }

    private List<NormalizedStackKey> expandKey(
          NormalizedStackKey key,
          Function<TagStackKey, ? extends Collection<ItemStackKey>> tagResolver
    ) {
        if (!(key instanceof TagStackKey tag)) {
            return List.of(key);
        }
        Collection<ItemStackKey> resolved = tagResolver.apply(tag);
        if (resolved == null || resolved.isEmpty()) {
            return List.of(tag);
        }
        return resolved.stream()
              .distinct()
              .sorted()
              .map(item -> (NormalizedStackKey) item)
              .toList();
    }
}
