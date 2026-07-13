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

public class CondenserBlock extends BaseEntityBlock {
    private final int tier;
    public static final MapCodec<CondenserBlock> CODEC = Block.simpleCodec(p -> new CondenserBlock(1, p));

    public CondenserBlock(int tier, Properties props) { super(props); this.tier = tier; }
    @Override public MapCodec<CondenserBlock> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return tier == 1 ? new CondenserBlockEntity.MK1(pos, state) : new CondenserBlockEntity.MK2(pos, state);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        if (l.isClientSide()) return null;
        if (t == ModBlockEntities.CONDENSER_MK1) return (lvl, p, st, be) -> CondenserBlockEntity.MK1.tick(lvl, p, st, (CondenserBlockEntity.MK1) be);
        if (t == ModBlockEntities.CONDENSER_MK2) return (lvl, p, st, be) -> CondenserBlockEntity.MK2.tick(lvl, p, st, (CondenserBlockEntity.MK2) be);
        return null;
    }
}
