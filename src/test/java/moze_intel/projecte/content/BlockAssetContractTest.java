package moze_intel.projecte.content;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BlockAssetContractTest {
    @Test
    void everyBlockModelContainsRenderableGeometry() throws Exception {
        Path models = Path.of("src/main/resources/assets/projecte/models/block");
        List<String> emptyModels = new ArrayList<>();
        try (var files = Files.list(models)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                JsonObject model = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                boolean hasParent = model.has("parent") && !model.get("parent").getAsString().isBlank();
                boolean hasElements = model.has("elements")
                      && model.getAsJsonArray("elements").size() > 0;
                if (!hasParent && !hasElements) {
                    emptyModels.add(file.getFileName().toString());
                }
            }
        }

        assertTrue(emptyModels.isEmpty(),
              "block models must have a parent or elements, but found empty models: " + emptyModels);
    }
}
