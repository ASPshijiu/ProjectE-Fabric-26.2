package moze_intel.projecte.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Optional;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.FakeStackKey;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.player.PlayerAttachmentAccess;
import moze_intel.projecte.player.PlayerAttachmentKeys;
import moze_intel.projecte.player.PlayerDataService;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class EmcCommandTest {
    private PlayerDataService service() {
        return new PlayerDataService(PlayerAttachmentAccess.inMemory(PlayerAttachmentKeys::initialValue));
    }

    @Test
    void setEmcSetsBalance() {
        PlayerDataService svc = service();
        EmcCommand.setEmc(svc, EmcValue.of(100));
        assertEquals(EmcValue.of(100), svc.emc());
    }

    @Test
    void addEmcAccumulates() {
        PlayerDataService svc = service();
        EmcCommand.setEmc(svc, EmcValue.of(50));
        EmcCommand.addEmc(svc, EmcValue.of(25));
        assertEquals(EmcValue.of(75), svc.emc());
    }

    @Test
    void addEmcOverflowsIsRejected() {
        PlayerDataService svc = service();
        EmcCommand.setEmc(svc, EmcValue.of(Long.MAX_VALUE - 1));
        assertThrows(ArithmeticException.class, () -> EmcCommand.addEmc(svc, EmcValue.of(5)));
        assertEquals(EmcValue.of(Long.MAX_VALUE - 1), svc.emc());
    }

    @Test
    void removeEmcUnderflowIsRejected() {
        PlayerDataService svc = service();
        EmcCommand.setEmc(svc, EmcValue.of(10));
        assertThrows(ArithmeticException.class, () -> EmcCommand.removeEmc(svc, EmcValue.of(20)));
        assertEquals(EmcValue.of(10), svc.emc());
    }

    @Test
    void queryEmcReturnsCurrent() {
        PlayerDataService svc = service();
        EmcCommand.setEmc(svc, EmcValue.of(42));
        Optional<EmcValue> read = EmcCommand.queryEmc(svc);
        assertEquals(EmcValue.of(42), read.orElseThrow());
    }

    @Test
    void testEmcReportsTrueWhenSufficient() {
        PlayerDataService svc = service();
        EmcCommand.setEmc(svc, EmcValue.of(100));
        assertTrue(EmcCommand.hasAtLeast(svc, EmcValue.of(50)));
        assertTrue(EmcCommand.hasAtLeast(svc, EmcValue.of(100)));
        org.junit.jupiter.api.Assertions.assertFalse(EmcCommand.hasAtLeast(svc, EmcValue.of(101)));
    }

    @Test
    void rejectsNegativeEmcArgument() {
        PlayerDataService svc = service();
        assertThrows(IllegalArgumentException.class, () -> EmcCommand.setEmc(svc, EmcValue.of(-1)));
    }

    private static NormalizedStackKey unusedKeyReference() {
        return new FakeStackKey(Identifier.fromNamespaceAndPath("projecte", "placeholder"));
    }
}
