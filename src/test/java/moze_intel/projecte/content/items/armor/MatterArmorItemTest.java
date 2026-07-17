package moze_intel.projecte.content.items.armor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.item.equipment.ArmorType;
import org.junit.jupiter.api.Test;

class MatterArmorItemTest {
    @Test
    void armorSlotWeightsAddUpToAFullSet() {
        float fullSet = MatterArmorItem.pieceEffectiveness(ArmorType.HELMET)
              + MatterArmorItem.pieceEffectiveness(ArmorType.CHESTPLATE)
              + MatterArmorItem.pieceEffectiveness(ArmorType.LEGGINGS)
              + MatterArmorItem.pieceEffectiveness(ArmorType.BOOTS);
        assertEquals(1.0F, fullSet, 0.0001F);
    }

    @Test
    void officialFullSetReductionTiersArePreserved() {
        assertEquals(0.8F, MatterArmorItem.Tier.DARK_MATTER.fullSetReduction(), 0.0001F);
        assertEquals(0.9F, MatterArmorItem.Tier.RED_MATTER.fullSetReduction(), 0.0001F);
        assertEquals(0.9F, MatterArmorItem.Tier.GEM.fullSetReduction(), 0.0001F);
    }
}
