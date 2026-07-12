package moze_intel.projecte.emc.data;

import java.util.Objects;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.NormalizedStackKey;
import net.minecraft.resources.Identifier;

public record ExplicitEmcEntry(
      NormalizedStackKey key,
      EmcValue value,
      Phase phase,
      Identifier source
) {
    public enum Phase {
        BEFORE,
        AFTER
    }

    public ExplicitEmcEntry {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(source, "source");
    }

    public String debugString() {
        return key.canonicalString() + ":" + value.longValue() + ":" + phase + ":" + source;
    }
}
