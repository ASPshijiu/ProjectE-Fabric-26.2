package moze_intel.projecte.player;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import moze_intel.projecte.emc.NormalizedStackKey;

/**
 * Immutable player knowledge state: the set of explicitly learned items plus a full-knowledge flag
 * (set by the Tome of Knowledge / commands).
 *
 * <p>Every mutator returns a new {@code PlayerKnowledge}; instances are safe to share and store in
 * a player attachment. The learned set is exposed as an unmodifiable view.
 */
public final class PlayerKnowledge {
    private final Set<NormalizedStackKey> learned;
    private final boolean fullKnowledge;

    private PlayerKnowledge(Set<NormalizedStackKey> learned, boolean fullKnowledge) {
        this.learned = Set.copyOf(learned);
        this.fullKnowledge = fullKnowledge;
    }

    public static PlayerKnowledge empty() {
        return new PlayerKnowledge(Set.of(), false);
    }

    /**
     * Reconstruction point used by the codec. The learned set is copied defensively.
     */
    public static PlayerKnowledge of(Set<NormalizedStackKey> learned, boolean fullKnowledge) {
        Objects.requireNonNull(learned, "learned");
        return new PlayerKnowledge(learned, fullKnowledge);
    }

    /**
     * @return a new knowledge state with the given item learned. Learning a known item returns an
     *     equal instance (no structural change).
     */
    public PlayerKnowledge learn(NormalizedStackKey key) {
        Objects.requireNonNull(key, "key");
        if (fullKnowledge || learned.contains(key)) {
            return this;
        }
        Set<NormalizedStackKey> next = new LinkedHashSet<>(learned);
        next.add(key);
        return new PlayerKnowledge(next, false);
    }

    /**
     * @return a new knowledge state without the given item. Unlearning an unknown item returns an
     *     equal instance. Full knowledge is preserved (clearing the flag is an explicit operation).
     */
    public PlayerKnowledge unlearn(NormalizedStackKey key) {
        Objects.requireNonNull(key, "key");
        if (!learned.contains(key)) {
            return this;
        }
        Set<NormalizedStackKey> next = new LinkedHashSet<>(learned);
        next.remove(key);
        return new PlayerKnowledge(next, fullKnowledge);
    }

    public boolean has(NormalizedStackKey key) {
        Objects.requireNonNull(key, "key");
        return fullKnowledge || learned.contains(key);
    }

    public Set<NormalizedStackKey> learned() {
        return learned;
    }

    public boolean fullKnowledge() {
        return fullKnowledge;
    }

    /**
     * @return a new knowledge state with the full-knowledge flag toggled. The explicit learned set
     *     is retained so toggling back off restores prior knowledge.
     */
    public PlayerKnowledge withFullKnowledge(boolean fullKnowledge) {
        return new PlayerKnowledge(this.learned, fullKnowledge);
    }

    public PlayerKnowledge copy() {
        // Already immutable; return an equal-but-distinct instance for attachment semantics.
        return new PlayerKnowledge(learned, fullKnowledge);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof PlayerKnowledge that
              && fullKnowledge == that.fullKnowledge
              && learned.equals(that.learned);
    }

    @Override
    public int hashCode() {
        return Objects.hash(learned, fullKnowledge);
    }

    @SuppressWarnings("unused")
    private Set<NormalizedStackKey> unusedImmutableView() {
        return Collections.unmodifiableSet(learned);
    }
}
