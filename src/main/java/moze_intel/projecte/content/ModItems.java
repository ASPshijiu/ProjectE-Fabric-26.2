package moze_intel.projecte.content;

import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.content.items.PhilosophersStoneItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/**
 * Central item registration hub. Stable identifiers are public constants so callers (commands,
 * creative tabs, tests) can reference them without forcing item construction. The items themselves
 * are constructed and registered only inside {@link #init()}, which is called during mod setup.
 *
 * <p>Construction is deferred because allocating an {@link Item} allocates an intrusive registry
 * holder, which the 26.2 builtin item registry only permits before it freezes during server/client
 * startup.
 */
public final class ModItems {
    /** Stable identifier of the Philosopher's Stone. */
    public static final Identifier PHILOSOPHERS_STONE_ID = ProjectEAPI.id("philosophers_stone");

    /** The registered Philosopher's Stone item, set by {@link #init()}. */
    public static Item PHILOSOPHERS_STONE;

    private ModItems() {
    }

    /**
     * Constructs and registers all ProjectE items. Idempotent: re-invoking is a no-op.
     */
    public static void init() {
        if (PHILOSOPHERS_STONE == null) {
            PHILOSOPHERS_STONE = register(PHILOSOPHERS_STONE_ID,
                  new PhilosophersStoneItem(properties(PHILOSOPHERS_STONE_ID).stacksTo(1)));
        }
    }

    /**
     * Creates item properties with the id pre-set, as required by Minecraft 26.2 (an item's
     * description id is derived from its id during construction).
     */
    private static Item.Properties properties(Identifier id) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        return new Item.Properties().setId(key);
    }

    private static Item register(Identifier id, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, id, item);
    }
}
