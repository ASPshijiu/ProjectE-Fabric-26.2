package moze_intel.projecte.emc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the EMC lookup path used by the client tooltip handler actually resolves a value:
 * build a snapshot the way ExplicitEmcLoader does (key from the canonical "{@code item|ns:path|{}}"
 * string), then look it up the way the tooltip handler does
 * ({@code new ItemStackKey(identifier, Map.of())}). This is the load→snapshot→lookup round-trip
 * that must hold for EMC tooltips to display; if it fails the tooltip silently shows nothing.
 */
class EmcTooltipLookupTest {

    @Test
    void tooltipLookupKeyMatchesDataFileKey() {
        // Server side: ExplicitEmcLoader parses "item|projecte:dark_matter|{}" into a key.
        NormalizedStackKey loadedKey = NormalizedStackKeyCodec.fromCanonical("item|projecte:dark_matter|{}");
        Map<NormalizedStackKey, EmcValue> values = new HashMap<>();
        values.put(loadedKey, EmcValue.of(139264));

        EmcMappingSnapshot<NormalizedStackKey> snapshot = new EmcMappingSnapshot<>(1, values);

        // Client side: the tooltip handler builds the key from the item's registry identifier.
        net.minecraft.resources.Identifier identifier =
              net.minecraft.resources.Identifier.fromNamespaceAndPath("projecte", "dark_matter");
        ItemStackKey tooltipKey = new ItemStackKey(identifier, Map.of());

        // The lookup must resolve to the data-file value.
        assertTrue(snapshot.valueFor(tooltipKey).isPresent(),
              "tooltip lookup key must match the data-file key");
        assertEquals(EmcValue.of(139264), snapshot.valueFor(tooltipKey).orElseThrow());
    }

    @Test
    void tooltipLookupKeyMatchesForVanillaItem() {
        NormalizedStackKey loadedKey = NormalizedStackKeyCodec.fromCanonical("item|minecraft:cobblestone|{}");
        Map<NormalizedStackKey, EmcValue> values = new HashMap<>();
        values.put(loadedKey, EmcValue.of(1));

        EmcMappingSnapshot<NormalizedStackKey> snapshot = new EmcMappingSnapshot<>(1, values);

        net.minecraft.resources.Identifier identifier =
              net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "cobblestone");
        ItemStackKey tooltipKey = new ItemStackKey(identifier, Map.of());

        assertTrue(snapshot.valueFor(tooltipKey).isPresent());
        assertEquals(EmcValue.of(1), snapshot.valueFor(tooltipKey).orElseThrow());
    }

    @Test
    void replacePublishesValuesForClientLookup() {
        // Simulate the client receiver: ProjectEEmc.service().replace(values).
        EmcMappingService<NormalizedStackKey> service = new EmcMappingService<>();
        NormalizedStackKey key = NormalizedStackKeyCodec.fromCanonical("item|projecte:aeternalis_fuel|{}");
        service.replace(Map.of(key, EmcValue.of(8192)));

        net.minecraft.resources.Identifier identifier =
              net.minecraft.resources.Identifier.fromNamespaceAndPath("projecte", "aeternalis_fuel");
        ItemStackKey tooltipKey = new ItemStackKey(identifier, Map.of());

        assertTrue(service.current().valueFor(tooltipKey).isPresent(),
              "after replace() the client lookup must find the value");
        assertEquals(EmcValue.of(8192), service.current().valueFor(tooltipKey).orElseThrow());
    }
}
