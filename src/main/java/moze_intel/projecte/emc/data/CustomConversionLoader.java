package moze_intel.projecte.emc.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import moze_intel.projecte.ProjectE;
import moze_intel.projecte.emc.FakeStackKey;
import moze_intel.projecte.emc.ItemStackKey;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.emc.TagStackKey;
import moze_intel.projecte.emc.recipe.RecipeConversion;
import net.minecraft.resources.Identifier;

/**
 * Parses the ProjectE custom-conversion data format ({@code data/<ns>/pe_custom_conversions/*.json})
 * into explicit EMC values, free-value declarations and recipe conversions that the
 * {@link moze_intel.projecte.emc.reload.EmcReloadProcessor} can feed into the EMC graph.
 *
 * <p>The format has two top-level shapes:
 * <ul>
 *   <li>{@code values.before} / {@code values.after}: explicit EMC values, each with a
 *       {@code type} ({@code projecte:item}, {@code projecte:fluid} or {@code projecte:fake}),
 *       a target ({@code id} for items, {@code tag} for items/fluids/fakes, or
 *       {@code description} for fakes) and an {@code emc_value} that is either a non-negative
 *       integer or the literal {@code "free"}.</li>
 *   <li>{@code values.conversion} and {@code groups.<name>.conversions}: recipe conversions,
 *       each with an {@code ingredients} list and an {@code output}, both using the same target
 *       descriptor shape. Ingredients may carry an {@code amount}; the output may carry a
 *       {@code count} (output stack size). A group ties equivalent conversions together so the
 *       solver can collapse redundant paths.</li>
 * </ul>
 *
 * <p>This loader is deterministic (it sorts by source identifier then by group name) and never
 * throws on an unrecognized entry: it omits malformed files with a warning, matching the contract of the
 * recipe conversion sources.
 */
public final class CustomConversionLoader {
    private static final String ITEM_TYPE = "projecte:item";
    private static final String FLUID_TYPE = "projecte:fluid";
    private static final String FAKE_TYPE = "projecte:fake";

    /**
     * @return the parsed explicit values, free keys and recipe conversions from every resource,
     *     aggregated and sorted deterministically.
     */
    public Result load(Map<Identifier, String> resources) {
        List<ExplicitEmcEntry> explicit = new ArrayList<>();
        List<NormalizedStackKey> freeKeys = new ArrayList<>();
        List<RecipeConversion> conversions = new ArrayList<>();

        List<Identifier> sources = new ArrayList<>(resources.keySet());
        sources.sort((a, b) -> a.toString().compareTo(b.toString()));
        for (Identifier source : sources) {
            String json = resources.get(source);
            List<ExplicitEmcEntry> sourceExplicit = new ArrayList<>();
            List<NormalizedStackKey> sourceFreeKeys = new ArrayList<>();
            List<RecipeConversion> sourceConversions = new ArrayList<>();
            try {
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                parseValues(source, root, sourceExplicit, sourceFreeKeys, sourceConversions);
                parseGroups(source, root, sourceConversions);
                explicit.addAll(sourceExplicit);
                freeKeys.addAll(sourceFreeKeys);
                conversions.addAll(sourceConversions);
            } catch (Exception exception) {
                ProjectE.LOGGER.warn("Skipping malformed custom EMC conversion {}: {}",
                      source, exception.getMessage());
            }
        }
        return new Result(List.copyOf(explicit), List.copyOf(freeKeys), List.copyOf(conversions));
    }

    private void parseValues(
          Identifier source,
          JsonObject root,
          List<ExplicitEmcEntry> explicit,
          List<NormalizedStackKey> freeKeys,
          List<RecipeConversion> conversions
    ) {
        JsonElement rawValues = root.get("values");
        if (rawValues == null || !rawValues.isJsonObject()) {
            return;
        }
        JsonObject values = rawValues.getAsJsonObject();

        parseValueSection(source, values.get("before"), ExplicitEmcEntry.Phase.BEFORE, explicit, freeKeys);
        parseValueSection(source, values.get("after"), ExplicitEmcEntry.Phase.AFTER, explicit, freeKeys);

        JsonElement rawConversions = values.get("conversion");
        if (rawConversions != null && rawConversions.isJsonArray()) {
            for (JsonElement element : rawConversions.getAsJsonArray()) {
                if (element.isJsonObject()) {
                    collectConversion(source, "values/conversion", element.getAsJsonObject(), conversions);
                }
            }
        }
    }

