package moze_intel.projecte.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ModEntityTypesTest {
    @Test
    void mobRandomizerUsesTheUpstreamProjectileTypeContract() throws Exception {
        String registration = Files.readString(Path.of(
              "src/main/java/moze_intel/projecte/content/ModEntityTypes.java"));

        assertEquals("projecte", ModEntityTypes.MOB_RANDOMIZER_ID.getNamespace());
        assertEquals("mob_randomizer", ModEntityTypes.MOB_RANDOMIZER_ID.getPath());
        assertTrue(registration.contains("MobCategory.MISC"));
        assertTrue(registration.contains(".noLootTable()"));
        assertTrue(registration.contains(".sized(0.5F, 0.5F)"));
        assertTrue(registration.contains(".clientTrackingRange(10)"));
        assertTrue(registration.contains(".updateInterval(10)"));
    }

    @Test
    void commonInitializerRegistersEntityTypes() throws Exception {
        String initializer = Files.readString(
              Path.of("src/main/java/moze_intel/projecte/ProjectE.java"));

        assertTrue(initializer.contains("ModEntityTypes.init();"));
    }

    @Test
    void philosophersStoneLaunchesTheDedicatedProjectile() throws Exception {
        String stone = Files.readString(Path.of(
              "src/main/java/moze_intel/projecte/content/items/PhilosophersStoneItem.java"));

        assertTrue(stone.contains(
              "new MobRandomizerProjectile(player.level(), player)"));
        assertTrue(!stone.contains("Items.ENDER_PEARL"));
    }

    @Test
    void clientUsesTheUpstreamRandomizerSpriteAndParticles() throws Exception {
        Path rendererPath = Path.of(
              "src/client/java/moze_intel/projecte/client/render/MobRandomizerRenderer.java");
        assertTrue(Files.exists(rendererPath),
              "the mob randomizer needs a dedicated sprite renderer");

        String renderer = Files.readString(rendererPath);
        String client = Files.readString(Path.of(
              "src/client/java/moze_intel/projecte/client/ProjectEClient.java"));
        String projectile = Files.readString(Path.of(
              "src/main/java/moze_intel/projecte/content/entity/MobRandomizerProjectile.java"));

        assertTrue(renderer.contains("textures/entity/randomizer.png"));
        assertTrue(renderer.contains("poseStack.scale(0.5F, 0.5F, 0.5F)"));
        assertTrue(client.contains("EntityRenderers.register("));
        assertTrue(client.contains("ModEntityTypes.MOB_RANDOMIZER"));
        assertTrue(client.contains("MobRandomizerRenderer::new"));
        assertTrue(projectile.contains("ParticleTypes.PORTAL"));
        assertTrue(projectile.contains("EntityEvent.DEATH"));
        assertTrue(Files.exists(Path.of(
              "src/main/resources/assets/projecte/textures/entity/randomizer.png")));
    }
}
