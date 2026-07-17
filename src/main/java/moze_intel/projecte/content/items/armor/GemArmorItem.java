package moze_intel.projecte.content.items.armor;

import moze_intel.projecte.content.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.phys.Vec3;

public class GemArmorItem extends MatterArmorItem {
    private final ArmorType armorType;

    public GemArmorItem(Properties properties, ArmorType armorType) {
        super(properties, armorType, Tier.GEM);
        this.armorType = armorType;
    }

    public static boolean hasFullSet(ServerPlayer player) {
        return isPiece(player, EquipmentSlot.HEAD, ArmorType.HELMET)
              && isPiece(player, EquipmentSlot.CHEST, ArmorType.CHESTPLATE)
              && isPiece(player, EquipmentSlot.LEGS, ArmorType.LEGGINGS)
              && isPiece(player, EquipmentSlot.FEET, ArmorType.BOOTS);
    }

    public static void tickPlayer(ServerPlayer player, boolean secondTick) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (isPiece(helmet, ArmorType.HELMET)) {
            if (secondTick && player.getHealth() < player.getMaxHealth()) {
                player.heal(2.0F);
            }
            if (helmet.getOrDefault(ModDataComponents.NIGHT_VISION, false)) {
                addHiddenEffect(player, MobEffects.NIGHT_VISION, 220, 0);
            }
        }

        if (isPiece(player, EquipmentSlot.CHEST, ArmorType.CHESTPLATE)) {
            player.clearFire();
            if (secondTick && player.getFoodData().needsFood()) {
                player.getFoodData().eat(2, 10.0F);
            }
        }

        if (isPiece(player, EquipmentSlot.LEGS, ArmorType.LEGGINGS)
              && player.isSecondaryUseActive()) {
            repelNearbyEntities(player);
        }

        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (isPiece(boots, ArmorType.BOOTS)) {
            player.resetFallDistance();
            addHiddenEffect(player, MobEffects.SPEED, 10, 4);
            addHiddenEffect(player, MobEffects.SLOW_FALLING, 10, 0);
            if (boots.getOrDefault(ModDataComponents.STEP_ASSIST, false)) {
                addHiddenEffect(player, MobEffects.JUMP_BOOST, 10, 1);
            }
        }
    }

    public static void toggleHelmet(ServerPlayer player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!isPiece(helmet, ArmorType.HELMET)) return;
        boolean enabled = !helmet.getOrDefault(ModDataComponents.NIGHT_VISION, false);
        helmet.set(ModDataComponents.NIGHT_VISION, enabled);
        if (!enabled) {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
        player.sendOverlayMessage(Component.translatable(
              "gem.projecte.night_vision",
              Component.translatable(enabled ? "gem.projecte.enabled" : "gem.projecte.disabled")));
    }

    public static void toggleBoots(ServerPlayer player) {
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!isPiece(boots, ArmorType.BOOTS)) return;
        boolean enabled = !boots.getOrDefault(ModDataComponents.STEP_ASSIST, false);
        boots.set(ModDataComponents.STEP_ASSIST, enabled);
        player.sendOverlayMessage(Component.translatable(
              "gem.projecte.step_assist",
              Component.translatable(enabled ? "gem.projecte.enabled" : "gem.projecte.disabled")));
    }

    private static boolean isPiece(ServerPlayer player, EquipmentSlot slot, ArmorType type) {
        return isPiece(player.getItemBySlot(slot), type);
    }

    private static boolean isPiece(ItemStack stack, ArmorType type) {
        return stack.getItem() instanceof GemArmorItem gem && gem.armorType == type;
    }

    private static void addHiddenEffect(
          ServerPlayer player,
          net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
          int duration,
          int amplifier) {
        player.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false));
    }

    private static void repelNearbyEntities(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        for (LivingEntity entity : level.getEntitiesOfClass(
              LivingEntity.class, player.getBoundingBox().inflate(3.5D),
              target -> target != player && target.isAlive() && !target.isAlliedTo(player))) {
            Vec3 away = entity.position().subtract(player.position());
            if (away.lengthSqr() > 0.0001D) {
                away = away.normalize();
                entity.push(away.x * 0.35D, 0.1D, away.z * 0.35D);
            }
        }
    }
}
