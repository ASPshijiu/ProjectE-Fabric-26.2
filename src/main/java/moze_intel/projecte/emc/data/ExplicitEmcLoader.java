package moze_intel.projecte.emc.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.FakeStackKey;
import moze_intel.projecte.emc.ItemStackKey;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.emc.TagStackKey;
import net.minecraft.resources.Identifier;

public final class ExplicitEmcLoader {
    private static final Set<String> ALLOWED_FIELDS = Set.of("value", "phase");

    public List<ExplicitEmcEntry> load(Map<Identifier, String> resources) {
        List<ExplicitEmcEntry> result = new ArrayList<>();
        Map<NormalizedStackKey, Identifier> seen = new HashMap<>();
        resources.entrySet().stream()
              .sorted(Map.Entry.comparingByKey((left, right) -> left.toString().compareTo(right.toString())))
              .forEach(resource -> loadResource(resource.getKey(), resource.getValue(), result, seen));
        return List.copyOf(result);
    }

    private void loadResource(
          Identifier source,
          String json,
          List<ExplicitEmcEntry> result,
          Map<NormalizedStackKey, Identifier> seen
    ) {
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonObject()) {
                throw new IllegalArgumentException("root must be an object");
            }
            TreeMap<String, JsonElement> entries = new TreeMap<>();
            parsed.getAsJsonObject().entrySet().forEach(entry -> entries.put(entry.getKey(), entry.getValue()));
            entries.forEach((rawKey, rawEntry) -> {
                ExplicitEmcEntry entry = decodeEntry(source, rawKey, rawEntry);
                Identifier previous = seen.putIfAbsent(entry.key(), source);
                if (previous != null) {
                    throw new IllegalArgumentException(
                          source + ": duplicate EMC key " + entry.key().canonicalString() + " previously declared by " + previous
                    );
                }
                result.add(entry);
            });
        } catch (IllegalArgumentException exception) {
            if (exception.getMessage() != null && exception.getMessage().startsWith(source.toString() + ":")) {
                throw exception;
            }
            throw new IllegalArgumentException(source + ": " + exception.getMessage(), exception);
        }
    }

    private ExplicitEmcEntry decodeEntry(Identifier source, String rawKey, JsonElement rawEntry) {
        if (!rawEntry.isJsonObject()) {
            throw new IllegalArgumentException("entry " + rawKey + " must be an object");
        }
        JsonObject object = rawEntry.getAsJsonObject();
        for (String field : object.keySet()) {
            if (!ALLOWED_FIELDS.contains(field)) {
                throw new IllegalArgumentException("unknown field " + field + " in " + rawKey);
            }
        }
        JsonElement rawValue = object.get("value");
        if (rawValue == null || !rawValue.isJsonPrimitive() || !rawValue.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("value for " + rawKey + " must be an integral number");
        }
        EmcValue value = parseValue(rawKey, rawValue.getAsJsonPrimitive());
        ExplicitEmcEntry.Phase phase = parsePhase(rawKey, object.get("phase"));
        return new ExplicitEmcEntry(parseKey(rawKey), value, phase, source);
    }

    private EmcValue parseValue(String key, JsonPrimitive primitive) {
        try {
            BigInteger value = new BigInteger(primitive.getAsString());
            if (value.signum() < 0) {
                throw new IllegalArgumentException("negative EMC value for " + key);
            }
            return EmcValue.of(value.longValueExact());
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("EMC value out of long range for " + key, exception);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("EMC value for " + key + " must be integral", exception);
        }
    }

    private ExplicitEmcEntry.Phase parsePhase(String key, JsonElement rawPhase) {
        if (rawPhase == null) {
            return ExplicitEmcEntry.Phase.BEFORE;
        }
        if (!rawPhase.isJsonPrimitive() || !rawPhase.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("phase for " + key + " must be a string");
        }
        return switch (rawPhase.getAsString()) {
            case "before" -> ExplicitEmcEntry.Phase.BEFORE;
            case "after" -> ExplicitEmcEntry.Phase.AFTER;
            default -> throw new IllegalArgumentException("unknown phase for " + key + ": " + rawPhase.getAsString());
        };
    }

    private NormalizedStackKey parseKey(String canonical) {
        String[] parts = canonical.split("\\|", 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException("invalid canonical EMC key: " + canonical);
        }
        Identifier identifier = parseIdentifier(parts[1]);
        return switch (parts[0]) {
            case "item" -> new ItemStackKey(identifier, parseComponents(parts[2]));
            case "tag" -> {
                requireEmptyComponents(canonical, parts[2]);
                yield new TagStackKey(identifier);
            }
            case "fake" -> {
                requireEmptyComponents(canonical, parts[2]);
                yield new FakeStackKey(identifier);
            }
            default -> throw new IllegalArgumentException("unknown EMC key type: " + parts[0]);
        };
    }

    private Map<String, JsonElement> parseComponents(String rawComponents) {
        JsonElement parsed = JsonParser.parseString(rawComponents);
        if (!parsed.isJsonObject()) {
            throw new IllegalArgumentException("item component payload must be an object");
        }
        Map<String, JsonElement> components = new HashMap<>();
        parsed.getAsJsonObject().entrySet().forEach(entry -> components.put(entry.getKey(), entry.getValue()));
        return components;
    }

    private void requireEmptyComponents(String canonical, String components) {
        if (!components.isEmpty()) {
            throw new IllegalArgumentException("non-item key must not have component data: " + canonical);
        }
    }

    private Identifier parseIdentifier(String value) {
        int separator = value.indexOf(':');
        if (separator <= 0 || separator == value.length() - 1) {
            throw new IllegalArgumentException("invalid identifier: " + value);
        }
        return Identifier.fromNamespaceAndPath(value.substring(0, separator), value.substring(separator + 1));
    }
}
