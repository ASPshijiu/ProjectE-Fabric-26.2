package moze_intel.projecte.content.menu.slots;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.emc.recipe.MinecraftStackKeyFactory;
import moze_intel.projecte.player.PlayerDataService;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Transmutation-table unlearn slot. Placing an item here removes it from the player's knowledge and
 * retains the item until it is taken back or the menu returns it on close. Only one item at a time
 * may occupy the slot.
 */
public class TransmuteUnlearnSlot extends Slot {
    private final BooleanSupplier serverSide;
    private final Consumer<ItemStack> unlearn;

    public TransmuteUnlearnSlot(Container container, int index, int x, int y,
          Player player, PlayerDataService service, MinecraftStackKeyFactory keyFactory) {
        this(container, index, x, y,
              () -> !player.level().isClientSide(),
              stack -> service.unlearn(keyOf(keyFactory, stack)));
    }

    TransmuteUnlearnSlot(Container container, int index, int x, int y,
          BooleanSupplier serverSide, Consumer<ItemStack> unlearn) {
        super(container, index, x, y);
        this.serverSide = serverSide;
        this.unlearn = unlearn;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return !stack.isEmpty() && !hasItem();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void set(ItemStack stack) {
        if (serverSide.getAsBoolean() && !stack.isEmpty()) {
            unlearn.accept(stack.copy());
        }
        super.set(stack);
    }

    private static NormalizedStackKey keyOf(MinecraftStackKeyFactory keyFactory, ItemStack stack) {
        return keyFactory.optionalKey(stack)
              .orElseThrow(() -> new IllegalArgumentException("unregistered item: " + stack));
    }
}
