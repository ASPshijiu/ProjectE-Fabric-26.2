package moze_intel.projecte.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.Map;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.emc.NormalizedStackKeyCodec;
import moze_intel.projecte.network.payloads.EmcMappingSyncPayload;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

/**
 * Exercises the wire codec for {@link EmcMappingSyncPayload} end-to-end: build a payload, encode it
 * to a {@link RegistryFriendlyByteBuf} exactly as the server would send it, then decode it as the
 * client would receive it. This is the path the live S2C packet takes; if encoding or decoding
 * silently drops entries the client snapshot stays empty and no tooltip ever shows EMC.
 */
class EmcMappingSyncPayloadCodecTest {

    @Test
    void payloadRoundTripsThroughRegistryFriendlyByteBuf() {
        Map<NormalizedStackKey, EmcValue> original = new HashMap<>();
        original.put(NormalizedStackKeyCodec.fromCanonical("item|projecte:dark_matter|{}"), EmcValue.of(139264));
        original.put(NormalizedStackKeyCodec.fromCanonical("item|minecraft:cobblestone|{}"), EmcValue.of(1));
        original.put(NormalizedStackKeyCodec.fromCanonical("item|projecte:dm_pick|{}"), EmcValue.of(434304));
        original.put(NormalizedStackKeyCodec.fromCanonical("item|minecraft:diamond|{}"), EmcValue.of(8192));
        EmcMappingSyncPayload payload = new EmcMappingSyncPayload(original);

        // Empty registry access is fine: the canonical-string stream codec never touches registries.
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        EmcMappingSyncPayload.STREAM_CODEC.encode(buf, payload);
        EmcMappingSyncPayload decoded = EmcMappingSyncPayload.STREAM_CODEC.decode(buf);

        assertEquals(original.size(), decoded.values().size(),
              "decoded payload must contain the same number of entries");
        for (Map.Entry<NormalizedStackKey, EmcValue> entry : original.entrySet()) {
            EmcValue got = decoded.values().get(entry.getKey());
            assertTrue(got != null,
                  "decoded payload missing key " + entry.getKey().canonicalString());
            assertEquals(entry.getValue(), got,
                  "value mismatch for " + entry.getKey().canonicalString());
        }
    }

    @Test
    void payloadRoundTripsLargeMapping() {
        // Simulate the ~680-entry real dataset to confirm the map codec does not truncate.
        Map<NormalizedStackKey, EmcValue> original = new HashMap<>();
        for (int i = 0; i < 700; i++) {
            original.put(
                  NormalizedStackKeyCodec.fromCanonical("item|projecte:item_" + i + "|{}"),
                  EmcValue.of(1000L + i));
        }
        EmcMappingSyncPayload payload = new EmcMappingSyncPayload(original);

        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        EmcMappingSyncPayload.STREAM_CODEC.encode(buf, payload);
        EmcMappingSyncPayload decoded = EmcMappingSyncPayload.STREAM_CODEC.decode(buf);

        assertEquals(700, decoded.values().size(),
              "large mapping must round-trip without truncation");
    }

    @Test
    void payloadRoundTripsEmptyMappingToClearClientState() {
        EmcMappingSyncPayload payload = new EmcMappingSyncPayload(Map.of());
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
              Unpooled.buffer(), RegistryAccess.EMPTY);

        EmcMappingSyncPayload.STREAM_CODEC.encode(buf, payload);

        assertTrue(EmcMappingSyncPayload.STREAM_CODEC.decode(buf).values().isEmpty());
    }
}
