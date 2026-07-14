package moze_intel.projecte.transmutation.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.UUID;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
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

    @Test
    void copiesBothSignFacesAndWaxStateToTheReplacement() {
        BlockPos pos = new BlockPos(1, 2, 3);
        SignBlockEntity source = new SignBlockEntity(
              pos, Blocks.OAK_SIGN.defaultBlockState());
        SignBlockEntity replacement = new SignBlockEntity(
              pos, Blocks.BIRCH_SIGN.defaultBlockState());
        SignText front = new SignText()
              .setMessage(0, Component.literal("front"))
              .setColor(DyeColor.BLUE)
              .setHasGlowingText(true);
        SignText back = new SignText()
              .setMessage(0, Component.literal("back"))
              .setColor(DyeColor.RED);
        HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(
              BuiltInRegistries.REGISTRY);
        TagValueOutput sourceData = TagValueOutput.createWithContext(
              ProblemReporter.DISCARDING, registries);
        sourceData.store("front_text", SignText.DIRECT_CODEC, front);
        sourceData.store("back_text", SignText.DIRECT_CODEC, back);
        sourceData.putBoolean("is_waxed", true);
        source.loadCustomOnly(TagValueInput.create(
              ProblemReporter.DISCARDING, registries, sourceData.buildResult()));
        UUID editor = UUID.fromString("5dcc7f87-c47d-4a1b-afab-5923baf4ea38");
        source.setAllowedPlayerEditor(editor);

        WorldTransmutationAction.copySignData(source, replacement, registries);

        assertEquals("front", replacement.getFrontText().getMessage(0, false).getString());
        assertEquals(DyeColor.BLUE, replacement.getFrontText().getColor());
        assertTrue(replacement.getFrontText().hasGlowingText());
        assertEquals("back", replacement.getBackText().getMessage(0, false).getString());
        assertEquals(DyeColor.RED, replacement.getBackText().getColor());
        assertTrue(replacement.isWaxed());
        assertEquals(editor, replacement.getPlayerWhoMayEdit());
    }
}
