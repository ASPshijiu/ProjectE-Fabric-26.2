package moze_intel.projecte.content.menu.slots;

import java.util.Optional;
import moze_intel.projecte.emc.ProjectEEmc;
import moze_intel.projecte.emc.StackEmcResolver;
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
        return !stack.isEmpty() && resolve(stack)
              .filter(resolved -> resolved.value().longValue() > 0)
              .isPresent();
    }

    @Override
    public void set(ItemStack stack) {
        if (!player.level().isClientSide() && !stack.isEmpty()) {
            var resolved = resolve(stack).filter(entry -> entry.value().longValue() > 0);
            if (resolved.isPresent()) {
                service.learn(resolved.get().key());
                service.addEmc(resolved.get().value().multiply(stack.getCount()));
                super.set(ItemStack.EMPTY);
                return;
            }
        }
        super.set(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    private Optional<StackEmcResolver.Resolved> resolve(ItemStack stack) {
        return keyFactory.optionalKey(stack)
              .flatMap(key -> StackEmcResolver.resolve(
                    stack, key,
                    ProjectEEmc.service().current()));
    }
}
