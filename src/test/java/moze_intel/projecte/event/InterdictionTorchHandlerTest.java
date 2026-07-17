package moze_intel.projecte.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class InterdictionTorchHandlerTest {
    @Test
    void repulsionPushesAwayFromTorchAndSlightlyUpward() {
        Vec3 push = InterdictionTorchHandler.repulsion(Vec3.ZERO, new Vec3(2, 0, 0));

        assertTrue(push.x > 0);
        assertEquals(0, push.z, 1.0E-8);
        assertEquals(0.08, push.y, 1.0E-8);
    }

    @Test
    void repulsionHasStableDirectionAtTorchCenter() {
        Vec3 push = InterdictionTorchHandler.repulsion(Vec3.ZERO, Vec3.ZERO);

        assertTrue(push.x > 0);
        assertEquals(0.08, push.y, 1.0E-8);
    }
}
