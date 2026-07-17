package moze_intel.projecte.content.items.tools;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import moze_intel.projecte.content.items.IItemCharge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Static helpers implementing ProjectE's tool area-of-effect behaviors. These port the original
 * {@code ToolHelper}'s AoE mining patterns, adapted to Fabric/vanilla item methods and the data-
 * component-based charge system ({@link moze_intel.projecte.content.items.IItemCharge}).
 */
public final class ToolHelper {
    private static final ThreadLocal<Boolean> DIGGING_BY_MODE =
          ThreadLocal.withInitial(() -> false);

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
    public static InteractionResult digAOE(Level level, Player player, ItemStack stack,
          BlockPos target, Direction faceHit, int radius, boolean flat) {
        if (radius <= 0) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
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
        boolean changed = false;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    changed |= breakBlock(level, serverPlayer, stack, pos);
                }
            }
        }
        return changed ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    /**
     * Breaks a single block as if the player mined it, respecting tool requirements and gamemode.
     * Drops are produced at the block's position. Matter tools are intentionally not damaged.
     */
    private static boolean breakBlock(
          Level level, ServerPlayer player, ItemStack stack, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0) {
            return false; // unbreakable (bedrock, etc.)
        }
        // Respect tool-tier gating: only break blocks the tool can actually harvest.
        if (!stack.isCorrectToolForDrops(state)) {
            return false;
        }
        return player.gameMode.destroyBlock(pos);
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

    public static InteractionResult veinMine(
          Level level, Player player, ItemStack stack, BlockPos origin, int radius) {
        BlockState target = level.getBlockState(origin);
        if (radius < 0 || target.isAir() || !stack.isCorrectToolForDrops(target)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        int limit = Math.min(512, 32 * (radius + 1));
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        pending.add(origin.immutable());
        boolean changed = false;
        while (!pending.isEmpty() && visited.size() < limit) {
            BlockPos pos = pending.removeFirst();
            if (!visited.add(pos) || Math.abs(pos.getX() - origin.getX()) > radius
                  || Math.abs(pos.getY() - origin.getY()) > radius
                  || Math.abs(pos.getZ() - origin.getZ()) > radius) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.is(target.getBlock())) {
                continue;
            }
            changed |= breakBlock(level, serverPlayer, stack, pos);
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x != 0 || y != 0 || z != 0) {
                            pending.add(pos.offset(x, y, z).immutable());
                        }
                    }
                }
            }
        }
        return changed ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    public static InteractionResult clearConnected(
          Level level, Player player, ItemStack stack, BlockPos origin,
          net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> tag, int radius) {
        if (radius <= 0 || !level.getBlockState(origin).is(tag)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        pending.add(origin.immutable());
        boolean changed = false;
        int limit = Math.min(1_024, 64 * (radius + 1));
        while (!pending.isEmpty() && visited.size() < limit) {
            BlockPos pos = pending.removeFirst();
            if (!visited.add(pos) || Math.abs(pos.getX() - origin.getX()) > radius
                  || Math.abs(pos.getY() - origin.getY()) > radius * 2
                  || Math.abs(pos.getZ() - origin.getZ()) > radius) {
                continue;
            }
            if (!level.getBlockState(pos).is(tag)) {
                continue;
            }
            changed |= breakBlock(level, serverPlayer, stack, pos);
            for (Direction direction : Direction.values()) {
                pending.add(pos.relative(direction).immutable());
            }
        }
        return changed ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    public static InteractionResult shearAOE(Player player, ItemStack stack) {
        int charge = charge(stack);
        if (charge <= 0) {
            return InteractionResult.PASS;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        double radius = Math.pow(2, 2 + charge);
        boolean changed = false;
        for (Entity entity : serverLevel.getEntitiesOfClass(
              Entity.class, player.getBoundingBox().inflate(radius, radius / 2.0, radius),
              candidate -> candidate instanceof Shearable shearable
                    && shearable.readyForShearing())) {
            ((Shearable) entity).shear(serverLevel, SoundSource.PLAYERS, stack);
            changed = true;
        }
        return changed ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    public static InteractionResult useAOE(
          UseOnContext context, Item vanillaBehavior, int radius, boolean flat) {
        ItemStack stack = context.getItemInHand();
        int damage = stack.getDamageValue();
        InteractionResult result = vanillaBehavior.useOn(context);
        if (!result.consumesAction() || radius <= 0 || context.getLevel().isClientSide()) {
            stack.setDamageValue(damage);
            return result;
        }
        BlockPos origin = context.getClickedPos();
        int minY = flat ? origin.getY() : origin.getY() - radius;
        int maxY = flat ? origin.getY() : origin.getY() + radius;
        for (int x = origin.getX() - radius; x <= origin.getX() + radius; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = origin.getZ() - radius; z <= origin.getZ() + radius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!pos.equals(origin)) {
                        vanillaBehavior.useOn(adjustedContext(context, pos));
                    }
                }
            }
        }
        stack.setDamageValue(damage);
        return result;
    }

    private static UseOnContext adjustedContext(UseOnContext source, BlockPos pos) {
        BlockPos origin = source.getClickedPos();
        Vec3 location = source.getClickLocation().add(
              pos.getX() - origin.getX(),
              pos.getY() - origin.getY(),
              pos.getZ() - origin.getZ());
        return new UseOnContext(source.getPlayer(), source.getHand(), new BlockHitResult(
              location, source.getClickedFace(), pos, source.isInside()));
    }

    public static void digBasedOnMode(Level level, LivingEntity miner, ItemStack stack,
          BlockPos target, MatterPickaxeItem.PickaxeMode mode) {
        if (level.isClientSide() || mode == MatterPickaxeItem.PickaxeMode.STANDARD
              || DIGGING_BY_MODE.get() || !(miner instanceof ServerPlayer player)) {
            return;
        }
        HitResult hit = player.pick(5.0, 0.0F, false);
        Direction face = miningFace(hit, player.getLookAngle());
        DIGGING_BY_MODE.set(true);
        try {
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
        } finally {
            DIGGING_BY_MODE.remove();
        }
    }

    static Direction miningFace(HitResult hit, Vec3 lookDirection) {
        if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit) {
            return blockHit.getDirection();
        }
        return Direction.getApproximateNearest(lookDirection).getOpposite();
    }

    public static void attackWithCharge(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(attacker instanceof Player player) || !(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int charge = charge(stack);
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

    public static int charge(ItemStack stack) {
        return stack.getItem() instanceof IItemCharge item ? item.getCharge(stack) : 0;
    }
}
