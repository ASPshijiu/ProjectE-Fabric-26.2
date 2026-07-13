package moze_intel.projecte.content;

import com.mojang.serialization.Codec;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.emc.EmcValue;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

/**
 * Custom {@link DataComponentType} registrations for ProjectE.
 *
 * <p>MC 26.2 uses data components instead of NBT for persistent item data.
 */
public final class ModDataComponents {
    private ModDataComponents() {}

    /** Stores the EMC value on Klein Stars and other EMC-bearing items. */
    public static final Identifier STORED_EMC_ID = ProjectEAPI.id("stored_emc");
    public static DataComponentType<Long> STORED_EMC;

    /** Stores the charge level (0..max) on chargeable tools (DM/RM hammers, rings, etc.). */
    public static final Identifier CHARGE_ID = ProjectEAPI.id("charge");
    public static DataComponentType<Integer> CHARGE;

    private static volatile boolean initialized;

    public static void init() {
        if (initialized) return;
        initialized = true;

        STORED_EMC = Registry.register(
              BuiltInRegistries.DATA_COMPONENT_TYPE,
              STORED_EMC_ID,
              DataComponentType.<Long>builder()
                    .persistent(Codec.LONG)
                    .networkSynchronized(ByteBufCodecs.VAR_LONG)
                    .build()
        );

        CHARGE = Registry.register(
              BuiltInRegistries.DATA_COMPONENT_TYPE,
              CHARGE_ID,
              DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build()
        );
    }
}
