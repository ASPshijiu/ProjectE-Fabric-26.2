package moze_intel.projecte.content.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

class DestructionCatalystItemTest {
    private static final BlockPos CLICKED = new BlockPos(10, 64, 10);

    @Test
    void zeroChargeTargetsOneThreeByThreeLayer() {
        List<BlockPos> targets = DestructionCatalystItem.targetPositions(
              CLICKED, Direction.NORTH, 0);

        assertEquals(9, targets.size());
        assertTrue(targets.stream().allMatch(pos -> pos.getZ() == CLICKED.getZ()));
    }

    @Test
    void chargeExpandsDepthIntoTheClickedFace() {
        assertEquals(36, targetCount(1));
        assertEquals(72, targetCount(2));
        List<BlockPos> maximum = DestructionCatalystItem.targetPositions(
              CLICKED, Direction.NORTH, 3);
        assertEquals(144, maximum.size());
        assertTrue(maximum.contains(CLICKED.relative(Direction.SOUTH, 15).offset(1, 1, 0)));
    }

    private static int targetCount(int charge) {
        return DestructionCatalystItem.targetPositions(CLICKED, Direction.NORTH, charge).size();
    }
}