    private void parseValueSection(
          Identifier source,
          JsonElement section,
          ExplicitEmcEntry.Phase phase,
          List<ExplicitEmcEntry> explicit,
          List<NormalizedStackKey> freeKeys
    ) {
        if (section == null || !section.isJsonArray()) {
            return;
        }
        for (JsonElement element : section.getAsJsonArray()) {
            if (!element.isJsonObject()) continue;
            JsonObject entry = element.getAsJsonObject();
            NormalizedStackKey key = parseTarget(entry);
            if (key == null) continue;

            JsonElement rawValue = entry.get("emc_value");
            if (rawValue != null && rawValue.isJsonPrimitive() && rawValue.getAsString().equals("free")) {
                freeKeys.add(key);
                continue;
            }
            if (rawValue == null || !rawValue.isJsonPrimitive() || !rawValue.getAsJsonPrimitive().isNumber()) {
                continue;
            }
            long value = exactLong(rawValue, "emc_value");
            if (value < 0) {
                throw new IllegalArgumentException("emc_value must not be negative");
            }
            explicit.add(new ExplicitEmcEntry(
                  key, moze_intel.projecte.emc.EmcValue.of(value), phase, source));
        }
    }

    private void parseGroups(Identifier source, JsonObject root, List<RecipeConversion> conversions) {
        JsonElement rawGroups = root.get("groups");
        if (rawGroups == null || !rawGroups.isJsonObject()) {
            return;
        }
        JsonObject groups = rawGroups.getAsJsonObject();
        List<String> groupNames = new ArrayList<>(groups.keySet());
        groupNames.sort(String::compareTo);
        for (String groupName : groupNames) {
            JsonElement groupElement = groups.get(groupName);
            if (groupElement == null || !groupElement.isJsonObject()) continue;
            JsonObject group = groupElement.getAsJsonObject();
            JsonElement rawConversions = group.get("conversions");
            if (rawConversions == null || !rawConversions.isJsonArray()) continue;
            for (JsonElement element : rawConversions.getAsJsonArray()) {
                if (element.isJsonObject()) {
                    collectConversion(source, groupName, element.getAsJsonObject(), conversions);
                }
            }
        }
    }

    private void collectConversion(
          Identifier source,
          String groupName,
          JsonObject conversion,
          List<RecipeConversion> conversions
    ) {
        NormalizedStackKey output = parseTarget(conversion.getAsJsonObject().get("output"));
        if (output == null) return;

        JsonElement rawIngredients = conversion.get("ingredients");
        if (rawIngredients == null || !rawIngredients.isJsonArray()) return;

        Map<NormalizedStackKey, Integer> ingredients = new TreeMap<>();
        for (JsonElement ingredientElement : rawIngredients.getAsJsonArray()) {
            if (!ingredientElement.isJsonObject()) return;
            NormalizedStackKey ingredient = parseTarget(ingredientElement.getAsJsonObject());
            if (ingredient == null) return;
            int amount = ingredientElement.getAsJsonObject().has("amount")
                  ? exactInt(ingredientElement.getAsJsonObject().get("amount"), "ingredient amount")
                  : 1;
            ingredients.merge(ingredient, amount, Math::addExact);
        }
        ingredients.entrySet().removeIf(entry -> entry.getValue() == 0);
        if (ingredients.isEmpty()) return;

        int outputCount = conversion.has("count")
              ? exactInt(conversion.get("count"), "output count")
              : 1;
        if (outputCount <= 0) {
            throw new IllegalArgumentException("output count must be positive");
        }

        Identifier recipeId = Identifier.fromNamespaceAndPath(
              source.getNamespace(), source.getPath() + "/" + groupName);
        conversions.add(new RecipeConversion(recipeId, outputCount, output, ingredients));
    }

