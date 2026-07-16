package moze_intel.projecte.content.blocks;

import com.mojang.serialization.MapCodec;
import moze_intel.projecte.content.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public class CollectorBlock extends BaseEntityBlock {
    private final int tier;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final MapCodec<CollectorBlock> CODEC = Block.simpleCodec(p -> new CollectorBlock(1, p));

    public CollectorBlock(int tier, Properties props) {
        super(props);
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override public MapCodec<CollectorBlock> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useWithoutItem(
          BlockState state,
          Level level,
          BlockPos pos,
          Player player,
          BlockHitResult hit
    ) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof CollectorBlockEntity.Base collector) {
                player.openMenu(collector);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (tier) {
            case 1 -> new CollectorBlockEntity.MK1(pos, state);
            case 2 -> new CollectorBlockEntity.MK2(pos, state);
            case 3 -> new CollectorBlockEntity.MK3(pos, state);
            default -> new CollectorBlockEntity.MK1(pos, state);
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        if (l.isClientSide()) return null;
        if (t == ModBlockEntities.COLLECTOR_MK1) return (lvl, p, st, be) -> CollectorBlockEntity.MK1.tick(lvl, p, st, (CollectorBlockEntity.MK1) be);
        if (t == ModBlockEntities.COLLECTOR_MK2) return (lvl, p, st, be) -> CollectorBlockEntity.MK2.tick(lvl, p, st, (CollectorBlockEntity.MK2) be);
        if (t == ModBlockEntities.COLLECTOR_MK3) return (lvl, p, st, be) -> CollectorBlockEntity.MK3.tick(lvl, p, st, (CollectorBlockEntity.MK3) be);
        return null;
    }
}
