package moze_intel.projecte.content;

import moze_intel.projecte.ProjectE;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.content.items.PhilosophersStoneItem;
import moze_intel.projecte.content.items.RepairTalismanItem;
import moze_intel.projecte.content.items.AlchemicalBagItem;
import moze_intel.projecte.content.items.KleinStarItem;
import moze_intel.projecte.content.items.TomeOfKnowledgeItem;
import moze_intel.projecte.content.items.TransmutationTabletItem;
import moze_intel.projecte.content.items.DarkMatterHammerItem;
import moze_intel.projecte.content.items.DestructionCatalystItem;
import moze_intel.projecte.content.items.DiviningRodItem;
import moze_intel.projecte.content.items.RedMatterHammerItem;
import moze_intel.projecte.content.items.RedMatterKatarItem;
import moze_intel.projecte.content.items.RedMatterMorningStarItem;
import moze_intel.projecte.content.items.SwiftwolfRendingGaleItem;
import moze_intel.projecte.content.items.HarvestGoddessBandItem;
import moze_intel.projecte.content.items.IgnitionRingItem;
import moze_intel.projecte.content.items.ZeroRingItem;
import moze_intel.projecte.content.items.VoidRingItem;
import moze_intel.projecte.content.items.BlackHoleBandItem;
import moze_intel.projecte.content.items.ManualItem;
import moze_intel.projecte.content.items.BodyStoneItem;
import moze_intel.projecte.content.items.SoulStoneItem;
import moze_intel.projecte.content.items.MindStoneItem;
import moze_intel.projecte.content.items.LifeStoneItem;
import moze_intel.projecte.content.items.WatchOfFlowingTimeItem;
import moze_intel.projecte.content.items.tools.MatterAxeItem;
import moze_intel.projecte.content.items.tools.MatterHoeItem;
import moze_intel.projecte.content.items.tools.MatterPickaxeItem;
import moze_intel.projecte.content.items.tools.MatterShearsItem;
import moze_intel.projecte.content.items.tools.MatterShovelItem;
import moze_intel.projecte.content.items.tools.MatterSwordItem;
import moze_intel.projecte.content.items.tools.MatterToolTags;
import moze_intel.projecte.content.items.tools.RedMatterSwordItem;
import moze_intel.projecte.content.items.armor.MatterArmorItem;
import moze_intel.projecte.content.items.armor.GemArmorItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorType;
import org.jetbrains.annotations.Nullable;

/**
 * Central item registration hub for ProjectE Fabric 26.2.
 *
 * <p>Registers every item present in the upstream ProjectE 1.21.1 baseline
 * (commit 15d4ce65bd06eb4222709b984255fbf5080e78bc). Simple materials use
 * vanilla {@link Item}; items with gameplay behaviour have dedicated classes.
 */
public final class ModItems {
    // ── Philosopher's Stone & Repair Talisman ──────────────────────────────
    public static final Identifier PHILOSOPHERS_STONE_ID = ProjectEAPI.id("philosophers_stone");
    public static final Identifier REPAIR_TALISMAN_ID = ProjectEAPI.id("repair_talisman");
    public static Item PHILOSOPHERS_STONE;
    public static Item REPAIR_TALISMAN;

    // ── Covalence Dusts ────────────────────────────────────────────────────
    public static final Identifier LOW_COVALENCE_DUST_ID = ProjectEAPI.id("low_covalence_dust");
    public static final Identifier MEDIUM_COVALENCE_DUST_ID = ProjectEAPI.id("medium_covalence_dust");
    public static final Identifier HIGH_COVALENCE_DUST_ID = ProjectEAPI.id("high_covalence_dust");
    public static Item LOW_COVALENCE_DUST;
    public static Item MEDIUM_COVALENCE_DUST;
    public static Item HIGH_COVALENCE_DUST;

    // ── Fuels ──────────────────────────────────────────────────────────────
    public static final Identifier ALCHEMICAL_COAL_ID = ProjectEAPI.id("alchemical_coal");
    public static final Identifier MOBIUS_FUEL_ID = ProjectEAPI.id("mobius_fuel");
    public static final Identifier AETERNALIS_FUEL_ID = ProjectEAPI.id("aeternalis_fuel");
    public static Item ALCHEMICAL_COAL;
    public static Item MOBIUS_FUEL;
    public static Item AETERNALIS_FUEL;

