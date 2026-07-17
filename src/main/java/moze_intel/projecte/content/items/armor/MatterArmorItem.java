package moze_intel.projecte.content.items.armor;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;

/** ProjectE armor with slot-weighted damage reduction and no durability loss. */
public class MatterArmorItem extends Item {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
          EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private final ArmorType armorType;
    private final Tier tier;

    public MatterArmorItem(Properties properties, ArmorType armorType, Tier tier) {
        super(properties);
        this.armorType = armorType;
        this.tier = tier;
    }

    @Override
    public void inventoryTick(
          ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot equipmentSlot) {
        if (equipmentSlot == armorType.getSlot() && stack.getDamageValue() != 0) {
            stack.setDamageValue(0);
        }
    }

    public Reduction reduction(DamageSource source) {
        float effectiveness = pieceEffectiveness(armorType);
        float maximum;
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            maximum = tier.explosionAbsorb;
        } else if (armorType == ArmorType.BOOTS && source.is(DamageTypeTags.IS_FALL)) {
            maximum = tier.fallAbsorb / effectiveness;
        } else if (armorType == ArmorType.HELMET && source.is(DamageTypeTags.IS_DROWNING)) {
            maximum = tier.drowningAbsorb / effectiveness;
        } else if (source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            return Reduction.NONE;
        } else {
            maximum = armorType == ArmorType.HELMET || armorType == ArmorType.BOOTS
                  ? tier.outerPieceAbsorb : tier.innerPieceAbsorb;
        }
        return new Reduction(tier.fullSetReduction * effectiveness, maximum * effectiveness);
    }

    public static float reduceDamage(Player player, DamageSource source, float amount) {
        float reduction = 0.0F;
        float maximum = 0.0F;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.getItem() instanceof MatterArmorItem armor) {
                Reduction piece = armor.reduction(source);
                reduction += piece.percent;
                maximum += piece.maximum;
            }
        }
        float absorbed = Math.min(amount * Math.min(1.0F, reduction), maximum);
        return Math.max(0.0F, amount - absorbed);
    }

    public static float pieceEffectiveness(ArmorType type) {
        return type == ArmorType.HELMET || type == ArmorType.BOOTS ? 0.2F : 0.3F;
    }

    public enum Tier {
        DARK_MATTER(0.8F, 350.0F, 100.0F, 150.0F, 5.0F, 5.0F),
        RED_MATTER(0.9F, 500.0F, 250.0F, 350.0F, 10.0F, 10.0F),
        GEM(0.9F, 750.0F, 400.0F, 500.0F, 15.0F, 15.0F);

        private final float fullSetReduction;
        private final float explosionAbsorb;
        private final float outerPieceAbsorb;
        private final float innerPieceAbsorb;
        private final float fallAbsorb;
        private final float drowningAbsorb;

        Tier(float fullSetReduction, float explosionAbsorb, float outerPieceAbsorb,
              float innerPieceAbsorb, float fallAbsorb, float drowningAbsorb) {
            this.fullSetReduction = fullSetReduction;
            this.explosionAbsorb = explosionAbsorb;
            this.outerPieceAbsorb = outerPieceAbsorb;
            this.innerPieceAbsorb = innerPieceAbsorb;
            this.fallAbsorb = fallAbsorb;
            this.drowningAbsorb = drowningAbsorb;
        }

        public float fullSetReduction() {
            return fullSetReduction;
        }
    }

    public record Reduction(float percent, float maximum) {
        private static final Reduction NONE = new Reduction(0.0F, 0.0F);
    }
}
