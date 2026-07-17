package moze_intel.projecte.content.menu.slots;

import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import moze_intel.projecte.content.items.TomeOfKnowledgeItem;
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
    private final BooleanSupplier serverSide;
    private final PlayerDataService service;
    private final Function<ItemStack, Optional<StackEmcResolver.Resolved>> resolver;

    public TransmuteConsumeSlot(Container container, int index, int x, int y,
          Player player, PlayerDataService service, MinecraftStackKeyFactory keyFactory) {
        super(container, index, x, y);
        this.serverSide = () -> !player.level().isClientSide();
        this.service = service;
        this.resolver = stack -> keyFactory.optionalKey(stack)
              .flatMap(key -> StackEmcResolver.resolve(
                    stack, key, ProjectEEmc.service().current()));
    }

    TransmuteConsumeSlot(Container container, int index, int x, int y,
          BooleanSupplier serverSide, PlayerDataService service,
          Function<ItemStack, Optional<StackEmcResolver.Resolved>> resolver) {
        super(container, index, x, y);
        this.serverSide = serverSide;
        this.service = service;
        this.resolver = resolver;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return !stack.isEmpty()
              && (TomeOfKnowledgeItem.isTome(stack) || sellable(stack).isPresent());
    }

    @Override
    public void set(ItemStack stack) {
        if (serverSide.getAsBoolean() && !stack.isEmpty()) {
            var resolved = sellable(stack);
            if (TomeOfKnowledgeItem.isTome(stack) || resolved.isPresent()) {
                if (TomeOfKnowledgeItem.isTome(stack)) {
                    TomeOfKnowledgeItem.learnAll(service);
                } else {
                    service.learn(resolved.orElseThrow().key());
                }
                resolved.ifPresent(entry -> service.addEmc(entry.value().multiply(stack.getCount())));
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
        return resolver.apply(stack);
    }

    private Optional<StackEmcResolver.Resolved> sellable(ItemStack stack) {
        return resolve(stack).filter(entry -> entry.value().longValue() > 0);
    }
}
