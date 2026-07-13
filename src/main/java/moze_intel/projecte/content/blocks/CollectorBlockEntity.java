package moze_intel.projecte.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

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
        long storedEmc;
        NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

        Base(BlockEntityType<?> type, BlockPos pos, BlockState state, int tier, int emcPerSecond) {
            super(type, pos, state);
            this.tier = tier;
            this.emcPerSecond = emcPerSecond;
        }

        @Override protected Component getDefaultName() { return Component.translatable("container.projecte.collector_mk" + tier); }
        @Override protected NonNullList<ItemStack> getItems() { return items; }
        @Override protected void setItems(NonNullList<ItemStack> list) { this.items = list; }
        @Override public int getContainerSize() { return 1; }
        @Override protected net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inv) { return null; }

        public long getStoredEmc() { return storedEmc; }
        public void setStoredEmc(long e) { this.storedEmc = e; }
        public int getEmcPerSecond() { return emcPerSecond; }

        static void doTick(Level level, BlockPos pos, BlockState state, Base entity) {
            if (level.isClientSide()) return;
            int light = level.getMaxLocalRawBrightness(pos);
            if (light > 0) entity.storedEmc += (long) entity.emcPerSecond * light / 15;
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
