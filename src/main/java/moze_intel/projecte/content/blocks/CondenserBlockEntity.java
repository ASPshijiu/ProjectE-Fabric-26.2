package moze_intel.projecte.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Energy Condenser MK1-2 (102 slots). */
public final class CondenserBlockEntity {
    private CondenserBlockEntity() {}

    public static BlockEntityType<MK1> MK1_TYPE;
    public static BlockEntityType<MK2> MK2_TYPE;

    private static final int SIZE = 102;

    abstract static class Base extends BaseContainerBlockEntity {
        final int tier;
        long storedEmc;
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

        Base(BlockEntityType<?> type, BlockPos pos, BlockState state, int tier) {
            super(type, pos, state); this.tier = tier;
        }

        @Override protected Component getDefaultName() { return Component.translatable("container.projecte.condenser_mk" + tier); }
        @Override protected NonNullList<ItemStack> getItems() { return items; }
        @Override protected void setItems(NonNullList<ItemStack> l) { this.items = l; }
        @Override protected AbstractContainerMenu createMenu(int id, Inventory inv) { return null; } @Override public int getContainerSize() { return SIZE; }

        public long getStoredEmc() { return storedEmc; }
        public void setStoredEmc(long e) { this.storedEmc = e; }

        static void doTick(Level level, BlockPos pos, BlockState state, Base entity) {
            if (level.isClientSide()) return;
        }
    }

    public static final class MK1 extends Base {
        public MK1(BlockPos pos, BlockState state) { super(MK1_TYPE, pos, state, 1); }
        public static void tick(Level l, BlockPos p, BlockState s, MK1 e) { doTick(l, p, s, e); }
    }
    public static final class MK2 extends Base {
        public MK2(BlockPos pos, BlockState state) { super(MK2_TYPE, pos, state, 2); }
        public static void tick(Level l, BlockPos p, BlockState s, MK2 e) { doTick(l, p, s, e); }
    }
}
