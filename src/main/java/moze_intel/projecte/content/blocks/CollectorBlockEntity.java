package moze_intel.projecte.content.blocks;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.ContainerHelper;
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
        final int tier;
        final int emcPerSecond;
        final long maximumEmc;
        long storedEmc;
        double unprocessedEmc;
        NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

        Base(BlockEntityType<?> type, BlockPos pos, BlockState state, int tier, int emcPerSecond) {
            super(type, pos, state);
            this.tier = tier;
            this.emcPerSecond = emcPerSecond;
            this.maximumEmc = switch (tier) {
                case 1 -> 10_000;
                case 2 -> 30_000;
                case 3 -> 60_000;
                default -> throw new IllegalArgumentException("Unknown collector tier: " + tier);
            };
        }

        @Override protected Component getDefaultName() { return Component.translatable("container.projecte.collector_mk" + tier); }
        @Override protected NonNullList<ItemStack> getItems() { return items; }
        @Override protected void setItems(NonNullList<ItemStack> list) { this.items = list; }
        @Override public int getContainerSize() { return 1; }
        @Override protected net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inv) { return null; }

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
            int sunLevel = level.environmentAttributes()
                  .getDimensionValue(EnvironmentAttributes.WATER_EVAPORATES)
                  ? 16
                  : level.getMaxLocalRawBrightness(pos.above()) + 1;
            entity.generateEmc(sunLevel);
            if (entity.storedEmc > 0) {
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
