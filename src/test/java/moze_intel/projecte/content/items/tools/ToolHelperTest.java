package moze_intel.projecte.content.items.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class ToolHelperTest {
    @Test
    void usesTheNextRaycastBlockFaceAfterTheOriginalBlockWasRemoved() {
        BlockPos nextBlock = new BlockPos(0, 64, -1);
        BlockHitResult hit = new BlockHitResult(
              Vec3.atCenterOf(nextBlock), Direction.SOUTH, nextBlock, false);

        assertEquals(Direction.SOUTH, ToolHelper.miningFace(hit, new Vec3(0, 0, -1)));
    }

    @Test
    void fallsBackToTheOppositeLookDirectionWhenRaycastMisses() {
        BlockHitResult miss = BlockHitResult.miss(
              new Vec3(0, 64, -5), Direction.NORTH, new BlockPos(0, 64, -5));

        assertEquals(Direction.SOUTH, ToolHelper.miningFace(miss, new Vec3(0, 0, -1)));
    }
}
