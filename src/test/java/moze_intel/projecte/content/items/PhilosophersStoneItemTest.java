package moze_intel.projecte.content.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.fabric.api.item.v1.FabricItem;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
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
    void fabricCraftingRemainderPreservesTheStoneAndItsComponents() throws Exception {
        assertTrue(FabricItem.class.isAssignableFrom(PhilosophersStoneItem.class),
              "The stone must use Fabric's stack-aware crafting remainder API");
        PhilosophersStoneItem stone = allocateWithoutRegistering();
        ItemStack original = new ItemStack(Holder.direct(stone, DataComponentMap.EMPTY));
        original.set(DataComponents.CUSTOM_NAME, Component.literal("charged stone"));

        ItemStackTemplate template = ((FabricItem) stone).getCraftingRemainder(original);
        assertNotNull(template);
        ItemStack returned = template.create();

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

    @Test
    void registersInteractionBeforeInteractiveBlocksConsumeTheClick() throws Exception {
        Class<?> handler;
        try {
            handler = Class.forName(
                  "moze_intel.projecte.event.PhilosophersStoneInteractionHandler");
        } catch (ClassNotFoundException exception) {
            fail("The stone needs a Fabric pre-block interaction handler", exception);
            return;
        }
        handler.getDeclaredMethod("register");
        String initializer = Files.readString(
              Path.of("src/main/java/moze_intel/projecte/ProjectE.java"));
        assertTrue(initializer.contains("PhilosophersStoneInteractionHandler.register();"));
    }

    @Test
    void sneakingPrefersACloserFluidHit() {
        BlockHitResult clicked = new BlockHitResult(
              new Vec3(0.5, 64.5, 2.0), Direction.NORTH,
              new BlockPos(0, 64, 2), false);
        BlockHitResult fluid = new BlockHitResult(
              new Vec3(0.5, 64.5, 1.0), Direction.NORTH,
              new BlockPos(0, 64, 1), false);

        assertEquals(fluid,
              PhilosophersStoneItem.selectTransmutationHit(clicked, fluid, true));
        assertEquals(clicked,
              PhilosophersStoneItem.selectTransmutationHit(clicked, fluid, false));
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
