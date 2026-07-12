package moze_intel.projecte.packaging;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "projecte.runtimeJar", matches = ".+")
class PackagedJarTest {
    @Test
    void runtimeJarContainsRequiredFilesAndNoPortingTools() throws Exception {
        Path jar = Path.of(System.getProperty("projecte.runtimeJar"));
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            assertNotNull(zip.getEntry("fabric.mod.json"));
            assertNotNull(zip.getEntry("moze_intel/projecte/ProjectE.class"));
            assertNotNull(zip.getEntry("moze_intel/projecte/client/ProjectEClient.class"));
            assertTrue(zip.stream().anyMatch(entry -> entry.getName().startsWith("LICENSE_")));
            assertNull(zip.getEntry("moze_intel/projecte/porting/BaselineExporter.class"));
        }
    }
}
