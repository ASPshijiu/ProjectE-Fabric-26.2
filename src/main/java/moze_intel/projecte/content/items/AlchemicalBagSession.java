package moze_intel.projecte.content.items;

import java.util.Objects;
import moze_intel.projecte.player.PlayerDataService;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Connects one colored bag item to its owning player's shared bag attachment. */
public final class AlchemicalBagSession {
    private final PlayerDataService service;
    private final DyeColor color;

    private AlchemicalBagSession(PlayerDataService service, DyeColor color) {
        this.service = service;
        this.color = color;
    }

    public static AlchemicalBagSession connect(
          PlayerDataService service, DyeColor color
    ) {
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(color, "color");
        return new AlchemicalBagSession(service, color);
    }

    public static AlchemicalBagSession open(
          PlayerDataService service, DyeColor color, ItemStack bagStack
    ) {
        Objects.requireNonNull(bagStack, "bagStack");
        AlchemicalBagSession session = connect(service, color);
        ItemContainerContents legacy = bagStack.getOrDefault(
              DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        if (legacy.nonEmptyItemCopyStream().findAny().isPresent()) {
            ItemContainerContents remaining = session.service.migrateAlchemicalBagContents(
                  color, legacy);
            if (remaining.nonEmptyItemCopyStream().findAny().isEmpty()) {
                bagStack.remove(DataComponents.CONTAINER);
            } else {
                bagStack.set(DataComponents.CONTAINER, remaining);
            }
        }
        return session;
    }

    public ItemContainerContents contents() {
        return service.alchemicalBagContents(color);
    }

    public void save(ItemContainerContents contents) {
        service.setAlchemicalBagContents(color, contents);
    }

    public void repairContents() {
        ItemContainerContents current = contents();
        ItemContainerContents repaired = AlchemicalBagItem.repairContents(current);
        if (repaired != current) {
            save(repaired);
        }
    }
}
