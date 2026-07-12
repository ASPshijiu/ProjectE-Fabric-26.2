package moze_intel.projecte.porting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.SortedMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BaselineExporterTest {
    @TempDir
    Path temp;

    @Test
    void exportsSortedDeterministicCategories() throws Exception {
        Path upstream = temp.resolve("upstream");
        Files.createDirectories(upstream.resolve("src/datagen/generated/data/projecte/recipe"));
        Files.createDirectories(upstream.resolve("src/main/resources/assets/projecte/textures/item"));
        Files.writeString(upstream.resolve("src/datagen/generated/data/projecte/recipe/z.json"), "{}");
        Files.writeString(upstream.resolve("src/datagen/generated/data/projecte/recipe/a.json"), "{}");
        Files.writeString(upstream.resolve("src/main/resources/assets/projecte/textures/item/test.png"), "png");

        SortedMap<String, List<String>> result = BaselineExporter.scan(upstream);
        assertEquals(List.of("a", "z"), result.get("recipes"));
        assertEquals(List.of("item/test.png"), result.get("textures"));

        Path first = temp.resolve("first.json");
        Path second = temp.resolve("second.json");
        BaselineExporter.write(upstream, first, "abc123");
        BaselineExporter.write(upstream, second, "abc123");
        assertEquals(Files.readString(first), Files.readString(second));
        assertTrue(Files.readString(first).endsWith("\n"));
    }
}
