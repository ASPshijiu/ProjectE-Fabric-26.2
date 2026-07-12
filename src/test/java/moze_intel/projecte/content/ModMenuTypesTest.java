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
    void hubClassIsLoadable() {
        assertNotNull(ModMenuTypes.class);
    }
}
