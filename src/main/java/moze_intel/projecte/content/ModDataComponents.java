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

    /** Stores the Philosopher's Stone area mode (cube, panel, or line). */
    public static final Identifier PHILOSOPHERS_STONE_MODE_ID = ProjectEAPI.id("philosophers_stone_mode");
    public static DataComponentType<Integer> PHILOSOPHERS_STONE_MODE;

    /** Stores the active mode for matter pickaxes, swords, the katar and the morning star. */
    public static final Identifier TOOL_MODE_ID = ProjectEAPI.id("tool_mode");
    public static DataComponentType<Integer> TOOL_MODE;

    public static final Identifier NIGHT_VISION_ID = ProjectEAPI.id("night_vision");
    public static DataComponentType<Boolean> NIGHT_VISION;

    public static final Identifier STEP_ASSIST_ID = ProjectEAPI.id("step_assist");
    public static DataComponentType<Boolean> STEP_ASSIST;

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

        PHILOSOPHERS_STONE_MODE = Registry.register(
              BuiltInRegistries.DATA_COMPONENT_TYPE,
              PHILOSOPHERS_STONE_MODE_ID,
              DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build()
        );

        TOOL_MODE = Registry.register(
              BuiltInRegistries.DATA_COMPONENT_TYPE,
              TOOL_MODE_ID,
              DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build()
        );

        NIGHT_VISION = Registry.register(
              BuiltInRegistries.DATA_COMPONENT_TYPE,
              NIGHT_VISION_ID,
              DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build()
        );

        STEP_ASSIST = Registry.register(
              BuiltInRegistries.DATA_COMPONENT_TYPE,
              STEP_ASSIST_ID,
              DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build()
        );
    }
}
