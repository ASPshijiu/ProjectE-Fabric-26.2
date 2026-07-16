package moze_intel.projecte.content.blocks;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
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

/** Fabric Transfer API views for collector automation. */
public final class CollectorItemStorage {
    private static boolean initialized;

    private CollectorItemStorage() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        Objects.requireNonNull(CollectorBlockEntity.MK1_TYPE, "collector MK1 type");
        Objects.requireNonNull(CollectorBlockEntity.MK2_TYPE, "collector MK2 type");
        Objects.requireNonNull(CollectorBlockEntity.MK3_TYPE, "collector MK3 type");

        ItemStorage.SIDED.registerForBlockEntity(
              (collector, direction) -> create(collector, direction),
              CollectorBlockEntity.MK1_TYPE);
        ItemStorage.SIDED.registerForBlockEntity(
              (collector, direction) -> create(collector, direction),
              CollectorBlockEntity.MK2_TYPE);
        ItemStorage.SIDED.registerForBlockEntity(
              (collector, direction) -> create(collector, direction),
              CollectorBlockEntity.MK3_TYPE);
        initialized = true;
    }

    static Storage<ItemVariant> create(
          CollectorBlockEntity.Base collector, Direction direction
    ) {
        return create(
              collector, direction, variant -> isAutomatableInput(collector, variant));
    }

    static Storage<ItemVariant> create(
          CollectorBlockEntity.Base collector,
          Direction direction,
          Predicate<ItemVariant> validInput
    ) {
        Objects.requireNonNull(collector, "collector");
        Objects.requireNonNull(validInput, "validInput");

        ContainerStorage inventory = ContainerStorage.of(collector, null);
        List<SingleSlotStorage<ItemVariant>> slots = inventory.getSlots();
        Storage<ItemVariant> input = new CombinedStorage<>(
              List.copyOf(slots.subList(0, collector.inputSlots)));
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

        Storage<ItemVariant> automationOutput = FilteringStorage.extractOnlyOf(
              slots.get(collector.inputSlots + 1));
        if (direction == null) {
            return new CombinedStorage<ItemVariant, Storage<ItemVariant>>(
                  List.of(automationInput, automationOutput));
        }
        return direction.getAxis().isVertical() ? automationOutput : automationInput;
    }

    private static boolean isAutomatableInput(
          CollectorBlockEntity.Base collector, ItemVariant variant
    ) {
        if (variant.isBlank()) {
            return false;
        }
        if (collector.getLevel() == null) {
            return false;
        }
        if (collector.stackKeys == null) {
            collector.stackKeys = new MinecraftStackKeyFactory(
                  collector.getLevel().registryAccess());
        }
        var snapshot = ProjectEEmc.service().current();
        ToLongFunction<ItemStack> emcValue = input -> collector.stackKeys.optionalKey(input)
              .flatMap(snapshot::valueFor)
              .orElse(EmcValue.ZERO)
              .longValue();
        return CollectorBlockEntity.Base.isCollectorInput(
              variant.toStack(), collector.getLevel().registryAccess(), emcValue);
    }
}
