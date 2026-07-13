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
 * Transmutation-table consume (burn-for-EMC) slot. Placing an item here learns it and immediately
 * converts it into EMC at its sell value (EMC × count), which is added to the player's balance.
 * The slot is then cleared. The item never returns.
 */
public class TransmuteConsumeSlot extends Slot {
    private final Player player;
    private final PlayerDataService service;
    private final MinecraftStackKeyFactory keyFactory;

    public TransmuteConsumeSlot(Container container, int index, int x, int y,
          Player player, PlayerDataService service, MinecraftStackKeyFactory keyFactory) {
        super(container, index, x, y);
        this.player = player;
        this.service = service;
        this.keyFactory = keyFactory;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return !stack.isEmpty() && ProjectEEmc.service().current()
              .valueFor(keyOf(stack)).filter(v -> v.longValue() > 0).isPresent();
    }

    @Override
    public void set(ItemStack stack) {
        if (!player.level().isClientSide() && !stack.isEmpty()) {
            NormalizedStackKey key = keyOf(stack);
            service.learn(key);
            EmcValue perItem = ProjectEEmc.service().current().valueFor(key).orElse(EmcValue.ZERO);
            if (perItem.longValue() > 0) {
                long total = Math.multiplyExact(perItem.longValue(), stack.getCount());
                service.addEmc(EmcValue.of(total));
            }
            // Item is consumed; clear the slot so it never returns.
            super.set(ItemStack.EMPTY);
            return;
        }
        super.set(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    private NormalizedStackKey keyOf(ItemStack stack) {
        return keyFactory.optionalKey(stack)
              .orElseThrow(() -> new IllegalArgumentException("unregistered item: " + stack));
    }
}