    // ── Dark / Red Matter ──────────────────────────────────────────────────
    public static final Identifier DARK_MATTER_ID = ProjectEAPI.id("dark_matter");
    public static final Identifier RED_MATTER_ID = ProjectEAPI.id("red_matter");
    public static Item DARK_MATTER;
    public static Item RED_MATTER;

    // ── Alchemical Bags (16 colours) ───────────────────────────────────────
    public static final DyeColor[] BAG_COLORS = DyeColor.values();
    public static Item[] ALCHEMICAL_BAGS = new Item[16];

    // ── Klein Stars (6 tiers) ──────────────────────────────────────────────
    public static Item KLEIN_STAR_EIN;
    public static Item KLEIN_STAR_ZWEI;
    public static Item KLEIN_STAR_DREI;
    public static Item KLEIN_STAR_VIER;
    public static Item KLEIN_STAR_SPHERE;
    public static Item KLEIN_STAR_OMEGA;

    // ── Dark Matter Tools ──────────────────────────────────────────────────
    public static Item DARK_MATTER_PICKAXE;
    public static Item DARK_MATTER_AXE;
    public static Item DARK_MATTER_SHOVEL;
    public static Item DARK_MATTER_SWORD;
    public static Item DARK_MATTER_HOE;
    public static Item DARK_MATTER_SHEARS;
    public static Item DARK_MATTER_HAMMER;

    // ── Red Matter Tools ───────────────────────────────────────────────────
    public static Item RED_MATTER_PICKAXE;
    public static Item RED_MATTER_AXE;
    public static Item RED_MATTER_SHOVEL;
    public static Item RED_MATTER_SWORD;
    public static Item RED_MATTER_HOE;
    public static Item RED_MATTER_SHEARS;
    public static Item RED_MATTER_HAMMER;
    public static Item RED_MATTER_KATAR;
    public static Item RED_MATTER_MORNING_STAR;

    // ── Dark Matter Armor ──────────────────────────────────────────────────
    public static Item DARK_MATTER_HELMET;
    public static Item DARK_MATTER_CHESTPLATE;
    public static Item DARK_MATTER_LEGGINGS;
    public static Item DARK_MATTER_BOOTS;

    // ── Red Matter Armor ───────────────────────────────────────────────────
    public static Item RED_MATTER_HELMET;
    public static Item RED_MATTER_CHESTPLATE;
    public static Item RED_MATTER_LEGGINGS;
    public static Item RED_MATTER_BOOTS;

    // ── Gem Armor ──────────────────────────────────────────────────────────
    public static Item GEM_HELMET;
    public static Item GEM_CHESTPLATE;
    public static Item GEM_LEGGINGS;
    public static Item GEM_BOOTS;

    // ── Rings, Amulets, and Accessories ────────────────────────────────────
    public static Item IRON_BAND;
    public static Item BLACK_HOLE_BAND;
    public static Item ARCHANGEL_SMITE;
    public static Item HARVEST_GODDESS_BAND;
    public static Item IGNITION_RING;
    public static Item ZERO_RING;
    public static Item SWIFTWOLF_RENDING_GALE;
    public static Item WATCH_OF_FLOWING_TIME;
    public static Item EVERTIDE_AMULET;
    public static Item VOLCANITE_AMULET;
    public static Item GEM_OF_ETERNAL_DENSITY;
    public static Item MERCURIAL_EYE;
    public static Item VOID_RING;
    public static Item ARCANA_RING;
    public static Item BODY_STONE;
    public static Item SOUL_STONE;
    public static Item MIND_STONE;
    public static Item LIFE_STONE;

    // ── Divining Rods ──────────────────────────────────────────────────────
    public static Item LOW_DIVINING_ROD;
    public static Item MEDIUM_DIVINING_ROD;
    public static Item HIGH_DIVINING_ROD;

    // ── Catalysts & Lenses ─────────────────────────────────────────────────
    public static Item DESTRUCTION_CATALYST;
    public static Item HYPERKINETIC_LENS;
    public static Item CATALYTIC_LENS;

