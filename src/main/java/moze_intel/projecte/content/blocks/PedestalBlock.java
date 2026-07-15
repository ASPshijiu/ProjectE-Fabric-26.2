package moze_intel.projecte.content.blocks;

import com.mojang.serialization.MapCodec;
import moze_intel.projecte.content.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

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

    @Override
    protected InteractionResult useItemOn(
          ItemStack stack, BlockState state, Level level, BlockPos pos,
          Player player, InteractionHand hand, BlockHitResult hit
    ) {
        if (stack.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()
              && level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal
              && PedestalBlockEntity.insertItem(pedestal, stack)) {
            pedestal.setActive(false);
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(
          BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit
    ) {
        if (!level.isClientSide()
              && level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal
              && !pedestal.isEmpty()) {
            pedestal.setActive(!pedestal.isActive());
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide()
              && level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal) {
            ItemStack removed = PedestalBlockEntity.takeItem(pedestal);
            if (!removed.isEmpty()) {
                pedestal.setActive(false);
                Block.popResource(level, pos, removed);
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
            }
        }
    }
}
