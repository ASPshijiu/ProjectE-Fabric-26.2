package moze_intel.projecte.content.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import moze_intel.projecte.content.ModDataComponents;
import moze_intel.projecte.content.items.KleinStarItem;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class KleinStarRecipeTest {
    private static final String RECIPE_TYPE =
          "projecte:crafting_shapeless_kleinstar";

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
    }

    @Test
    void craftingUpgradePreservesTotalStoredEmc() throws Exception {
        KleinStarItem inputStar = allocateKleinStar("ein");
        KleinStarItem outputStar = allocateKleinStar("zwei");
        CraftingRecipe recipe = createRecipe(outputStar);

        ItemStack result = recipe.assemble(CraftingInput.of(2, 2, List.of(
              starWithEmc(inputStar, 10_000),
              starWithEmc(inputStar, 20_000),
              starWithEmc(inputStar, 30_000),
              starWithEmc(inputStar, 40_000)
        )));

        assertEquals(100_000, KleinStarItem.getStoredEmc(result));
    }

    @Test
    void craftingUpgradeCapsStoredEmcWithoutOverflow() throws Exception {
        KleinStarItem inputStar = allocateKleinStar("ein");
        KleinStarItem outputStar = allocateKleinStar("zwei");
        CraftingRecipe recipe = createRecipe(outputStar);
        ItemStack overfilled = starWithEmc(inputStar, 0);
        overfilled.set(ModDataComponents.STORED_EMC, Long.MAX_VALUE);

        ItemStack result = recipe.assemble(CraftingInput.of(2, 2, List.of(
              overfilled,
              starWithEmc(inputStar, 1),
              starWithEmc(inputStar, 1),
              starWithEmc(inputStar, 1)
        )));

        assertEquals(KleinStarItem.MAX_ZWEI, KleinStarItem.getStoredEmc(result));
    }

    @Test
    void allUpgradeRecipesUseEmcPreservingSerializer() throws Exception {
        Path recipes = Path.of("src/main/resources/data/projecte/recipe");
        for (String tier : List.of("zwei", "drei", "vier", "sphere", "omega")) {
            JsonObject recipe = JsonParser.parseString(Files.readString(
                  recipes.resolve("klein_star_" + tier + ".json"))).getAsJsonObject();
            assertEquals(RECIPE_TYPE, recipe.get("type").getAsString(), tier);
        }
    }

    private static CraftingRecipe createRecipe(KleinStarItem outputStar) {
        ItemStack output = new ItemStack(Holder.direct(outputStar, DataComponentMap.EMPTY));
        ShapelessRecipe internal = new ShapelessRecipe(
              new Recipe.CommonInfo(true),
              new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.EQUIPMENT, ""),
              ItemStackTemplate.fromNonEmptyStack(output),
              List.of()
        );
        return new KleinStarRecipe(internal);
    }

    private static ItemStack starWithEmc(KleinStarItem star, long emc) {
        ItemStack stack = new ItemStack(Holder.direct(star, DataComponentMap.EMPTY));
        KleinStarItem.setStoredEmc(stack, emc);
        return stack;
    }

    private static KleinStarItem allocateKleinStar(String tier) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object unsafe = unsafeField.get(null);
        Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        KleinStarItem star = (KleinStarItem) allocateInstance.invoke(
              unsafe, KleinStarItem.class);
        Field tierField = KleinStarItem.class.getDeclaredField("tier");
        tierField.setAccessible(true);
        tierField.set(star, tier);
        return star;
    }
}
