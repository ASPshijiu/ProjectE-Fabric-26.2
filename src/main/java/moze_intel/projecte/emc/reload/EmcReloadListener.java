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
import moze_intel.projecte.emc.ItemStackKey;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.emc.ProjectEEmc;
import moze_intel.projecte.emc.TagStackKey;
import moze_intel.projecte.emc.recipe.RecipeConversion;
import net.minecraft.core.registries.Registries;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;

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
    private volatile PreparedEmc lastPrepared;

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
        HolderLookup.Provider registries = state.get(ResourceLoader.REGISTRY_LOOKUP_KEY);
        List<RecipeConversion> conversions = collectConversions();
        return new PreparedEmc(explicit, custom, conversions, createTagResolver(registries));
    }

    private List<RecipeConversionSource> resolveServerRecipeSources() {
        List<RecipeConversionSource> resolved = serverRecipeSources.get();
        return resolved == null ? List.of() : List.copyOf(resolved);
    }

    @Override
    protected void apply(PreparedEmc prepared, PreparableReloadListener.SharedState state) {
        EmcMappingService.RebuildResult<NormalizedStackKey> result = service.rebuild(
              () -> processor.rebuild(prepared.explicitResources(), prepared.customConversionResources(),
                    prepared.recipeConversions(), prepared.tagResolver())
        );
        if (result.success()) {
            lastPrepared = prepared;
        }
        publishResult("reload", result);
    }

    /**
     * Extends the initial explicit mapping after the server's live recipe manager is available.
     * The first data-pack reload happens before Fabric fires SERVER_STARTING, so server-bound recipe
     * sources cannot participate until this second, recipe-only pass.
     */
    public void refreshRecipeMappings() {
        List<RecipeConversion> conversions;
        try {
            conversions = collectConversions();
        } catch (RuntimeException exception) {
            ProjectE.LOGGER.warn(
                  "EMC recipe refresh failed while collecting conversions; preserving snapshot version {}",
                  service.current().version(), exception);
            return;
        }
        if (conversions.isEmpty()) {
            ProjectE.LOGGER.warn("EMC recipe refresh found no recipe conversions; keeping {} values",
                  service.current().values().size());
            return;
        }
        PreparedEmc prepared = lastPrepared;
        EmcMappingService.RebuildResult<NormalizedStackKey> result = prepared == null
              ? service.rebuild(() -> processor.extend(service.current().values(), conversions))
              : service.rebuild(() -> processor.rebuild(
                    prepared.explicitResources(), prepared.customConversionResources(),
                    conversions, prepared.tagResolver()));
        publishResult("recipe refresh", result);
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

    private List<RecipeConversion> collectConversions() {
        List<RecipeConversion> all = new ArrayList<>();
        for (RecipeConversionSource source : recipeSources) {
            all.addAll(source.conversions());
        }
        for (RecipeConversionSource source : resolveServerRecipeSources()) {
            all.addAll(source.conversions());
        }
        return List.copyOf(all);
    }

    private java.util.function.Function<TagStackKey, List<ItemStackKey>> createTagResolver(
          HolderLookup.Provider registries
    ) {
        return tag -> registries.lookup(Registries.ITEM)
              .flatMap(items -> items.get(TagKey.create(Registries.ITEM, tag.identifier())))
              .stream()
              .flatMap(named -> named.isBound() ? named.stream() : java.util.stream.Stream.empty())
              .flatMap(holder -> holder.unwrapKey().stream())
              .map(key -> new ItemStackKey(key.identifier(), Map.of()))
              .sorted()
              .toList();
    }

    private void publishResult(
          String phase,
          EmcMappingService.RebuildResult<NormalizedStackKey> result
    ) {
        if (!result.success()) {
            ProjectE.LOGGER.warn("EMC {} failed; preserving snapshot version {}",
                  phase, service.current().version(), result.exception().orElse(null));
            return;
        }
        EmcMappingSnapshot<NormalizedStackKey> snapshot = result.snapshot();
        ProjectE.LOGGER.info("EMC {} published snapshot version {} with {} values",
              phase, snapshot.version(), snapshot.values().size());
        for (java.util.function.Consumer<EmcMappingSnapshot<NormalizedStackKey>> callback : reloadCallbacks) {
            callback.accept(snapshot);
        }
    }

    /**
     * Parsed reload payload: explicit EMC resources keyed by their identifier, the custom-conversion
     * resources, and the gathered recipe conversions. All are immutable; the apply stage only
     * consumes them.
     */
    record PreparedEmc(
          Map<Identifier, String> explicitResources,
          Map<Identifier, String> customConversionResources,
          List<RecipeConversion> recipeConversions,
          java.util.function.Function<TagStackKey, List<ItemStackKey>> tagResolver
    ) {
        PreparedEmc {
            explicitResources = Map.copyOf(explicitResources);
            customConversionResources = Map.copyOf(customConversionResources);
            recipeConversions = List.copyOf(recipeConversions);
            java.util.Objects.requireNonNull(tagResolver, "tagResolver");
        }
    }
}
