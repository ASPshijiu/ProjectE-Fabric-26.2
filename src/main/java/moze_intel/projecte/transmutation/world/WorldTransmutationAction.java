package moze_intel.projecte.transmutation.world;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;

/**
 * Performs a server-side world transmutation: looks up the transmutation for the clicked block in
 * {@link WorldTransmutationStore}, copies any state properties shared between origin and result,
 * and writes the new block state.
 *
 * <p>The state-property copy is exposed as a pure static helper so it can be unit-tested without a
 * live world; the {@link #apply} method is the live adapter.
 */
public final class WorldTransmutationAction {
    private WorldTransmutationAction() {
    }

    /**
     * @return true if a transmutation was found and applied; false if the block has no entry.
     */
    public static boolean apply(Level level, BlockPos pos, boolean useAlternate) {
        return apply(level, pos, level.getBlockState(pos), useAlternate);
    }

    /**
     * Variant accepting an already-read origin state, to avoid a redundant world read.
     */
    public static boolean apply(Level level, BlockPos pos, BlockState originState, boolean useAlternate) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(originState, "originState");
        var candidates = WorldTransmutationStore.current().forOrigin(originState.getBlock());
        if (candidates.isEmpty()) {
            return false;
        }
        return apply(level, pos, originState, candidates.getFirst(), useAlternate);
    }

    /** Applies a rule selected from the originally clicked block to one target position. */
    public static boolean apply(
          Level level,
          BlockPos pos,
          SimpleWorldTransmutation transmutation,
          boolean useAlternate
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        return apply(level, pos, level.getBlockState(pos), transmutation, useAlternate);
    }

    private static boolean apply(
          Level level,
          BlockPos pos,
          BlockState originState,
          SimpleWorldTransmutation transmutation,
          boolean useAlternate
    ) {
        Objects.requireNonNull(transmutation, "transmutation");
        BlockState resultState = transmutation.result(originState, useAlternate);
        if (resultState == null) {
            return false;
        }
        SignBlockEntity originalSign = level.getBlockEntity(pos) instanceof SignBlockEntity sign
              ? sign : null;
        if (!level.setBlockAndUpdate(pos, resultState)) {
            return false;
        }
        if (originalSign != null
              && level.getBlockEntity(pos) instanceof SignBlockEntity replacementSign) {
            copySignData(originalSign, replacementSign, level.registryAccess());
            replacementSign.setChanged();
            level.sendBlockUpdated(
                  pos, replacementSign.getBlockState(), replacementSign.getBlockState(), Block.UPDATE_ALL);
        }
        return true;
    }

    static void copySignData(
          SignBlockEntity source,
          SignBlockEntity target,
          HolderLookup.Provider registries
    ) {
        target.loadCustomOnly(TagValueInput.create(
              ProblemReporter.DISCARDING, registries, source.saveCustomOnly(registries)));
        target.setAllowedPlayerEditor(source.getPlayerWhoMayEdit());
    }

    /**
     * Copy every state property present on both the origin and the result's default state from the
     * origin onto the result, mirroring ProjectE's behavior (e.g. axis on logs).
     */
    public static BlockState copySharedStateProperties(BlockState origin, BlockState result) {
        BlockState merged = result;
        for (net.minecraft.world.level.block.state.properties.Property<?> property : origin.getProperties()) {
            if (merged.hasProperty(property)) {
                merged = copyProperty(origin, merged, property);
            }
        }
        return merged;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(
          BlockState source, BlockState target, net.minecraft.world.level.block.state.properties.Property<T> property
    ) {
        return target.setValue(property, source.getValue(property));
    }
}
