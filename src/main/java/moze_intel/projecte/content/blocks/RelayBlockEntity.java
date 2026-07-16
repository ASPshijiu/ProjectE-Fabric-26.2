package moze_intel.projecte.content.blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToLongFunction;
import moze_intel.projecte.content.items.KleinStarItem;
import moze_intel.projecte.content.menu.RelayMenu;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.ProjectEEmc;
import moze_intel.projecte.emc.recipe.MinecraftStackKeyFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Anti-Matter Relay MK1-3. */
public final class RelayBlockEntity {
    private RelayBlockEntity() {}

    public static BlockEntityType<MK1> MK1_TYPE;
    public static BlockEntityType<MK2> MK2_TYPE;
    public static BlockEntityType<MK3> MK3_TYPE;

    public abstract static class Base extends BaseContainerBlockEntity {
        final int tier;
        final int transferRate;
        final long maximumEmc;
        final double collectorBonus;
        final int inputSlots;
        long storedEmc;
        double bonusEmc;
        NonNullList<ItemStack> items;
        MinecraftStackKeyFactory stackKeys;

        Base(BlockEntityType<?> type, BlockPos pos, BlockState state, int tier, int rate) {
            super(type, pos, state);
            this.tier = tier;
            this.transferRate = rate;
            this.maximumEmc = switch (tier) {
                case 1 -> 100_000;
                case 2 -> 1_000_000;
                case 3 -> 10_000_000;
                default -> throw new IllegalArgumentException("Unknown relay tier: " + tier);
            };
            this.collectorBonus = switch (tier) {
                case 1 -> 0.05;
                case 2 -> 0.15;
                case 3 -> 0.5;
                default -> throw new IllegalArgumentException("Unknown relay tier: " + tier);
            };
            this.inputSlots = switch (tier) {
                case 1 -> 7;
                case 2 -> 13;
                case 3 -> 21;
                default -> throw new IllegalArgumentException("Unknown relay tier: " + tier);
            };
            this.items = NonNullList.withSize(inputSlots + 1, ItemStack.EMPTY);
        }

        @Override protected Component getDefaultName() { return Component.translatable("container.projecte.relay_mk" + tier); }
        @Override protected NonNullList<ItemStack> getItems() { return items; }
        @Override protected void setItems(NonNullList<ItemStack> l) { this.items = l; }
        @Override protected AbstractContainerMenu createMenu(int id, Inventory inv) {
            return new RelayMenu(id, inv, this);
        }
        @Override public int getContainerSize() { return inputSlots + 1; }

        public int getTier() { return tier; }
        public long getStoredEmc() { return storedEmc; }
        public void setStoredEmc(long emc) {
            long clamped = Math.max(0, Math.min(emc, maximumEmc));
            if (storedEmc != clamped) {
                storedEmc = clamped;
                setChanged();
            }
        }
        public long getMaximumEmc() { return maximumEmc; }
        long getNeededEmc() { return maximumEmc - storedEmc; }

        public static boolean isChargeable(ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() instanceof KleinStarItem;
        }

        public static boolean isRelayInput(
              ItemStack stack, ToLongFunction<ItemStack> emcValue
        ) {
            return isChargeable(stack) || emcValue.applyAsLong(stack) > 0;
        }

        long insertEmc(long emc) {
            if (emc <= 0) return 0;
            long inserted = Math.min(emc, getNeededEmc());
            setStoredEmc(storedEmc + inserted);
            return inserted;
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            storedEmc = Math.max(0, Math.min(input.getLongOr("emc", 0), maximumEmc));
            bonusEmc = input.getDoubleOr("bonus_emc", 0);
            items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
            ContainerHelper.loadAllItems(input, items);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putLong("emc", storedEmc);
            output.putDouble("bonus_emc", bonusEmc);
            ContainerHelper.saveAllItems(output, items);
        }

        void addBonus() {
            bonusEmc += collectorBonus;
            if (bonusEmc >= 1) {
                long emcToStore = (long) bonusEmc;
                setStoredEmc(storedEmc + emcToStore);
                bonusEmc -= emcToStore;
            }
            setChanged();
        }

