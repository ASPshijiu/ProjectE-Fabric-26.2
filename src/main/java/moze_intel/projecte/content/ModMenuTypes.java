package moze_intel.projecte.content;

import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.content.menu.AlchemicalBagMenu;
import moze_intel.projecte.content.menu.TransmutationTableMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;

/**
 * Registration hub for ProjectE {@link MenuType}s. Stable identifiers are public constants; the
 * menu types themselves are constructed and registered inside {@link #init()}.
 */
public final class ModMenuTypes {
    public static final Identifier TRANSMUTATION_TABLE_ID = ProjectEAPI.id("transmutation_table");
    public static final Identifier ALCHEMICAL_BAG_ID = ProjectEAPI.id("alchemical_bag");

    public static MenuType<TransmutationTableMenu> TRANSMUTATION_TABLE;
    public static MenuType<AlchemicalBagMenu> ALCHEMICAL_BAG;

    private ModMenuTypes() {
    }

    public static void init() {
        if (TRANSMUTATION_TABLE == null) {
            TRANSMUTATION_TABLE = register(TRANSMUTATION_TABLE_ID,
                  new MenuType<>((containerId, inventory) ->
                        new TransmutationTableMenu(containerId, inventory),
                        net.minecraft.world.flag.FeatureFlags.VANILLA_SET));
        }
        if (ALCHEMICAL_BAG == null) {
            ALCHEMICAL_BAG = register(ALCHEMICAL_BAG_ID,
                  new MenuType<>((containerId, inventory) ->
                        new AlchemicalBagMenu(containerId, inventory),
                        net.minecraft.world.flag.FeatureFlags.VANILLA_SET));
        }
    }

    private static <T extends net.minecraft.world.inventory.AbstractContainerMenu> MenuType<T> register(
          Identifier id, MenuType<T> type
    ) {
        return Registry.register(BuiltInRegistries.MENU, id, type);
    }
}
