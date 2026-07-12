package moze_intel.projecte.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import moze_intel.projecte.emc.EmcMappingSnapshot;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.FakeStackKey;
import moze_intel.projecte.emc.NormalizedStackKey;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class PlayerSyncHandlersTest {
    private PlayerDataService service() {
        return new PlayerDataService(PlayerAttachmentAccess.inMemory(PlayerAttachmentKeys::initialValue));
    }

    @Test
    void onPlayerJoinInitializesAllAttachmentsToDefaults() {
        // The in-memory access lazily initializes on first read; onPlayerJoin must read every
        // attachment so they exist (mirroring Fabric's auto-sync-on-attach behavior).
        PlayerDataService access = service();
        List<String> touched = new ArrayList<>();
        PlayerSyncHandlers handlers = new PlayerSyncHandlers(snapshot -> { });
        handlers.onPlayerJoin(new PlayerDataService(PlayerAttachmentAccess.inMemory(key -> {
            touched.add(key.toString());
            return PlayerAttachmentKeys.initialValue(key);
        })));
        assertTrue(touched.contains(PlayerAttachmentKeys.EMC));
        assertTrue(touched.contains(PlayerAttachmentKeys.KNOWLEDGE));
        assertTrue(touched.contains(PlayerAttachmentKeys.INPUT_LOCKS));
        assertTrue(touched.contains(PlayerAttachmentKeys.GEM_ARMOR));
    }

    @Test
    void onPlayerJoinDoesNotMutatePersonalState() {
        PlayerDataService access = service();
        access.setEmc(EmcValue.of(42));
        new PlayerSyncHandlers(snapshot -> { }).onPlayerJoin(access);
        assertEquals(EmcValue.of(42), access.emc());
    }

    @Test
    void onEmcReloadedFansOutSnapshotWithoutTouchingPersonalEmc() {
        PlayerDataService access = service();
        access.setEmc(EmcValue.of(7));
        FakeStackKey shared = new FakeStackKey(Identifier.fromNamespaceAndPath("projecte", "diamond"));
        EmcMappingSnapshot<NormalizedStackKey> snapshot = new EmcMappingSnapshot<>(1L, Map.of(shared, EmcValue.of(8192)));

        List<EmcMappingSnapshot<NormalizedStackKey>> received = new ArrayList<>();
        PlayerSyncHandlers handlers = new PlayerSyncHandlers(received::add);
        handlers.onEmcReloaded(snapshot);

        assertEquals(List.of(snapshot), received);
        // personal EMC untouched
        assertEquals(EmcValue.of(7), access.emc());
    }
}
