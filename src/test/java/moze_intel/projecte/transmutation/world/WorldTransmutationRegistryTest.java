package moze_intel.projecte.transmutation.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class WorldTransmutationRegistryTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftTestHarness.bootstrap();
    }

    private Holder<Block> block(String id) {
        return BuiltInRegistries.BLOCK.get(Identifier.parse(id)).orElseThrow();
    }

    @Test
    void emptyRegistryHasNoEntries() {
        WorldTransmutationRegistry registry = WorldTransmutationRegistry.empty();
        assertTrue(registry.isEmpty());
        assertTrue(registry.forOrigin(Blocks.STONE).isEmpty());
    }

    @Test
    void groupsTransmutationsByOrigin() {
        SimpleWorldTransmutation stoneToCobble = new SimpleWorldTransmutation(
              block("minecraft:stone"), block("minecraft:cobblestone"));
        SimpleWorldTransmutation stoneToDirt = new SimpleWorldTransmutation(
              block("minecraft:stone"), block("minecraft:dirt"), block("minecraft:grass_block"));
        SimpleWorldTransmutation cobbleToStone = new SimpleWorldTransmutation(
              block("minecraft:cobblestone"), block("minecraft:stone"));
        WorldTransmutationRegistry registry = WorldTransmutationRegistry.of(
              List.of(stoneToCobble, stoneToDirt, cobbleToStone));

        List<SimpleWorldTransmutation> fromStone = registry.forOrigin(Blocks.STONE);
        assertEquals(2, fromStone.size());
        assertTrue(fromStone.contains(stoneToCobble));
        assertTrue(fromStone.contains(stoneToDirt));
        assertEquals(List.of(cobbleToStone), registry.forOrigin(Blocks.COBBLESTONE));
    }

    @Test
    void rejectsDuplicateOriginResultPair() {
        SimpleWorldTransmutation first = new SimpleWorldTransmutation(
              block("minecraft:stone"), block("minecraft:cobblestone"));
        SimpleWorldTransmutation duplicate = new SimpleWorldTransmutation(
              block("minecraft:stone"), block("minecraft:cobblestone"), block("minecraft:dirt"));
        assertThrows(IllegalStateException.class,
              () -> WorldTransmutationRegistry.of(List.of(first, duplicate)));
    }

    @Test
    void entriesViewIsImmutable() {
        SimpleWorldTransmutation entry = new SimpleWorldTransmutation(
              block("minecraft:stone"), block("minecraft:cobblestone"));
        WorldTransmutationRegistry registry = WorldTransmutationRegistry.of(List.of(entry));
        assertThrows(UnsupportedOperationException.class,
              () -> registry.entries().put(Blocks.DIRT, List.of()));
    }
}
