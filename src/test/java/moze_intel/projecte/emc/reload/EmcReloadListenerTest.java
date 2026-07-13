package moze_intel.projecte.emc.reload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import moze_intel.projecte.emc.EmcMappingSnapshot;
import moze_intel.projecte.emc.EmcMappingService;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.FakeStackKey;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.emc.recipe.RecipeConversion;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EmcReloadListenerTest {
    @TempDir
    Path temp;

    @Test
    void publishesExplicitValuesFromReloadResources() throws Exception {
        writeEmcResource("projecte", "values", "{\"fake|projecte:anchor|\":{\"value\":4}}");
        EmcMappingService<NormalizedStackKey> service = new EmcMappingService<>();
        EmcReloadListener listener = newListener(service, List.of());

        PreparedReload.run(listener, resourceManager());

        FakeStackKey anchor = new FakeStackKey(Identifier.fromNamespaceAndPath("projecte", "anchor"));
        assertEquals(EmcValue.of(4), service.current().valueFor(anchor).orElseThrow());
        assertTrue(service.current().version() > 0);
    }

    @Test
    void injectsRecipeConversionSourceResults() throws Exception {
        writeEmcResource("projecte", "values", "{\"fake|projecte:gem|\":{\"value\":3}}");
        FakeStackKey gem = new FakeStackKey(Identifier.fromNamespaceAndPath("projecte", "gem"));
        FakeStackKey ring = new FakeStackKey(Identifier.fromNamespaceAndPath("projecte", "ring"));
        RecipeConversion conversion = new RecipeConversion(
              Identifier.fromNamespaceAndPath("projecte", "ring_recipe"), 1, ring, Map.of(gem, 2));
        RecipeConversionSource source = () -> List.of(conversion);

        EmcMappingService<NormalizedStackKey> service = new EmcMappingService<>();
        EmcReloadListener listener = newListener(service, List.of(source));

        PreparedReload.run(listener, resourceManager());

        assertEquals(EmcValue.of(3), service.current().valueFor(gem).orElseThrow());
        assertEquals(EmcValue.of(6), service.current().valueFor(ring).orElseThrow());
    }

    @Test
    void malformedReloadPreservesPreviousSnapshot() throws Exception {
        EmcMappingService<NormalizedStackKey> service = new EmcMappingService<>();
        FakeStackKey keep = new FakeStackKey(Identifier.fromNamespaceAndPath("projecte", "keep"));
        EmcMappingSnapshot<NormalizedStackKey> previous = service.replace(Map.of(keep, EmcValue.of(9)));

        writeEmcResource("projecte", "values", "{\"fake|projecte:broken|\":{\"value\":-1}}");
        EmcReloadListener listener = newListener(service, List.of());
        PreparedReload.run(listener, resourceManager());

        assertEquals(previous, service.current());
        assertEquals(EmcValue.of(9), service.current().valueFor(keep).orElseThrow());
    }

    @Test
    void reloadCallbackFiresOnlyOnSuccess() throws Exception {
        writeEmcResource("projecte", "values", "{\"fake|projecte:gem|\":{\"value\":3}}");
        EmcMappingService<NormalizedStackKey> service = new EmcMappingService<>();
        java.util.List<EmcMappingSnapshot<NormalizedStackKey>> fired = new java.util.ArrayList<>();
        EmcReloadListener listener = new EmcReloadListener(
              service, new EmcReloadProcessor(), List.of(), () -> List.of(), List.of(fired::add));

        PreparedReload.run(listener, resourceManager());
        assertEquals(1, fired.size());
        assertEquals(service.current(), fired.get(0));

        // A failing reload must not fire the callback.
        writeEmcResource("projecte", "values", "{\"fake|projecte:bad|\":{\"value\":-1}}");
        PreparedReload.run(listener, resourceManager());
        assertEquals(1, fired.size(), "callback must not fire on failed reload");
    }

    private EmcReloadListener newListener(
          EmcMappingService<NormalizedStackKey> service, List<RecipeConversionSource> sources
    ) {
        return new EmcReloadListener(service, new EmcReloadProcessor(), sources, () -> List.of(), List.of());
    }

    private ResourceManager resourceManager() {
        PackLocationInfo info = new PackLocationInfo(
              "test", Component.literal("test"), PackSource.BUILT_IN, java.util.Optional.empty());
        PathPackResources pack = new PathPackResources(info, temp);
        return new MultiPackResourceManager(PackType.SERVER_DATA, List.of(pack));
    }

    private void writeEmcResource(String namespace, String name, String json) throws Exception {
        // PathPackResources resolves data-pack files under <root>/data/<namespace>/...
        Path folder = temp.resolve("data").resolve(namespace).resolve(EmcReloadListener.EXPLICIT_FOLDER);
        Files.createDirectories(folder);
        Files.writeString(folder.resolve(name + ".json"), json);
    }

    /**
     * Minimal driver that invokes prepare/apply directly with a freshly built {@link SharedState},
     * avoiding the asynchronous reload machinery for a deterministic unit test.
     */
    private static final class PreparedReload {
        static void run(EmcReloadListener listener, ResourceManager manager) {
            PreparableReloadListener.SharedState state = new PreparableReloadListener.SharedState(manager);
            state.set(net.fabricmc.fabric.api.resource.v1.ResourceLoader.REGISTRY_LOOKUP_KEY,
                  net.minecraft.core.HolderLookup.Provider.create(java.util.stream.Stream.empty()));
            EmcReloadListener.PreparedEmc prepared = listener.prepare(state);
            listener.apply(prepared, state);
        }
    }
}
