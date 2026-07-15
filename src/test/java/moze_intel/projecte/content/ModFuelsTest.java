package moze_intel.projecte.content;

import static org.junit.jupiter.api.Assertions.assertEquals;

import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.fabricmc.fabric.api.registry.FuelValueEvents;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FuelValues;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ModFuelsTest {
    private static final int BASE_SMELT_TIME = 200;

    @BeforeAll
    static void bootstrap() {
        MinecraftTestHarness.bootstrap();
    }

    @Test
    void registersUpstreamFurnaceBurnTimes() {
        ModItems.ALCHEMICAL_COAL = Items.PAPER;
        ModItems.MOBIUS_FUEL = Items.BOOK;
        ModItems.AETERNALIS_FUEL = Items.DIAMOND;
        ModBlocks.ALCHEMICAL_COAL_BLOCK_ITEM = (BlockItem) Blocks.COAL_BLOCK.asItem();
        ModBlocks.MOBIUS_FUEL_BLOCK_ITEM = (BlockItem) Blocks.GOLD_BLOCK.asItem();
        ModBlocks.AETERNALIS_FUEL_BLOCK_ITEM = (BlockItem) Blocks.DIAMOND_BLOCK.asItem();

        ModFuels.init();

        HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(
              BuiltInRegistries.REGISTRY);
        FuelValues.Builder builder = new FuelValues.Builder(registries, FeatureFlags.VANILLA_SET);
        FuelValueEvents.BUILD.invoker().build(builder, new FuelValueEvents.Context() {
            @Override
            public int baseSmeltTime() {
                return BASE_SMELT_TIME;
            }

            @Override
            public HolderLookup.Provider registries() {
                return registries;
            }

            @Override
            public FeatureFlagSet enabledFeatures() {
                return FeatureFlags.VANILLA_SET;
            }
        });
        FuelValues values = builder.build();

        assertEquals(6_400, values.burnDuration(new ItemStack(ModItems.ALCHEMICAL_COAL)));
        assertEquals(25_600, values.burnDuration(new ItemStack(ModItems.MOBIUS_FUEL)));
        assertEquals(102_400, values.burnDuration(new ItemStack(ModItems.AETERNALIS_FUEL)));
        assertEquals(57_600,
              values.burnDuration(new ItemStack(ModBlocks.ALCHEMICAL_COAL_BLOCK_ITEM)));
        assertEquals(230_400,
              values.burnDuration(new ItemStack(ModBlocks.MOBIUS_FUEL_BLOCK_ITEM)));
        assertEquals(921_600,
              values.burnDuration(new ItemStack(ModBlocks.AETERNALIS_FUEL_BLOCK_ITEM)));
    }

}
