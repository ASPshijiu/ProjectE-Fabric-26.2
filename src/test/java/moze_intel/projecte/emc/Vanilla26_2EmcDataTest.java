package moze_intel.projecte.emc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class Vanilla26_2EmcDataTest {
    private static final Path VANILLA_EMC =
          Path.of("src/main/resources/data/projecte/emc/vanilla.json");

    private static final String EXPLICIT_NEW_SURVIVAL_ITEMS = """
          blue_egg
          brown_egg
          bush
          cactus_flower
          cinnabar
          closed_eyeblossom
          copper_golem_statue
          copper_horse_armor
          copper_nautilus_armor
          diamond_nautilus_armor
          exposed_copper_bars
          exposed_copper_chain
          exposed_copper_chest
          exposed_copper_golem_statue
          exposed_copper_lantern
          exposed_lightning_rod
          firefly_bush
          golden_nautilus_armor
          iron_nautilus_armor
          music_disc_bounce
          music_disc_lava_chicken
          music_disc_tears
          open_eyeblossom
          oxidized_copper_bars
          oxidized_copper_chain
          oxidized_copper_chest
          oxidized_copper_golem_statue
          oxidized_copper_lantern
          oxidized_lightning_rod
          pale_hanging_moss
          pale_moss_block
          pale_oak_leaves
          pale_oak_log
          pale_oak_sapling
          resin_clump
          short_dry_grass
          stripped_pale_oak_log
          sulfur
          sulfur_cube_bucket
          sulfur_spike
          tall_dry_grass
          weathered_copper_bars
          weathered_copper_chain
          weathered_copper_chest
          weathered_copper_golem_statue
          weathered_copper_lantern
          weathered_lightning_rod
          wildflowers
          """;

    private static final String FIXED_NEW_SURVIVAL_ITEMS = """
          acacia_shelf
          bamboo_shelf
          birch_shelf
          black_bundle
          black_harness
          blue_bundle
          blue_harness
          bordure_indented_banner_pattern
          brown_bundle
          brown_harness
          cherry_shelf
          chiseled_cinnabar
          chiseled_resin_bricks
          chiseled_sulfur
          cinnabar_brick_slab
          cinnabar_brick_stairs
          cinnabar_brick_wall
          cinnabar_bricks
          cinnabar_slab
          cinnabar_stairs
          cinnabar_wall
          copper_axe
          copper_bars
          copper_boots
          copper_chain
          copper_chest
          copper_chestplate
          copper_helmet
          copper_hoe
          copper_lantern
          copper_leggings
          copper_nugget
          copper_pickaxe
          copper_shovel
          copper_spear
          copper_sword
          copper_torch
          creaking_heart
          crimson_shelf
          cyan_bundle
          cyan_harness
          dark_oak_shelf
          diamond_spear
          dried_ghast
          field_masoned_banner_pattern
          golden_dandelion
          golden_spear
          gray_bundle
          gray_harness
          green_bundle
          green_harness
          iron_chain
          iron_spear
          jungle_shelf
          leaf_litter
          light_blue_bundle
          light_blue_harness
          light_gray_bundle
          light_gray_harness
          lime_bundle
          lime_harness
          magenta_bundle
          magenta_harness
          mangrove_shelf
          netherite_horse_armor
          netherite_nautilus_armor
          netherite_spear
          oak_shelf
          orange_bundle
          orange_harness
          pale_moss_carpet
          pale_oak_boat
          pale_oak_button
          pale_oak_chest_boat
          pale_oak_door
          pale_oak_fence
          pale_oak_fence_gate
          pale_oak_hanging_sign
          pale_oak_planks
          pale_oak_pressure_plate
          pale_oak_shelf
          pale_oak_sign
          pale_oak_slab
          pale_oak_stairs
          pale_oak_trapdoor
          pale_oak_wood
          pink_bundle
          pink_harness
          polished_cinnabar
          polished_cinnabar_slab
          polished_cinnabar_stairs
          polished_cinnabar_wall
          polished_sulfur
          polished_sulfur_slab
          polished_sulfur_stairs
          polished_sulfur_wall
          potent_sulfur
          purple_bundle
          purple_harness
          red_bundle
          red_harness
          resin_block
          resin_brick
          resin_brick_slab
          resin_brick_stairs
          resin_brick_wall
          resin_bricks
          spruce_shelf
          stone_spear
          stripped_pale_oak_wood
          sulfur_brick_slab
          sulfur_brick_stairs
          sulfur_brick_wall
          sulfur_bricks
          sulfur_slab
          sulfur_stairs
          sulfur_wall
          warped_shelf
          waxed_copper_bars
          waxed_copper_chain
          waxed_copper_chest
          waxed_copper_golem_statue
          waxed_copper_lantern
          waxed_exposed_copper_bars
          waxed_exposed_copper_chain
          waxed_exposed_copper_chest
          waxed_exposed_copper_golem_statue
          waxed_exposed_copper_lantern
          waxed_exposed_lightning_rod
          waxed_lightning_rod
          waxed_oxidized_copper_bars
          waxed_oxidized_copper_chain
          waxed_oxidized_copper_chest
          waxed_oxidized_copper_golem_statue
          waxed_oxidized_copper_lantern
          waxed_oxidized_lightning_rod
          waxed_weathered_copper_bars
          waxed_weathered_copper_chain
          waxed_weathered_copper_chest
          waxed_weathered_copper_golem_statue
          waxed_weathered_copper_lantern
          waxed_weathered_lightning_rod
          white_bundle
          white_harness
          wooden_spear
          yellow_bundle
          yellow_harness
          """;

    @Test
    void naturalNewItemsHavePositiveExplicitEmc() throws Exception {
        JsonObject emc = readEmc();
        for (String item : EXPLICIT_NEW_SURVIVAL_ITEMS.lines().map(String::strip)
              .filter(line -> !line.isEmpty()).toList()) {
            String key = key(item);
            assertTrue(emc.has(key), item + " is missing explicit EMC");
            assertTrue(emc.getAsJsonObject(key).get("value").getAsLong() > 0,
                  item + " must have positive EMC");
        }
    }

    @Test
    void craftableNewItemsHavePositiveExplicitEmc() throws Exception {
        JsonObject emc = readEmc();
        for (String item : FIXED_NEW_SURVIVAL_ITEMS.lines().map(String::strip)
              .filter(line -> !line.isEmpty()).toList()) {
            String key = key(item);
            assertTrue(emc.has(key), item + " is missing explicit EMC");
            assertTrue(emc.getAsJsonObject(key).get("value").getAsLong() > 0,
                  item + " must have positive EMC");
        }
    }

    @Test
    void representativeFixedValuesStayStable() throws Exception {
        JsonObject emc = readEmc();
        Map<String, Long> expected = Map.of(
              "copper_ingot", 128L,
              "iron_ingot", 256L,
              "gold_ingot", 2_048L,
              "copper_nugget", 14L,
              "copper_spear", 136L,
              "netherite_pickaxe", 89_425L,
              "netherite_chestplate", 130_377L,
              "netherite_horse_armor", 66_889L,
              "netherite_nautilus_armor", 73_033L,
              "music_disc_tears", 8_192L
        );
        expected.forEach((item, value) -> assertEquals(
              value.longValue(), emc.getAsJsonObject(key(item)).get("value").getAsLong(), item));
    }

    @Test
    void creativeOnlyEntriesRemainExcluded() throws Exception {
        JsonObject emc = readEmc();
        for (String item : new String[]{
              "camel_husk_spawn_egg", "copper_golem_spawn_egg", "creaking_spawn_egg",
              "happy_ghast_spawn_egg", "nautilus_spawn_egg", "parched_spawn_egg",
              "sulfur_cube_spawn_egg", "zombie_nautilus_spawn_egg", "test_block",
              "test_instance_block"
        }) {
            assertFalse(emc.has(key(item)), item + " should not receive explicit EMC");
        }
    }

    @Test
    void netheriteUpgradesHavePositiveExplicitEmc() throws Exception {
        JsonObject emc = readEmc();
        for (String item : new String[]{
              "netherite_sword", "netherite_shovel", "netherite_pickaxe", "netherite_axe",
              "netherite_hoe", "netherite_helmet", "netherite_chestplate", "netherite_leggings",
              "netherite_boots", "netherite_spear", "netherite_horse_armor",
              "netherite_nautilus_armor"
        }) {
            String key = key(item);
            assertTrue(emc.has(key), item + " is missing explicit EMC");
            assertTrue(emc.getAsJsonObject(key).get("value").getAsLong() > 0,
                  item + " must have positive EMC");
        }
    }

    private JsonObject readEmc() throws Exception {
        return JsonParser.parseString(Files.readString(VANILLA_EMC)).getAsJsonObject();
    }

    private String key(String item) {
        return "item|minecraft:" + item + "|{}";
    }
}
