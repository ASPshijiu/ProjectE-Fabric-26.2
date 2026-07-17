package moze_intel.projecte.emc.reload;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import moze_intel.projecte.emc.ItemStackKey;
import moze_intel.projecte.emc.recipe.RecipeConversion;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class VanillaRecipeConversionSourceTest {
    private static HolderLookup.Provider registries;

    @BeforeAll
    static void bootstrap() {
        MinecraftTestHarness.bootstrap();
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    @Test
    void mapsCookingStonecuttingAndSmithingTransformRecipes() {
        RecipeManager manager = new FixedRecipeManager(registries, List.of(
              holder("smelt_iron", new SmeltingRecipe(
                    new Recipe.CommonInfo(true),
                    new AbstractCookingRecipe.CookingBookInfo(CookingBookCategory.BLOCKS, ""),
                    Ingredient.of(Items.RAW_IRON), new ItemStackTemplate(Items.IRON_INGOT), 0.7F, 200)),
              holder("cut_stone", new StonecutterRecipe(
                    new Recipe.CommonInfo(true), Ingredient.of(Items.STONE),
                    new ItemStackTemplate(Items.STONE_STAIRS))),
              holder("upgrade_pickaxe", new SmithingTransformRecipe(
                    new Recipe.CommonInfo(true),
                    Optional.of(Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)),
                    Ingredient.of(Items.DIAMOND_PICKAXE),
                    Optional.of(Ingredient.of(Items.NETHERITE_INGOT)),
                    new ItemStackTemplate(Items.NETHERITE_PICKAXE)))
        ));

        Map<ItemStackKey, RecipeConversion> byOutput = new VanillaRecipeConversionSource(manager, registries)
              .conversions().stream()
              .collect(Collectors.toMap(conversion -> (ItemStackKey) conversion.output(), Function.identity()));

        assertEquals(Map.of(key("raw_iron"), 1), byOutput.get(key("iron_ingot")).ingredients());
        assertEquals(Map.of(key("stone"), 1), byOutput.get(key("stone_stairs")).ingredients());
        assertEquals(Map.of(
              key("netherite_upgrade_smithing_template"), 1,
              key("diamond_pickaxe"), 1,
              key("netherite_ingot"), 1
        ), byOutput.get(key("netherite_pickaxe")).ingredients());
    }

    private static RecipeHolder<?> holder(String path, Recipe<?> recipe) {
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, id(path));
        return new RecipeHolder<>(key, recipe);
    }

    private static ItemStackKey key(String path) {
        return new ItemStackKey(id(path), Map.of());
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("minecraft", path);
    }

    private static final class FixedRecipeManager extends RecipeManager {
        private final Collection<RecipeHolder<?>> recipes;

        private FixedRecipeManager(HolderLookup.Provider registries, Collection<RecipeHolder<?>> recipes) {
            super(registries);
            this.recipes = List.copyOf(recipes);
        }

        @Override
        public Collection<RecipeHolder<?>> getRecipes() {
            return recipes;
        }
    }
}
