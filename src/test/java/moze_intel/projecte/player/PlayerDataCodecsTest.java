package moze_intel.projecte.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.LinkedHashSet;
import java.util.Set;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.FakeStackKey;
import moze_intel.projecte.emc.NormalizedStackKey;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class PlayerDataCodecsTest {
    private static FakeStackKey key(String path) {
        return new FakeStackKey(Identifier.fromNamespaceAndPath("projecte", path));
    }

    @Test
    void knowledgeCodecRoundTripsEmpty() {
        PlayerKnowledge knowledge = PlayerKnowledge.empty();
        JsonElement encoded = PlayerDataCodecs.KNOWLEDGE_CODEC.encodeStart(JsonOps.INSTANCE, knowledge)
              .getOrThrow(IllegalStateException::new);
        PlayerKnowledge decoded = PlayerDataCodecs.KNOWLEDGE_CODEC.parse(JsonOps.INSTANCE, encoded)
              .getOrThrow(IllegalStateException::new);
        assertEquals(knowledge, decoded);
        assertTrue(decoded.learned().isEmpty());
    }

    @Test
    void knowledgeCodecRoundTripsLearnedItems() {
        NormalizedStackKey gem = key("gem");
        NormalizedStackKey ring = key("ring");
        Set<NormalizedStackKey> learned = new LinkedHashSet<>();
        learned.add(gem);
        learned.add(ring);
        PlayerKnowledge knowledge = PlayerKnowledge.of(learned, false);

        JsonElement encoded = PlayerDataCodecs.KNOWLEDGE_CODEC.encodeStart(JsonOps.INSTANCE, knowledge)
              .getOrThrow(IllegalStateException::new);
        PlayerKnowledge decoded = PlayerDataCodecs.KNOWLEDGE_CODEC.parse(JsonOps.INSTANCE, encoded)
              .getOrThrow(IllegalStateException::new);

        assertEquals(knowledge, decoded);
        assertEquals(Set.of(gem, ring), decoded.learned());
    }

    @Test
    void knowledgeCodecRoundTripsFullKnowledgeFlag() {
        PlayerKnowledge full = PlayerKnowledge.empty().withFullKnowledge(true);
        JsonElement encoded = PlayerDataCodecs.KNOWLEDGE_CODEC.encodeStart(JsonOps.INSTANCE, full)
              .getOrThrow(IllegalStateException::new);
        PlayerKnowledge decoded = PlayerDataCodecs.KNOWLEDGE_CODEC.parse(JsonOps.INSTANCE, encoded)
              .getOrThrow(IllegalStateException::new);
        assertEquals(full, decoded);
        assertTrue(decoded.fullKnowledge());
    }

    @Test
    void knowledgeCodecOmitsFullKnowledgeDefaultsToFalse() {
        // Encode then check the stored flag is present and default parses to false when absent.
        String json = "{\"knowledge\":[]}";
        PlayerKnowledge decoded = PlayerDataCodecs.KNOWLEDGE_CODEC.parse(
              JsonOps.INSTANCE, com.google.gson.JsonParser.parseString(json))
              .getOrThrow(IllegalStateException::new);
        assertEquals(PlayerKnowledge.empty(), decoded);
        assertFalse(decoded.fullKnowledge());
    }

    @Test
    void inputLocksCodecRoundTripsMixedSlots() {
        NormalizedStackKey gem = key("gem");
        NormalizedStackKey ring = key("ring");
        PlayerInputLocks locks = PlayerInputLocks.empty().set(0, gem).set(8, ring);

        JsonElement encoded = PlayerDataCodecs.INPUT_LOCKS_CODEC.encodeStart(JsonOps.INSTANCE, locks)
              .getOrThrow(IllegalStateException::new);
        PlayerInputLocks decoded = PlayerDataCodecs.INPUT_LOCKS_CODEC.parse(JsonOps.INSTANCE, encoded)
              .getOrThrow(IllegalStateException::new);

        assertEquals(locks, decoded);
        assertEquals(gem, decoded.get(0));
        assertEquals(ring, decoded.get(8));
        assertTrue(decoded.isEmpty(1));
    }

    @Test
    void emcCodecRoundTripsNonNegativeLongs() {
        for (long value : new long[] {0L, 1L, 1_000_000L, Long.MAX_VALUE}) {
            JsonElement encoded = PlayerDataCodecs.EMC_CODEC.encodeStart(JsonOps.INSTANCE, EmcValue.of(value))
                  .getOrThrow(IllegalStateException::new);
            EmcValue decoded = PlayerDataCodecs.EMC_CODEC.parse(JsonOps.INSTANCE, encoded)
                  .getOrThrow(IllegalStateException::new);
            assertEquals(EmcValue.of(value), decoded);
        }
    }

    private static void assertFalse(boolean condition) {
        org.junit.jupiter.api.Assertions.assertFalse(condition);
    }
}
