package moze_intel.projecte.mixin;

import moze_intel.projecte.content.items.armor.MatterArmorItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
abstract class LivingEntityArmorMixin {
    @ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float projecte$applyMatterArmorReduction(
          float amount, ServerLevel level, DamageSource source) {
        return (Object) this instanceof Player player
              ? MatterArmorItem.reduceDamage(player, source, amount)
              : amount;
    }
}
