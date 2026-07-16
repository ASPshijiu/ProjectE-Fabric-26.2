package moze_intel.projecte.content.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
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
    void inventorySizeMatchesCollectorTier() {
        assertEquals(11, new TestCollector(1, 4).getContainerSize());
        assertEquals(15, new TestCollector(2, 12).getContainerSize());
        assertEquals(19, new TestCollector(3, 40).getContainerSize());
    }

    @Test
    void saveAndLoadPreserveLastAuxiliarySlot() {
        TestCollector source = new TestCollector(2, 12);
        source.setItem(14, new ItemStack(Items.EMERALD));

        CompoundTag saved = source.saveCustomOnly(registries);
        TestCollector restored = new TestCollector(2, 12);
        restored.loadCustomOnly(TagValueInput.create(
              ProblemReporter.DISCARDING, registries, saved));

        assertTrue(restored.getItem(14).is(Items.EMERALD));
        assertEquals(1, restored.getItem(14).getCount());
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

    @Test
    void fullLightProducesConfiguredEmcPerSecondForEveryTier() {
        TestCollector mk1 = new TestCollector(1, 4);
        TestCollector mk2 = new TestCollector(2, 12);
        TestCollector mk3 = new TestCollector(3, 40);

        generateForTicks(mk1, 16, 20);
        generateForTicks(mk2, 16, 20);
        generateForTicks(mk3, 16, 20);

        assertEquals(4, mk1.getStoredEmc());
        assertEquals(12, mk2.getStoredEmc());
        assertEquals(40, mk3.getStoredEmc());
    }

    @Test
    void fractionalEmcAccumulatesAcrossTicks() {
        TestCollector collector = new TestCollector(1, 4);

        generateForTicks(collector, 4, 19);
        assertEquals(0, collector.getStoredEmc());

        collector.generateEmc(4);
        assertEquals(1, collector.getStoredEmc());
    }

    @Test
    void saveAndLoadPreserveFractionalEmc() {
        TestCollector source = new TestCollector(1, 4);
        generateForTicks(source, 4, 10);

        CompoundTag saved = source.saveCustomOnly(registries);
        TestCollector restored = new TestCollector(1, 4);
        restored.loadCustomOnly(TagValueInput.create(
              ProblemReporter.DISCARDING, registries, saved));
        generateForTicks(restored, 4, 10);

        assertEquals(1, restored.getStoredEmc());
    }

    @Test
    void collectorStopsGeneratingAtMaximumCapacity() {
        TestCollector collector = new TestCollector(1, 4);
        collector.setStoredEmc(collector.getMaximumEmc());

        generateForTicks(collector, 16, 20);

        assertEquals(collector.getMaximumEmc(), collector.getStoredEmc());
    }

    @Test
    void sendsConfiguredEmcToSingleRelay() {
        TestCollector collector = new TestCollector(1, 4);
        TestRelay relay = new TestRelay();
        collector.setStoredEmc(100);

        long sent = sendEmcToRelays(collector, List.of(relay));

        assertEquals(4, sent);
        assertEquals(96, collector.getStoredEmc());
        assertEquals(4, relay.getStoredEmc());
    }

    @Test
    void splitsTransferEvenlyBetweenRelays() {
        TestCollector collector = new TestCollector(1, 4);
        TestRelay first = new TestRelay();
        TestRelay second = new TestRelay();
        collector.setStoredEmc(100);

        long sent = sendEmcToRelays(collector, List.of(first, second));

        assertEquals(4, sent);
        assertEquals(96, collector.getStoredEmc());
        assertEquals(2, first.getStoredEmc());
        assertEquals(2, second.getStoredEmc());
    }

    @Test
    void fullRelayDoesNotReduceOtherRelayShare() {
        TestCollector collector = new TestCollector(1, 4);
        TestRelay full = new TestRelay();
        TestRelay accepting = new TestRelay();
        collector.setStoredEmc(100);
        full.setStoredEmc(full.getMaximumEmc());

        long sent = sendEmcToRelays(collector, List.of(full, accepting));

        assertEquals(4, sent);
        assertEquals(96, collector.getStoredEmc());
        assertEquals(full.getMaximumEmc(), full.getStoredEmc());
        assertEquals(4, accepting.getStoredEmc());
    }

    @Test
    void transferDeductsOnlyWhatRelayAccepts() {
        TestCollector collector = new TestCollector(1, 4);
        TestRelay relay = new TestRelay();
        collector.setStoredEmc(10);
        relay.setStoredEmc(relay.getMaximumEmc() - 2);

        long sent = sendEmcToRelays(collector, List.of(relay));

        assertEquals(2, sent);
        assertEquals(8, collector.getStoredEmc());
        assertEquals(relay.getMaximumEmc(), relay.getStoredEmc());
    }

    @Test
    void transferCannotExceedCollectorBalance() {
        TestCollector collector = new TestCollector(1, 4);
        TestRelay relay = new TestRelay();
        collector.setStoredEmc(2);

        long sent = sendEmcToRelays(collector, List.of(relay));

        assertEquals(2, sent);
        assertEquals(0, collector.getStoredEmc());
        assertEquals(2, relay.getStoredEmc());
    }

    private static void generateForTicks(TestCollector collector, int light, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            collector.generateEmc(light);
        }
    }

    private static long sendEmcToRelays(
          TestCollector collector, List<RelayBlockEntity.Base> relays
    ) {
        return CollectorBlockEntity.Base.sendEmcToRelays(collector, relays);
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

    private static final class TestRelay extends RelayBlockEntity.Base {
        private TestRelay() {
            super(collectorType, BlockPos.ZERO, Blocks.FURNACE.defaultBlockState(), 1, 64);
        }
    }
}
