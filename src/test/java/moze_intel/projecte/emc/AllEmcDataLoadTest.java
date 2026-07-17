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
 * Loads every data/projecte/emc/*.json file through ExplicitEmcLoader exactly as the live reload
 * does, confirming the explicit EMC anchors parse cleanly. Craftable vanilla items are deliberately
 * absent here because the live recipe graph derives their values.
 */
class AllEmcDataLoadTest {
    @Test
    void allEmcDataFilesLoadCleanly() throws Exception {
        Map<Identifier, String> resources = new HashMap<>();
        for (String name : new String[]{"projecte", "projecte_items", "vanilla"}) {
            Path file = Path.of("src/main/resources/data/projecte/emc/" + name + ".json");
            resources.put(Identifier.fromNamespaceAndPath("projecte", "emc/" + name),
                  Files.readString(file, StandardCharsets.UTF_8));
        }
        List<ExplicitEmcEntry> entries = new ExplicitEmcLoader().load(resources);
        assertTrue(entries.size() >= 720,
              "expected >= 720 explicit EMC anchors across all data files, got " + entries.size());
        // Confirm at least one projecte tool and one vanilla item resolved.
        assertTrue(entries.stream().anyMatch(e -> e.key().canonicalString().equals("item|projecte:dm_pick|{}")));
        assertTrue(entries.stream().anyMatch(e -> e.key().canonicalString().equals("item|minecraft:cobblestone|{}")));
    }
}
