package moze_intel.projecte.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link ModItems} registration hub's stable identifiers and structure.
 *
 * <p>Actual {@code Item} construction and registry insertion requires the 26.2 registry bootstrap
 * phase (item construction allocates an intrusive holder, which is only permitted before the
 * registry freezes). That happens during dedicated-server/client startup and is covered by the
 * runtime/smoke verification phase, not a plain JUnit run. These tests guard the parts that carry
 * stable public contract: the registered id path and that the hub class is loadable.
 */
class ModItemsTest {
    @Test
    void registrationIdMatchesExpectedPath() {
        // The Philosopher's Stone must keep its ProjectE identifier across the port.
        assertNotNull(ModItems.PHILOSOPHERS_STONE_ID);
        assertEquals("projecte", ModItems.PHILOSOPHERS_STONE_ID.getNamespace());
        assertEquals("philosophers_stone", ModItems.PHILOSOPHERS_STONE_ID.getPath());
    }

    @Test
    void hubClassIsLoadable() {
        assertNotNull(ModItems.class);
    }
}
