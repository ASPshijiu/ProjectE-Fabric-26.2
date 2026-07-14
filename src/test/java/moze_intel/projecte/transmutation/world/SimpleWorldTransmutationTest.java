package moze_intel.projecte.transmutation.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SimpleWorldTransmutationTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftTestHarness.bootstrap();
    }

    private Holder<Block> block(String id) {
        return BuiltInRegistries.BLOCK.get(Identifier.parse(id)).orElseThrow();
    }

    @Test
    void roundTripsOriginResultAndAltResult() {
        SimpleWorldTransmutation transmutation = new SimpleWorldTransmutation(
              block("minecraft:stone"), block("minecraft:cobblestone"), block("minecraft:grass_block"));
        JsonElement encoded = SimpleWorldTransmutation.CODEC.encodeStart(JsonOps.INSTANCE, transmutation)
              .getOrThrow(IllegalStateException::new);
        SimpleWorldTransmutation decoded = SimpleWorldTransmutation.CODEC.parse(JsonOps.INSTANCE, encoded)
              .getOrThrow(IllegalStateException::new);
        assertEquals(transmutation, decoded);
        assertEquals(Blocks.STONE, decoded.origin().value());
        assertEquals(Blocks.COBBLESTONE, decoded.result().value());
        assertEquals(Blocks.GRASS_BLOCK, decoded.altResult().value());
        assertTrue(decoded.hasAlternate());
    }

    @Test
    void altResultDefaultsToResultWhenAbsent() {
        String json = "{\"origin\":\"minecraft:stone\",\"result\":\"minecraft:cobblestone\"}";
        SimpleWorldTransmutation decoded = SimpleWorldTransmutation.CODEC.parse(
              JsonOps.INSTANCE, com.google.gson.JsonParser.parseString(json))
              .getOrThrow(IllegalStateException::new);
        assertEquals(Blocks.STONE, decoded.origin().value());
        assertEquals(Blocks.COBBLESTONE, decoded.result().value());
        assertEquals(decoded.result(), decoded.altResult());
        assertFalse(decoded.hasAlternate());
    }

    @Test
    void rejectsNullArguments() {
        Holder<Block> stone = block("minecraft:stone");
        assertThrows(NullPointerException.class,
              () -> new SimpleWorldTransmutation(null, stone, stone));
        assertThrows(NullPointerException.class,
              () -> new SimpleWorldTransmutation(stone, null, stone));
    }

    @Test
    void selectedTransmutationOnlyTransformsMatchingOrigins() {
        SimpleWorldTransmutation stoneToCobble = new SimpleWorldTransmutation(
              block("minecraft:stone"), block("minecraft:cobblestone"), block("minecraft:grass_block"));

        assertEquals(Blocks.COBBLESTONE.defaultBlockState(),
              stoneToCobble.result(Blocks.STONE.defaultBlockState(), false));
        assertEquals(Blocks.GRASS_BLOCK.defaultBlockState(),
              stoneToCobble.result(Blocks.STONE.defaultBlockState(), true));
        assertNull(stoneToCobble.result(Blocks.DIRT.defaultBlockState(), false));
    }
}