    // ── Tome & Tablet ──────────────────────────────────────────────────────
    public static Item TOME_OF_KNOWLEDGE;
    public static Item TRANSMUTATION_TABLET;

    // ── Misc (Manual) ──────────────────────────────────────────────────────
    public static Item MANUAL;

    private static boolean initialized = false;

    private ModItems() {}

    /**
     * Constructs and registers all ProjectE items. Idempotent.
     */
    public static void init() {
        if (initialized) return;
        initialized = true;

        // ── Philosopher's Stone ──
        PHILOSOPHERS_STONE = register(PHILOSOPHERS_STONE_ID,
              new PhilosophersStoneItem(properties(PHILOSOPHERS_STONE_ID)
                    .stacksTo(1)
                    .component(ModDataComponents.CHARGE, 0)
                    .component(ModDataComponents.PHILOSOPHERS_STONE_MODE, 0)));

        // ── Repair Talisman ──
        REPAIR_TALISMAN = register(REPAIR_TALISMAN_ID,
              new RepairTalismanItem(properties(REPAIR_TALISMAN_ID).stacksTo(1)));

        // ── Covalence Dusts ──
        LOW_COVALENCE_DUST = register(LOW_COVALENCE_DUST_ID, simple(LOW_COVALENCE_DUST_ID));
        MEDIUM_COVALENCE_DUST = register(MEDIUM_COVALENCE_DUST_ID, simple(MEDIUM_COVALENCE_DUST_ID));
        HIGH_COVALENCE_DUST = register(HIGH_COVALENCE_DUST_ID, simple(HIGH_COVALENCE_DUST_ID));

        // ── Fuels ──
        ALCHEMICAL_COAL = register(ALCHEMICAL_COAL_ID, simple(ALCHEMICAL_COAL_ID));
        MOBIUS_FUEL = register(MOBIUS_FUEL_ID, simple(MOBIUS_FUEL_ID));
        AETERNALIS_FUEL = register(AETERNALIS_FUEL_ID,
              new Item(properties(AETERNALIS_FUEL_ID).rarity(Rarity.RARE)));

        // ── Dark / Red Matter ──
        DARK_MATTER = register(DARK_MATTER_ID, fireImmune(DARK_MATTER_ID));
        RED_MATTER = register(RED_MATTER_ID, fireImmune(RED_MATTER_ID));

        // ── Alchemical Bags (16 colours) ──
        for (int i = 0; i < BAG_COLORS.length; i++) {
            DyeColor color = BAG_COLORS[i];
            Identifier bagId = ProjectEAPI.id(color.getName() + "_alchemical_bag");
            ALCHEMICAL_BAGS[i] = register(bagId,
                  new AlchemicalBagItem(properties(bagId).stacksTo(1), color));
        }

        // ── Klein Stars (6 tiers) ──
        KLEIN_STAR_EIN = registerKleinStar("ein", false);
        KLEIN_STAR_ZWEI = registerKleinStar("zwei", false);
        KLEIN_STAR_DREI = registerKleinStar("drei", false);
        KLEIN_STAR_VIER = registerKleinStar("vier", false);
        KLEIN_STAR_SPHERE = registerKleinStar("sphere", false);
        KLEIN_STAR_OMEGA = registerKleinStar("omega", true);

        // ── Dark Matter Tools ──
        DARK_MATTER_PICKAXE = register(ProjectEAPI.id("dm_pick"), dmPickaxe(ProjectEAPI.id("dm_pick")));
        DARK_MATTER_AXE = register(ProjectEAPI.id("dm_axe"), dmAxe(ProjectEAPI.id("dm_axe")));
        DARK_MATTER_SHOVEL = register(ProjectEAPI.id("dm_shovel"), dmShovel(ProjectEAPI.id("dm_shovel")));
        DARK_MATTER_SWORD = register(ProjectEAPI.id("dm_sword"), dmSword(ProjectEAPI.id("dm_sword")));
        DARK_MATTER_HOE = register(ProjectEAPI.id("dm_hoe"), dmHoe(ProjectEAPI.id("dm_hoe")));
        DARK_MATTER_SHEARS = register(ProjectEAPI.id("dm_shears"), dmShears(ProjectEAPI.id("dm_shears")));
        DARK_MATTER_HAMMER = register(ProjectEAPI.id("dm_hammer"),
              new DarkMatterHammerItem(dmToolProps(ProjectEAPI.id("dm_hammer")).tool(
                    ModToolMaterials.DARK_MATTER, MatterToolTags.HAMMER, 10.0F, -3.0F, 0.0F)));

        // ── Red Matter Tools ──
        RED_MATTER_PICKAXE = register(ProjectEAPI.id("rm_pick"), rmPickaxe(ProjectEAPI.id("rm_pick")));
        RED_MATTER_AXE = register(ProjectEAPI.id("rm_axe"), rmAxe(ProjectEAPI.id("rm_axe")));
        RED_MATTER_SHOVEL = register(ProjectEAPI.id("rm_shovel"), rmShovel(ProjectEAPI.id("rm_shovel")));
        RED_MATTER_SWORD = register(ProjectEAPI.id("rm_sword"), rmSword(ProjectEAPI.id("rm_sword")));
        RED_MATTER_HOE = register(ProjectEAPI.id("rm_hoe"), rmHoe(ProjectEAPI.id("rm_hoe")));
        RED_MATTER_SHEARS = register(ProjectEAPI.id("rm_shears"), rmShears(ProjectEAPI.id("rm_shears")));
        RED_MATTER_HAMMER = register(ProjectEAPI.id("rm_hammer"),
              new RedMatterHammerItem(rmToolProps(ProjectEAPI.id("rm_hammer")).tool(
                    ModToolMaterials.RED_MATTER, MatterToolTags.HAMMER, 10.0F, -3.0F, 0.0F)));
        RED_MATTER_KATAR = register(ProjectEAPI.id("rm_katar"),
              new RedMatterKatarItem(rmToolProps(ProjectEAPI.id("rm_katar")).tool(
                    ModToolMaterials.RED_MATTER, MatterToolTags.KATAR, 19.0F, -2.4F, 0.0F)));
        RED_MATTER_MORNING_STAR = register(ProjectEAPI.id("rm_morning_star"),
              new RedMatterMorningStarItem(rmToolProps(ProjectEAPI.id("rm_morning_star")).tool(
                    ModToolMaterials.RED_MATTER, MatterToolTags.MORNING_STAR,
                    16.0F, -3.0F, 0.0F)));

        // ── Dark Matter Armor ──
        DARK_MATTER_HELMET = register(ProjectEAPI.id("dm_helmet"),
              new MatterArmorItem(dmArmorProps(ProjectEAPI.id("dm_helmet"), ArmorType.HELMET),
                    ArmorType.HELMET, MatterArmorItem.Tier.DARK_MATTER));
        DARK_MATTER_CHESTPLATE = register(ProjectEAPI.id("dm_chestplate"),
              new MatterArmorItem(dmArmorProps(ProjectEAPI.id("dm_chestplate"), ArmorType.CHESTPLATE),
                    ArmorType.CHESTPLATE, MatterArmorItem.Tier.DARK_MATTER));
        DARK_MATTER_LEGGINGS = register(ProjectEAPI.id("dm_leggings"),
              new MatterArmorItem(dmArmorProps(ProjectEAPI.id("dm_leggings"), ArmorType.LEGGINGS),
                    ArmorType.LEGGINGS, MatterArmorItem.Tier.DARK_MATTER));
        DARK_MATTER_BOOTS = register(ProjectEAPI.id("dm_boots"),
              new MatterArmorItem(dmArmorProps(ProjectEAPI.id("dm_boots"), ArmorType.BOOTS),
                    ArmorType.BOOTS, MatterArmorItem.Tier.DARK_MATTER));

        // ── Red Matter Armor ──
        RED_MATTER_HELMET = register(ProjectEAPI.id("rm_helmet"),
              new MatterArmorItem(rmArmorProps(ProjectEAPI.id("rm_helmet"), ArmorType.HELMET),
                    ArmorType.HELMET, MatterArmorItem.Tier.RED_MATTER));
        RED_MATTER_CHESTPLATE = register(ProjectEAPI.id("rm_chestplate"),
              new MatterArmorItem(rmArmorProps(ProjectEAPI.id("rm_chestplate"), ArmorType.CHESTPLATE),
                    ArmorType.CHESTPLATE, MatterArmorItem.Tier.RED_MATTER));
        RED_MATTER_LEGGINGS = register(ProjectEAPI.id("rm_leggings"),
              new MatterArmorItem(rmArmorProps(ProjectEAPI.id("rm_leggings"), ArmorType.LEGGINGS),
                    ArmorType.LEGGINGS, MatterArmorItem.Tier.RED_MATTER));
        RED_MATTER_BOOTS = register(ProjectEAPI.id("rm_boots"),
              new MatterArmorItem(rmArmorProps(ProjectEAPI.id("rm_boots"), ArmorType.BOOTS),
                    ArmorType.BOOTS, MatterArmorItem.Tier.RED_MATTER));

        // ── Gem Armor ──
        GEM_HELMET = register(ProjectEAPI.id("gem_helmet"),
              new GemArmorItem(gemArmorProps(ProjectEAPI.id("gem_helmet"), ArmorType.HELMET),
                    ArmorType.HELMET));
        GEM_CHESTPLATE = register(ProjectEAPI.id("gem_chestplate"),
              new GemArmorItem(gemArmorProps(ProjectEAPI.id("gem_chestplate"), ArmorType.CHESTPLATE),
                    ArmorType.CHESTPLATE));
        GEM_LEGGINGS = register(ProjectEAPI.id("gem_leggings"),
              new GemArmorItem(gemArmorProps(ProjectEAPI.id("gem_leggings"), ArmorType.LEGGINGS),
                    ArmorType.LEGGINGS));
        GEM_BOOTS = register(ProjectEAPI.id("gem_boots"),
              new GemArmorItem(gemArmorProps(ProjectEAPI.id("gem_boots"), ArmorType.BOOTS),
                    ArmorType.BOOTS));

        // ── Rings & Accessories ──
        IRON_BAND = register(ProjectEAPI.id("iron_band"), simple(ProjectEAPI.id("iron_band")));
        BLACK_HOLE_BAND = register(ProjectEAPI.id("black_hole_band"),
              new BlackHoleBandItem(properties(ProjectEAPI.id("black_hole_band")).fireResistant().stacksTo(1)));
        ARCHANGEL_SMITE = register(ProjectEAPI.id("archangel_smite"), fireImmune(ProjectEAPI.id("archangel_smite")));
        HARVEST_GODDESS_BAND = register(ProjectEAPI.id("harvest_goddess_band"),
              new HarvestGoddessBandItem(properties(ProjectEAPI.id("harvest_goddess_band")).fireResistant().stacksTo(1)));
        IGNITION_RING = register(ProjectEAPI.id("ignition_ring"),
              new IgnitionRingItem(properties(ProjectEAPI.id("ignition_ring")).fireResistant().stacksTo(1)));
        ZERO_RING = register(ProjectEAPI.id("zero_ring"),
              new ZeroRingItem(properties(ProjectEAPI.id("zero_ring")).fireResistant().stacksTo(1)));
        SWIFTWOLF_RENDING_GALE = register(ProjectEAPI.id("swiftwolf_rending_gale"),
              new SwiftwolfRendingGaleItem(properties(ProjectEAPI.id("swiftwolf_rending_gale")).fireResistant().stacksTo(1)));
        WATCH_OF_FLOWING_TIME = register(ProjectEAPI.id("watch_of_flowing_time"),
              new WatchOfFlowingTimeItem(properties(ProjectEAPI.id("watch_of_flowing_time")).fireResistant().stacksTo(1)));
        EVERTIDE_AMULET = register(ProjectEAPI.id("evertide_amulet"), fireImmune(ProjectEAPI.id("evertide_amulet")));
        VOLCANITE_AMULET = register(ProjectEAPI.id("volcanite_amulet"), fireImmune(ProjectEAPI.id("volcanite_amulet")));
        GEM_OF_ETERNAL_DENSITY = register(ProjectEAPI.id("gem_of_eternal_density"), fireImmune(ProjectEAPI.id("gem_of_eternal_density")));
        MERCURIAL_EYE = register(ProjectEAPI.id("mercurial_eye"), fireImmune(ProjectEAPI.id("mercurial_eye")));
        VOID_RING = register(ProjectEAPI.id("void_ring"),
              new VoidRingItem(properties(ProjectEAPI.id("void_ring")).fireResistant().stacksTo(1)));
        ARCANA_RING = register(ProjectEAPI.id("arcana_ring"),
              new Item(properties(ProjectEAPI.id("arcana_ring")).fireResistant().rarity(Rarity.RARE)));
        BODY_STONE = register(ProjectEAPI.id("body_stone"),
              new BodyStoneItem(properties(ProjectEAPI.id("body_stone")).fireResistant().stacksTo(1)));
        SOUL_STONE = register(ProjectEAPI.id("soul_stone"),
              new SoulStoneItem(properties(ProjectEAPI.id("soul_stone")).fireResistant().stacksTo(1)));
        MIND_STONE = register(ProjectEAPI.id("mind_stone"),
              new MindStoneItem(properties(ProjectEAPI.id("mind_stone")).fireResistant().stacksTo(1)));
        LIFE_STONE = register(ProjectEAPI.id("life_stone"),
              new LifeStoneItem(properties(ProjectEAPI.id("life_stone")).fireResistant().stacksTo(1)));

        // ── Divining Rods ──
        LOW_DIVINING_ROD = register(ProjectEAPI.id("divining_rod_1"),
              new DiviningRodItem(properties(ProjectEAPI.id("divining_rod_1")).stacksTo(1), 3, 1));
        MEDIUM_DIVINING_ROD = register(ProjectEAPI.id("divining_rod_2"),
              new DiviningRodItem(properties(ProjectEAPI.id("divining_rod_2")).stacksTo(1), 5, 3));
        HIGH_DIVINING_ROD = register(ProjectEAPI.id("divining_rod_3"),
              new DiviningRodItem(properties(ProjectEAPI.id("divining_rod_3")).stacksTo(1), 7, 5));

        // ── Catalysts & Lenses ──
        DESTRUCTION_CATALYST = register(ProjectEAPI.id("destruction_catalyst"),
              new DestructionCatalystItem(properties(ProjectEAPI.id("destruction_catalyst"))
                    .fireResistant().stacksTo(1)));
        HYPERKINETIC_LENS = register(ProjectEAPI.id("hyperkinetic_lens"),
              fireImmune(ProjectEAPI.id("hyperkinetic_lens")));
        CATALYTIC_LENS = register(ProjectEAPI.id("catalytic_lens"),
              fireImmune(ProjectEAPI.id("catalytic_lens")));

        // ── Tome & Tablet ──
        TOME_OF_KNOWLEDGE = register(ProjectEAPI.id("tome"),
              new TomeOfKnowledgeItem(properties(ProjectEAPI.id("tome")).stacksTo(1).rarity(Rarity.EPIC)));
        TRANSMUTATION_TABLET = register(ProjectEAPI.id("transmutation_tablet"),
              new TransmutationTabletItem(properties(ProjectEAPI.id("transmutation_tablet"))
                    .fireResistant().stacksTo(1)));

        // ── Manual ──
        MANUAL = register(ProjectEAPI.id("manual"),
              new ManualItem(properties(ProjectEAPI.id("manual")).stacksTo(1)));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    static Item.Properties properties(Identifier id) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id));
    }

    private static Item simple(Identifier id) {
        return new Item(properties(id));
    }

    private static Item fireImmune(Identifier id) {
        return new Item(properties(id).fireResistant());
    }

    // ── Dark Matter tool helpers ──────────────────────────────────────────
    private static Item.Properties dmToolProps(Identifier id) {
        return properties(id).fireResistant().durability(ModToolMaterials.DARK_MATTER.durability());
    }

    private static Item dmPickaxe(Identifier id) {
        return new MatterPickaxeItem(
              dmToolProps(id).pickaxe(ModToolMaterials.DARK_MATTER, 1.0F, -2.8F), 2, 12.0F);
    }

    private static Item dmAxe(Identifier id) {
        return new MatterAxeItem(
              ModToolMaterials.DARK_MATTER, 6.0F, -3.0F, dmToolProps(id), 2, 12.0F);
    }

    private static Item dmShovel(Identifier id) {
        return new MatterShovelItem(
              ModToolMaterials.DARK_MATTER, 1.5F, -3.0F, dmToolProps(id), 2, 12.0F);
    }

    private static Item dmSword(Identifier id) {
        return new MatterSwordItem(
              dmToolProps(id).sword(ModToolMaterials.DARK_MATTER, 3.0F, -2.4F), 2, 12.0F);
    }

    private static Item dmHoe(Identifier id) {
        return new MatterHoeItem(
              ModToolMaterials.DARK_MATTER, -3.0F, 0.0F, dmToolProps(id), 2, 12.0F);
    }

    private static Item dmShears(Identifier id) {
        return new MatterShearsItem(dmToolProps(id), 2, 12.0F);
    }

    // ── Dark Matter armor helpers ─────────────────────────────────────────
    private static Item.Properties dmArmorProps(Identifier id, ArmorType type) {
        return properties(id).fireResistant()
              .humanoidArmor(ModArmorMaterials.DARK_MATTER, type);
    }

    // ── Red Matter tool helpers ───────────────────────────────────────────
    private static Item.Properties rmToolProps(Identifier id) {
        return properties(id).fireResistant().durability(ModToolMaterials.RED_MATTER.durability());
    }

    private static Item rmPickaxe(Identifier id) {
        return new MatterPickaxeItem(
              rmToolProps(id).pickaxe(ModToolMaterials.RED_MATTER, 1.0F, -2.8F), 3, 14.0F);
    }

    private static Item rmAxe(Identifier id) {
        return new MatterAxeItem(
              ModToolMaterials.RED_MATTER, 7.0F, -3.0F, rmToolProps(id), 3, 14.0F);
    }

    private static Item rmShovel(Identifier id) {
        return new MatterShovelItem(
              ModToolMaterials.RED_MATTER, 1.5F, -3.0F, rmToolProps(id), 3, 14.0F);
    }

    private static Item rmSword(Identifier id) {
        return new RedMatterSwordItem(
              rmToolProps(id).sword(ModToolMaterials.RED_MATTER, 3.0F, -2.4F), 3, 14.0F);
    }

    private static Item rmHoe(Identifier id) {
        return new MatterHoeItem(
              ModToolMaterials.RED_MATTER, -3.0F, 0.0F, rmToolProps(id), 3, 14.0F);
    }

    private static Item rmShears(Identifier id) {
        return new MatterShearsItem(rmToolProps(id), 3, 14.0F);
    }

    // ── Red Matter armor helpers ──────────────────────────────────────────
    private static Item.Properties rmArmorProps(Identifier id, ArmorType type) {
        return properties(id).fireResistant()
              .humanoidArmor(ModArmorMaterials.RED_MATTER, type);
    }

    // ── Gem armor helpers ─────────────────────────────────────────────────
    private static Item.Properties gemArmorProps(Identifier id, ArmorType type) {
        Item.Properties properties = properties(id).fireResistant()
              .humanoidArmor(ModArmorMaterials.GEM, type);
        return type == ArmorType.BOOTS
              ? properties.attributes(GemArmorItem.bootModifiers())
              : properties;
    }

    // ── Registration ──────────────────────────────────────────────────────

    private static Item register(Identifier id, Item.Properties props) {
        Item item = new Item(props);
        Item registered = Registry.register(BuiltInRegistries.ITEM, id, item);
        ProjectE.LOGGER.debug("Registered item: {} -> {}", id, BuiltInRegistries.ITEM.getKey(registered));
        return registered;
    }

    private static Item register(Identifier id, Item item) {
        Item registered = Registry.register(BuiltInRegistries.ITEM, id, item);
        ProjectE.LOGGER.debug("Registered item: {} -> {}", id, BuiltInRegistries.ITEM.getKey(registered));
        return registered;
    }

    private static Item registerKleinStar(String tierName, boolean isEpic) {
        Identifier id = ProjectEAPI.id("klein_star_" + tierName);
        Item.Properties props = properties(id).fireResistant().stacksTo(1);
        if (isEpic) {
            props = props.rarity(Rarity.EPIC);
        }
        return register(id, new KleinStarItem(props, tierName));
    }

    // ── Public lookup helpers ─────────────────────────────────────────────

    public static @Nullable Item alchemicalBag(DyeColor color) {
        int idx = color.ordinal();
        return (idx >= 0 && idx < ALCHEMICAL_BAGS.length) ? ALCHEMICAL_BAGS[idx] : null;
    }
}
