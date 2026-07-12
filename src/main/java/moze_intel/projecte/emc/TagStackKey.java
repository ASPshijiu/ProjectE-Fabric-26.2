package moze_intel.projecte.emc;

import java.util.Objects;
import net.minecraft.resources.Identifier;

public record TagStackKey(Identifier identifier) implements NormalizedStackKey {
    public TagStackKey {
        Objects.requireNonNull(identifier, "identifier");
    }

    @Override
    public String type() {
        return "tag";
    }

    @Override
    public String canonicalComponentData() {
        return "";
    }

    @Override
    public String toString() {
        return canonicalString();
    }
}
