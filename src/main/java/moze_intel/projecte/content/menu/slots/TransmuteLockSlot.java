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
 * Transmutation-table lock slot. Acts as an EMC filter cap: the placed item's EMC limits which
 * outputs the resolver will offer (only items at or below this EMC are shown). Placing an item also
 * learns it. Like the input slot it holds a single reference item.
 */
public class TransmuteLockSlot extends Slot {
    private final Player player;
    private final PlayerDataService service;
    private final MinecraftStackKeyFactory keyFactory;

    public TransmuteLockSlot(Container container, int index, int x, int y,
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
            service.learn(keyOf(stack));
        }
        super.set(stack);
    }

    private NormalizedStackKey keyOf(ItemStack stack) {
        return keyFactory.optionalKey(stack)
              .orElseThrow(() -> new IllegalArgumentException("unregistered item: " + stack));
    }
}
