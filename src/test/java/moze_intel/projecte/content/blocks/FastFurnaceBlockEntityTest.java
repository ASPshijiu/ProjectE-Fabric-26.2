package moze_intel.projecte.content.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.FuelValues;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FastFurnaceBlockEntityTest {
    @BeforeAll
    @SuppressWarnings({"rawtypes", "unchecked"})
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
        BlockEntityType furnaceType = BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(
              net.minecraft.resources.Identifier.withDefaultNamespace("furnace"));
        FastFurnaceBlockEntity.DM_TYPE = furnaceType;
        FastFurnaceBlockEntity.RM_TYPE = furnaceType;
    }

    @Test
    void scalesRecipeCookingTimeToMatterFurnaceSpeed() {
        assertEquals(10, FastFurnaceBlockEntity.scaledCookingTime(200, 10));
        assertEquals(3, FastFurnaceBlockEntity.scaledCookingTime(200, 3));
        assertEquals(5, FastFurnaceBlockEntity.scaledCookingTime(100, 10));
        assertEquals(2, FastFurnaceBlockEntity.scaledCookingTime(100, 3));
    }

    @Test
    void rescalesExistingCookingProgressWhenSpeedChanges() {
        SimpleContainerData data = new SimpleContainerData(
              AbstractFurnaceBlockEntity.NUM_DATA_VALUES);
        data.set(AbstractFurnaceBlockEntity.DATA_COOKING_PROGRESS, 100);
        data.set(AbstractFurnaceBlockEntity.DATA_COOKING_TOTAL_TIME, 200);

        FastFurnaceBlockEntity.applyCookingTime(data, 10);

        assertEquals(5, data.get(AbstractFurnaceBlockEntity.DATA_COOKING_PROGRESS));
        assertEquals(10, data.get(AbstractFurnaceBlockEntity.DATA_COOKING_TOTAL_TIME));
    }

    @Test
    void scalesFuelDurationByMatterFurnaceEfficiency() throws Exception {
        HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(
              BuiltInRegistries.REGISTRY);
        FuelValues.Builder builder = new FuelValues.Builder(registries, FeatureFlags.VANILLA_SET);
        builder.add(Items.COAL, 1_600);
        FuelValues fuelValues = builder.build();

        FastFurnaceBlockEntity.DM dm = new FastFurnaceBlockEntity.DM(
              net.minecraft.core.BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
        FastFurnaceBlockEntity.RM rm = new FastFurnaceBlockEntity.RM(
              net.minecraft.core.BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());

        assertEquals(240, burnDuration(dm, fuelValues));
        assertEquals(96, burnDuration(rm, fuelValues));
    }

    private static int burnDuration(
          AbstractFurnaceBlockEntity furnace, FuelValues fuelValues
    ) throws Exception {
        Method method = AbstractFurnaceBlockEntity.class.getDeclaredMethod(
              "getBurnDuration", FuelValues.class, ItemStack.class);
        method.setAccessible(true);
        return (int) method.invoke(furnace, fuelValues, new ItemStack(Items.COAL));
    }
}
