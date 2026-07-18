package moze_intel.projecte.content.blocks;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.ProjectEEmc;
import moze_intel.projecte.emc.StackEmcResolver;
import moze_intel.projecte.emc.recipe.MinecraftStackKeyFactory;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.minecraft.world.item.ItemStack;

/** Fabric Transfer API views for condenser automation. */
public final class CondenserItemStorage {
    private static boolean initialized;

    private CondenserItemStorage() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        Objects.requireNonNull(CondenserBlockEntity.MK1_TYPE, "condenser MK1 type");
        Objects.requireNonNull(CondenserBlockEntity.MK2_TYPE, "condenser MK2 type");

        ItemStorage.SIDED.registerForBlockEntity(
              (condenser, direction) -> create(condenser), CondenserBlockEntity.MK1_TYPE);
        ItemStorage.SIDED.registerForBlockEntity(
              (condenser, direction) -> create(condenser), CondenserBlockEntity.MK2_TYPE);
        initialized = true;
    }

    static Storage<ItemVariant> create(CondenserBlockEntity.Base condenser) {
        return create(condenser, variant -> hasPositiveEmc(condenser, variant));
    }

    static Storage<ItemVariant> create(
          CondenserBlockEntity.Base condenser, Predicate<ItemVariant> hasPositiveEmc
    ) {
        Objects.requireNonNull(condenser, "condenser");
        Objects.requireNonNull(hasPositiveEmc, "hasPositiveEmc");

        ContainerStorage inventory = ContainerStorage.of(condenser, null);
        Predicate<ItemVariant> validInput = variant -> !variant.isBlank()
              && hasPositiveEmc.test(variant)
              && !matchesTarget(condenser, variant);

        if (condenser.getTier() == 1) {
            return new FilteringStorage<>(inventory) {
                @Override
                protected boolean canInsert(ItemVariant variant) {
                    return validInput.test(variant);
                }

                @Override
                protected boolean canExtract(ItemVariant variant) {
                    return condenser.getRequiredEmc() > 0 && matchesTarget(condenser, variant);
                }
            };
        }

        List<SingleSlotStorage<ItemVariant>> slots = inventory.getSlots();
        Storage<ItemVariant> input = new CombinedStorage<>(
              List.copyOf(slots.subList(0, condenser.inputSlots)));
        Storage<ItemVariant> automationInput = new FilteringStorage<>(input) {
            @Override
            protected boolean canInsert(ItemVariant variant) {
                return validInput.test(variant);
            }

            @Override
            protected boolean canExtract(ItemVariant variant) {
                return false;
            }
        };

        Storage<ItemVariant> output = new CombinedStorage<>(
              List.copyOf(slots.subList(condenser.outputStart, slots.size())));
        Storage<ItemVariant> automationOutput = FilteringStorage.extractOnlyOf(output);
        return new CombinedStorage<ItemVariant, Storage<ItemVariant>>(
              List.of(automationInput, automationOutput));
    }

    private static boolean hasPositiveEmc(
          CondenserBlockEntity.Base condenser, ItemVariant variant
    ) {
        if (variant.isBlank() || condenser.getLevel() == null) {
            return false;
        }
        if (condenser.stackKeys == null) {
            condenser.stackKeys = new MinecraftStackKeyFactory(condenser.getLevel().registryAccess());
        }
        ItemStack stack = variant.toStack();
        var snapshot = ProjectEEmc.service().current();
        return condenser.stackKeys.optionalKey(stack)
              .flatMap(key -> StackEmcResolver.resolve(stack, key, snapshot))
              .map(StackEmcResolver.Resolved::value)
              .orElse(EmcValue.ZERO)
              .longValue() > 0;
    }

    private static boolean matchesTarget(
          CondenserBlockEntity.Base condenser, ItemVariant variant
    ) {
        ItemStack target = condenser.getTarget();
        return !variant.isBlank() && !target.isEmpty() && variant.matches(target);
    }
}
