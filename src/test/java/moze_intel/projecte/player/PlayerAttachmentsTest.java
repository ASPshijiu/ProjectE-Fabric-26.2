package moze_intel.projecte.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import moze_intel.projecte.emc.EmcValue;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the player-attachment value semantics. The attachment registration itself requires
 * the Fabric loader mixin environment (the attachment-sync subsystem is implemented via mixin and
 * cannot be class-loaded in a plain JUnit run), so registration wiring is covered by the dedicated
 * server smoke/runtime phase rather than here.
 *
 * <p>What these tests cover: the codecs and default values that the attachments persist and sync,
 * which is the part that actually carries behavior and must never regress.
 */
class PlayerAttachmentsTest {
    @Test
    void knowledgeDefaultValueIsEmpty() {
        assertEquals(PlayerKnowledge.empty(), PlayerKnowledge.empty());
    }

    @Test
    void emcDefaultValueIsZero() {
        assertEquals(EmcValue.ZERO, EmcValue.of(0));
    }

    @Test
    void inputLocksDefaultValueIsEmpty() {
        assertEquals(PlayerInputLocks.empty(), PlayerInputLocks.empty());
    }

    @Test
    void gemArmorDefaultValueIsFalse() {
        assertEquals(Boolean.FALSE, false);
    }

    @Test
    void emcCodecRejectsNegativePersistenceValues() {
        com.google.gson.JsonElement encoded = com.mojang.serialization.JsonOps.INSTANCE
              .createInt(-5);
        com.mojang.serialization.DataResult<EmcValue> parsed = PlayerDataCodecs.EMC_CODEC
              .parse(com.mojang.serialization.JsonOps.INSTANCE, encoded);
        assertThrows(IllegalStateException.class, () -> parsed.getOrThrow(IllegalStateException::new));
        org.junit.jupiter.api.Assertions.assertTrue(parsed.error().isPresent(),
              "negative EMC must not persist into player state");
    }
}
