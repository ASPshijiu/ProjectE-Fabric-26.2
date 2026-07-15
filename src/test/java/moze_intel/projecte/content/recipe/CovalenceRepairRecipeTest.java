package moze_intel.projecte.content.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.enchantment.Repairable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CovalenceRepairRecipeTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
    }

    @Test
    void repairsDamageAccordingToDustAndToolEmc() {
        CraftingRecipe recipe = createRecipe(stack -> stack.is(Items.REDSTONE), stack -> {
            if (stack.is(Items.REDSTONE)) return 12;
            if (stack.is(Items.IRON_PICKAXE)) return 1_000;
            return 0;
        });
        ItemStack tool = damagedIronTool(Items.IRON_PICKAXE, 10);
        CraftingInput input = CraftingInput.of(2, 2, List.of(
              tool,
              new ItemStack(Items.REDSTONE),
              new ItemStack(Items.REDSTONE),
              ItemStack.EMPTY
        ));

        assertTrue(recipe.matches(input, null));
        ItemStack result = recipe.assemble(input);

        assertEquals(Items.IRON_PICKAXE, result.getItem());
        assertEquals(4, result.getDamageValue());
        assertEquals(10, tool.getDamageValue());
    }

    @Test
    void doesNotMatchWhenDustCannotRepairOneDurability() {
        CraftingRecipe recipe = createRecipe(stack -> stack.is(Items.REDSTONE), stack ->
              stack.is(Items.REDSTONE) ? 3 : 1_000);
        ItemStack tool = damagedIronTool(Items.IRON_PICKAXE, 10);

        assertFalse(recipe.matches(CraftingInput.of(2, 1, List.of(
              tool, new ItemStack(Items.REDSTONE)
        )), null));
    }

    @Test
    void rejectsMoreThanOneRepairTarget() {
        CraftingRecipe recipe = createRecipe(stack -> stack.is(Items.REDSTONE), ignored -> 1_000);
        ItemStack pickaxe = damagedIronTool(Items.IRON_PICKAXE, 10);
        ItemStack sword = damagedIronTool(Items.IRON_SWORD, 10);

        assertFalse(recipe.matches(CraftingInput.of(2, 2, List.of(
              pickaxe, sword, new ItemStack(Items.REDSTONE), ItemStack.EMPTY
        )), null));
    }

    @Test
    void dataPackContainsCovalenceRepairRecipe() throws Exception {
        Path file = Path.of("src/main/resources/data/projecte/recipe/covalence_repair.json");
        assertTrue(Files.isRegularFile(file), "missing upstream covalence_repair recipe");
        JsonObject recipe = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        assertEquals("projecte:covalence_repair", recipe.get("type").getAsString());
    }

    private static CraftingRecipe createRecipe(
          Predicate<ItemStack> isDust, ToLongFunction<ItemStack> emcValue
    ) {
        return new CovalenceRepairRecipe(isDust, emcValue);
    }

    private static ItemStack damagedIronTool(Item item, int damage) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.MAX_DAMAGE, 250);
        stack.set(DataComponents.DAMAGE, damage);
        stack.set(DataComponents.REPAIRABLE, new Repairable(HolderSet.direct(
              BuiltInRegistries.ITEM.wrapAsHolder(Items.IRON_INGOT))));
        return stack;
    }
}
