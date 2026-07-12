package moze_intel.projecte.transmutation.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class WorldTransmutationActionTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftTestHarness.bootstrap();
    }

    @Test
    void copiesAxisFromOriginToResult() {
        // Stripped logs share the AXIS property; converting an axis=y log to a different log keeps
        // the axis.
        BlockState origin = Blocks.STRIPPED_OAK_LOG.defaultBlockState()
              .setValue(BlockStateProperties.AXIS, net.minecraft.core.Direction.Axis.Y);
        BlockState result = WorldTransmutationAction.copySharedStateProperties(
              origin, Blocks.STRIPPED_BIRCH_LOG.defaultBlockState());
        assertEquals(net.minecraft.core.Direction.Axis.Y, result.getValue(BlockStateProperties.AXIS));
    }

    @Test
    void leavesResultPropertiesUntouchedWhenOriginLacksThem() {
        // Stone has no properties; copying onto a log leaves the log's default axis alone.
        BlockState origin = Blocks.STONE.defaultBlockState();
        BlockState result = WorldTransmutationAction.copySharedStateProperties(
              origin, Blocks.OAK_LOG.defaultBlockState());
        assertEquals(Blocks.OAK_LOG.defaultBlockState(), result);
    }

    @Test
    void doesNotMutateInputs() {
        BlockState origin = Blocks.STRIPPED_OAK_LOG.defaultBlockState()
              .setValue(BlockStateProperties.AXIS, net.minecraft.core.Direction.Axis.X);
        BlockState resultDefault = Blocks.STRIPPED_BIRCH_LOG.defaultBlockState();
        BlockState merged = WorldTransmutationAction.copySharedStateProperties(origin, resultDefault);
        // origin unchanged
        assertEquals(net.minecraft.core.Direction.Axis.X, origin.getValue(BlockStateProperties.AXIS));
        // the default we passed in is still the default (BlockState is immutable, returns new)
        assertEquals(Blocks.STRIPPED_BIRCH_LOG.defaultBlockState(), resultDefault);
        assertNotEquals(resultDefault, merged);
    }

    @Test
    void copyOnBlocksWithNoSharedPropertiesIsIdentity() {
        BlockState origin = Blocks.COBBLESTONE.defaultBlockState();
        BlockState result = Blocks.STONE.defaultBlockState();
        BlockState merged = WorldTransmutationAction.copySharedStateProperties(origin, result);
        assertEquals(result, merged);
        assertTrue(merged.getProperties().isEmpty());
    }
}
