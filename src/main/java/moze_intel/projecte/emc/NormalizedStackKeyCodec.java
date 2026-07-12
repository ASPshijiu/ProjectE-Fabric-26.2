package moze_intel.projecte.emc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * DFU {@link Codec} and Fabric {@link StreamCodec} for the sealed {@link NormalizedStackKey}
 * hierarchy, using the canonical {@code type|namespace:path|component-json} string form.
 *
 * <p>Storing the canonical string keeps save and network formats identical to the explicit-EMC
 * loader's wire form, so the same key decoding path validates both data and player state.
 */
public final class NormalizedStackKeyCodec {
    private NormalizedStackKeyCodec() {
    }

    public static final Codec<NormalizedStackKey> CODEC = Codec.STRING
          .comapFlatMap(
                value -> {
                    try {
                        return DataResult.success(fromCanonical(value));
                    } catch (IllegalArgumentException exception) {
                        return DataResult.error(() -> exception.getMessage());
                    }
                },
                NormalizedStackKey::canonicalString);

    public static final StreamCodec<RegistryFriendlyByteBuf, NormalizedStackKey> STREAM_CODEC =
          ByteBufCodecs.STRING_UTF8.<NormalizedStackKey>map(NormalizedStackKeyCodec::fromCanonical, NormalizedStackKey::canonicalString)
                .cast();

    /**
     * Parse a canonical string into the concrete key subtype.
     */
    public static NormalizedStackKey fromCanonical(String canonical) {
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

    private static Map<String, JsonElement> parseComponents(String rawComponents) {
        JsonElement parsed = JsonParser.parseString(rawComponents);
        if (!parsed.isJsonObject()) {
            throw new IllegalArgumentException("item component payload must be an object");
        }
        Map<String, JsonElement> components = new HashMap<>();
        parsed.getAsJsonObject().entrySet().forEach(entry -> components.put(entry.getKey(), entry.getValue()));
        return components;
    }

    private static void requireEmptyComponents(String canonical, String components) {
        if (!components.isEmpty()) {
            throw new IllegalArgumentException("non-item key must not have component data: " + canonical);
        }
    }

    private static Identifier parseIdentifier(String value) {
        int separator = value.indexOf(':');
        if (separator <= 0 || separator == value.length() - 1) {
            throw new IllegalArgumentException("invalid identifier: " + value);
        }
        return Identifier.fromNamespaceAndPath(value.substring(0, separator), value.substring(separator + 1));
    }
}
