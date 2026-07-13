package moze_intel.projecte.content.menu.slots;

import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.emc.recipe.MinecraftStackKeyFactory;
import moze_intel.projecte.player.PlayerDataService;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Transmutation-table unlearn slot. Placing an item here unlearns it (removes it from the player's
 * knowledge) and immediately bounces the item back so the player does not lose it. Only one item at
 * a time may occupy the slot.
 */
public class TransmuteUnlearnSlot extends Slot {
    private final Player player;
    private final PlayerDataService service;
    private final MinecraftStackKeyFactory keyFactory;

    public TransmuteUnlearnSlot(Container container, int index, int x, int y,
          Player player, PlayerDataService service, MinecraftStackKeyFactory keyFactory) {
        super(container, index, x, y);
        this.player = player;
        this.service = service;
        this.keyFactory = keyFactory;
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
        if (!player.level().isClientSide() && !stack.isEmpty()) {
            NormalizedStackKey key = keyOf(stack);
            service.unlearn(key);
        }
        // Bounce the item back: clear the slot so the player keeps the item.
        super.set(ItemStack.EMPTY);
    }

    private NormalizedStackKey keyOf(ItemStack stack) {
        return keyFactory.optionalKey(stack)
              .orElseThrow(() -> new IllegalArgumentException("unregistered item: " + stack));
    }
}