        boolean burnOneInput(ToLongFunction<ItemStack> emcValue) {
            for (int slot = 0; slot < inputSlots; slot++) {
                ItemStack stack = items.get(slot);
                if (stack.isEmpty()) continue;
                if (stack.getItem() instanceof KleinStarItem) {
                    long transferred = KleinStarItem.removeEmc(
                          stack, Math.min(transferRate, getNeededEmc()));
                    if (transferred > 0) {
                        insertEmc(transferred);
                        return true;
                    }
                    return false;
                }

                long value = emcValue.applyAsLong(stack);
                if (value <= 0) continue;
                if (value > getNeededEmc()) return false;

                insertEmc(value);
                stack.shrink(1);
                if (stack.isEmpty()) {
                    items.set(slot, ItemStack.EMPTY);
                }
                setChanged();
                return true;
            }
            return false;
        }

        boolean chargeOutput() {
            ItemStack stack = items.get(inputSlots);
            if (!(stack.getItem() instanceof KleinStarItem)) return false;

            long available = Math.min(transferRate, storedEmc);
            if (available <= 0) return false;

            long transferred = available - KleinStarItem.addEmc(stack, available);
            if (transferred <= 0) return false;

            setStoredEmc(storedEmc - transferred);
            return true;
        }

        static long sendEmcToCondensers(
              Base relay, List<CondenserBlockEntity.Base> adjacentCondensers
        ) {
            if (relay.storedEmc <= 0 || adjacentCondensers.isEmpty()) return 0;
            List<CondenserBlockEntity.Base> acceptingCondensers = adjacentCondensers.stream()
                  .filter(CondenserBlockEntity.Base::canAcceptEmc)
                  .toList();
            if (acceptingCondensers.isEmpty()) return 0;

            long transfer = Math.min(relay.storedEmc, relay.transferRate);
            long transferPerCondenser = transfer / acceptingCondensers.size();
            if (transferPerCondenser == 0) return 0;

            long sent = 0;
            for (CondenserBlockEntity.Base condenser : acceptingCondensers) {
                sent += condenser.insertEmc(transferPerCondenser);
            }
            relay.setStoredEmc(relay.storedEmc - sent);
            return sent;
        }

        static void doTick(Level level, BlockPos pos, BlockState state, Base entity) {
            if (level.isClientSide()) return;
            if (entity.storedEmc > 0) {
                List<CondenserBlockEntity.Base> condensers = new ArrayList<>();
                for (Direction direction : Direction.values()) {
                    BlockPos condenserPos = pos.relative(direction);
                    if (level.isLoaded(condenserPos)) {
                        BlockEntity neighbor = level.getBlockEntity(condenserPos);
                        if (neighbor instanceof CondenserBlockEntity.Base condenser) {
                            condensers.add(condenser);
                        }
                    }
                }
                sendEmcToCondensers(entity, condensers);
            }
            if (entity.stackKeys == null) {
                entity.stackKeys = new MinecraftStackKeyFactory(level.registryAccess());
            }
            var snapshot = ProjectEEmc.service().current();
            entity.burnOneInput(stack -> entity.stackKeys.optionalKey(stack)
                  .flatMap(snapshot::valueFor)
                  .orElse(EmcValue.ZERO)
                  .longValue());
            entity.chargeOutput();
        }
    }

    public static final class MK1 extends Base {
        public MK1(BlockPos pos, BlockState state) { super(MK1_TYPE, pos, state, 1, 64); }
        public static void tick(Level l, BlockPos p, BlockState s, MK1 e) { doTick(l, p, s, e); }
    }
    public static final class MK2 extends Base {
        public MK2(BlockPos pos, BlockState state) { super(MK2_TYPE, pos, state, 2, 192); }
        public static void tick(Level l, BlockPos p, BlockState s, MK2 e) { doTick(l, p, s, e); }
    }
    public static final class MK3 extends Base {
        public MK3(BlockPos pos, BlockState state) { super(MK3_TYPE, pos, state, 3, 640); }
        public static void tick(Level l, BlockPos p, BlockState s, MK3 e) { doTick(l, p, s, e); }
    }
}
