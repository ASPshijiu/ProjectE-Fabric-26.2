package moze_intel.projecte.emc;

import java.util.Objects;
import net.minecraft.resources.Identifier;

public record FakeStackKey(Identifier identifier) implements NormalizedStackKey {
    public FakeStackKey {
        Objects.requireNonNull(identifier, "identifier");
    }

    @Override
    public String type() {
        return "fake";
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
