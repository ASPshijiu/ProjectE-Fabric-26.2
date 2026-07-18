package moze_intel.projecte.content.blocks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.content.items.KleinStarItem;
import moze_intel.projecte.content.menu.CollectorMenu;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.ProjectEEmc;
import moze_intel.projecte.emc.StackEmcResolver;
import moze_intel.projecte.emc.recipe.MinecraftStackKeyFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Energy Collector MK1-3 block entities. Each tier uses a static type
 * holder so the constructor matches {@code BlockEntityType.BlockEntitySupplier}.
 */
public final class CollectorBlockEntity {
    private CollectorBlockEntity() {}

    public static BlockEntityType<MK1> MK1_TYPE;
    public static BlockEntityType<MK2> MK2_TYPE;
    public static BlockEntityType<MK3> MK3_TYPE;

    public static abstract class Base extends net.minecraft.world.level.block.entity.BaseContainerBlockEntity {
        private static final int AUXILIARY_SLOTS = 3;
        private static final TagKey<Item> COLLECTOR_FUEL = TagKey.create(
              Registries.ITEM, ProjectEAPI.id("collector_fuel"));

        final int tier;
        final int emcPerSecond;
        final long maximumEmc;
        final int inputSlots;
        long storedEmc;
        double unprocessedEmc;
        NonNullList<ItemStack> items;
        MinecraftStackKeyFactory stackKeys;

        Base(BlockEntityType<?> type, BlockPos pos, BlockState state, int tier, int emcPerSecond) {
            super(type, pos, state);
            this.tier = tier;
            this.emcPerSecond = emcPerSecond;
            this.inputSlots = switch (tier) {
                case 1 -> 8;
                case 2 -> 12;
                case 3 -> 16;
                default -> throw new IllegalArgumentException("Unknown collector tier: " + tier);
            };
            this.maximumEmc = switch (tier) {
                case 1 -> 10_000;
                case 2 -> 30_000;
                case 3 -> 60_000;
                default -> throw new IllegalArgumentException("Unknown collector tier: " + tier);
            };
            this.items = NonNullList.withSize(inputSlots + AUXILIARY_SLOTS, ItemStack.EMPTY);
        }

        @Override protected Component getDefaultName() { return Component.translatable("container.projecte.collector_mk" + tier); }
        @Override protected NonNullList<ItemStack> getItems() { return items; }
        @Override protected void setItems(NonNullList<ItemStack> list) { this.items = list; }
        @Override public int getContainerSize() { return inputSlots + AUXILIARY_SLOTS; }
        @Override protected net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inv) {
            return new CollectorMenu(id, inv, this);
        }

        @Override
        public void preRemoveSideEffects(BlockPos pos, BlockState state) {
            items.set(inputSlots + 2, ItemStack.EMPTY);
            super.preRemoveSideEffects(pos, state);
        }

