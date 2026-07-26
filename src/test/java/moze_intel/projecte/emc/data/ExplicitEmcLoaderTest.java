package moze_intel.projecte.emc.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import moze_intel.projecte.emc.EmcValue;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class ExplicitEmcLoaderTest {
    private final ExplicitEmcLoader loader = new ExplicitEmcLoader();

    @Test
    void loadsResourcesAndKeysDeterministically() {
        Identifier later = Identifier.fromNamespaceAndPath("projecte", "emc/z_later.json");
        Identifier earlier = Identifier.fromNamespaceAndPath("projecte", "emc/a_earlier.json");
        Map<Identifier, String> resources = new LinkedHashMap<>();
        resources.put(later, """
              {"item|minecraft:diamond|{}":{"value":8192}}
              """);
        resources.put(earlier, """
              {
                "tag|c:gems/diamond|":{"value":16,"phase":"after"},
                "item|minecraft:dirt|{}":{"value":1}
              }
              """);

        List<ExplicitEmcEntry> entries = loader.load(resources);

        assertEquals(List.of(
              "item|minecraft:dirt|{}:1:BEFORE:projecte:emc/a_earlier.json",
              "tag|c:gems/diamond|:16:AFTER:projecte:emc/a_earlier.json",
              "item|minecraft:diamond|{}:8192:BEFORE:projecte:emc/z_later.json"
        ), entries.stream().map(ExplicitEmcEntry::debugString).toList());
    }

    @Test
    void laterResourceOverridesDuplicateKeyFromEarlierResource() {
        Map<Identifier, String> resources = Map.of(
              Identifier.fromNamespaceAndPath("projecte", "emc/one.json"),
              "{\"item|minecraft:stone|{}\":{\"value\":1}}",
              Identifier.fromNamespaceAndPath("projecte", "emc/two.json"),
              "{\"item|minecraft:stone|{}\":{\"value\":2}}"
        );
        // 跨文件重复按后者覆盖处理：数据包必须能调整本模组自带的 EMC 值，
        // 抛异常会让整张 EMC 表重建失败并归零。
        List<ExplicitEmcEntry> entries = loader.load(resources);
        assertEquals(1, entries.size());
        assertEquals(EmcValue.of(2), entries.getFirst().value());
    }

    @Test
    void rejectsDuplicateKeyWithinSingleResource() {
        Map<Identifier, String> resources = Map.of(
              Identifier.fromNamespaceAndPath("projecte", "emc/dup.json"),
              "{\"item|minecraft:stone|{}\":{\"value\":1},\"item|minecraft:stone|{ }\":{\"value\":2}}"
        );
        IllegalArgumentException error = assertThrows(
              IllegalArgumentException.class, () -> loader.load(resources));
        assertTrue(error.getMessage().contains("duplicate"));
    }

    @Test
    void rejectsNegativeOverflowUnknownPhaseAndUnknownFields() {
        assertInvalid("negative.json", "{\"item|minecraft:stone|{}\":{\"value\":-1}}", "negative");
        assertInvalid("overflow.json", "{\"item|minecraft:stone|{}\":{\"value\":9223372036854775808}}", "range");
        assertInvalid("phase.json", "{\"item|minecraft:stone|{}\":{\"value\":1,\"phase\":\"during\"}}", "phase");
        assertInvalid("field.json", "{\"item|minecraft:stone|{}\":{\"value\":1,\"extra\":true}}", "extra");
    }

    private void assertInvalid(String path, String json, String expectedMessage) {
        Identifier source = Identifier.fromNamespaceAndPath("projecte", "emc/" + path);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> loader.load(Map.of(source, json)));
        assertTrue(error.getMessage().contains(source.toString()));
        assertTrue(error.getMessage().toLowerCase().contains(expectedMessage));
    }
}
