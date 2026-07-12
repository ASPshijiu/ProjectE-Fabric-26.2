package moze_intel.projecte.transmutation.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;

/**
 * Decoded form of a {@code data/<ns>/pe_world_transmutations/<name>.json} file: an optional comment
 * and a list of {@link SimpleWorldTransmutation} entries.
 */
public record WorldTransmutationFile(String comment, List<SimpleWorldTransmutation> transmutations) {
    public static final Codec<WorldTransmutationFile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Codec.STRING.optionalFieldOf("comment")
                .forGetter(file -> Optional.ofNullable(file.comment())),
          SimpleWorldTransmutation.CODEC.listOf().fieldOf("transmutations")
                .forGetter(WorldTransmutationFile::transmutations)
    ).apply(instance, (comment, transmutations) ->
          new WorldTransmutationFile(comment.orElse(null), transmutations)));

    public WorldTransmutationFile {
        transmutations = List.copyOf(transmutations);
    }
}
