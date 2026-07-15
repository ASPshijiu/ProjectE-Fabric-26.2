package moze_intel.projecte.content.blocks;

import com.mojang.serialization.MapCodec;
import moze_intel.projecte.content.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class AlchemicalChestBlock extends BaseEntityBlock {
    public static final MapCodec<AlchemicalChestBlock> CODEC = Block.simpleCodec(AlchemicalChestBlock::new);

    public AlchemicalChestBlock(Properties properties) {
        super(properties);
    }

    @Override public MapCodec<AlchemicalChestBlock> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlchemicalChestBlockEntity(pos, state);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
          Level level, BlockState state, BlockEntityType<T> type
    ) {
        if (level.isClientSide() || type != ModBlockEntities.ALCHEMICAL_CHEST) return null;
        return (tickerLevel, pos, tickerState, entity) -> AlchemicalChestBlockEntity.tick(
              tickerLevel, pos, tickerState, (AlchemicalChestBlockEntity) entity);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AlchemicalChestBlockEntity chest) {
                player.openMenu(chest);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
