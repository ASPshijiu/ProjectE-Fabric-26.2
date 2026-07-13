package moze_intel.projecte.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.lang.reflect.Field;

/**
 * Nova Catalyst — explodes with power 8 (2× TNT).
 */
public class NovaCatalystBlock extends TntBlock {
    private static final float EXPLOSION_POWER = 8.0F;
    private static final Field EXPLOSION_POWER_FIELD = findField();

    private static Field findField() {
        try {
            Field f = PrimedTnt.class.getDeclaredField("explosionPower");
            f.setAccessible(true);
            return f;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Cannot access PrimedTnt.explosionPower", e);
        }
    }

    public NovaCatalystBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void wasExploded(ServerLevel level, BlockPos pos, net.minecraft.world.level.Explosion explosion) {
        spawnNova(level, pos, null);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                          BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.isEmpty() && (stack.getItem() == Items.FLINT_AND_STEEL || stack.getItem() == Items.FIRE_CHARGE)) {
            if (level instanceof ServerLevel serverLevel) {
                spawnNova(serverLevel, pos, player);
                level.removeBlock(pos, false);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    private void spawnNova(ServerLevel level, BlockPos pos, LivingEntity igniter) {
        PrimedTnt tnt = new PrimedTnt(level,
              pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, igniter);
        tnt.setFuse(40);
        try { EXPLOSION_POWER_FIELD.setFloat(tnt, EXPLOSION_POWER); }
        catch (IllegalAccessException ignored) {}
        level.addFreshEntity(tnt);
    }
}
