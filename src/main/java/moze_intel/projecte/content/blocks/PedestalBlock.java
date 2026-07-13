package moze_intel.projecte.content.blocks;

import com.mojang.serialization.MapCodec;
import moze_intel.projecte.content.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Dark Matter Pedestal block. */
public class PedestalBlock extends BaseEntityBlock {
    public static final MapCodec<PedestalBlock> CODEC = Block.simpleCodec(PedestalBlock::new);

    public PedestalBlock(Properties props) { super(props); }
    @Override public MapCodec<PedestalBlock> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PedestalBlockEntity(pos, state);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        return l.isClientSide() ? null : (lvl, pos, st, be) -> PedestalBlockEntity.tick(lvl, pos, st, (PedestalBlockEntity) be);
    }
}
