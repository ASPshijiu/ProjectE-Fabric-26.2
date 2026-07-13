package moze_intel.projecte.content.items;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Red Matter Katar — a fast melee weapon that applies bonus damage scaled by charge and attacks in
 * an area around slain targets. {@link #hurtEnemy} deals the charge-scaled bonus to nearby hostiles
 * within a small radius.
 */
public class RedMatterKatarItem extends Item implements IItemCharge {
    private static final int MAX_CHARGE = 2;

    public RedMatterKatarItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getMaxCharge(ItemStack stack) {
        return MAX_CHARGE;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        if (!target.level().isClientSide() && attacker instanceof Player player) {
            int charge = getCharge(stack);
            float radius = 2.0F + charge;
            for (LivingEntity nearby : target.level().getEntitiesOfClass(
                  LivingEntity.class, target.getBoundingBox().inflate(radius),
                  e -> e != target && e != player && e.isAlive() && !e.isAlliedTo(player))) {
                nearby.invulnerableTime = 0;
                nearby.hurt(target.damageSources().playerAttack(player), 4.0F + charge * 2.0F);
            }
        }
    }
}
