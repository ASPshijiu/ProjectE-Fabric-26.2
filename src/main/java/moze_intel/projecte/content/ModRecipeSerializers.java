package moze_intel.projecte.content;

import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.content.recipe.KleinStarRecipe;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** ProjectE recipe serializer registrations. */
public final class ModRecipeSerializers {
    public static final Identifier KLEIN_STAR_ID =
          ProjectEAPI.id("crafting_shapeless_kleinstar");
    public static final RecipeSerializer<KleinStarRecipe> KLEIN_STAR =
          KleinStarRecipe.SERIALIZER;

    private static boolean initialized;

    private ModRecipeSerializers() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, KLEIN_STAR_ID, KLEIN_STAR);
    }
}
