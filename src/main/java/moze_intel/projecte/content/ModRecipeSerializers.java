package moze_intel.projecte.content;

import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.content.recipe.CovalenceRepairRecipe;
import moze_intel.projecte.content.recipe.KleinStarRecipe;
import moze_intel.projecte.content.recipe.PhiloStoneSmeltingRecipe;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** ProjectE recipe serializer registrations. */
public final class ModRecipeSerializers {
    public static final Identifier COVALENCE_REPAIR_ID =
          ProjectEAPI.id("covalence_repair");
    public static final Identifier KLEIN_STAR_ID =
          ProjectEAPI.id("crafting_shapeless_kleinstar");
    public static final Identifier PHILO_STONE_SMELTING_ID =
          ProjectEAPI.id("philo_stone_smelting");
    public static final RecipeSerializer<CovalenceRepairRecipe> COVALENCE_REPAIR =
          CovalenceRepairRecipe.SERIALIZER;
    public static final RecipeSerializer<KleinStarRecipe> KLEIN_STAR =
          KleinStarRecipe.SERIALIZER;
    public static final RecipeSerializer<PhiloStoneSmeltingRecipe> PHILO_STONE_SMELTING =
          PhiloStoneSmeltingRecipe.SERIALIZER;

    private static boolean initialized;

    private ModRecipeSerializers() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
              COVALENCE_REPAIR_ID, COVALENCE_REPAIR);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, KLEIN_STAR_ID, KLEIN_STAR);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
              PHILO_STONE_SMELTING_ID, PHILO_STONE_SMELTING);
    }
}
