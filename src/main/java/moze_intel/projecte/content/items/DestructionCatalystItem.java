package moze_intel.projecte.content.items;

import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.content.ModDataComponents;
import moze_intel.projecte.emc.PlayerFuelConsumer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Destroys a charged 3x3 tunnel while consuming eight EMC per block. */
public final class DestructionCatalystItem extends ChargeableItem {
    static final long EMC_PER_BLOCK = 8;

    public DestructionCatalystItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getMaxCharge(ItemStack stack) {
        return 3;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.FAIL;
        }
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)
              || !(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.FAIL;
        }

        ItemStack catalyst = context.getItemInHand();
        List<ItemStack> drops = new ArrayList<>();
        boolean changed = false;
        for (BlockPos pos : targetPositions(
              context.getClickedPos(), context.getClickedFace(), getCharge(catalyst))) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            float hardness = state.getDestroySpeed(level, pos);
            if (state.isAir() || hardness == Block.INDESTRUCTIBLE || hardness >= 50.0F
                  || !level.mayInteract(serverPlayer, pos)
                  || !serverPlayer.mayUseItemAt(pos, context.getClickedFace(), catalyst)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(
                  level, serverPlayer, pos, state, blockEntity)) {
                PlayerBlockBreakEvents.CANCELED.invoker().onBlockBreakCanceled(
                      level, serverPlayer, pos, state, blockEntity);
                continue;
            }
            if (!consumeEmc(serverPlayer, catalyst)) {
                break;
            }
            List<ItemStack> blockDrops = Block.getDrops(
                  state, level, pos, blockEntity, serverPlayer, catalyst);
            if (level.removeBlock(pos, false)) {
                PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(
                      level, serverPlayer, pos, state, blockEntity);
                drops.addAll(blockDrops);
                changed = true;
                if (level.getRandom().nextInt(8) == 0) {
                    level.sendParticles(
                          ParticleTypes.LARGE_SMOKE,
                          pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                          2, 0.1, 0.1, 0.1, 0.0);
                }
            } else {
                refundEmc(serverPlayer, catalyst);
            }
        }
        if (changed) {
            drops.forEach(drop -> Block.popResource(level, context.getClickedPos(), drop));
            level.playSound(
                  null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE,
                  SoundSource.PLAYERS, 0.75F, 1.25F);
        }
        return InteractionResult.CONSUME;
    }

    static List<BlockPos> targetPositions(BlockPos clicked, Direction face, int charge) {
        int safeCharge = Math.max(0, Math.min(3, charge));
        int depth = safeCharge == 0 ? 1 : 1 << (safeCharge + 1);
        BlockPos back = clicked.relative(face.getOpposite(), depth - 1);
        int breadthX = face.getAxis() == Direction.Axis.X ? 0 : 1;
        int breadthY = face.getAxis() == Direction.Axis.Y ? 0 : 1;
        int breadthZ = face.getAxis() == Direction.Axis.Z ? 0 : 1;
        List<BlockPos> targets = new ArrayList<>(9 * depth);
        for (BlockPos pos : BlockPos.betweenClosed(
              Math.min(clicked.getX(), back.getX()) - breadthX,
              Math.min(clicked.getY(), back.getY()) - breadthY,
              Math.min(clicked.getZ(), back.getZ()) - breadthZ,
              Math.max(clicked.getX(), back.getX()) + breadthX,
              Math.max(clicked.getY(), back.getY()) + breadthY,
              Math.max(clicked.getZ(), back.getZ()) + breadthZ)) {
            targets.add(pos.immutable());
        }
        return targets;
    }

    private static boolean consumeEmc(ServerPlayer player, ItemStack stack) {
        if (player.isCreative()) {
            return true;
        }
        long stored = stack.getOrDefault(ModDataComponents.STORED_EMC, 0L);
        if (stored < EMC_PER_BLOCK) {
            long consumed = PlayerFuelConsumer.consume(player, EMC_PER_BLOCK - stored);
            if (consumed < 0) {
                return false;
            }
            stored += Math.min(consumed, Long.MAX_VALUE - stored);
        }
        stack.set(ModDataComponents.STORED_EMC, stored - EMC_PER_BLOCK);
        return true;
    }

    private static void refundEmc(ServerPlayer player, ItemStack stack) {
        if (!player.isCreative()) {
            long stored = stack.getOrDefault(ModDataComponents.STORED_EMC, 0L);
            stack.set(ModDataComponents.STORED_EMC, Math.addExact(stored, EMC_PER_BLOCK));
        }
    }
}
