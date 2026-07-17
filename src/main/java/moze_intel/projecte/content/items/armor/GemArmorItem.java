package moze_intel.projecte.content.items.armor;

import moze_intel.projecte.content.ModDataComponents;
import moze_intel.projecte.content.ModArmorMaterials;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.player.PlayerAttachmentKeys;
import moze_intel.projecte.player.PlayerDataService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class GemArmorItem extends MatterArmorItem {
    private static final AttributeModifier STEP_ASSIST_MODIFIER = new AttributeModifier(
          ProjectEAPI.id("gem_step_assist"), 0.4D, AttributeModifier.Operation.ADD_VALUE);
    private static final ItemAttributeModifiers BOOT_MODIFIERS =
          ModArmorMaterials.GEM.createAttributes(ArmorType.BOOTS).withModifierAdded(
                Attributes.MOVEMENT_SPEED,
                new AttributeModifier(ProjectEAPI.id("armor"), 1.0D,
                      AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                EquipmentSlotGroup.FEET);

    private final ArmorType armorType;

    public GemArmorItem(Properties properties, ArmorType armorType) {
        super(properties, armorType, Tier.GEM);
        this.armorType = armorType;
    }

    private static boolean hasAnyPiece(ServerPlayer player) {
        return isPiece(player, EquipmentSlot.HEAD, ArmorType.HELMET)
              || isPiece(player, EquipmentSlot.CHEST, ArmorType.CHESTPLATE)
              || isPiece(player, EquipmentSlot.LEGS, ArmorType.LEGGINGS)
              || isPiece(player, EquipmentSlot.FEET, ArmorType.BOOTS);
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
        }
        updateStepAssist(player, isPiece(boots, ArmorType.BOOTS)
              && boots.getOrDefault(ModDataComponents.STEP_ASSIST, false));
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

    public static void toggleActive(ServerPlayer player) {
        if (!player.getMainHandItem().isEmpty() || !hasAnyPiece(player)) {
            return;
        }
        PlayerDataService service = playerData(player);
        boolean enabled = !service.gemArmorEnabled();
        service.setGemArmor(enabled);
        player.sendOverlayMessage(Component.translatable(
              enabled ? "gem.projecte.activate" : "gem.projecte.deactivate"));
    }

    public static void explode(ServerPlayer player) {
        if (!canUseActiveAbility(player)
              || !isPiece(player, EquipmentSlot.CHEST, ArmorType.CHESTPLATE)) {
            return;
        }
        player.level().explode(
              player, player.getX(), player.getY(), player.getZ(), 9.0F,
              Level.ExplosionInteraction.BLOCK);
    }

    public static void zap(ServerPlayer player) {
        if (!canUseActiveAbility(player)
              || !isPiece(player, EquipmentSlot.HEAD, ArmorType.HELMET)) {
            return;
        }
        HitResult target = player.pick(120.0D, 1.0F, false);
        if (target.getType() == HitResult.Type.MISS) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        LightningBolt lightning = EntityTypes.LIGHTNING_BOLT.create(
              level, EntitySpawnReason.TRIGGERED);
        if (lightning != null) {
            BlockPos strikePos = BlockPos.containing(target.getLocation());
            lightning.setPos(Vec3.atCenterOf(strikePos));
            lightning.setCause(player);
            level.addFreshEntity(lightning);
        }
    }

    private static boolean canUseActiveAbility(ServerPlayer player) {
        return player.getMainHandItem().isEmpty()
              && playerData(player).gemArmorEnabled();
    }

    private static PlayerDataService playerData(ServerPlayer player) {
        return new PlayerDataService(PlayerAttachmentKeys.fabricAdapter(player));
    }

    public static ItemAttributeModifiers bootModifiers() {
        return BOOT_MODIFIERS;
    }

    private static void updateStepAssist(ServerPlayer player, boolean enabled) {
        AttributeInstance stepHeight = player.getAttribute(Attributes.STEP_HEIGHT);
        if (stepHeight == null) {
            return;
        }
        if (enabled) {
            stepHeight.addOrUpdateTransientModifier(STEP_ASSIST_MODIFIER);
        } else {
            stepHeight.removeModifier(STEP_ASSIST_MODIFIER.id());
        }
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
