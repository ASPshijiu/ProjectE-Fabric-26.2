package moze_intel.projecte.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Set;
import moze_intel.projecte.emc.FakeStackKey;
import moze_intel.projecte.emc.NormalizedStackKey;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class PlayerKnowledgeTest {
    private static NormalizedStackKey item(String path) {
        return new FakeStackKey(Identifier.fromNamespaceAndPath("projecte", path));
    }

    @Test
    void emptyKnowsNothing() {
        PlayerKnowledge knowledge = PlayerKnowledge.empty();
        assertTrue(knowledge.learned().isEmpty());
        assertFalse(knowledge.fullKnowledge());
        assertFalse(knowledge.has(item("missing")));
    }

    @Test
    void learningNewItemAddsAndReportsChange() {
        PlayerKnowledge knowledge = PlayerKnowledge.empty();
        NormalizedStackKey gem = item("gem");
        PlayerKnowledge next = knowledge.learn(gem);
        assertTrue(next.has(gem));
        assertEquals(Set.of(gem), next.learned());
        assertFalse(next.fullKnowledge());
        // original is untouched
        assertFalse(knowledge.has(gem));
        assertTrue(knowledge.learned().isEmpty());
    }

    @Test
    void learningKnownItemIsNoOp() {
        NormalizedStackKey gem = item("gem");
        PlayerKnowledge knowledge = PlayerKnowledge.empty().learn(gem);
        PlayerKnowledge next = knowledge.learn(gem);
        assertEquals(knowledge, next);
        assertEquals(Set.of(gem), next.learned());
    }

    @Test
    void unlearningUnknownIsNoOp() {
        NormalizedStackKey gem = item("gem");
        PlayerKnowledge knowledge = PlayerKnowledge.empty();
        PlayerKnowledge next = knowledge.unlearn(gem);
        assertEquals(knowledge, next);
        assertFalse(next.has(gem));
    }

    @Test
    void unlearningKnownRemovesIt() {
        NormalizedStackKey gem = item("gem");
        PlayerKnowledge knowledge = PlayerKnowledge.empty().learn(gem);
        PlayerKnowledge next = knowledge.unlearn(gem);
        assertFalse(next.has(gem));
        assertTrue(next.learned().isEmpty());
    }

    @Test
    void fullKnowledgeShortCircuitsHas() {
        PlayerKnowledge full = PlayerKnowledge.empty().withFullKnowledge(true);
        assertTrue(full.fullKnowledge());
        assertTrue(full.has(item("anything")));
        assertTrue(full.has(item("also_unknown")));
        assertTrue(full.learned().isEmpty());
    }

    @Test
    void togglingFullKnowledgeOffRestoresExplicitSet() {
        NormalizedStackKey gem = item("gem");
        PlayerKnowledge full = PlayerKnowledge.empty().learn(gem).withFullKnowledge(true);
        PlayerKnowledge back = full.withFullKnowledge(false);
        assertFalse(back.fullKnowledge());
        assertTrue(back.has(gem));
        assertFalse(back.has(item("unknown")));
    }

    @Test
    void learnedSetIsImmutable() {
        NormalizedStackKey gem = item("gem");
        PlayerKnowledge knowledge = PlayerKnowledge.empty().learn(gem);
        Set<NormalizedStackKey> learned = knowledge.learned();
        org.junit.jupiter.api.Assertions.assertThrows(
              UnsupportedOperationException.class, () -> learned.add(item("extra")));
    }

    @Test
    void copyIsIndependentInstance() {
        NormalizedStackKey gem = item("gem");
        PlayerKnowledge knowledge = PlayerKnowledge.empty().learn(gem);
        PlayerKnowledge copy = knowledge.copy();
        assertEquals(knowledge, copy);
        assertNotSame(knowledge, copy);
        PlayerKnowledge mutated = copy.learn(item("ring"));
        assertTrue(mutated.has(item("ring")));
        assertFalse(knowledge.has(item("ring")));
    }
}
