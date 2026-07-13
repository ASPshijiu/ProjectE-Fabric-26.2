package moze_intel.projecte.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fast furnace shared logic. Both DM (2× speed) and RM (4× speed) furnaces
 * use this pattern. The block entity type is injected via a static holder so
 * the constructor stays compatible with {@code BlockEntityType.BlockEntitySupplier}.
 */
public final class FastFurnaceBlockEntity {
    private FastFurnaceBlockEntity() {}

    /** Holder for the DM furnace type, set during {@code ModBlocks.init()}. */
    public static BlockEntityType<DM> DM_TYPE;

    /** Holder for the RM furnace type, set during {@code ModBlocks.init()}. */
    public static BlockEntityType<RM> RM_TYPE;

    // ── DM Furnace (2× speed) ─────────────────────────────────────────────

    public static final class DM extends AbstractFurnaceBlockEntity {
        private static final Component NAME = Component.translatable("container.projecte.dm_furnace");

        public DM(BlockPos pos, BlockState state) {
            super(DM_TYPE, pos, state, RecipeType.SMELTING);
        }

        @Override
        protected Component getDefaultName() { return NAME; }

        @Override
        protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
            return new FurnaceMenu(id, inventory, this, this.dataAccess);
        }

        public static void fastTick(ServerLevel level, BlockPos pos, BlockState state, DM self) {
            for (int i = 0; i < 2; i++) {
                AbstractFurnaceBlockEntity.serverTick(level, pos, state, self);
            }
        }
    }

    // ── RM Furnace (4× speed) ─────────────────────────────────────────────

    public static final class RM extends AbstractFurnaceBlockEntity {
        private static final Component NAME = Component.translatable("container.projecte.rm_furnace");

        public RM(BlockPos pos, BlockState state) {
            super(RM_TYPE, pos, state, RecipeType.SMELTING);
        }

        @Override
        protected Component getDefaultName() { return NAME; }

        @Override
        protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
            return new FurnaceMenu(id, inventory, this, this.dataAccess);
        }

        public static void fastTick(ServerLevel level, BlockPos pos, BlockState state, RM self) {
            for (int i = 0; i < 4; i++) {
                AbstractFurnaceBlockEntity.serverTick(level, pos, state, self);
            }
        }
    }
}
