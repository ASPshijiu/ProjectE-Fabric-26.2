package moze_intel.projecte.content.menu.slots;

import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.emc.ProjectEEmc;
import moze_intel.projecte.emc.recipe.MinecraftStackKeyFactory;
import moze_intel.projecte.player.PlayerDataService;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Transmutation-table output slot. Read-only; populated by the menu from
 * {@link moze_intel.projecte.transmutation.table.TransmutationOutputResolver}. Taking a stack
 * spends EMC at the item's value × count through the validated
 * {@link PlayerDataService#tryRemoveEmc} transaction; if the player cannot afford the requested
 * amount the extraction returns nothing.
 */
public class TransmuteOutputSlot extends Slot {
    private final Player player;
    private final PlayerDataService service;
    private final MinecraftStackKeyFactory keyFactory;

    public TransmuteOutputSlot(Container container, int index, int x, int y,
          Player player, PlayerDataService service, MinecraftStackKeyFactory keyFactory) {
        super(container, index, x, y);
        this.player = player;
        this.service = service;
        this.keyFactory = keyFactory;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack remove(int amount) {
        ItemStack stored = getItem();
        if (stored.isEmpty() || player.level().isClientSide()) {
            return super.remove(amount);
        }
        NormalizedStackKey key = keyOf(stored);
        EmcValue perItem = ProjectEEmc.service().current().valueFor(key).orElse(EmcValue.ZERO);
        if (perItem.longValue() <= 0) {
            return ItemStack.EMPTY;
        }
        int affordable = affordableCount(perItem.longValue(), amount);
        if (affordable <= 0) {
            return ItemStack.EMPTY;
        }
        long cost = Math.multiplyExact(perItem.longValue(), affordable);
        if (!service.tryRemoveEmc(EmcValue.of(cost))) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = stored.copy();
        taken.setCount(affordable);
        // Output slots are virtual: they always show the resolver candidate, so leave the slot as-is.
        return taken;
    }

    private int affordableCount(long perItem, int requested) {
        long balance = service.emc().longValue();
        long max = balance / perItem;
        return (int) Math.min(Math.min(requested, max), 64);
    }

    private NormalizedStackKey keyOf(ItemStack stack) {
        return keyFactory.optionalKey(stack)
              .orElseThrow(() -> new IllegalArgumentException("unregistered item: " + stack));
    }
}
