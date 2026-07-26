package moze_intel.projecte.content.blocks;

import java.lang.reflect.Field;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Shared base for the Nova Catalyst/Cataclysm explosives.
 *
 * <p>Vanilla {@link TntBlock} ignites through several paths (redstone via
 * {@code onPlace}/{@code neighborChanged}, burning projectiles via {@code onProjectileHit},
 * and unstable breaking via {@code playerWillDestroy}) that all funnel into the static
 * {@code prime()} helper and therefore spawn an ordinary power-4 TNT. Every path is overridden
 * here so a Nova block always detonates with its own power.
 */
public abstract class NovaBlock extends TntBlock {
    private static final Field EXPLOSION_POWER_FIELD = findPowerField();

    private static Field findPowerField() {
        try {
            Field field = PrimedTnt.class.getDeclaredField("explosionPower");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Cannot access PrimedTnt.explosionPower", e);
        }
    }

    protected NovaBlock(Properties properties) {
        super(properties);
    }

    /** @return the explosion power of this block (vanilla TNT is 4). */
    protected abstract float explosionPower();

    /** @return the fuse in ticks before detonation. */
    protected int fuseTicks() {
        return 40;
    }

    @Override
    public void wasExploded(ServerLevel level, BlockPos pos, net.minecraft.world.level.Explosion explosion) {
        spawnNova(level, pos, null);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
          BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.isEmpty()
              && (stack.getItem() == Items.FLINT_AND_STEEL || stack.getItem() == Items.FIRE_CHARGE)) {
            primeNova(level, pos, player);
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
          boolean movedByPiston) {
        if (!oldState.is(state.getBlock()) && level.hasNeighborSignal(pos)) {
            primeNova(level, pos, null);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
          Orientation orientation, boolean movedByPiston) {
        if (level.hasNeighborSignal(pos)) {
            primeNova(level, pos, null);
        }
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit,
          Projectile projectile) {
        if (level instanceof ServerLevel serverLevel && projectile.isOnFire()
              && projectile.mayInteract(serverLevel, hit.getBlockPos())) {
            primeNova(level, hit.getBlockPos(),
                  projectile.getOwner() instanceof LivingEntity living ? living : null);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !player.getAbilities().instabuild && state.getValue(UNSTABLE)) {
            primeNova(level, pos, player);
        }
        // Hand super a state with UNSTABLE cleared so TntBlock does not additionally spawn a
        // vanilla power-4 TNT; the remaining behaviour (particles, game event) is unchanged.
        return super.playerWillDestroy(level, pos, state.setValue(UNSTABLE, false), player);
    }

    /** Removes the block and spawns a Nova-powered primed entity, mirroring {@code TntBlock.prime}. */
    protected void primeNova(Level level, BlockPos pos, LivingEntity igniter) {
        if (level.isClientSide()) {
            return;
        }
        if (level instanceof ServerLevel serverLevel
              && !serverLevel.getGameRules().get(GameRules.TNT_EXPLODES)) {
            return;
        }
        PrimedTnt tnt = spawnNova(level, pos, igniter);
        level.playSound(null, tnt.getX(), tnt.getY(), tnt.getZ(),
              SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(igniter, GameEvent.PRIME_FUSE, pos);
        level.removeBlock(pos, false);
    }

    private PrimedTnt spawnNova(Level level, BlockPos pos, LivingEntity igniter) {
        PrimedTnt tnt = new PrimedTnt(level,
              pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, igniter);
        tnt.setFuse(fuseTicks());
        try {
            EXPLOSION_POWER_FIELD.setFloat(tnt, explosionPower());
        } catch (IllegalAccessException ignored) {
            // Falls back to vanilla power; never fatal.
        }
        level.addFreshEntity(tnt);
        return tnt;
    }
}
