package moze_intel.projecte.transmutation.world;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import moze_intel.projecte.ProjectE;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Server-data reload listener that reads every {@code data/<namespace>/pe_world_transmutations/
 * <path>.json} file and atomically republishes the solved {@link WorldTransmutationRegistry}.
 *
 * <p>Preparation (off-thread) parses all files into a flat list of transmutations in deterministic
 * resource order; apply builds the immutable registry and replaces the store only on success,
 * preserving the previous registry on any failure.
 */
public final class WorldTransmutationReloadListener
      extends SimpleReloadListener<WorldTransmutationReloadListener.PreparedTransmutations> {
    public static final String FOLDER = "pe_world_transmutations";

    @Override
    protected PreparedTransmutations prepare(PreparableReloadListener.SharedState state) {
        ResourceManager manager = state.resourceManager();
        return new PreparedTransmutations(readTransmutations(manager));
    }

    @Override
    protected void apply(PreparedTransmutations prepared, PreparableReloadListener.SharedState state) {
        try {
            WorldTransmutationRegistry next = WorldTransmutationRegistry.of(prepared.transmutations);
            WorldTransmutationStore.replace(next);
            ProjectE.LOGGER.info("Loaded {} world transmutation entries from {} files",
                  next.entries().size(), prepared.transmutations.size());
        } catch (RuntimeException exception) {
            ProjectE.LOGGER.warn("World transmutation reload failed; preserving previous registry", exception);
        }
    }

    /**
     * Registers this listener with the Fabric SERVER_DATA reload pipeline.
     */
    public void register() {
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(id(), this);
    }

    public Identifier id() {
        return Identifier.fromNamespaceAndPath("projecte", FOLDER);
    }

    private List<SimpleWorldTransmutation> readTransmutations(ResourceManager manager) {
        Map<Identifier, Resource> found = manager.listResources(FOLDER,
              resourceId -> resourceId.getPath().endsWith(".json"));
        List<SimpleWorldTransmutation> all = new ArrayList<>();
        // Deterministic order by resource id.
        List<Identifier> ids = new ArrayList<>(found.keySet());
        ids.sort((left, right) -> left.toString().compareTo(right.toString()));
        for (Identifier id : ids) {
            try (var input = found.get(id).open()) {
                JsonElement parsed = JsonParser.parseString(new String(input.readAllBytes(), StandardCharsets.UTF_8));
                DataResult<WorldTransmutationFile> result = WorldTransmutationFile.CODEC.parse(JsonOps.INSTANCE, parsed);
                WorldTransmutationFile file = result.getOrThrow(message ->
                      new IllegalStateException(id + ": " + message));
                all.addAll(file.transmutations());
            } catch (IOException | RuntimeException exception) {
                // 单个损坏文件只跳过自己：整包 reload 失败会让所有世界转化一起消失，
                // 与"逐文件降级"的既定行为约定不符。
                ProjectE.LOGGER.warn("Skipping malformed world transmutation resource {}", id, exception);
            }
        }
        return List.copyOf(all);
    }

    record PreparedTransmutations(List<SimpleWorldTransmutation> transmutations) {
        PreparedTransmutations {
            transmutations = List.copyOf(transmutations);
        }
    }
}
