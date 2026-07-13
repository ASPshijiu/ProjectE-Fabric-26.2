package moze_intel.projecte.content;

import moze_intel.projecte.api.ProjectEAPI;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;

/**
 * Custom {@link ToolMaterial} definitions for ProjectE tools.
 *
 * <p>MC 26.2 uses data-component-driven tools where tier properties are
 * set via {@code Item.Properties.pickaxe(ToolMaterial, float, float)} etc.
 * Special abilities (AoE, vein mining, etc.) are handled through Fabric API
 * block-interaction events rather than custom item subclasses.
 */
public final class ModToolMaterials {
    private ModToolMaterials() {}

    // ── Tag keys for tier-gating ──────────────────────────────────────────
    public static final TagKey<Block> INCORRECT_FOR_DARK_MATTER =
          TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                ProjectEAPI.id("incorrect_for_dark_matter_tool"));
    public static final TagKey<Block> INCORRECT_FOR_RED_MATTER =
          TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                ProjectEAPI.id("incorrect_for_red_matter_tool"));

    public static final TagKey<Item> DARK_MATTER_REPAIR_ITEMS =
          TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                ProjectEAPI.id("dark_matter_repair"));
    public static final TagKey<Item> RED_MATTER_REPAIR_ITEMS =
          TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                ProjectEAPI.id("red_matter_repair"));

    // ── Dark Matter tier ──────────────────────────────────────────────────
    // Durability: 3x diamond; speed: 2x diamond; attack: diamond+6; enchantability: high
    public static final ToolMaterial DARK_MATTER = new ToolMaterial(
          INCORRECT_FOR_DARK_MATTER,
          4683,          // durability (diamond=1561, ~3x)
          12.0F,         // mining speed (diamond=8.0)
          5.0F,          // attack damage bonus (diamond=3.0)
          25,            // enchantment value (diamond=10)
          DARK_MATTER_REPAIR_ITEMS
    );

    // ── Red Matter tier ───────────────────────────────────────────────────
    // Durability: 3x dark matter; speed: 3x diamond; attack: diamond+10; enchantability: superb
    public static final ToolMaterial RED_MATTER = new ToolMaterial(
          INCORRECT_FOR_RED_MATTER,
          14049,         // durability (3x dark matter)
          20.0F,         // mining speed
          9.0F,          // attack damage bonus
          35,            // enchantment value
          RED_MATTER_REPAIR_ITEMS
    );
}
