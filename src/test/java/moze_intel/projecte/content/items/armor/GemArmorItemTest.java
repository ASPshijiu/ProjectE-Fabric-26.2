package moze_intel.projecte.content.items.armor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import moze_intel.projecte.network.payloads.ArmorTogglePayload;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GemArmorItemTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
    }

    @Test
    void gemArmorHasFiveServerValidatedActions() {
        assertEquals(5, ArmorTogglePayload.Action.values().length);
    }

    @Test
    void gemBootsDoubleBaseMovementSpeed() {
        assertEquals(0.2D, GemArmorItem.bootModifiers().compute(
              Attributes.MOVEMENT_SPEED, 0.1D, EquipmentSlot.FEET), 0.000_001D);
    }

    @Test
    void offensiveAbilitiesHaveServerCooldowns() {
        assertTrue(GemArmorItem.EXPLOSION_COOLDOWN_TICKS > 0);
        assertTrue(GemArmorItem.ZAP_COOLDOWN_TICKS > 0);
    }
}
