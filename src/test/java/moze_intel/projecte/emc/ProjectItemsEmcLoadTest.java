package moze_intel.projecte.emc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import moze_intel.projecte.emc.data.ExplicitEmcEntry;
import moze_intel.projecte.emc.data.ExplicitEmcLoader;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

/**
 * Loads the actual projecte_items.json data file through ExplicitEmcLoader to confirm it parses
 * without error and produces entries whose keys round-trip through the tooltip lookup path.
 */
class ProjectItemsEmcLoadTest {
    @Test
    void projectItemsJsonParsesAndKeysMatchTooltipLookup() throws Exception {
        Path file = Path.of("src/main/resources/data/projecte/emc/projecte_items.json");
        String json = Files.readString(file, StandardCharsets.UTF_8);
        Map<Identifier, String> resources = new HashMap<>();
        resources.put(Identifier.fromNamespaceAndPath("projecte", "emc/projecte_items"), json);

        ExplicitEmcLoader loader = new ExplicitEmcLoader();
        List<ExplicitEmcEntry> entries = loader.load(resources);
        assertTrue(entries.size() >= 50, "expected >= 50 projecte items, got " + entries.size());

        // Verify a representative entry: dm_pick should have a positive EMC and be a lookup-able ItemStackKey.
        ExplicitEmcEntry dmPick = entries.stream()
              .filter(e -> e.key().canonicalString().equals("item|projecte:dm_pick|{}"))
              .findFirst().orElseThrow();
        assertTrue(dmPick.value().longValue() > 0, "dm_pick EMC must be positive");

        // The loaded key must equal the tooltip-built key (empty components).
        ItemStackKey tooltipKey = new ItemStackKey(
              Identifier.fromNamespaceAndPath("projecte", "dm_pick"), Map.of());
        assertTrue(dmPick.key().equals(tooltipKey),
              "loaded key must equal the tooltip lookup key");
    }
}
