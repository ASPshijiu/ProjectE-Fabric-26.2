package moze_intel.projecte.content.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
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
                    breakBlock(level, serverPlayer, stack, hand, pos);
                }
            }
        }
    }

    /**
     * Breaks a single block as if the player mined it, respecting tool requirements and gamemode.
     * Drops are produced at the block's position. The tool is damaged by 1 if it actually broke a
     * non-air, non-fluid block.
     */
    private static void breakBlock(Level level, ServerPlayer player, ItemStack stack,
          InteractionHand hand, BlockPos pos) {
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
        // Damage the tool per extra block; stop if it breaks.
        if (stack.isDamageableItem()) {
            stack.hurtAndBreak(1, player, hand);
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
}
