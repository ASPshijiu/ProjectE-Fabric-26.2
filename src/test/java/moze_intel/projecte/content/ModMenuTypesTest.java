package moze_intel.projecte.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link ModMenuTypes} registration hub's stable identifiers. Actual MenuType
 * construction requires the pre-freeze registry phase and is runtime-verified.
 */
class ModMenuTypesTest {
    @Test
    void transmutationTableIdMatchesExpectedPath() {
        assertNotNull(ModMenuTypes.TRANSMUTATION_TABLE_ID);
        assertEquals("projecte", ModMenuTypes.TRANSMUTATION_TABLE_ID.getNamespace());
        assertEquals("transmutation_table", ModMenuTypes.TRANSMUTATION_TABLE_ID.getPath());
    }

    @Test
    void condenserMk1IdMatchesExpectedPath() {
        assertNotNull(ModMenuTypes.CONDENSER_MK1_ID);
        assertEquals("projecte", ModMenuTypes.CONDENSER_MK1_ID.getNamespace());
        assertEquals("condenser_mk1", ModMenuTypes.CONDENSER_MK1_ID.getPath());
    }

    @Test
    void condenserMk2IdMatchesExpectedPath() {
        assertNotNull(ModMenuTypes.CONDENSER_MK2_ID);
        assertEquals("projecte", ModMenuTypes.CONDENSER_MK2_ID.getNamespace());
        assertEquals("condenser_mk2", ModMenuTypes.CONDENSER_MK2_ID.getPath());
    }

    @Test
    void collectorIdsMatchExpectedPaths() {
        assertEquals("collector_mk1", ModMenuTypes.COLLECTOR_MK1_ID.getPath());
        assertEquals("collector_mk2", ModMenuTypes.COLLECTOR_MK2_ID.getPath());
        assertEquals("collector_mk3", ModMenuTypes.COLLECTOR_MK3_ID.getPath());
    }

    @Test
    void hubClassIsLoadable() {
        assertNotNull(ModMenuTypes.class);
    }
}
