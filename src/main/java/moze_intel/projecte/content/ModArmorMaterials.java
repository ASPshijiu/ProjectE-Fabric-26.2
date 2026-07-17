package moze_intel.projecte.content;

import java.util.Map;
import moze_intel.projecte.api.ProjectEAPI;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

/**
 * Custom {@link ArmorMaterial} definitions for ProjectE armor sets:
 * Dark Matter, Red Matter, and Gem armor.
 */
public final class ModArmorMaterials {
    private ModArmorMaterials() {}

    // ── Equipment asset keys ──────────────────────────────────────────────
    public static final ResourceKey<EquipmentAsset> DARK_MATTER_ARMOR_ASSET =
          ResourceKey.create(EquipmentAssets.ROOT_ID, ProjectEAPI.id("dark_matter"));
    public static final ResourceKey<EquipmentAsset> RED_MATTER_ARMOR_ASSET =
          ResourceKey.create(EquipmentAssets.ROOT_ID, ProjectEAPI.id("red_matter"));
    public static final ResourceKey<EquipmentAsset> GEM_ARMOR_ASSET =
          ResourceKey.create(EquipmentAssets.ROOT_ID, ProjectEAPI.id("gem"));

    // ── Repair tags ───────────────────────────────────────────────────────
    public static final TagKey<Item> DARK_MATTER_REPAIR =
          TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                ProjectEAPI.id("dark_matter_repair"));
    public static final TagKey<Item> RED_MATTER_REPAIR =
          TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                ProjectEAPI.id("red_matter_repair"));
    public static final TagKey<Item> GEM_REPAIR =
          TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                ProjectEAPI.id("gem_repair"));

    // ── Defense values per slot ───────────────────────────────────────────
    private static final Map<ArmorType, Integer> DEFENSE = Map.of(
          ArmorType.HELMET, 3,
          ArmorType.CHESTPLATE, 8,
          ArmorType.LEGGINGS, 6,
          ArmorType.BOOTS, 3,
          ArmorType.BODY, 11
    );

    // ── Material instances ────────────────────────────────────────────────
    // durability multiplier = base durability for helmet (typically 11 * multiplier)

    /** Dark Matter: diamond base defense plus ProjectE's slot-weighted special reduction. */
    public static final ArmorMaterial DARK_MATTER = new ArmorMaterial(
          45,
          DEFENSE,
          1,
          SoundEvents.ARMOR_EQUIP_DIAMOND,
          2.0F,
          0.1F,
          DARK_MATTER_REPAIR,
          DARK_MATTER_ARMOR_ASSET
    );

    /** Red Matter: diamond base defense plus stronger ProjectE reduction. */
    public static final ArmorMaterial RED_MATTER = new ArmorMaterial(
          60,                          // durability multiplier
          DEFENSE,
          1,
          SoundEvents.ARMOR_EQUIP_NETHERITE,
          2.0F,
          0.2F,
          RED_MATTER_REPAIR,
          RED_MATTER_ARMOR_ASSET
    );

    /** Gem armor: diamond base defense, maximum ProjectE reduction and knockback resistance. */
    public static final ArmorMaterial GEM = new ArmorMaterial(
          80,                          // durability multiplier
          DEFENSE,
          1,
          SoundEvents.ARMOR_EQUIP_NETHERITE,
          2.0F,
          0.25F,
          GEM_REPAIR,
          GEM_ARMOR_ASSET
    );
}
