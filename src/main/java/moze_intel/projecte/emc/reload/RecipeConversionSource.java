package moze_intel.projecte.emc.reload;

import java.util.List;
import moze_intel.projecte.emc.recipe.RecipeConversion;

/**
 * Source of {@link RecipeConversion} entries gathered during a server data reload.
 *
 * <p>Implementations adapt a specific Minecraft recipe system (for example the 26.2
 * {@code RecipeManager} and its {@code RecipeDisplay} graph) to ProjectE's loader-neutral
 * {@link RecipeConversion} model. The reload pipeline queries every registered source and feeds
 * the union of their conversions into {@link EmcReloadProcessor}.
 *
 * <p>Sources must be deterministic: given the same resources and registries they must emit the
 * same conversion list in stable order. They must not throw on unsupported recipes; they should
 * simply omit them.
 */
public interface RecipeConversionSource {
    /**
     * @return a stable, deterministic list of recipe conversions for the current reload.
     */
    List<RecipeConversion> conversions();
}
