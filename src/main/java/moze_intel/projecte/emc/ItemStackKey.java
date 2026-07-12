package moze_intel.projecte.emc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import net.minecraft.resources.Identifier;

public final class ItemStackKey implements NormalizedStackKey {
    private final Identifier identifier;
    private final SortedMap<String, JsonElement> components;
    private final String canonicalComponentData;

    public ItemStackKey(Identifier identifier, Map<String, ? extends JsonElement> components) {
        this.identifier = Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(components, "components");
        TreeMap<String, JsonElement> copy = new TreeMap<>();
        components.forEach((key, value) -> copy.put(
              Objects.requireNonNull(key, "component key"),
              Objects.requireNonNull(value, "component value").deepCopy()
        ));
        this.components = Collections.unmodifiableSortedMap(copy);
        JsonObject canonical = new JsonObject();
        this.components.forEach(canonical::add);
        this.canonicalComponentData = canonical.toString();
    }

    @Override
    public String type() {
        return "item";
    }

    @Override
    public Identifier identifier() {
        return identifier;
    }

    public SortedMap<String, JsonElement> components() {
        TreeMap<String, JsonElement> copy = new TreeMap<>();
        components.forEach((key, value) -> copy.put(key, value.deepCopy()));
        return Collections.unmodifiableSortedMap(copy);
    }

    @Override
    public String canonicalComponentData() {
        return canonicalComponentData;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof ItemStackKey key
              && identifier.equals(key.identifier)
              && components.equals(key.components);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, components);
    }

    @Override
    public String toString() {
        return canonicalString();
    }
}
