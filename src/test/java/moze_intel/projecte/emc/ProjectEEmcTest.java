package moze_intel.projecte.emc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.Test;

class ProjectEEmcTest {
    @Test
    void exposesStableSingletonService() {
        EmcMappingService<NormalizedStackKey> first = ProjectEEmc.service();
        EmcMappingService<NormalizedStackKey> second = ProjectEEmc.service();
        assertNotNull(first);
        assertSame(first, second);
        assertSame(first, ProjectEEmc.service());
    }

    @Test
    void startsEmptyBeforeFirstReload() {
        EmcMappingSnapshot<NormalizedStackKey> snapshot = ProjectEEmc.service().current();
        assertNotNull(snapshot);
        assertSame(EmcMappingSnapshot.<NormalizedStackKey>empty().getClass(), snapshot.getClass());
        // A fresh service is empty and at version zero until the server reloads it.
        org.junit.jupiter.api.Assertions.assertTrue(snapshot.values().isEmpty());
        org.junit.jupiter.api.Assertions.assertEquals(0L, snapshot.version());
    }
}
