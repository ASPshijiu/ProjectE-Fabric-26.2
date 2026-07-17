package moze_intel.projecte.content.items.tools;

import moze_intel.projecte.content.items.IExtraFunction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MatterSwordItem extends MatterToolItem implements IExtraFunction {
    public MatterSwordItem(Properties properties, int maxCharge, float chargeSpeedModifier) {
        super(properties, maxCharge, chargeSpeedModifier);
    }

    protected boolean slayAll(ItemStack stack) {
        return false;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        ToolHelper.attackWithCharge(stack, target, attacker);
    }

    @Override
    public void doExtraFunction(Player player, ItemStack stack, InteractionHand hand) {
        if (player.getAttackStrengthScale(0.0F) < 1.0F) {
            return;
        }
        ToolHelper.attackAOE(stack, player, slayAll(stack),
              (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE));
        player.resetAttackStrengthTicker();
    }
}
