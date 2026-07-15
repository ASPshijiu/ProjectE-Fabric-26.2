package moze_intel.projecte.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
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

    abstract static class Base extends BaseContainerBlockEntity {
        final int tier;
        final int transferRate;
        final long maximumEmc;
        long storedEmc;
        NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

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
        }

        @Override protected Component getDefaultName() { return Component.translatable("container.projecte.relay_mk" + tier); }
        @Override protected NonNullList<ItemStack> getItems() { return items; }
        @Override protected void setItems(NonNullList<ItemStack> l) { this.items = l; }
        @Override protected AbstractContainerMenu createMenu(int id, Inventory inv) { return null; } @Override public int getContainerSize() { return 1; }

        public long getStoredEmc() { return storedEmc; }
        public void setStoredEmc(long emc) {
            long clamped = Math.max(0, Math.min(emc, maximumEmc));
            if (storedEmc != clamped) {
                storedEmc = clamped;
                setChanged();
            }
        }
        public long getMaximumEmc() { return maximumEmc; }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            storedEmc = Math.max(0, Math.min(input.getLongOr("emc", 0), maximumEmc));
            items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
            ContainerHelper.loadAllItems(input, items);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putLong("emc", storedEmc);
            ContainerHelper.saveAllItems(output, items);
        }

        static void doTick(Level level, BlockPos pos, BlockState state, Base entity) {
            if (level.isClientSide()) return;
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
