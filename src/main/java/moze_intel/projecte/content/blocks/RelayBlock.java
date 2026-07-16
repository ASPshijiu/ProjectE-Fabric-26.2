package moze_intel.projecte.content.blocks;

import com.mojang.serialization.MapCodec;
import moze_intel.projecte.content.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
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

public class RelayBlock extends BaseEntityBlock {
    private final int tier;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final MapCodec<RelayBlock> CODEC = Block.simpleCodec(p -> new RelayBlock(1, p));

    public RelayBlock(int tier, Properties props) {
        super(props);
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override public MapCodec<RelayBlock> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }
    @Override public BlockState rotate(BlockState s, Rotation r) { return s.setValue(FACING, r.rotate(s.getValue(FACING))); }
    @Override public BlockState mirror(BlockState s, Mirror m) { return s.rotate(m.getRotation(s.getValue(FACING))); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }

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
            if (blockEntity instanceof RelayBlockEntity.Base relay) {
                player.openMenu(relay);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (tier) {
            case 1 -> new RelayBlockEntity.MK1(pos, state);
            case 2 -> new RelayBlockEntity.MK2(pos, state);
            case 3 -> new RelayBlockEntity.MK3(pos, state);
            default -> new RelayBlockEntity.MK1(pos, state);
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        if (l.isClientSide()) return null;
        if (t == ModBlockEntities.RELAY_MK1) return (lvl, p, st, be) -> RelayBlockEntity.MK1.tick(lvl, p, st, (RelayBlockEntity.MK1) be);
        if (t == ModBlockEntities.RELAY_MK2) return (lvl, p, st, be) -> RelayBlockEntity.MK2.tick(lvl, p, st, (RelayBlockEntity.MK2) be);
        if (t == ModBlockEntities.RELAY_MK3) return (lvl, p, st, be) -> RelayBlockEntity.MK3.tick(lvl, p, st, (RelayBlockEntity.MK3) be);
        return null;
    }
}
