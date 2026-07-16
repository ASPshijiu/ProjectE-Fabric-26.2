package moze_intel.projecte.content.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToLongFunction;
import moze_intel.projecte.content.items.KleinStarItem;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
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
    void chargesKleinStarAtCollectorTierRate() throws Exception {
        TestCollector mk1 = new TestCollector(1, 4);
        TestCollector mk2 = new TestCollector(2, 12);
        TestCollector mk3 = new TestCollector(3, 40);
        ItemStack mk1Star = kleinStarStack(0);
        ItemStack mk2Star = kleinStarStack(0);
        ItemStack mk3Star = kleinStarStack(0);
        mk1.setStoredEmc(100);
        mk2.setStoredEmc(100);
        mk3.setStoredEmc(100);
        mk1.setItem(mk1.inputSlots, mk1Star);
        mk2.setItem(mk2.inputSlots, mk2Star);
        mk3.setItem(mk3.inputSlots, mk3Star);

        assertTrue(mk1.chargeItem());
        assertTrue(mk2.chargeItem());
        assertTrue(mk3.chargeItem());

        assertEquals(4, KleinStarItem.getStoredEmc(mk1Star));
        assertEquals(12, KleinStarItem.getStoredEmc(mk2Star));
        assertEquals(40, KleinStarItem.getStoredEmc(mk3Star));
        assertEquals(96, mk1.getStoredEmc());
        assertEquals(88, mk2.getStoredEmc());
        assertEquals(60, mk3.getStoredEmc());
    }

    @Test
    void chargingDoesNotSpendMoreThanKleinStarCanAccept() throws Exception {
        TestCollector collector = new TestCollector(3, 40);
        ItemStack star = kleinStarStack(KleinStarItem.MAX_EIN - 2);
        collector.setStoredEmc(100);
        collector.setItem(collector.inputSlots, star);

        assertTrue(collector.chargeItem());

        assertEquals(KleinStarItem.MAX_EIN, KleinStarItem.getStoredEmc(star));
        assertEquals(98, collector.getStoredEmc());
    }

    @Test
    void chargingDoesNotSpendMoreThanCollectorStores() throws Exception {
        TestCollector collector = new TestCollector(3, 40);
        ItemStack star = kleinStarStack(0);
        collector.setStoredEmc(3);
        collector.setItem(collector.inputSlots, star);

        assertTrue(collector.chargeItem());

        assertEquals(3, KleinStarItem.getStoredEmc(star));
        assertEquals(0, collector.getStoredEmc());
    }

    @Test
    void doesNotChargeKleinStarFromMainInventory() throws Exception {
        TestCollector collector = new TestCollector(1, 4);
        ItemStack star = kleinStarStack(0);
        collector.setStoredEmc(100);
        collector.setItem(0, star);

        assertFalse(collector.chargeItem());

        assertEquals(0, KleinStarItem.getStoredEmc(star));
        assertEquals(100, collector.getStoredEmc());
    }

    @Test
    void nextFuelFollowsAscendingPositiveEmcValues() {
        ToLongFunction<ItemStack> emcValue = emcValues(Map.of(
              Items.DIRT, 1L,
              Items.COAL, 128L,
              Items.DIAMOND, 8_192L));
        List<net.minecraft.world.item.Item> fuels = List.of(
              Items.DIAMOND, Items.DIRT, Items.COAL, Items.EMERALD);

        assertTrue(CollectorBlockEntity.Base.nextFuel(
              itemStack(Items.DIRT, 1), fuels, emcValue).is(Items.COAL));
        assertTrue(CollectorBlockEntity.Base.nextFuel(
              itemStack(Items.COAL, 1), fuels, emcValue).is(Items.DIAMOND));
        assertTrue(CollectorBlockEntity.Base.nextFuel(
              itemStack(Items.DIAMOND, 1), fuels, emcValue).isEmpty());
        assertTrue(CollectorBlockEntity.Base.nextFuel(
              itemStack(Items.EMERALD, 1), fuels, emcValue).isEmpty());
    }

    @Test
    void upgradesOneFuelForItsEmcDifference() {
        TestCollector collector = new TestCollector(1, 4);
        collector.setStoredEmc(384);
        collector.setItem(collector.inputSlots, itemStack(Items.COAL, 2));
        ToLongFunction<ItemStack> emcValue = emcValues(Map.of(
              Items.COAL, 128L, Items.DIAMOND, 512L));

        assertTrue(collector.upgradeFuel(
              ignored -> itemStack(Items.DIAMOND, 1), emcValue));

        assertEquals(0, collector.getStoredEmc());
        assertEquals(1, collector.getItem(collector.inputSlots).getCount());
        assertTrue(collector.getItem(collector.inputSlots + 1).is(Items.DIAMOND));
        assertEquals(1, collector.getItem(collector.inputSlots + 1).getCount());
    }

    @Test
    void fuelUpgradeStacksIntoMatchingOutput() {
        TestCollector collector = new TestCollector(1, 4);
        collector.setStoredEmc(384);
        collector.setItem(collector.inputSlots, itemStack(Items.COAL, 1));
        collector.setItem(collector.inputSlots + 1, itemStack(Items.DIAMOND, 63));
        ToLongFunction<ItemStack> emcValue = emcValues(Map.of(
              Items.COAL, 128L, Items.DIAMOND, 512L));

        assertTrue(collector.upgradeFuel(
              ignored -> itemStack(Items.DIAMOND, 1), emcValue));

        assertEquals(0, collector.getStoredEmc());
        assertTrue(collector.getItem(collector.inputSlots).isEmpty());
        assertEquals(64, collector.getItem(collector.inputSlots + 1).getCount());
    }

    @Test
    void validFuelWaitsForEnoughEmc() {
        TestCollector collector = new TestCollector(1, 4);
        collector.setStoredEmc(383);
        collector.setItem(collector.inputSlots, itemStack(Items.COAL, 1));
        ToLongFunction<ItemStack> emcValue = emcValues(Map.of(
              Items.COAL, 128L, Items.DIAMOND, 512L));

        assertTrue(collector.upgradeFuel(
              ignored -> itemStack(Items.DIAMOND, 1), emcValue));

        assertEquals(383, collector.getStoredEmc());
        assertEquals(1, collector.getItem(collector.inputSlots).getCount());
        assertTrue(collector.getItem(collector.inputSlots + 1).isEmpty());
    }

    @Test
    void validFuelWaitsForOutputSpace() {
        TestCollector collector = new TestCollector(1, 4);
        collector.setStoredEmc(384);
        collector.setItem(collector.inputSlots, itemStack(Items.COAL, 1));
        collector.setItem(collector.inputSlots + 1, itemStack(Items.EMERALD, 1));
        ToLongFunction<ItemStack> emcValue = emcValues(Map.of(
              Items.COAL, 128L, Items.DIAMOND, 512L));

        assertTrue(collector.upgradeFuel(
              ignored -> itemStack(Items.DIAMOND, 1), emcValue));

        assertEquals(384, collector.getStoredEmc());
        assertEquals(1, collector.getItem(collector.inputSlots).getCount());
        assertTrue(collector.getItem(collector.inputSlots + 1).is(Items.EMERALD));
    }

    @Test
    void highestFuelIsNotHandledAsAnUpgrade() {
        TestCollector collector = new TestCollector(1, 4);
        collector.setStoredEmc(100);
        collector.setItem(collector.inputSlots, itemStack(Items.DIAMOND, 1));

        assertFalse(collector.upgradeFuel(
              ignored -> ItemStack.EMPTY, ignored -> 8_192));

        assertEquals(100, collector.getStoredEmc());
        assertEquals(1, collector.getItem(collector.inputSlots).getCount());
    }

    @Test
    void compactionMovesFirstMainStackIntoUpgradingSlot() {
        TestCollector collector = new TestCollector(1, 4);
        collector.setItem(3, itemStack(Items.COAL, 5));

        collector.compactInputs();

        assertTrue(collector.getItem(collector.inputSlots).is(Items.COAL));
        assertEquals(5, collector.getItem(collector.inputSlots).getCount());
        assertTrue(collector.getItem(3).isEmpty());
    }

    @Test
    void compactionMergesMatchingStacksInUpgradingSlotFirst() {
        TestCollector collector = new TestCollector(1, 4);
        collector.setItem(collector.inputSlots, itemStack(Items.COAL, 60));
        collector.setItem(0, itemStack(Items.COAL, 10));

        collector.compactInputs();

        assertEquals(64, collector.getItem(collector.inputSlots).getCount());
        assertTrue(collector.getItem(0).is(Items.COAL));
        assertEquals(6, collector.getItem(0).getCount());
    }

    @Test
    void compactionDoesNotTouchOutputOrLockSlots() {
        TestCollector collector = new TestCollector(1, 4);
        collector.setItem(0, itemStack(Items.COAL, 1));
        collector.setItem(collector.inputSlots + 1, itemStack(Items.DIAMOND, 2));
        collector.setItem(collector.inputSlots + 2, itemStack(Items.EMERALD, 1));

        collector.compactInputs();

        assertEquals(2, collector.getItem(collector.inputSlots + 1).getCount());
        assertTrue(collector.getItem(collector.inputSlots + 1).is(Items.DIAMOND));
        assertTrue(collector.getItem(collector.inputSlots + 2).is(Items.EMERALD));
    }

    @Test
    void unlockedOutputReturnsToMainInventory() {
        TestCollector collector = new TestCollector(1, 4);
        collector.setItem(collector.inputSlots + 1, itemStack(Items.DIAMOND, 5));

        collector.rotateOutput();

        assertTrue(collector.getItem(0).is(Items.DIAMOND));
        assertEquals(5, collector.getItem(0).getCount());
        assertTrue(collector.getItem(collector.inputSlots + 1).isEmpty());
    }

    @Test
    void matchingLockHoldsPartialOutput() {
        TestCollector collector = new TestCollector(1, 4);
        collector.setItem(collector.inputSlots + 1, itemStack(Items.DIAMOND, 5));
        collector.setItem(collector.inputSlots + 2, itemStack(Items.DIAMOND, 1));

        collector.rotateOutput();

        assertEquals(5, collector.getItem(collector.inputSlots + 1).getCount());
        assertTrue(collector.getItem(0).isEmpty());
    }

    @Test
    void matchingLockReleasesFullOutputStack() {
        TestCollector collector = new TestCollector(1, 4);
        collector.setItem(collector.inputSlots + 1, itemStack(Items.DIAMOND, 64));
        collector.setItem(collector.inputSlots + 2, itemStack(Items.DIAMOND, 1));

        collector.rotateOutput();

        assertEquals(64, collector.getItem(0).getCount());
        assertTrue(collector.getItem(collector.inputSlots + 1).isEmpty());
    }

    @Test
    void outputRemainsWhenMainInventoryIsFull() {
        TestCollector collector = new TestCollector(1, 4);
        for (int slot = 0; slot < collector.inputSlots; slot++) {
            collector.setItem(slot, itemStack(Items.EMERALD, 64));
        }
        collector.setItem(collector.inputSlots + 1, itemStack(Items.DIAMOND, 5));

        collector.rotateOutput();

        assertEquals(5, collector.getItem(collector.inputSlots + 1).getCount());
        for (int slot = 0; slot < collector.inputSlots; slot++) {
            assertEquals(64, collector.getItem(slot).getCount());
            assertTrue(collector.getItem(slot).is(Items.EMERALD));
        }
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

    private static KleinStarItem allocateKleinStar() throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object unsafe = unsafeField.get(null);
        Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        KleinStarItem star = (KleinStarItem) allocateInstance.invoke(
              unsafe, KleinStarItem.class);
        Field tierField = KleinStarItem.class.getDeclaredField("tier");
        tierField.setAccessible(true);
        tierField.set(star, "ein");
        return star;
    }

    private static ItemStack kleinStarStack(long storedEmc) throws Exception {
        ItemStack stack = new ItemStack(Holder.direct(allocateKleinStar(), DataComponentMap.EMPTY));
        KleinStarItem.setStoredEmc(stack, storedEmc);
        return stack;
    }

    private static ToLongFunction<ItemStack> emcValues(
          Map<net.minecraft.world.item.Item, Long> values
    ) {
        return stack -> values.getOrDefault(stack.getItem(), 0L);
    }

    private static ItemStack itemStack(net.minecraft.world.item.Item item, int count) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.MAX_STACK_SIZE, 64);
        stack.setCount(count);
        return stack;
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
