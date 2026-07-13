package moze_intel.projecte.content.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TransmutationTableBlockShapeTest {
    @BeforeAll
    static void bootstrapRegistries() {
        MinecraftTestHarness.bootstrap();
    }

    @Test
    void tableUsesQuarterBlockOutlineCollisionAndOcclusion() throws Exception {
        TransmutationTableBlock block = allocateWithoutRegistering();

        assertQuarterHeight(invokeShape(block, "getShape",
              new Class<?>[]{BlockState.class, BlockGetter.class, BlockPos.class, CollisionContext.class},
              new Object[]{null, null, null, CollisionContext.empty()}));
        assertQuarterHeight(invokeShape(block, "getCollisionShape",
              new Class<?>[]{BlockState.class, BlockGetter.class, BlockPos.class, CollisionContext.class},
              new Object[]{null, null, null, CollisionContext.empty()}));
        assertQuarterHeight(invokeShape(block, "getOcclusionShape",
              new Class<?>[]{BlockState.class}, new Object[]{null}));
    }

    private static VoxelShape invokeShape(
          TransmutationTableBlock block, String name, Class<?>[] parameterTypes, Object[] arguments
    ) throws Exception {
        Method method;
        try {
            method = TransmutationTableBlock.class.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException exception) {
            fail("TransmutationTableBlock must declare " + name, exception);
            return null;
        }
        method.setAccessible(true);
        return (VoxelShape) method.invoke(block, arguments);
    }

    private static void assertQuarterHeight(VoxelShape shape) {
        assertEquals(0.25, shape.max(Direction.Axis.Y));
    }

    private static TransmutationTableBlock allocateWithoutRegistering() throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field field = unsafeClass.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Object unsafe = field.get(null);
        Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        return (TransmutationTableBlock) allocateInstance.invoke(unsafe, TransmutationTableBlock.class);
    }
}