    private static long exactLong(JsonElement raw, String field) {
        try {
            return exactInteger(raw, field).longValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(field + " is out of long range", exception);
        }
    }

    private static int exactInt(JsonElement raw, String field) {
        try {
            return exactInteger(raw, field).intValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(field + " is out of int range", exception);
        }
    }

    private static BigInteger exactInteger(JsonElement raw, String field) {
        if (raw == null || !raw.isJsonPrimitive() || !raw.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(field + " must be an integral number");
        }
        try {
            return new BigInteger(raw.getAsString());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(field + " must be an integral number", exception);
        }
    }

    /**
     * Parses a target descriptor into a {@link NormalizedStackKey}, or {@code null} if it is
     * unrecognized. Recognized shapes:
     * <ul>
     *   <li>{@code {"type":"projecte:item","id":"namespace:path"}} → item key (no components)</li>
     *   <li>{@code {"type":"projecte:item","tag":"namespace:path"}} → tag key</li>
     *   <li>{@code {"type":"projecte:fluid","tag":"namespace:path"}} → fake key under
     *       {@code projecte:fluid/<tag>}</li>
     *   <li>{@code {"type":"projecte:fake","description":"name"}} → fake key under
     *       {@code projecte:fake/<description>}</li>
     * </ul>
     */
    private NormalizedStackKey parseTarget(JsonElement raw) {
        if (raw == null || !raw.isJsonObject()) return null;
        JsonObject descriptor = raw.getAsJsonObject();
        JsonElement rawType = descriptor.get("type");
        String type = rawType != null && rawType.isJsonPrimitive() ? rawType.getAsString() : ITEM_TYPE;

        if (ITEM_TYPE.equals(type)) {
            JsonElement rawId = descriptor.get("id");
            if (rawId != null && rawId.isJsonPrimitive()) {
                Identifier id = parseIdentifier(rawId.getAsString());
                if (id != null) {
                    Map<String, JsonElement> components = new HashMap<>();
                    JsonElement rawData = descriptor.get("data");
                    if (rawData != null) {
                        if (!rawData.isJsonObject()) return null;
                        rawData.getAsJsonObject().entrySet().forEach(entry ->
                              components.put(entry.getKey(), entry.getValue().deepCopy()));
                    }
                    return new ItemStackKey(id, components);
                }
            }
            JsonElement rawTag = descriptor.get("tag");
            if (rawTag != null && rawTag.isJsonPrimitive()) {
                Identifier id = parseIdentifier(rawTag.getAsString());
                if (id != null) {
                    return new TagStackKey(id);
                }
            }
            return null;
        }
        if (FLUID_TYPE.equals(type)) {
            JsonElement rawTag = descriptor.get("tag");
            if (rawTag != null && rawTag.isJsonPrimitive()) {
                Identifier id = parseIdentifier(rawTag.getAsString());
                if (id != null) {
                    return new FakeStackKey(Identifier.fromNamespaceAndPath(
                          "projecte", "fluid/" + id.getNamespace() + "/" + id.getPath()));
                }
            }
            return null;
        }
        if (FAKE_TYPE.equals(type)) {
            JsonElement rawDescription = descriptor.get("description");
            if (rawDescription != null && rawDescription.isJsonPrimitive()) {
                return new FakeStackKey(Identifier.fromNamespaceAndPath(
                      "projecte", "fake/" + rawDescription.getAsString()));
            }
            return null;
        }
        return null;
    }

    private Identifier parseIdentifier(String value) {
        int separator = value.indexOf(':');
        if (separator <= 0 || separator == value.length() - 1) {
            return null;
        }
        return Identifier.fromNamespaceAndPath(
              value.substring(0, separator), value.substring(separator + 1));
    }

    /**
     * Parsed custom-conversion payload: explicit EMC values, keys declared free, and recipe
     * conversions, all immutable and deterministically ordered.
     */
    public record Result(
          List<ExplicitEmcEntry> explicit,
          List<NormalizedStackKey> freeKeys,
          List<RecipeConversion> conversions
    ) {
        public Result {
            explicit = List.copyOf(explicit);
            freeKeys = List.copyOf(freeKeys);
            conversions = List.copyOf(conversions);
        }
    }
}
