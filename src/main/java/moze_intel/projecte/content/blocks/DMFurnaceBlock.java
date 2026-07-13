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
 * Dark Matter Furnace — cooks at 2× speed.
 */
public class DMFurnaceBlock extends AbstractFurnaceBlock {
    public static final MapCodec<DMFurnaceBlock> CODEC = Block.simpleCodec(DMFurnaceBlock::new);

    public DMFurnaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<DMFurnaceBlock> codec() { return CODEC; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FastFurnaceBlockEntity.DM(pos, state);
    }

    @Override
    protected void openContainer(Level level, BlockPos pos, Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof FastFurnaceBlockEntity.DM dm) {
            player.openMenu(dm);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
          Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        if (type == FastFurnaceBlockEntity.DM_TYPE) {
            return (lvl, pos, st, be) ->
                  FastFurnaceBlockEntity.DM.fastTick((ServerLevel) lvl, pos, st,
                        (FastFurnaceBlockEntity.DM) be);
        }
        return null;
    }
}
