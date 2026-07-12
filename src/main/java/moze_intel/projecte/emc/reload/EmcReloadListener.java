package moze_intel.projecte.emc.reload;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import moze_intel.projecte.ProjectE;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.emc.EmcMappingSnapshot;
import moze_intel.projecte.emc.EmcMappingService;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.emc.ProjectEEmc;
import moze_intel.projecte.emc.recipe.RecipeConversion;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Server-data reload listener that rebuilds and atomically republishes the EMC mapping.
 *
 * <p>During {@link #prepare} it reads every {@code data/<namespace>/emc/<path>.json} resource and
 * collects recipe conversions from every registered {@link RecipeConversionSource}; both happen off
 * the main thread. During {@link #apply} it solves the EMC graph through {@link EmcReloadProcessor}
 * and, only on success, replaces the snapshot published by {@link ProjectEEmc#service()}. A failure
 * at any stage preserves the previously published snapshot.
 */
public final class EmcReloadListener extends SimpleReloadListener<EmcReloadListener.PreparedEmc> {
    /**
     * Folder under {@code data/<namespace>/} that holds explicit EMC value files.
     */
    public static final String EXPLICIT_FOLDER = "emc";

    private final EmcMappingService<NormalizedStackKey> service;
    private final EmcReloadProcessor processor;
    private final List<RecipeConversionSource> recipeSources;

    public EmcReloadListener(Collection<? extends RecipeConversionSource> recipeSources) {
        this(ProjectEEmc.service(), new EmcReloadProcessor(), List.copyOf(recipeSources));
    }

    EmcReloadListener(
          EmcMappingService<NormalizedStackKey> service,
          EmcReloadProcessor processor,
          List<RecipeConversionSource> recipeSources
    ) {
        this.service = service;
        this.processor = processor;
        this.recipeSources = List.copyOf(recipeSources);
    }

    /**
     * Registers this listener with the Fabric SERVER_DATA reload pipeline.
     */
    public void register() {
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(id(), this);
    }

    /**
     * @return the stable identifier of this listener ({@code projecte:emc}).
     */
    public Identifier id() {
        return Identifier.fromNamespaceAndPath(ProjectEAPI.MOD_ID, EXPLICIT_FOLDER);
    }

    @Override
    protected PreparedEmc prepare(PreparableReloadListener.SharedState state) {
        ResourceManager manager = state.resourceManager();
        Map<Identifier, String> explicit = readExplicitResources(manager);
        List<RecipeConversion> conversions = collectConversions(registriesOrEmpty(state));
        return new PreparedEmc(explicit, conversions);
    }

    /**
     * Returns the registry lookup published by the reload pipeline, or an empty provider when no
     * recipe source needs one (for example in unit tests that only exercise explicit values).
     */
    private HolderLookup.Provider registriesOrEmpty(PreparableReloadListener.SharedState state) {
        if (recipeSources.isEmpty()) {
            return HolderLookup.Provider.create(java.util.stream.Stream.empty());
        }
        return state.get(ResourceLoader.REGISTRY_LOOKUP_KEY);
    }

    @Override
    protected void apply(PreparedEmc prepared, PreparableReloadListener.SharedState state) {
        EmcMappingService.RebuildResult<NormalizedStackKey> result = service.rebuild(
              () -> processor.rebuild(prepared.explicitResources(), prepared.recipeConversions())
        );
        if (!result.success()) {
            ProjectE.LOGGER.warn("EMC reload failed; preserving snapshot version {}",
                  service.current().version(), result.exception().orElse(null));
            return;
        }
        EmcMappingSnapshot<NormalizedStackKey> snapshot = result.snapshot();
        ProjectE.LOGGER.info("EMC reload published snapshot version {} with {} values",
              snapshot.version(), snapshot.values().size());
    }

    private Map<Identifier, String> readExplicitResources(ResourceManager manager) {
        Map<Identifier, Resource> found = manager.listResources(
              EXPLICIT_FOLDER, EmcReloadListener::isEmcResource);
        TreeMap<Identifier, String> explicit = new TreeMap<>(
              (left, right) -> left.toString().compareTo(right.toString()));
        for (Map.Entry<Identifier, Resource> entry : found.entrySet()) {
            try {
                String json = new String(entry.getValue().open().readAllBytes(), StandardCharsets.UTF_8);
                explicit.put(entry.getKey(), json);
            } catch (IOException exception) {
                throw new IllegalStateException("Failed reading EMC resource " + entry.getKey(), exception);
            }
        }
        return explicit;
    }

    private static boolean isEmcResource(Identifier id) {
        return id.getPath().endsWith(".json");
    }

    private List<RecipeConversion> collectConversions(HolderLookup.Provider registries) {
        List<RecipeConversion> all = new ArrayList<>();
        for (RecipeConversionSource source : recipeSources) {
            all.addAll(source.conversions());
        }
        return List.copyOf(all);
    }

    /**
     * Parsed reload payload: explicit EMC resources keyed by their identifier and the gathered
     * recipe conversions. Both are immutable; the apply stage only consumes them.
     */
    record PreparedEmc(Map<Identifier, String> explicitResources, List<RecipeConversion> recipeConversions) {
        PreparedEmc {
            explicitResources = Map.copyOf(explicitResources);
            recipeConversions = List.copyOf(recipeConversions);
        }
    }
}
