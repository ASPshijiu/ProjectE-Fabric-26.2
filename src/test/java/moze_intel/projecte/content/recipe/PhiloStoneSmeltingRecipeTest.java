package moze_intel.projecte.content.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PhiloStoneSmeltingRecipeTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
    }

    @Test
    void smeltsSevenInputsUsingOneCoalAndThePhilosophersStone() {
        PhiloStoneSmeltingRecipe recipe = createRecipe(List.of(
              smelting(Items.IRON_ORE, Items.IRON_INGOT, 2)
        ));
        CraftingInput input = craftingInput(Items.IRON_ORE, 7);

        assertTrue(recipe.matches(input, null));
        ItemStack result = recipe.assemble(input);

        assertEquals(Items.IRON_INGOT, result.getItem());
        assertEquals(14, result.getCount());
    }

    @Test
    void rejectsInputsThatDoNotShareASmeltingRecipe() {
        PhiloStoneSmeltingRecipe recipe = createRecipe(List.of(
              smelting(Items.IRON_ORE, Items.IRON_INGOT, 1),
              smelting(Items.SAND, Items.GLASS, 1)
        ));
        List<ItemStack> stacks = validStacks(Items.IRON_ORE, 6);
        stacks.add(new ItemStack(Items.SAND));

        assertFalse(recipe.matches(CraftingInput.of(3, 3, stacks), null));
    }

    @Test
    void rejectsAnythingOtherThanNineOccupiedSlots() {
        PhiloStoneSmeltingRecipe recipe = createRecipe(List.of(
              smelting(Items.IRON_ORE, Items.IRON_INGOT, 1)
        ));
        List<ItemStack> tooFew = validStacks(Items.IRON_ORE, 6);
        tooFew.add(ItemStack.EMPTY);
        List<ItemStack> tooMany = validStacks(Items.IRON_ORE, 8);

        assertFalse(recipe.matches(CraftingInput.of(3, 3, tooFew), null));
        assertFalse(recipe.matches(CraftingInput.of(5, 2, tooMany), null));
    }

    @Test
    void dataPackContainsPhiloStoneSmeltingRecipe() throws Exception {
        Path file = Path.of("src/main/resources/data/projecte/recipe/philo_stone_smelting.json");
        assertTrue(Files.isRegularFile(file), "missing upstream philo_stone_smelting recipe");
        JsonObject recipe = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        assertEquals("projecte:philo_stone_smelting", recipe.get("type").getAsString());
    }

    private static PhiloStoneSmeltingRecipe createRecipe(List<SmeltingRecipe> smeltingRecipes) {
        return new PhiloStoneSmeltingRecipe(
              stack -> stack.is(Items.NETHER_STAR),
              stack -> stack.is(Items.COAL),
              level -> smeltingRecipes
        );
    }

    private static SmeltingRecipe smelting(Item input, Item output, int outputCount) {
        ItemStack result = new ItemStack(output);
        result.set(DataComponents.MAX_STACK_SIZE, 64);
        result.setCount(outputCount);
        return new SmeltingRecipe(
              new Recipe.CommonInfo(true),
              new AbstractCookingRecipe.CookingBookInfo(CookingBookCategory.MISC, ""),
              Ingredient.of(input),
              ItemStackTemplate.fromNonEmptyStack(result),
              0,
              200
        );
    }

    private static CraftingInput craftingInput(Item smeltingInput, int inputCount) {
        List<ItemStack> stacks = validStacks(smeltingInput, inputCount);
        return CraftingInput.of(3, 3, stacks);
    }

    private static List<ItemStack> validStacks(Item smeltingInput, int inputCount) {
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(new ItemStack(Items.NETHER_STAR));
        stacks.add(new ItemStack(Items.COAL));
        for (int i = 0; i < inputCount; i++) {
            stacks.add(new ItemStack(smeltingInput));
        }
        return stacks;
    }
}
