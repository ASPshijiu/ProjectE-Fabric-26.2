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

    /**
     * Folder under {@code data/<namespace>/} that holds ProjectE custom-conversion files
     * ({@code values.before/after} explicit values plus {@code values.conversion} and
     * {@code groups.<name>.conversions} recipe conversions).
     */
    public static final String CUSTOM_CONVERSIONS_FOLDER = "pe_custom_conversions";

    private final EmcMappingService<NormalizedStackKey> service;
    private final EmcReloadProcessor processor;
    private final List<RecipeConversionSource> recipeSources;
    private final java.util.function.Supplier<List<RecipeConversionSource>> serverRecipeSources;
    private final List<java.util.function.Consumer<EmcMappingSnapshot<NormalizedStackKey>>> reloadCallbacks;

    public EmcReloadListener(Collection<? extends RecipeConversionSource> recipeSources) {
        this(ProjectEEmc.service(), new EmcReloadProcessor(),
              List.copyOf(recipeSources), () -> List.of(), List.of());
    }

    EmcReloadListener(
          EmcMappingService<NormalizedStackKey> service,
          EmcReloadProcessor processor,
          List<RecipeConversionSource> recipeSources,
          java.util.function.Supplier<List<RecipeConversionSource>> serverRecipeSources,
          List<java.util.function.Consumer<EmcMappingSnapshot<NormalizedStackKey>>> reloadCallbacks
    ) {
        this.service = service;
        this.processor = processor;
        this.recipeSources = List.copyOf(recipeSources);
        this.serverRecipeSources = serverRecipeSources;
        this.reloadCallbacks = List.copyOf(reloadCallbacks);
    }

    /**
     * @return a copy of this listener that resolves the server-bound recipe sources (e.g. the
     *     vanilla {@link VanillaRecipeConversionSource} that needs the live {@code RecipeManager})
     *     from the given supplier on every reload. The static {@code recipeSources} are always
     *     applied; the supplier's sources are added when it returns a non-empty list.
     */
    public EmcReloadListener withServerRecipeSources(
          java.util.function.Supplier<List<RecipeConversionSource>> serverRecipeSources
    ) {
        return new EmcReloadListener(service, processor, recipeSources,
              java.util.Objects.requireNonNull(serverRecipeSources), reloadCallbacks);
    }

    /**
     * @return a copy of this listener that fires the given callback after each successful reload,
     *     so gameplay systems (e.g. the player sync handler) can rebroadcast the new shared mapping.
     */
    public EmcReloadListener withReloadCallback(
          java.util.function.Consumer<EmcMappingSnapshot<NormalizedStackKey>> callback
    ) {
        java.util.List<java.util.function.Consumer<EmcMappingSnapshot<NormalizedStackKey>>> next =
              new java.util.ArrayList<>(reloadCallbacks);
        next.add(callback);
        return new EmcReloadListener(service, processor, recipeSources, serverRecipeSources, List.copyOf(next));
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
        Map<Identifier, String> explicit = readExplicitResources(manager, EXPLICIT_FOLDER);
        Map<Identifier, String> custom = readExplicitResources(manager, CUSTOM_CONVERSIONS_FOLDER);
        List<RecipeConversion> conversions = collectConversions(registriesOrEmpty(state));
        return new PreparedEmc(explicit, custom, conversions);
    }

    /**
     * Returns the registry lookup published by the reload pipeline, or an empty provider when no
     * recipe source needs one (for example in unit tests that only exercise explicit values).
     */
    private HolderLookup.Provider registriesOrEmpty(PreparableReloadListener.SharedState state) {
        boolean needsRegistries = !recipeSources.isEmpty() || !resolveServerRecipeSources().isEmpty();
        if (!needsRegistries) {
            return HolderLookup.Provider.create(java.util.stream.Stream.empty());
        }
        return state.get(ResourceLoader.REGISTRY_LOOKUP_KEY);
    }

    private List<RecipeConversionSource> resolveServerRecipeSources() {
        try {
            List<RecipeConversionSource> resolved = serverRecipeSources.get();
            return resolved == null ? List.of() : List.copyOf(resolved);
        } catch (Exception ignored) {
            // The server may not be available (e.g. unit tests); treat as no server sources.
            return List.of();
        }
    }

    @Override
    protected void apply(PreparedEmc prepared, PreparableReloadListener.SharedState state) {
        EmcMappingService.RebuildResult<NormalizedStackKey> result = service.rebuild(
              () -> processor.rebuild(prepared.explicitResources(), prepared.customConversionResources(), prepared.recipeConversions())
        );
        if (!result.success()) {
            ProjectE.LOGGER.warn("EMC reload failed; preserving snapshot version {}",
                  service.current().version(), result.exception().orElse(null));
            return;
        }
        EmcMappingSnapshot<NormalizedStackKey> snapshot = result.snapshot();
        ProjectE.LOGGER.info("EMC reload published snapshot version {} with {} values",
              snapshot.version(), snapshot.values().size());
        for (java.util.function.Consumer<EmcMappingSnapshot<NormalizedStackKey>> callback : reloadCallbacks) {
            callback.accept(snapshot);
        }
    }

    private Map<Identifier, String> readExplicitResources(ResourceManager manager, String folder) {
        Map<Identifier, Resource> found = manager.listResources(
              folder, EmcReloadListener::isEmcResource);
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
        for (RecipeConversionSource source : resolveServerRecipeSources()) {
            try {
                all.addAll(source.conversions());
            } catch (Exception ignored) {
                // A failing recipe source must not break the EMC reload; omit its conversions.
            }
        }
        return List.copyOf(all);
    }

    /**
     * Parsed reload payload: explicit EMC resources keyed by their identifier, the custom-conversion
     * resources, and the gathered recipe conversions. All are immutable; the apply stage only
     * consumes them.
     */
    record PreparedEmc(
          Map<Identifier, String> explicitResources,
          Map<Identifier, String> customConversionResources,
          List<RecipeConversion> recipeConversions
    ) {
        PreparedEmc {
            explicitResources = Map.copyOf(explicitResources);
            customConversionResources = Map.copyOf(customConversionResources);
            recipeConversions = List.copyOf(recipeConversions);
        }
    }
}
