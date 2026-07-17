package moze_intel.projecte.content.items.armor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import moze_intel.projecte.network.payloads.ArmorTogglePayload;
import org.junit.jupiter.api.Test;

class GemArmorItemTest {
    @Test
    void gemArmorHasFiveServerValidatedActions() {
        assertEquals(5, ArmorTogglePayload.Action.values().length);
    }
}
