package moze_intel.projecte.content.items.tools;

import moze_intel.projecte.content.items.IItemCharge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Static helpers implementing ProjectE's tool area-of-effect behaviors. These port the original
 * {@code ToolHelper}'s AoE mining patterns, adapted to Fabric/vanilla item methods and the data-
 * component-based charge system ({@link moze_intel.projecte.content.items.IItemCharge}).
 */
public final class ToolHelper {
    private ToolHelper() {
    }

    /**
     * Mines a cube of blocks centered on the targeted block. The cube edge length is
     * {@code 2 * radius + 1}. Only blocks the player can actually mine (correct tool + gamemode) are
     * broken, and blocks are dropped at the targeted block's position so the player collects them.
     *
     * @param level    the server level.
     * @param player   the mining player.
     * @param stack    the tool stack (used for tool-tier checks and damage).
     * @param target   the targeted block position (cube center).
     * @param faceHit  the face hit (used to orient the cube when {@code flat} is true).
     * @param radius   the cube half-size (0 = single block, 1 = 3×3×3, etc.).
     * @param flat     if true, mine a flat plane perpendicular to {@code faceHit} instead of a cube.
     */
    public static void digAOE(Level level, Player player, ItemStack stack, InteractionHand hand,
          BlockPos target, Direction faceHit, int radius, boolean flat) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer) || radius < 0) {
            return;
        }
        int minX = target.getX() - radius, maxX = target.getX() + radius;
        int minY = target.getY() - radius, maxY = target.getY() + radius;
        int minZ = target.getZ() - radius, maxZ = target.getZ() + radius;
        if (flat) {
            // Collapse one axis to a single layer perpendicular to the hit face.
            switch (faceHit.getAxis()) {
                case X -> { minX = maxX = target.getX(); }
                case Y -> { minY = maxY = target.getY(); }
                case Z -> { minZ = maxZ = target.getZ(); }
            }
        }
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (pos.equals(target)) continue; // the targeted block is broken by the caller
                    breakBlock(level, serverPlayer, stack, pos);
                }
            }
        }
    }

    /**
     * Breaks a single block as if the player mined it, respecting tool requirements and gamemode.
     * Drops are produced at the block's position. Matter tools are intentionally not damaged.
     */
    private static void breakBlock(Level level, ServerPlayer player, ItemStack stack, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0) {
            return; // unbreakable (bedrock, etc.)
        }
        // Respect tool-tier gating: only break blocks the tool can actually harvest.
        if (!stack.isCorrectToolForDrops(state)) {
            return;
        }
        if (!player.gameMode.destroyBlock(pos)) {
            return;
        }
    }

    /**
     * Resolves the block the player is currently looking at (within reach), or null.
     */
    public static BlockPos targetedBlock(Player player) {
        HitResult hit = player.pick(5.0, 0.0f, false);
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            return blockHit.getBlockPos();
        }
        return null;
    }

    public static void digBasedOnMode(Level level, LivingEntity miner, ItemStack stack,
          BlockPos target, MatterPickaxeItem.PickaxeMode mode) {
        if (level.isClientSide() || mode == MatterPickaxeItem.PickaxeMode.STANDARD
              || !(miner instanceof ServerPlayer player)) {
            return;
        }
        HitResult hit = player.pick(5.0, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || !target.equals(blockHit.getBlockPos())) {
            return;
        }
        Direction face = blockHit.getDirection();
        switch (mode) {
            case TALLSHOT -> {
                breakBlock(level, player, stack, target.below());
                breakBlock(level, player, stack, target.above());
            }
            case WIDESHOT -> {
                Direction.Axis axis = face.getAxis() == Direction.Axis.Y
                      ? player.getDirection().getAxis() : face.getAxis();
                if (axis == Direction.Axis.X) {
                    breakBlock(level, player, stack, target.north());
                    breakBlock(level, player, stack, target.south());
                } else if (axis == Direction.Axis.Z) {
                    breakBlock(level, player, stack, target.west());
                    breakBlock(level, player, stack, target.east());
                }
            }
            case LONGSHOT -> {
                breakBlock(level, player, stack, target.relative(face.getOpposite()));
                breakBlock(level, player, stack, target.relative(face.getOpposite(), 2));
            }
            case STANDARD -> { }
        }
    }

    public static void attackWithCharge(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(attacker instanceof Player player) || !(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int charge = stack.getItem() instanceof IItemCharge item
              ? item.getCharge(stack) : 0;
        target.invulnerableTime = 0;
        target.hurtServer(serverLevel, target.damageSources().playerAttack(player), 1.0F + charge);
    }

    public static void attackAOE(ItemStack stack, Player player, boolean slayAll, float damage) {
        if (!(player.level() instanceof ServerLevel serverLevel)
              || !(stack.getItem() instanceof IItemCharge item)) {
            return;
        }
        int charge = item.getCharge(stack);
        if (charge == 0) {
            return;
        }
        for (LivingEntity target : serverLevel.getEntitiesOfClass(
              LivingEntity.class,
              player.getBoundingBox().inflate(2.5F * charge),
              entity -> entity != player && entity.isAlive() && !entity.isAlliedTo(player)
                    && (slayAll || entity instanceof Enemy))) {
            target.invulnerableTime = 0;
            target.hurtServer(serverLevel, target.damageSources().playerAttack(player), damage);
        }
    }
}
