package moze_intel.projecte.emc.reload;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import moze_intel.projecte.api.mapper.EmcMappingCollector;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.emc.data.CustomConversionLoader;
import moze_intel.projecte.emc.data.ExplicitEmcEntry;
import moze_intel.projecte.emc.data.ExplicitEmcLoader;
import moze_intel.projecte.emc.graph.EmcGraphMapper;
import moze_intel.projecte.emc.recipe.RecipeConversion;
import net.minecraft.resources.Identifier;

public final class EmcReloadProcessor {
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
        EmcGraphMapper<NormalizedStackKey> mapper = EmcGraphMapper.create();
        EmcMappingCollector<NormalizedStackKey> collector = mapper.collector();

        for (ExplicitEmcEntry entry : explicitLoader.load(explicitResources)) {
            if (entry.phase() == ExplicitEmcEntry.Phase.BEFORE) {
                collector.setValueBefore(entry.key(), entry.value());
            } else {
                collector.setValueAfter(entry.key(), entry.value());
            }
        }

        CustomConversionLoader.Result custom = customLoader.load(customConversionResources);
        for (ExplicitEmcEntry entry : custom.explicit()) {
            if (entry.phase() == ExplicitEmcEntry.Phase.BEFORE) {
                collector.setValueBefore(entry.key(), entry.value());
            } else {
                collector.setValueAfter(entry.key(), entry.value());
            }
        }
        for (NormalizedStackKey freeKey : custom.freeKeys()) {
            collector.setFree(freeKey);
        }

        List<RecipeConversion> sortedRecipes = new ArrayList<>(recipeConversions);
        sortedRecipes.addAll(custom.conversions());
        sortedRecipes.sort(Comparator
              .comparing((RecipeConversion conversion) -> conversion.recipeId().toString())
              .thenComparing(conversion -> conversion.output().canonicalString())
              .thenComparing(conversion -> conversion.ingredients().toString()));
        for (RecipeConversion conversion : sortedRecipes) {
            collector.addConversion(
                  conversion.outputCount(),
                  conversion.output(),
                  Map.copyOf(conversion.ingredients())
            );
        }
        return mapper.generator().generate();
    }
}
