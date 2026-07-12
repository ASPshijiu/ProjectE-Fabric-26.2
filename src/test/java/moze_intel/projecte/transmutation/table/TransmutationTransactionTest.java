package moze_intel.projecte.transmutation.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Map;
import moze_intel.projecte.emc.EmcMappingSnapshot;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.FakeStackKey;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.player.PlayerAttachmentAccess;
import moze_intel.projecte.player.PlayerAttachmentKeys;
import moze_intel.projecte.player.PlayerDataService;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class TransmutationTransactionTest {
    private static NormalizedStackKey key(String path) {
        return new FakeStackKey(Identifier.fromNamespaceAndPath("projecte", path));
    }

    private PlayerDataService service() {
        return new PlayerDataService(PlayerAttachmentAccess.inMemory(PlayerAttachmentKeys::initialValue));
    }

    @Test
    void extractsKnownAffordableItemAndSpendsEmc() {
        NormalizedStackKey iron = key("iron");
        EmcMappingSnapshot<NormalizedStackKey> snap = new EmcMappingSnapshot<>(1, Map.of(iron, EmcValue.of(256)));
        PlayerDataService svc = service();
        svc.learn(iron);
        svc.setEmc(EmcValue.of(1000));

        TransmutationTransaction.Outcome outcome = TransmutationTransaction.extract(
              svc, snap, iron, 3, 64);

        assertTrue(outcome.success());
        assertEquals(3, outcome.producedCount());
        assertEquals(EmcValue.of(1000 - 256 * 3), svc.emc());
    }

    @Test
    void clampsCountToStackSize() {
        NormalizedStackKey item = key("item");
        EmcMappingSnapshot<NormalizedStackKey> snap = new EmcMappingSnapshot<>(1, Map.of(item, EmcValue.of(1)));
        PlayerDataService svc = service();
        svc.learn(item);
        svc.setEmc(EmcValue.of(Long.MAX_VALUE));

        TransmutationTransaction.Outcome outcome = TransmutationTransaction.extract(
              svc, snap, item, 100, 16);

        assertTrue(outcome.success());
        assertEquals(16, outcome.producedCount());
    }

    @Test
    void rejectsUnknownItem() {
        NormalizedStackKey item = key("item");
        EmcMappingSnapshot<NormalizedStackKey> snap = new EmcMappingSnapshot<>(1, Map.of(item, EmcValue.of(1)));
        PlayerDataService svc = service();
        svc.setEmc(EmcValue.of(Long.MAX_VALUE));

        TransmutationTransaction.Outcome outcome = TransmutationTransaction.extract(
              svc, snap, item, 1, 64);

        assertFalse(outcome.success());
        assertEquals(0, outcome.producedCount());
        assertEquals(EmcValue.of(Long.MAX_VALUE), svc.emc(), "EMC must not change on failure");
    }

    @Test
    void rejectsItemWithNoEmcMapping() {
        NormalizedStackKey item = key("item");
        EmcMappingSnapshot<NormalizedStackKey> snap = new EmcMappingSnapshot<>(1, Map.of());
        PlayerDataService svc = service();
        svc.learn(item);

        TransmutationTransaction.Outcome outcome = TransmutationTransaction.extract(
              svc, snap, item, 1, 64);

        assertFalse(outcome.success());
    }

    @Test
    void insufficientEmcProducesAffordablePartialCount() {
        NormalizedStackKey item = key("item");
        EmcMappingSnapshot<NormalizedStackKey> snap = new EmcMappingSnapshot<>(1, Map.of(item, EmcValue.of(10)));
        PlayerDataService svc = service();
        svc.learn(item);
        svc.setEmc(EmcValue.of(25));

        TransmutationTransaction.Outcome outcome = TransmutationTransaction.extract(
              svc, snap, item, 5, 64);

        assertTrue(outcome.success());
        assertEquals(2, outcome.producedCount(), "only 2 affordable (25/10)");
        assertEquals(EmcValue.of(5), svc.emc());
    }

    @Test
    void totalInsufficiencyRejects() {
        NormalizedStackKey item = key("item");
        EmcMappingSnapshot<NormalizedStackKey> snap = new EmcMappingSnapshot<>(1, Map.of(item, EmcValue.of(100)));
        PlayerDataService svc = service();
        svc.learn(item);
        svc.setEmc(EmcValue.of(50));

        TransmutationTransaction.Outcome outcome = TransmutationTransaction.extract(
              svc, snap, item, 1, 64);

        assertFalse(outcome.success());
        assertEquals(EmcValue.of(50), svc.emc(), "EMC must not change when nothing is produced");
    }

    @Test
    void rejectsZeroOrNegativeCount() {
        NormalizedStackKey item = key("item");
        EmcMappingSnapshot<NormalizedStackKey> snap = new EmcMappingSnapshot<>(1, Map.of(item, EmcValue.of(1)));
        PlayerDataService svc = service();
        svc.learn(item);

        assertFalse(TransmutationTransaction.extract(svc, snap, item, -1, 64).success());
        TransmutationTransaction.Outcome zero = TransmutationTransaction.extract(svc, snap, item, 0, 64);
        assertFalse(zero.success());
    }
}
