package moze_intel.projecte.content.items;

import moze_intel.projecte.content.items.tools.ToolHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Red Matter Morning Star — combines the hammer's block AoE with the katar's combat AoE. Right-
 * click breaks a cube around the targeted block; attacking a foe damages nearby hostiles.
 */
public class RedMatterMorningStarItem extends Item implements IItemCharge {
    private static final int MAX_CHARGE = 4;

    public RedMatterMorningStarItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getMaxCharge(ItemStack stack) {
        return MAX_CHARGE;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = player.pick(5.0, 0.0f, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }
        BlockPos target = blockHit.getBlockPos();
        Direction face = blockHit.getDirection();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        int radius = getCharge(stack);
        BlockState state = level.getBlockState(target);
        if (!state.isAir() && stack.isCorrectToolForDrops(state)) {
            level.destroyBlock(target, true, player);
        }
        ToolHelper.digAOE(level, player, stack, hand, target, face, radius, false);
        if (stack.isDamageableItem()) {
            stack.hurtAndBreak(1, player, hand);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        if (!target.level().isClientSide() && attacker instanceof Player player) {
            int charge = getCharge(stack);
            float radius = 3.0F + charge;
            for (LivingEntity nearby : target.level().getEntitiesOfClass(
                  LivingEntity.class, target.getBoundingBox().inflate(radius),
                  e -> e != target && e != player && e.isAlive() && !e.isAlliedTo(player))) {
                nearby.invulnerableTime = 0;
                nearby.hurt(target.damageSources().playerAttack(player), 6.0F + charge * 2.0F);
            }
        }
    }
}
