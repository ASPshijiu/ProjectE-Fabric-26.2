package moze_intel.projecte.content.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CollectorBlockEntityTest {
    private static HolderLookup.Provider registries;
    private static BlockEntityType<?> collectorType;

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        collectorType = Objects.requireNonNull(BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(
              Identifier.withDefaultNamespace("furnace")));
    }

    @Test
    void saveAndLoadPreserveStoredEmcAndInventory() {
        TestCollector source = new TestCollector();
        source.setStoredEmc(123_456);
        source.setItem(0, new ItemStack(Items.DIAMOND));

        CompoundTag saved = source.saveCustomOnly(registries);
        TestCollector restored = new TestCollector();
        restored.loadCustomOnly(TagValueInput.create(
              ProblemReporter.DISCARDING, registries, saved));

        assertEquals(123_456, restored.getStoredEmc());
        assertTrue(restored.getItem(0).is(Items.DIAMOND));
        assertEquals(1, restored.getItem(0).getCount());
    }

    private static final class TestCollector extends CollectorBlockEntity.Base {
        private TestCollector() {
            this(BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
        }

        private TestCollector(BlockPos pos, BlockState state) {
            super(collectorType, pos, state, 1, 4);
        }
    }
}
