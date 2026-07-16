package moze_intel.projecte.content.blocks;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import moze_intel.projecte.content.items.KleinStarItem;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.ProjectEEmc;
import moze_intel.projecte.emc.recipe.MinecraftStackKeyFactory;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

/** Fabric Transfer API views for anti-matter relay automation. */
public final class RelayItemStorage {
    private static boolean initialized;

    private RelayItemStorage() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        Objects.requireNonNull(RelayBlockEntity.MK1_TYPE, "relay MK1 type");
        Objects.requireNonNull(RelayBlockEntity.MK2_TYPE, "relay MK2 type");
        Objects.requireNonNull(RelayBlockEntity.MK3_TYPE, "relay MK3 type");

        ItemStorage.SIDED.registerForBlockEntity(
              (relay, direction) -> create(relay, direction), RelayBlockEntity.MK1_TYPE);
        ItemStorage.SIDED.registerForBlockEntity(
              (relay, direction) -> create(relay, direction), RelayBlockEntity.MK2_TYPE);
        ItemStorage.SIDED.registerForBlockEntity(
              (relay, direction) -> create(relay, direction), RelayBlockEntity.MK3_TYPE);
        initialized = true;
    }

    static Storage<ItemVariant> create(
          RelayBlockEntity.Base relay, Direction direction
    ) {
        Predicate<ItemVariant> chargeable = RelayItemStorage::isChargeable;
        return create(
              relay,
              direction,
              variant -> isChargeable(variant) || hasPositiveEmc(relay, variant),
              chargeable,
              RelayItemStorage::isFullyCharged);
    }

    static Storage<ItemVariant> create(
          RelayBlockEntity.Base relay,
          Direction direction,
          Predicate<ItemVariant> validInput,
          Predicate<ItemVariant> chargeable,
          Predicate<ItemVariant> fullyCharged
    ) {
        Objects.requireNonNull(relay, "relay");
        Objects.requireNonNull(validInput, "validInput");
        Objects.requireNonNull(chargeable, "chargeable");
        Objects.requireNonNull(fullyCharged, "fullyCharged");

        ContainerStorage inventory = ContainerStorage.of(relay, null);
        List<SingleSlotStorage<ItemVariant>> slots = inventory.getSlots();
        Storage<ItemVariant> input = new CombinedStorage<>(
              List.copyOf(slots.subList(0, relay.inputSlots)));
        Storage<ItemVariant> automationInput = new FilteringStorage<>(input) {
            @Override
            protected boolean canInsert(ItemVariant variant) {
                return !variant.isBlank() && validInput.test(variant);
            }

            @Override
            protected boolean canExtract(ItemVariant variant) {
                return false;
            }
        };
        Storage<ItemVariant> automationOutput = new FilteringStorage<>(
              slots.get(relay.inputSlots)) {
            @Override
            protected boolean canInsert(ItemVariant variant) {
                return !variant.isBlank() && chargeable.test(variant);
            }

            @Override
            protected boolean canExtract(ItemVariant variant) {
                return !variant.isBlank() && fullyCharged.test(variant);
            }
        };

        if (direction == null) {
            return new CombinedStorage<ItemVariant, Storage<ItemVariant>>(
                  List.of(automationInput, automationOutput));
        }
        return direction.getAxis().isVertical() ? automationOutput : automationInput;
    }

    private static boolean hasPositiveEmc(
          RelayBlockEntity.Base relay, ItemVariant variant
    ) {
        if (variant.isBlank() || relay.getLevel() == null) {
            return false;
        }
        if (relay.stackKeys == null) {
            relay.stackKeys = new MinecraftStackKeyFactory(relay.getLevel().registryAccess());
        }
        ItemStack stack = variant.toStack();
        return relay.stackKeys.optionalKey(stack)
              .flatMap(ProjectEEmc.service().current()::valueFor)
              .orElse(EmcValue.ZERO)
              .longValue() > 0;
    }

    private static boolean isChargeable(ItemVariant variant) {
        return !variant.isBlank() && variant.getItem() instanceof KleinStarItem;
    }

    private static boolean isFullyCharged(ItemVariant variant) {
        if (!isChargeable(variant)) return false;
        ItemStack stack = variant.toStack();
        KleinStarItem star = (KleinStarItem) stack.getItem();
        return KleinStarItem.getStoredEmc(stack) >= star.getMaxEmc();
    }
}
