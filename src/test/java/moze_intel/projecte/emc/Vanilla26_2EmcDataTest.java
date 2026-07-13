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

    private static final String NEW_SURVIVAL_ITEMS = """
          acacia_shelf
          bamboo_shelf
          birch_shelf
          black_bundle
          black_harness
          blue_bundle
          blue_egg
          blue_harness
          bordure_indented_banner_pattern
          brown_bundle
          brown_egg
          brown_harness
          bush
          cactus_flower
          cherry_shelf
          chiseled_cinnabar
          chiseled_resin_bricks
          chiseled_sulfur
          cinnabar
          cinnabar_brick_slab
          cinnabar_brick_stairs
          cinnabar_brick_wall
          cinnabar_bricks
          cinnabar_slab
          cinnabar_stairs
          cinnabar_wall
          closed_eyeblossom
          copper_axe
          copper_bars
          copper_boots
          copper_chain
          copper_chest
          copper_chestplate
          copper_golem_statue
          copper_helmet
          copper_hoe
          copper_horse_armor
          copper_lantern
          copper_leggings
          copper_nautilus_armor
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
          diamond_nautilus_armor
          diamond_spear
          dried_ghast
          exposed_copper_bars
          exposed_copper_chain
          exposed_copper_chest
          exposed_copper_golem_statue
          exposed_copper_lantern
          exposed_lightning_rod
          field_masoned_banner_pattern
          firefly_bush
          golden_dandelion
          golden_nautilus_armor
          golden_spear
          gray_bundle
          gray_harness
          green_bundle
          green_harness
          iron_chain
          iron_nautilus_armor
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
          music_disc_bounce
          music_disc_lava_chicken
          music_disc_tears
          netherite_horse_armor
          netherite_nautilus_armor
          netherite_spear
          oak_shelf
          open_eyeblossom
          orange_bundle
          orange_harness
          oxidized_copper_bars
          oxidized_copper_chain
          oxidized_copper_chest
          oxidized_copper_golem_statue
          oxidized_copper_lantern
          oxidized_lightning_rod
          pale_hanging_moss
          pale_moss_block
          pale_moss_carpet
          pale_oak_boat
          pale_oak_button
          pale_oak_chest_boat
          pale_oak_door
          pale_oak_fence
          pale_oak_fence_gate
          pale_oak_hanging_sign
          pale_oak_leaves
          pale_oak_log
          pale_oak_planks
          pale_oak_pressure_plate
          pale_oak_sapling
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
          resin_clump
          short_dry_grass
          spruce_shelf
          stone_spear
          stripped_pale_oak_log
          stripped_pale_oak_wood
          sulfur
          sulfur_brick_slab
          sulfur_brick_stairs
          sulfur_brick_wall
          sulfur_bricks
          sulfur_cube_bucket
          sulfur_slab
          sulfur_spike
          sulfur_stairs
          sulfur_wall
          tall_dry_grass
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
          weathered_copper_bars
          weathered_copper_chain
          weathered_copper_chest
          weathered_copper_golem_statue
          weathered_copper_lantern
          weathered_lightning_rod
          white_bundle
          white_harness
          wildflowers
          wooden_spear
          yellow_bundle
          yellow_harness
          """;

    @Test
    void allNewSurvivalItemsHavePositiveExplicitEmc() throws Exception {
        JsonObject emc = readEmc();
        for (String item : NEW_SURVIVAL_ITEMS.lines().map(String::strip)
              .filter(line -> !line.isEmpty()).toList()) {
            String key = key(item);
            assertTrue(emc.has(key), item + " is missing explicit EMC");
            assertTrue(emc.getAsJsonObject(key).get("value").getAsLong() > 0,
                  item + " must have positive EMC");
        }
    }

    @Test
    void representativeEstimatesStayStable() throws Exception {
        JsonObject emc = readEmc();
        Map<String, Long> expected = Map.of(
              "sulfur", 16L,
              "cinnabar", 32L,
              "resin_clump", 8L,
              "copper_nugget", 14L,
              "copper_spear", 136L,
              "pale_oak_log", 32L,
              "black_harness", 242L,
              "netherite_spear", 65_544L
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

    private JsonObject readEmc() throws Exception {
        return JsonParser.parseString(Files.readString(VANILLA_EMC)).getAsJsonObject();
    }

    private String key(String item) {
        return "item|minecraft:" + item + "|{}";
    }
}
