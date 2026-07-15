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
        source.setStoredEmc(9_876);
        source.setItem(0, new ItemStack(Items.DIAMOND));

        CompoundTag saved = source.saveCustomOnly(registries);
        TestCollector restored = new TestCollector();
        restored.loadCustomOnly(TagValueInput.create(
              ProblemReporter.DISCARDING, registries, saved));

        assertEquals(9_876, restored.getStoredEmc());
        assertTrue(restored.getItem(0).is(Items.DIAMOND));
        assertEquals(1, restored.getItem(0).getCount());
    }

    @Test
    void storedEmcIsCappedByCollectorTier() {
        TestCollector mk1 = new TestCollector(1, 4);
        TestCollector mk2 = new TestCollector(2, 12);
        TestCollector mk3 = new TestCollector(3, 40);

        mk1.setStoredEmc(Long.MAX_VALUE);
        mk2.setStoredEmc(Long.MAX_VALUE);
        mk3.setStoredEmc(Long.MAX_VALUE);

        assertEquals(10_000, mk1.getStoredEmc());
        assertEquals(30_000, mk2.getStoredEmc());
        assertEquals(60_000, mk3.getStoredEmc());
        mk1.setStoredEmc(-1);
        assertEquals(0, mk1.getStoredEmc());
    }

    private static final class TestCollector extends CollectorBlockEntity.Base {
        private TestCollector() {
            this(1, 4);
        }

        private TestCollector(int tier, int emcPerSecond) {
            this(BlockPos.ZERO, Blocks.FURNACE.defaultBlockState(), tier, emcPerSecond);
        }

        private TestCollector(
              BlockPos pos, BlockState state, int tier, int emcPerSecond
        ) {
            super(collectorType, pos, state, tier, emcPerSecond);
        }
    }
}
