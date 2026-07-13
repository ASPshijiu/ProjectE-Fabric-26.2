package moze_intel.projecte.content.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PhilosophersStoneItemTest {
    @BeforeAll
    static void bootstrapRegistries() {
        MinecraftTestHarness.bootstrap();
    }

    @Test
    void stoneSupportsFourChargeLevels() throws Exception {
        assertTrue(IItemCharge.class.isAssignableFrom(PhilosophersStoneItem.class));

        PhilosophersStoneItem stone = allocateWithoutRegistering();
        assertEquals(4, ((IItemCharge) stone).getMaxCharge(null));
    }

    @Test
    void modesCycleCubePanelLine() {
        assertEquals(PhilosophersStoneItem.Mode.PANEL,
              PhilosophersStoneItem.Mode.CUBE.next());
        assertEquals(PhilosophersStoneItem.Mode.LINE,
              PhilosophersStoneItem.Mode.PANEL.next());
        assertEquals(PhilosophersStoneItem.Mode.CUBE,
              PhilosophersStoneItem.Mode.LINE.next());
    }

    @Test
    void chargeAndModeSelectTheUpstreamTargetShapes() {
        BlockPos center = new BlockPos(10, 20, 30);

        var cube = PhilosophersStoneItem.targetPositions(
              center, Direction.UP, Direction.NORTH,
              PhilosophersStoneItem.Mode.CUBE, 1);
        assertEquals(27, cube.size());

        var panel = PhilosophersStoneItem.targetPositions(
              center, Direction.NORTH, Direction.EAST,
              PhilosophersStoneItem.Mode.PANEL, 2);
        assertEquals(25, panel.size());
        assertTrue(panel.stream().allMatch(pos -> pos.getZ() == center.getZ()));

        var line = PhilosophersStoneItem.targetPositions(
              center, Direction.UP, Direction.EAST,
              PhilosophersStoneItem.Mode.LINE, 2);
        assertEquals(5, line.size());
        assertTrue(line.stream().allMatch(pos ->
              pos.getY() == center.getY() && pos.getZ() == center.getZ()));
    }

    @Test
    void craftingReturnsAnUnconsumedCopyOfTheStone() throws Exception {
        Method remainder;
        try {
            remainder = PhilosophersStoneItem.class.getDeclaredMethod(
                  "getRecipeRemainder", ItemStack.class);
        } catch (NoSuchMethodException exception) {
            fail("PhilosophersStoneItem must declare getRecipeRemainder", exception);
            return;
        }
        PhilosophersStoneItem stone = allocateWithoutRegistering();
        ItemStack original = new ItemStack(Items.STONE);
        ItemStack returned = (ItemStack) remainder.invoke(stone, original);

        assertTrue(returned != original);
        assertTrue(ItemStack.matches(original, returned));
    }

    @Test
    void exposesTheUpstreamExtraActions() throws Exception {
        assertEquals(384, PhilosophersStoneItem.MOB_RANDOMIZER_EMC_COST);
        PhilosophersStoneItem.class.getDeclaredMethod(
              "openPortableCrafting", net.minecraft.server.level.ServerPlayer.class, ItemStack.class);
        PhilosophersStoneItem.class.getDeclaredMethod(
              "shootMobRandomizer", net.minecraft.server.level.ServerPlayer.class, ItemStack.class);
    }

    private static PhilosophersStoneItem allocateWithoutRegistering() throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field field = unsafeClass.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Object unsafe = field.get(null);
        Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        return (PhilosophersStoneItem) allocateInstance.invoke(unsafe, PhilosophersStoneItem.class);
    }
}
