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
    private static final Map<ArmorType, Integer> DM_DEFENSE = Map.of(
          ArmorType.HELMET, 3,
          ArmorType.CHESTPLATE, 8,
          ArmorType.LEGGINGS, 6,
          ArmorType.BOOTS, 3,
          ArmorType.BODY, 11
    );

    private static final Map<ArmorType, Integer> RM_DEFENSE = Map.of(
          ArmorType.HELMET, 4,
          ArmorType.CHESTPLATE, 10,
          ArmorType.LEGGINGS, 8,
          ArmorType.BOOTS, 4,
          ArmorType.BODY, 14
    );

    private static final Map<ArmorType, Integer> GEM_DEFENSE = Map.of(
          ArmorType.HELMET, 5,
          ArmorType.CHESTPLATE, 12,
          ArmorType.LEGGINGS, 10,
          ArmorType.BOOTS, 5,
          ArmorType.BODY, 16
    );

    // ── Material instances ────────────────────────────────────────────────
    // durability multiplier = base durability for helmet (typically 11 * multiplier)

    /** Dark Matter: ~2× diamond durability, defense slightly better */
    public static final ArmorMaterial DARK_MATTER = new ArmorMaterial(
          45,                          // durability multiplier (diamond=33)
          DM_DEFENSE,
          20,                          // enchantment value
          SoundEvents.ARMOR_EQUIP_DIAMOND,
          3.0F,                        // toughness (diamond=2.0)
          0.1F,                        // knockback resistance (netherite=0.1, diamond=0.0)
          DARK_MATTER_REPAIR,
          DARK_MATTER_ARMOR_ASSET
    );

    /** Red Matter: ~3× diamond durability, excellent defense */
    public static final ArmorMaterial RED_MATTER = new ArmorMaterial(
          60,                          // durability multiplier
          RM_DEFENSE,
          30,                          // enchantment value
          SoundEvents.ARMOR_EQUIP_NETHERITE,
          4.0F,                        // toughness
          0.15F,                       // knockback resistance
          RED_MATTER_REPAIR,
          RED_MATTER_ARMOR_ASSET
    );

    /** Gem: end-game armor, best durability and defense */
    public static final ArmorMaterial GEM = new ArmorMaterial(
          80,                          // durability multiplier
          GEM_DEFENSE,
          40,                          // enchantment value
          SoundEvents.ARMOR_EQUIP_NETHERITE,
          5.0F,                        // toughness
          0.2F,                        // knockback resistance
          GEM_REPAIR,
          GEM_ARMOR_ASSET
    );
}
