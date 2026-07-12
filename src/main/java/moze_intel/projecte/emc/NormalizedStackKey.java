package moze_intel.projecte.emc;

import net.minecraft.resources.Identifier;

public sealed interface NormalizedStackKey extends Comparable<NormalizedStackKey>
      permits ItemStackKey, TagStackKey, FakeStackKey {
    String type();
    Identifier identifier();
    String canonicalComponentData();

    default String canonicalString() {
        return type() + "|" + identifier() + "|" + canonicalComponentData();
    }

    @Override
    default int compareTo(NormalizedStackKey other) {
        return canonicalString().compareTo(other.canonicalString());
    }
}
