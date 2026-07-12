package moze_intel.projecte.porting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public final class BaselineExporter {
    private static final SortedMap<String, String> CATEGORY_ROOTS;

    static {
        SortedMap<String, String> roots = new TreeMap<>();
        roots.put("advancements", "src/datagen/generated/data/projecte/advancement");
        roots.put("blockstates", "src/datagen/generated/assets/projecte/blockstates");
        roots.put("custom_conversions", "src/datagen/generated/data/projecte/pe_custom_conversions");
        roots.put("item_models", "src/datagen/generated/assets/projecte/models/item");
        roots.put("loot_tables", "src/datagen/generated/data/projecte/loot_table");
        roots.put("recipes", "src/datagen/generated/data/projecte/recipe");
        roots.put("sounds", "src/main/resources/assets/projecte/sounds");
        roots.put("tags", "src/datagen/generated/data/projecte/tags");
        roots.put("textures", "src/main/resources/assets/projecte/textures");
        roots.put("world_transmutations", "src/datagen/generated/data/projecte/pe_world_transmutations");
        CATEGORY_ROOTS = Collections.unmodifiableSortedMap(roots);
    }

    private BaselineExporter() {
    }

    public static SortedMap<String, List<String>> scan(Path upstream) throws IOException {
        SortedMap<String, List<String>> categories = new TreeMap<>();
        for (Map.Entry<String, String> entry : CATEGORY_ROOTS.entrySet()) {
            Path categoryRoot = upstream.resolve(entry.getValue());
            List<String> ids = new ArrayList<>();
            if (Files.isDirectory(categoryRoot)) {
                try (var paths = Files.walk(categoryRoot)) {
                    paths.filter(Files::isRegularFile)
                        .map(categoryRoot::relativize)
                        .map(Path::toString)
                        .map(value -> value.replace('\\', '/'))
                        .map(BaselineExporter::removeJsonExtension)
                        .sorted()
                        .forEach(ids::add);
                }
            }
            categories.put(entry.getKey(), List.copyOf(ids));
        }
        return Collections.unmodifiableSortedMap(categories);
    }

    public static void write(Path upstream, Path output, String commit) throws IOException {
        if (commit.isBlank()) {
            throw new IllegalArgumentException("Source commit must not be blank");
        }
        Path absoluteOutput = output.toAbsolutePath();
        Path parent = absoluteOutput.getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, absoluteOutput.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temporary, toJson(scan(upstream), commit), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, absoluteOutput, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, absoluteOutput, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String removeJsonExtension(String value) {
        return value.endsWith(".json") ? value.substring(0, value.length() - 5) : value;
    }

    private static String toJson(SortedMap<String, List<String>> categories, String commit) {
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"schema\": 1,\n  \"source_commit\": \"")
            .append(escape(commit))
            .append("\",\n  \"categories\": {\n");
        int categoryIndex = 0;
        for (Map.Entry<String, List<String>> entry : categories.entrySet()) {
            if (categoryIndex++ > 0) {
                json.append(",\n");
            }
            json.append("    \"").append(escape(entry.getKey())).append("\": [");
            for (int itemIndex = 0; itemIndex < entry.getValue().size(); itemIndex++) {
                if (itemIndex > 0) {
                    json.append(", ");
                }
                json.append("\"").append(escape(entry.getValue().get(itemIndex))).append("\"");
            }
            json.append(']');
        }
        return json.append("\n  }\n}\n").toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("Usage: BaselineExporter <upstream> <output> <commit>");
        }
        write(Path.of(args[0]), Path.of(args[1]), args[2]);
    }
}
