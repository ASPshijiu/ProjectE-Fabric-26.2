package moze_intel.projecte.content;

import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.content.menu.AlchemicalBagMenu;
import moze_intel.projecte.content.menu.CollectorMenu;
import moze_intel.projecte.content.menu.CondenserMenu;
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
    public static final Identifier CONDENSER_MK1_ID = ProjectEAPI.id("condenser_mk1");
    public static final Identifier CONDENSER_MK2_ID = ProjectEAPI.id("condenser_mk2");
    public static final Identifier COLLECTOR_MK1_ID = ProjectEAPI.id("collector_mk1");
    public static final Identifier COLLECTOR_MK2_ID = ProjectEAPI.id("collector_mk2");
    public static final Identifier COLLECTOR_MK3_ID = ProjectEAPI.id("collector_mk3");

    public static MenuType<TransmutationTableMenu> TRANSMUTATION_TABLE;
    public static MenuType<AlchemicalBagMenu> ALCHEMICAL_BAG;
    public static MenuType<CondenserMenu> CONDENSER_MK1;
    public static MenuType<CondenserMenu> CONDENSER_MK2;
    public static MenuType<CollectorMenu> COLLECTOR_MK1;
    public static MenuType<CollectorMenu> COLLECTOR_MK2;
    public static MenuType<CollectorMenu> COLLECTOR_MK3;

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
        if (CONDENSER_MK1 == null) {
            CONDENSER_MK1 = register(CONDENSER_MK1_ID,
                  new MenuType<>(CondenserMenu::clientMk1,
                        net.minecraft.world.flag.FeatureFlags.VANILLA_SET));
        }
        if (CONDENSER_MK2 == null) {
            CONDENSER_MK2 = register(CONDENSER_MK2_ID,
                  new MenuType<>(CondenserMenu::clientMk2,
                        net.minecraft.world.flag.FeatureFlags.VANILLA_SET));
        }
        if (COLLECTOR_MK1 == null) {
            COLLECTOR_MK1 = register(COLLECTOR_MK1_ID,
                  new MenuType<>(CollectorMenu::clientMk1,
                        net.minecraft.world.flag.FeatureFlags.VANILLA_SET));
        }
        if (COLLECTOR_MK2 == null) {
            COLLECTOR_MK2 = register(COLLECTOR_MK2_ID,
                  new MenuType<>(CollectorMenu::clientMk2,
                        net.minecraft.world.flag.FeatureFlags.VANILLA_SET));
        }
        if (COLLECTOR_MK3 == null) {
            COLLECTOR_MK3 = register(COLLECTOR_MK3_ID,
                  new MenuType<>(CollectorMenu::clientMk3,
                        net.minecraft.world.flag.FeatureFlags.VANILLA_SET));
        }
    }

    private static <T extends net.minecraft.world.inventory.AbstractContainerMenu> MenuType<T> register(
          Identifier id, MenuType<T> type
    ) {
        return Registry.register(BuiltInRegistries.MENU, id, type);
    }
}
