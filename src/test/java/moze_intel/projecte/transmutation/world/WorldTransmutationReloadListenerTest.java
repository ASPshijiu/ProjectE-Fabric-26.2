package moze_intel.projecte.transmutation.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorldTransmutationReloadListenerTest {
    @TempDir
    Path temp;

    @BeforeAll
    static void bootstrap() {
        MinecraftTestHarness.bootstrap();
    }

    @Test
    void loadsTransmutationsFromResources() throws Exception {
        writeFile("defaults", "{\"transmutations\":["
              + "{\"origin\":\"minecraft:stone\",\"result\":\"minecraft:cobblestone\",\"alt_result\":\"minecraft:grass_block\"}"
              + "]}");
        WorldTransmutationStore.replace(WorldTransmutationRegistry.empty());
        WorldTransmutationReloadListener listener = new WorldTransmutationReloadListener();
        runReload(listener);

        assertEquals(1, WorldTransmutationStore.current().forOrigin(Blocks.STONE).size());
        assertEquals(Blocks.COBBLESTONE, WorldTransmutationStore.current().forOrigin(Blocks.STONE).get(0).result().value());
    }

    @Test
    void malformedReloadPreservesPreviousRegistry() throws Exception {
        writeFile("defaults", "{\"transmutations\":[{\"origin\":\"minecraft:stone\",\"result\":\"minecraft:cobblestone\"}]}");
        WorldTransmutationReloadListener listener = new WorldTransmutationReloadListener();
        runReload(listener);
        assertTrue(WorldTransmutationStore.current().forOrigin(Blocks.STONE).size() >= 1);

        // A duplicate origin+result pair parses fine but fails registry construction; apply must
        // catch it and preserve the previously published registry.
        writeFile("dup", "{\"transmutations\":["
              + "{\"origin\":\"minecraft:stone\",\"result\":\"minecraft:cobblestone\"},"
              + "{\"origin\":\"minecraft:stone\",\"result\":\"minecraft:cobblestone\"}"
              + "]}");
        WorldTransmutationRegistry before = WorldTransmutationStore.current();
        runReload(listener);
        assertSame(before, WorldTransmutationStore.current());
        assertEquals(1, WorldTransmutationStore.current().forOrigin(Blocks.STONE).size());
    }

    private void runReload(WorldTransmutationReloadListener listener) {
        PreparableReloadListener.SharedState state = new PreparableReloadListener.SharedState(resourceManager());
        var prepared = listener.prepare(state);
        listener.apply(prepared, state);
    }

    private ResourceManager resourceManager() {
        PackLocationInfo info = new PackLocationInfo(
              "test", Component.literal("test"), PackSource.BUILT_IN, java.util.Optional.empty());
        return new MultiPackResourceManager(PackType.SERVER_DATA, java.util.List.of(new PathPackResources(info, temp)));
    }

    private void writeFile(String name, String json) throws Exception {
        Path folder = temp.resolve("data").resolve("projecte").resolve(WorldTransmutationReloadListener.FOLDER);
        Files.createDirectories(folder);
        Files.writeString(folder.resolve(name + ".json"), json);
    }
}
