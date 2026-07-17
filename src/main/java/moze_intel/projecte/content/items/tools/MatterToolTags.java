package moze_intel.projecte.content.items.tools;

import moze_intel.projecte.api.ProjectEAPI;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class MatterToolTags {
    public static final TagKey<Block> HAMMER = projecte("mineable/hammer");
    public static final TagKey<Block> KATAR = projecte("mineable/katar");
    public static final TagKey<Block> MORNING_STAR = projecte("mineable/morning_star");
    public static final TagKey<Block> VEIN_SHOVEL = projecte("vein/shovel");
    public static final TagKey<Block> ORES = TagKey.create(
          Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores"));

    private MatterToolTags() {
    }

    private static TagKey<Block> projecte(String path) {
        return TagKey.create(Registries.BLOCK, ProjectEAPI.id(path));
    }
}
