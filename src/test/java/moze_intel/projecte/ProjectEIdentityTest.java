package moze_intel.projecte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import moze_intel.projecte.api.ProjectEAPI;
import org.junit.jupiter.api.Test;

class ProjectEIdentityTest {
    @Test
    void keepsStableProjectEIdentity() {
        assertEquals("projecte", ProjectEAPI.MOD_ID);
        assertEquals("projecte:test", ProjectEAPI.id("test").toString());
    }
}
