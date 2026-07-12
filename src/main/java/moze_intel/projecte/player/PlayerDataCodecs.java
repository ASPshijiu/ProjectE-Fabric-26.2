package moze_intel.projecte.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.emc.NormalizedStackKeyCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * DFU {@link Codec}s and Fabric {@link StreamCodec}s for the player attachment payloads.
 *
 * <p>Save format (Codec):
 * <ul>
 *   <li>knowledge: {@code {"knowledge": ["item|ns:path|...", ...], "full_knowledge": bool}}</li>
 *   <li>input locks: {@code {"slots": ["key"|null x9]}}</li>
 *   <li>emc: non-negative long</li>
 * </ul>
 *
 * <p>Network format (StreamCodec) mirrors the same logical shape.
 */
public final class PlayerDataCodecs {
    private PlayerDataCodecs() {
    }

    public static final Codec<PlayerKnowledge> KNOWLEDGE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
          NormalizedStackKeyCodec.CODEC.listOf().fieldOf("knowledge")
                .forGetter(knowledge -> new ArrayList<>(knowledge.learned())),
          Codec.BOOL.optionalFieldOf("full_knowledge", false)
                .forGetter(PlayerKnowledge::fullKnowledge)
    ).apply(instance, (learned, full) -> PlayerKnowledge.of(new java.util.LinkedHashSet<>(learned), full)));

    public static final Codec<PlayerInputLocks> INPUT_LOCKS_CODEC = RecordCodecBuilder.create(instance -> instance.group(
          fixedSizeKeyList(PlayerInputLocks.LOCK_SLOTS)
                .fieldOf("slots")
                .forGetter(locks -> {
                      List<NormalizedStackKey> slots = new ArrayList<>(PlayerInputLocks.LOCK_SLOTS);
                      NormalizedStackKey[] raw = locks.toArray();
                      for (int slot = 0; slot < PlayerInputLocks.LOCK_SLOTS; slot++) {
                          slots.add(raw[slot]);
                      }
                      return slots;
                  })
    ).apply(instance, slots -> PlayerInputLocks.of(slots.toArray(new NormalizedStackKey[0]))));

    public static final Codec<EmcValue> EMC_CODEC = Codec.LONG
          .comapFlatMap(
                value -> value < 0
                      ? com.mojang.serialization.DataResult.error(() -> "negative EMC: " + value)
                      : com.mojang.serialization.DataResult.success(EmcValue.of(value)),
                EmcValue::longValue);

    private static Codec<List<NormalizedStackKey>> fixedSizeKeyList(int size) {
        return nullableElementList(size);
    }

    /**
     * Codec for a fixed-size list whose elements may be {@code null} (empty input-lock slots).
     * Each slot is encoded as its canonical key string; empty slots use the empty-string sentinel
     * {@code ""}, which can never be a valid canonical key.
     */
    private static Codec<List<NormalizedStackKey>> nullableElementList(int size) {
        return Codec.STRING.listOf().comapFlatMap(
              list -> {
                  if (list.size() != size) {
                      return com.mojang.serialization.DataResult.error(
                            () -> "expected " + size + " slots, got " + list.size());
                  }
                  List<NormalizedStackKey> slots = new ArrayList<>(size);
                  for (String entry : list) {
                      if (entry.isEmpty()) {
                          slots.add(null);
                      } else {
                          try {
                              slots.add(NormalizedStackKeyCodec.fromCanonical(entry));
                          } catch (IllegalArgumentException exception) {
                              return com.mojang.serialization.DataResult.error(exception::getMessage);
                          }
                      }
                  }
                  return com.mojang.serialization.DataResult.success(slots);
              },
              list -> {
                  List<String> encoded = new ArrayList<>(list.size());
                  for (NormalizedStackKey key : list) {
                      encoded.add(key == null ? "" : key.canonicalString());
                  }
                  return encoded;
              });
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerKnowledge> KNOWLEDGE_STREAM_CODEC = StreamCodec.composite(
          NormalizedStackKeyCodec.STREAM_CODEC.apply(ByteBufCodecs.collection(java.util.HashSet::new)),
          knowledge -> new java.util.HashSet<>(knowledge.learned()),
          ByteBufCodecs.BOOL,
          PlayerKnowledge::fullKnowledge,
          (learned, full) -> PlayerKnowledge.of(learned, full));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerInputLocks> INPUT_LOCKS_STREAM_CODEC = StreamCodec.composite(
          // Encode each slot as its canonical key string, using the empty-string sentinel for empty
          // slots (a value ByteBufCodecs cannot carry as null).
          ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.collection(java.util.ArrayList::new, PlayerInputLocks.LOCK_SLOTS)),
          locks -> {
              List<String> encoded = new java.util.ArrayList<>(PlayerInputLocks.LOCK_SLOTS);
              for (NormalizedStackKey key : locks.toArray()) {
                  encoded.add(key == null ? "" : key.canonicalString());
              }
              return encoded;
          },
          encoded -> {
              NormalizedStackKey[] slots = new NormalizedStackKey[PlayerInputLocks.LOCK_SLOTS];
              for (int slot = 0; slot < PlayerInputLocks.LOCK_SLOTS && slot < encoded.size(); slot++) {
                  String entry = encoded.get(slot);
                  slots[slot] = entry.isEmpty() ? null : NormalizedStackKeyCodec.fromCanonical(entry);
              }
              return PlayerInputLocks.of(slots);
          });

    public static final StreamCodec<RegistryFriendlyByteBuf, EmcValue> EMC_STREAM_CODEC =
          ByteBufCodecs.VAR_LONG.<EmcValue>map(EmcValue::of, EmcValue::longValue).cast();
}
