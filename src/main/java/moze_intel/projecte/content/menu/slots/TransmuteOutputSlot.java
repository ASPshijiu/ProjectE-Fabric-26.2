package moze_intel.projecte.content.menu.slots;

import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import moze_intel.projecte.emc.EmcMappingSnapshot;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.emc.ProjectEEmc;
import moze_intel.projecte.emc.recipe.MinecraftStackKeyFactory;
import moze_intel.projecte.player.PlayerDataService;
import moze_intel.projecte.transmutation.table.TransmutationTransaction;
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
    private final BooleanSupplier serverSide;
    private final PlayerDataService service;
    private final Function<ItemStack, NormalizedStackKey> keyResolver;
    private final Supplier<EmcMappingSnapshot<NormalizedStackKey>> snapshot;

    public TransmuteOutputSlot(Container container, int index, int x, int y,
          Player player, PlayerDataService service, MinecraftStackKeyFactory keyFactory) {
        this(container, index, x, y, () -> !player.level().isClientSide(), service,
              stack -> keyFactory.optionalKey(stack).orElseThrow(
                    () -> new IllegalArgumentException("unregistered item: " + stack)),
              () -> ProjectEEmc.service().current());
    }

    TransmuteOutputSlot(Container container, int index, int x, int y,
          BooleanSupplier serverSide, PlayerDataService service,
          Function<ItemStack, NormalizedStackKey> keyResolver,
          Supplier<EmcMappingSnapshot<NormalizedStackKey>> snapshot) {
        super(container, index, x, y);
        this.serverSide = serverSide;
        this.service = service;
        this.keyResolver = keyResolver;
        this.snapshot = snapshot;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack remove(int amount) {
        ItemStack stored = getItem();
        if (stored.isEmpty() || !serverSide.getAsBoolean()) {
            return super.remove(amount);
        }
        NormalizedStackKey key = keyOf(stored);
        TransmutationTransaction.Outcome outcome = TransmutationTransaction.extract(
              service, snapshot.get(), key, amount, stored.getMaxStackSize());
        if (!outcome.success()) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = stored.copy();
        taken.setCount(outcome.producedCount());
        // Output slots are virtual: they always show the resolver candidate, so leave the slot as-is.
        return taken;
    }

    private NormalizedStackKey keyOf(ItemStack stack) {
        return keyResolver.apply(stack);
    }
}
