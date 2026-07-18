package moze_intel.projecte.emc;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.content.items.KleinStarItem;
import moze_intel.projecte.emc.recipe.MinecraftStackKeyFactory;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Consumes ProjectE fuel from a player's inventory using the upstream priority rules. */
public final class PlayerFuelConsumer {
    private static final TagKey<Item> COLLECTOR_FUEL = TagKey.create(
          Registries.ITEM, ProjectEAPI.id("collector_fuel"));

    private PlayerFuelConsumer() {
    }

    /**
     * @return the EMC represented by the consumed item(s), or {@code -1} when no valid source can
     *         satisfy the request
     */
    public static long consume(Player player, long minimumEmc) {
        Objects.requireNonNull(player, "player");
        if (minimumEmc < 0) {
            throw new IllegalArgumentException("minimumEmc must not be negative");
        }
        if (player.isCreative() || minimumEmc == 0) {
            return minimumEmc;
        }

        MinecraftStackKeyFactory keys = new MinecraftStackKeyFactory(
              player.level().registryAccess());
        EmcMappingSnapshot<NormalizedStackKey> snapshot = ProjectEEmc.service().current();
        long consumed = consume(
              player.getInventory(), minimumEmc,
              stack -> stack.is(COLLECTOR_FUEL),
              stack -> keys.optionalKey(stack)
                    .flatMap(key -> StackEmcResolver.resolve(stack, key, snapshot))
                    .map(StackEmcResolver.Resolved::value)
                    .orElse(EmcValue.ZERO)
                    .longValue());
        if (consumed >= 0) {
            player.containerMenu.broadcastChanges();
        }
        return consumed;
    }

    static long consume(
          Container inventory,
          long minimumEmc,
          Predicate<ItemStack> isFuel,
          ToLongFunction<ItemStack> emcValue
    ) {
        Objects.requireNonNull(inventory, "inventory");
        Objects.requireNonNull(isFuel, "isFuel");
        Objects.requireNonNull(emcValue, "emcValue");
        if (minimumEmc < 0) {
            throw new IllegalArgumentException("minimumEmc must not be negative");
        }
        if (minimumEmc == 0) {
            return 0;
        }

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof KleinStarItem
                  && KleinStarItem.getStoredEmc(stack) >= minimumEmc) {
                long removed = KleinStarItem.removeEmc(stack, minimumEmc);
                inventory.setChanged();
                return removed;
            }
        }

        int[] removalPlan = new int[inventory.getContainerSize()];
        long plannedEmc = 0;
        for (int slot = 0; slot < inventory.getContainerSize() && plannedEmc < minimumEmc; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !isFuel.test(stack)) {
                continue;
            }
            long value = emcValue.applyAsLong(stack);
            if (value <= 0) {
                continue;
            }
            long neededItems = Math.ceilDiv(minimumEmc - plannedEmc, value);
            int toRemove = (int) Math.min(stack.getCount(), neededItems);
            removalPlan[slot] = toRemove;
            plannedEmc = saturatingAdd(plannedEmc, saturatingMultiply(value, toRemove));
        }
        if (plannedEmc < minimumEmc) {
            return -1;
        }

        for (int slot = 0; slot < removalPlan.length; slot++) {
            int toRemove = removalPlan[slot];
            if (toRemove > 0) {
                inventory.getItem(slot).shrink(toRemove);
            }
        }
        inventory.setChanged();
        return plannedEmc;
    }

    private static long saturatingMultiply(long value, int count) {
        try {
            return Math.multiplyExact(value, count);
        } catch (ArithmeticException ignored) {
            return Long.MAX_VALUE;
        }
    }

    private static long saturatingAdd(long first, long second) {
        try {
            return Math.addExact(first, second);
        } catch (ArithmeticException ignored) {
            return Long.MAX_VALUE;
        }
    }
}