        public int getTier() { return tier; }
        public long getStoredEmc() { return storedEmc; }
        public void setStoredEmc(long emc) {
            long clamped = Math.max(0, Math.min(emc, maximumEmc));
            if (storedEmc != clamped) {
                storedEmc = clamped;
                setChanged();
            }
        }
        public int getEmcPerSecond() { return emcPerSecond; }
        public long getMaximumEmc() { return maximumEmc; }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            storedEmc = Math.max(0, Math.min(input.getLongOr("emc", 0), maximumEmc));
            unprocessedEmc = input.getDoubleOr("unprocessed_emc", 0);
            items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
            ContainerHelper.loadAllItems(input, items);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putLong("emc", storedEmc);
            output.putDouble("unprocessed_emc", unprocessedEmc);
            ContainerHelper.saveAllItems(output, items);
        }

        static void doTick(Level level, BlockPos pos, BlockState state, Base entity) {
            if (level.isClientSide()) return;
            entity.compactInputs();
            entity.generateEmc(sunLevel(level, pos));
            boolean handled = entity.chargeItem();
            ItemStack upgrading = entity.items.get(entity.inputSlots);
            if (!handled && !upgrading.isEmpty()
                  && !(upgrading.getItem() instanceof KleinStarItem)) {
                if (entity.stackKeys == null) {
                    entity.stackKeys = new MinecraftStackKeyFactory(level.registryAccess());
                }
                var snapshot = ProjectEEmc.service().current();
                ToLongFunction<ItemStack> emcValue = stack -> entity.stackKeys.optionalKey(stack)
                      .flatMap(key -> StackEmcResolver.resolve(stack, key, snapshot))
                      .map(StackEmcResolver.Resolved::value)
                      .orElse(EmcValue.ZERO)
                      .longValue();
                handled = entity.upgradeFuel(
                      stack -> nextFuel(stack, level.registryAccess(), emcValue), emcValue);
            }
            if (!handled && entity.storedEmc > 0) {
                List<RelayBlockEntity.Base> relays = new ArrayList<>();
                for (Direction direction : Direction.values()) {
                    BlockPos relayPos = pos.relative(direction);
                    if (level.isLoaded(relayPos)) {
                        BlockEntity neighbor = level.getBlockEntity(relayPos);
                        if (neighbor instanceof RelayBlockEntity.Base relay) {
                            relays.add(relay);
                        }
                    }
                }
                sendEmcToRelays(entity, relays);
                relays.forEach(Base::sendRelayBonus);
            }
            entity.rotateOutput();
        }

        void generateEmc(int sunLevel) {
            if (storedEmc >= maximumEmc) return;
            unprocessedEmc += emcPerSecond * (sunLevel / 320.0);
            long emcToStore = (long) unprocessedEmc;
            if (emcToStore > 0) {
                long inserted = Math.min(emcToStore, maximumEmc - storedEmc);
                setStoredEmc(storedEmc + inserted);
                unprocessedEmc -= inserted;
            }
            setChanged();
        }

        boolean chargeItem() {
            ItemStack stack = items.get(inputSlots);
            if (!(stack.getItem() instanceof KleinStarItem)) return false;

            long available = Math.min(emcPerSecond, storedEmc);
            if (available <= 0) return false;

            long transferred = available - KleinStarItem.addEmc(stack, available);
            if (transferred <= 0) return false;

            setStoredEmc(storedEmc - transferred);
            return true;
        }

        boolean upgradeFuel(
              Function<ItemStack, ItemStack> nextFuel,
              ToLongFunction<ItemStack> emcValue
        ) {
            ItemStack upgrading = items.get(inputSlots);
            if (upgrading.isEmpty()) return false;

            ItemStack standardUpgrade = nextFuel.apply(upgrading);
            if (standardUpgrade.isEmpty()) return false;

            ItemStack lock = items.get(inputSlots + 2);
            ItemStack result = lock.isEmpty()
                  ? standardUpgrade
                  : lock.copyWithCount(1);

            long inputEmc = emcValue.applyAsLong(upgrading);
            long outputEmc = emcValue.applyAsLong(result);
            if (inputEmc <= 0 || outputEmc < inputEmc) return true;

            long cost = outputEmc - inputEmc;
            if (storedEmc < cost) return true;

            int outputSlot = inputSlots + 1;
            ItemStack output = items.get(outputSlot);
            if (output.isEmpty()) {
                items.set(outputSlot, result.copyWithCount(1));
            } else if (ItemStack.isSameItemSameComponents(output, result)
                  && output.getCount() < output.getMaxStackSize()) {
                output.grow(1);
            } else {
                return true;
            }

            upgrading.shrink(1);
            if (upgrading.isEmpty()) {
                items.set(inputSlots, ItemStack.EMPTY);
            }
            setStoredEmc(storedEmc - cost);
            setChanged();
            return true;
        }

        static ItemStack nextFuel(
              ItemStack input, List<Item> fuels, ToLongFunction<ItemStack> emcValue
        ) {
            List<Item> sorted = fuels.stream()
                  .filter(item -> emcValue.applyAsLong(new ItemStack(item)) > 0)
                  .sorted(Comparator.comparingLong(
                        item -> emcValue.applyAsLong(new ItemStack(item))))
                  .toList();
            for (int index = 0; index < sorted.size(); index++) {
                if (input.is(sorted.get(index))) {
                    return index + 1 < sorted.size()
                          ? new ItemStack(sorted.get(index + 1))
                          : ItemStack.EMPTY;
                }
            }
            return ItemStack.EMPTY;
        }

        public static ItemStack nextFuel(
              ItemStack input,
              RegistryAccess registryAccess,
              ToLongFunction<ItemStack> emcValue
        ) {
            List<Item> fuels = registryAccess.lookup(Registries.ITEM)
                  .flatMap(items -> items.get(COLLECTOR_FUEL))
                  .stream()
                  .flatMap(named -> named.stream())
                  .map(holder -> holder.value())
                  .toList();
            return nextFuel(input, fuels, emcValue);
        }

        public static boolean isCollectorFuel(ItemStack stack) {
            return !stack.isEmpty() && stack.is(COLLECTOR_FUEL);
        }

        public static boolean isCollectorInput(
              ItemStack stack,
              RegistryAccess registryAccess,
              ToLongFunction<ItemStack> emcValue
        ) {
            return stack.getItem() instanceof KleinStarItem
                  || !nextFuel(stack, registryAccess, emcValue).isEmpty();
        }

        public int getSunLevel() {
            return level == null ? 0 : sunLevel(level, worldPosition);
        }

        private static int sunLevel(Level level, BlockPos pos) {
            return level.environmentAttributes()
                  .getDimensionValue(EnvironmentAttributes.WATER_EVAPORATES)
                  ? 16
                  : level.getMaxLocalRawBrightness(pos.above()) + 1;
        }

        void compactInputs() {
            boolean hasInput = false;
            for (int slot = 0; slot < inputSlots; slot++) {
                if (!items.get(slot).isEmpty()) {
                    hasInput = true;
                    break;
                }
            }
            if (!hasInput) return;

            List<ItemStack> stacks = new ArrayList<>();
            int upgradingSlot = inputSlots;
            if (!items.get(upgradingSlot).isEmpty()) {
                stacks.add(items.get(upgradingSlot).copy());
                items.set(upgradingSlot, ItemStack.EMPTY);
            }
            for (int slot = 0; slot < inputSlots; slot++) {
                if (!items.get(slot).isEmpty()) {
                    stacks.add(items.get(slot).copy());
                    items.set(slot, ItemStack.EMPTY);
                }
            }
            if (stacks.isEmpty()) return;

            for (ItemStack stack : stacks) {
                insertIntoProcessingSlots(stack);
            }
            setChanged();
        }

        void rotateOutput() {
            int outputSlot = inputSlots + 1;
            ItemStack output = items.get(outputSlot);
            if (output.isEmpty()) return;

            ItemStack lock = items.get(inputSlots + 2);
            if (!lock.isEmpty() && output.is(lock.getItem())
                  && output.getCount() < output.getMaxStackSize()) {
                return;
            }

            items.set(outputSlot, insertIntoMainInventory(output));
            setChanged();
        }

        private ItemStack insertIntoProcessingSlots(ItemStack source) {
            ItemStack remaining = source.copy();
            remaining = mergeIntoSlot(remaining, inputSlots);
            for (int slot = 0; slot < inputSlots && !remaining.isEmpty(); slot++) {
                remaining = mergeIntoSlot(remaining, slot);
            }
            remaining = fillEmptySlot(remaining, inputSlots);
            for (int slot = 0; slot < inputSlots && !remaining.isEmpty(); slot++) {
                remaining = fillEmptySlot(remaining, slot);
            }
            return remaining;
        }

        private ItemStack insertIntoMainInventory(ItemStack source) {
            ItemStack remaining = source.copy();
            for (int slot = 0; slot < inputSlots && !remaining.isEmpty(); slot++) {
                remaining = mergeIntoSlot(remaining, slot);
            }
            for (int slot = 0; slot < inputSlots && !remaining.isEmpty(); slot++) {
                remaining = fillEmptySlot(remaining, slot);
            }
            return remaining;
        }

        private ItemStack mergeIntoSlot(ItemStack remaining, int slot) {
            if (remaining.isEmpty()) return ItemStack.EMPTY;

            ItemStack target = items.get(slot);
            if (!ItemStack.isSameItemSameComponents(target, remaining)) return remaining;

            int moved = Math.min(
                  remaining.getCount(), target.getMaxStackSize() - target.getCount());
            if (moved > 0) {
                target.grow(moved);
                remaining.shrink(moved);
            }
            return remaining;
        }

        private ItemStack fillEmptySlot(ItemStack remaining, int slot) {
            if (remaining.isEmpty()) return ItemStack.EMPTY;
            if (!items.get(slot).isEmpty()) return remaining;

            int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
            items.set(slot, remaining.copyWithCount(moved));
            remaining.shrink(moved);
            return remaining;
        }

        static long sendEmcToRelays(
              Base collector, List<RelayBlockEntity.Base> adjacentRelays
        ) {
            if (collector.storedEmc <= 0 || adjacentRelays.isEmpty()) return 0;
            List<RelayBlockEntity.Base> acceptingRelays = adjacentRelays.stream()
                  .filter(relay -> relay.getNeededEmc() > 0)
                  .toList();
            if (acceptingRelays.isEmpty()) return 0;

            long transfer = Math.min(collector.storedEmc, collector.emcPerSecond);
            long transferPerRelay = transfer / acceptingRelays.size();
            if (transferPerRelay == 0) return 0;

            long sent = 0;
            for (RelayBlockEntity.Base relay : acceptingRelays) {
                sent += relay.insertEmc(transferPerRelay);
            }
            collector.setStoredEmc(collector.storedEmc - sent);
            return sent;
        }

        static void sendRelayBonus(BlockEntity neighbor) {
            if (neighbor instanceof RelayBlockEntity.Base relay) {
                relay.addBonus();
            }
        }
    }

    public static final class MK1 extends Base {
        public MK1(BlockPos pos, BlockState state) { super(MK1_TYPE, pos, state, 1, 4); }
        public static void tick(Level l, BlockPos p, BlockState s, MK1 e) { doTick(l, p, s, e); }
    }

    public static final class MK2 extends Base {
        public MK2(BlockPos pos, BlockState state) { super(MK2_TYPE, pos, state, 2, 12); }
        public static void tick(Level l, BlockPos p, BlockState s, MK2 e) { doTick(l, p, s, e); }
    }

    public static final class MK3 extends Base {
        public MK3(BlockPos pos, BlockState state) { super(MK3_TYPE, pos, state, 3, 40); }
        public static void tick(Level l, BlockPos p, BlockState s, MK3 e) { doTick(l, p, s, e); }
    }
}
