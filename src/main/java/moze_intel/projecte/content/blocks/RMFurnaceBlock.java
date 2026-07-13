package moze_intel.projecte.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Red Matter Furnace — cooks at 4× speed.
 */
public class RMFurnaceBlock extends AbstractFurnaceBlock {
    public static final MapCodec<RMFurnaceBlock> CODEC = Block.simpleCodec(RMFurnaceBlock::new);

    public RMFurnaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<RMFurnaceBlock> codec() { return CODEC; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FastFurnaceBlockEntity.RM(pos, state);
    }

    @Override
    protected void openContainer(Level level, BlockPos pos, Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof FastFurnaceBlockEntity.RM rm) {
            player.openMenu(rm);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
          Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        if (type == FastFurnaceBlockEntity.RM_TYPE) {
            return (lvl, pos, st, be) ->
                  FastFurnaceBlockEntity.RM.fastTick((ServerLevel) lvl, pos, st,
                        (FastFurnaceBlockEntity.RM) be);
        }
        return null;
    }
}
