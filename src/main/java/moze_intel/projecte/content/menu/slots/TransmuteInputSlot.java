package moze_intel.projecte.content.menu.slots;

import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.emc.ProjectEEmc;
import moze_intel.projecte.emc.recipe.MinecraftStackKeyFactory;
import moze_intel.projecte.player.PlayerDataService;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Transmutation-table input slot. Placing an item here learns it server-side. The slot holds a
 * single reference item so the player can keep it as a visual filter; taking the item back simply
 * empties the slot without unlearning.
 */
public class TransmuteInputSlot extends Slot {
    private final Player player;
    private final PlayerDataService service;
    private final MinecraftStackKeyFactory keyFactory;

    public TransmuteInputSlot(Container container, int index, int x, int y,
          Player player, PlayerDataService service, MinecraftStackKeyFactory keyFactory) {
        super(container, index, x, y);
        this.player = player;
        this.service = service;
        this.keyFactory = keyFactory;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return !stack.isEmpty()
              && ProjectEEmc.service().current().valueFor(keyOf(stack)).isPresent();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void set(ItemStack stack) {
        if (!player.level().isClientSide() && !stack.isEmpty()) {
            NormalizedStackKey key = keyOf(stack);
            service.learn(key);
        }
        super.set(stack);
    }

    private NormalizedStackKey keyOf(ItemStack stack) {
        return keyFactory.optionalKey(stack)
              .orElseThrow(() -> new IllegalArgumentException("unregistered item: " + stack));
    }
}
