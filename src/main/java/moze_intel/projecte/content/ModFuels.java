package moze_intel.projecte.content;

import net.fabricmc.fabric.api.registry.FuelValueEvents;
import net.minecraft.world.level.block.entity.FuelValues;

/** Registers ProjectE items and storage blocks as furnace fuels. */
public final class ModFuels {
    private static boolean initialized;

    private ModFuels() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        FuelValueEvents.BUILD.register(ModFuels::addFuelValues);
    }

    private static void addFuelValues(
          FuelValues.Builder builder, FuelValueEvents.Context context
    ) {
        int baseSmeltTime = context.baseSmeltTime();
        builder.add(ModItems.ALCHEMICAL_COAL, baseSmeltTime * 32);
        builder.add(ModItems.MOBIUS_FUEL, baseSmeltTime * 128);
        builder.add(ModItems.AETERNALIS_FUEL, baseSmeltTime * 512);
        builder.add(ModBlocks.ALCHEMICAL_COAL_BLOCK_ITEM, baseSmeltTime * 32 * 9);
        builder.add(ModBlocks.MOBIUS_FUEL_BLOCK_ITEM, baseSmeltTime * 128 * 9);
        builder.add(ModBlocks.AETERNALIS_FUEL_BLOCK_ITEM, baseSmeltTime * 512 * 9);
    }
}
