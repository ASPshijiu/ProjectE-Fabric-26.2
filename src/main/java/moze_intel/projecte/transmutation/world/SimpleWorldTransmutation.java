package moze_intel.projecte.transmutation.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A world transmutation from one block to another, with an optional alternate (shift) result.
 *
 * <p>When {@code altResult} equals {@code result}, {@link #hasAlternate()} reports false and the
 * codec omits the redundant field — matching ProjectE's wire format.
 *
 * @param origin    the block that matches this transmutation.
 * @param result    the normal right-click result.
 * @param altResult the shift right-click result (defaults to {@code result}).
 */
public record SimpleWorldTransmutation(
      Holder<Block> origin, Holder<Block> result, Holder<Block> altResult
) {
    /** Codec key for the origin block. */
    public static final String ORIGIN_KEY = "origin";
    /** Codec key for the result block. */
    public static final String RESULT_KEY = "result";
    /** Codec key for the alternate result block. */
    public static final String ALT_RESULT_KEY = "alt_result";

    private static final Codec<Holder<Block>> BLOCK_CODEC = BuiltInRegistries.BLOCK.holderByNameCodec();
    private static final StreamCodec<RegistryFriendlyByteBuf, Holder<Block>> BLOCK_STREAM_CODEC =
          ByteBufCodecs.holderRegistry(Registries.BLOCK);

    public SimpleWorldTransmutation {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(altResult, "altResult");
    }

    public SimpleWorldTransmutation(Holder<Block> origin, Holder<Block> result) {
        this(origin, result, result);
    }

    public boolean hasAlternate() {
        return !result.equals(altResult);
    }

    /**
     * Applies this selected rule to a target state, returning {@code null} when the target does not
     * have the same origin block. A charged Philosopher's Stone selects one rule from its center
     * block and reuses it across the whole area.
     */
    public @Nullable BlockState result(BlockState state, boolean useAlternate) {
        Objects.requireNonNull(state, "state");
        if (state.getBlock() != origin.value()) {
            return null;
        }
        Block resultBlock = (useAlternate ? altResult : result).value();
        return WorldTransmutationAction.copySharedStateProperties(
              state, resultBlock.defaultBlockState());
    }

    public static final Codec<SimpleWorldTransmutation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          BLOCK_CODEC.fieldOf(ORIGIN_KEY).forGetter(SimpleWorldTransmutation::origin),
          BLOCK_CODEC.fieldOf(RESULT_KEY).forGetter(SimpleWorldTransmutation::result),
          BLOCK_CODEC.optionalFieldOf(ALT_RESULT_KEY)
                .forGetter(entry -> entry.hasAlternate() ? Optional.of(entry.altResult) : Optional.empty())
    ).apply(instance, (origin, result, altResult) ->
          new SimpleWorldTransmutation(origin, result, altResult.orElse(result))));

    public static final StreamCodec<RegistryFriendlyByteBuf, SimpleWorldTransmutation> STREAM_CODEC =
          new StreamCodec<>() {
              @Override
              public SimpleWorldTransmutation decode(RegistryFriendlyByteBuf buffer) {
                  Holder<Block> origin = BLOCK_STREAM_CODEC.decode(buffer);
                  Holder<Block> result = BLOCK_STREAM_CODEC.decode(buffer);
                  if (buffer.readBoolean()) {
                      return new SimpleWorldTransmutation(origin, result, BLOCK_STREAM_CODEC.decode(buffer));
                  }
                  return new SimpleWorldTransmutation(origin, result);
              }

              @Override
              public void encode(RegistryFriendlyByteBuf buffer, SimpleWorldTransmutation value) {
                  BLOCK_STREAM_CODEC.encode(buffer, value.origin);
                  BLOCK_STREAM_CODEC.encode(buffer, value.result);
                  boolean hasAlt = value.hasAlternate();
                  buffer.writeBoolean(hasAlt);
                  if (hasAlt) {
                      BLOCK_STREAM_CODEC.encode(buffer, value.altResult);
                  }
              }
          };
}
