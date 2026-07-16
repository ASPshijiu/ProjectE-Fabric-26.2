package moze_intel.projecte.content.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.function.ToLongFunction;
import moze_intel.projecte.content.items.KleinStarItem;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
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

class RelayBlockEntityTest {
    private static HolderLookup.Provider registries;
    private static BlockEntityType<?> relayType;

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        relayType = Objects.requireNonNull(BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(
              Identifier.withDefaultNamespace("furnace")));
    }

    @Test
    void saveAndLoadPreserveStoredEmcAndInventory() {
        TestRelay source = new TestRelay();
        source.setStoredEmc(65_432);
        source.setItem(0, new ItemStack(Items.EMERALD));

        CompoundTag saved = source.saveCustomOnly(registries);
        TestRelay restored = new TestRelay();
        restored.loadCustomOnly(TagValueInput.create(
              ProblemReporter.DISCARDING, registries, saved));

        assertEquals(65_432, restored.getStoredEmc());
        assertTrue(restored.getItem(0).is(Items.EMERALD));
        assertEquals(1, restored.getItem(0).getCount());
    }

    @Test
    void storedEmcIsCappedByRelayTier() {
        TestRelay mk1 = new TestRelay(1, 64);
        TestRelay mk2 = new TestRelay(2, 192);
        TestRelay mk3 = new TestRelay(3, 640);

        mk1.setStoredEmc(Long.MAX_VALUE);
        mk2.setStoredEmc(Long.MAX_VALUE);
        mk3.setStoredEmc(Long.MAX_VALUE);

        assertEquals(100_000, mk1.getStoredEmc());
        assertEquals(1_000_000, mk2.getStoredEmc());
        assertEquals(10_000_000, mk3.getStoredEmc());
        mk1.setStoredEmc(-1);
        assertEquals(0, mk1.getStoredEmc());
    }

    @Test
    void inventorySizeMatchesRelayTier() {
        TestRelay mk1 = new TestRelay(1, 64);
        TestRelay mk2 = new TestRelay(2, 192);
        TestRelay mk3 = new TestRelay(3, 640);

        assertEquals(8, mk1.getContainerSize());
        assertEquals(14, mk2.getContainerSize());
        assertEquals(22, mk3.getContainerSize());
    }

    @Test
    void saveAndLoadPreserveChargingSlot() {
        TestRelay source = new TestRelay(1, 64);
        assertEquals(8, source.getContainerSize());
        source.setItem(7, new ItemStack(Items.DIAMOND));

        CompoundTag saved = source.saveCustomOnly(registries);
        TestRelay restored = new TestRelay(1, 64);
        restored.loadCustomOnly(TagValueInput.create(
              ProblemReporter.DISCARDING, registries, saved));

        assertTrue(restored.getItem(7).is(Items.DIAMOND));
        assertEquals(1, restored.getItem(7).getCount());
    }

    @Test
    void burnsOneItemFromInputSlotsPerTick() {
        TestRelay relay = new TestRelay(1, 64);
        relay.setItem(3, stack(Items.REDSTONE, 1));
        relay.setItem(4, stack(Items.REDSTONE, 1));

        assertTrue(burnOneInput(relay, ignored -> 64));

        assertEquals(64, relay.getStoredEmc());
        assertTrue(relay.getItem(3).isEmpty());
        assertEquals(1, relay.getItem(4).getCount());
    }

    @Test
    void doesNotBurnItemWhenItsEmcExceedsRemainingCapacity() {
        TestRelay relay = new TestRelay(1, 64);
        relay.setStoredEmc(relay.getMaximumEmc() - 32);
        relay.setItem(0, stack(Items.REDSTONE, 1));

        assertFalse(burnOneInput(relay, ignored -> 64));

        assertEquals(relay.getMaximumEmc() - 32, relay.getStoredEmc());
        assertEquals(1, relay.getItem(0).getCount());
    }

    @Test
    void doesNotBurnItemWithoutEmcValue() {
        TestRelay relay = new TestRelay(1, 64);
        relay.setItem(0, stack(Items.REDSTONE, 1));

        assertFalse(burnOneInput(relay, ignored -> 0));

        assertEquals(0, relay.getStoredEmc());
        assertEquals(1, relay.getItem(0).getCount());
    }

    @Test
    void doesNotBurnItemFromChargingSlot() {
        TestRelay relay = new TestRelay(1, 64);
        relay.setItem(7, stack(Items.DIAMOND, 1));

        assertFalse(burnOneInput(relay, ignored -> 8_192));

        assertEquals(0, relay.getStoredEmc());
        assertEquals(1, relay.getItem(7).getCount());
    }

    @Test
    void doesNotConsumeKleinStarAsLooseFuel() throws Exception {
        TestRelay relay = new TestRelay(1, 64);
        KleinStarItem star = allocateKleinStar("ein");
        ItemStack starStack = new ItemStack(Holder.direct(star, DataComponentMap.EMPTY));
        relay.setItem(0, starStack);

        assertFalse(burnOneInput(relay, ignored -> 139_264));

        assertEquals(0, relay.getStoredEmc());
        assertTrue(relay.getItem(0).is(star));
    }

    @Test
    void dischargesKleinStarAtRelayTierRate() throws Exception {
        TestRelay mk1 = new TestRelay(1, 64);
        TestRelay mk2 = new TestRelay(2, 192);
        TestRelay mk3 = new TestRelay(3, 640);
        ItemStack mk1Star = kleinStarStack(1_000);
        ItemStack mk2Star = kleinStarStack(1_000);
        ItemStack mk3Star = kleinStarStack(1_000);
        mk1.setItem(0, mk1Star);
        mk2.setItem(0, mk2Star);
        mk3.setItem(0, mk3Star);

        assertTrue(burnOneInput(mk1, ignored -> 139_264));
        assertTrue(burnOneInput(mk2, ignored -> 139_264));
        assertTrue(burnOneInput(mk3, ignored -> 139_264));

        assertEquals(64, mk1.getStoredEmc());
        assertEquals(192, mk2.getStoredEmc());
        assertEquals(640, mk3.getStoredEmc());
        assertEquals(936, KleinStarItem.getStoredEmc(mk1Star));
        assertEquals(808, KleinStarItem.getStoredEmc(mk2Star));
        assertEquals(360, KleinStarItem.getStoredEmc(mk3Star));
    }

    @Test
    void kleinStarDischargeDoesNotExceedRelayCapacity() throws Exception {
        TestRelay relay = new TestRelay(1, 64);
        relay.setStoredEmc(relay.getMaximumEmc() - 32);
        ItemStack starStack = kleinStarStack(1_000);
        relay.setItem(0, starStack);

        assertTrue(burnOneInput(relay, ignored -> 139_264));

        assertEquals(relay.getMaximumEmc(), relay.getStoredEmc());
        assertEquals(968, KleinStarItem.getStoredEmc(starStack));
    }

    @Test
    void kleinStarDischargeDoesNotExceedStoredEmc() throws Exception {
        TestRelay relay = new TestRelay(3, 640);
        ItemStack starStack = kleinStarStack(32);
        relay.setItem(0, starStack);

        assertTrue(burnOneInput(relay, ignored -> 139_264));

        assertEquals(32, relay.getStoredEmc());
        assertEquals(0, KleinStarItem.getStoredEmc(starStack));
    }

    @Test
    void chargesKleinStarAtRelayTierRate() throws Exception {
        TestRelay mk1 = new TestRelay(1, 64);
        TestRelay mk2 = new TestRelay(2, 192);
        TestRelay mk3 = new TestRelay(3, 640);
        ItemStack mk1Star = kleinStarStack(0);
        ItemStack mk2Star = kleinStarStack(0);
        ItemStack mk3Star = kleinStarStack(0);
        mk1.setStoredEmc(1_000);
        mk2.setStoredEmc(1_000);
        mk3.setStoredEmc(1_000);
        mk1.setItem(mk1.getContainerSize() - 1, mk1Star);
        mk2.setItem(mk2.getContainerSize() - 1, mk2Star);
        mk3.setItem(mk3.getContainerSize() - 1, mk3Star);

        assertTrue(chargeOutput(mk1));
        assertTrue(chargeOutput(mk2));
        assertTrue(chargeOutput(mk3));

        assertEquals(64, KleinStarItem.getStoredEmc(mk1Star));
        assertEquals(192, KleinStarItem.getStoredEmc(mk2Star));
        assertEquals(640, KleinStarItem.getStoredEmc(mk3Star));
        assertEquals(936, mk1.getStoredEmc());
        assertEquals(808, mk2.getStoredEmc());
        assertEquals(360, mk3.getStoredEmc());
    }

    @Test
    void chargingDoesNotSpendMoreEmcThanKleinStarCanAccept() throws Exception {
        TestRelay relay = new TestRelay(3, 640);
        ItemStack starStack = kleinStarStack(KleinStarItem.MAX_EIN - 32);
        relay.setStoredEmc(1_000);
        relay.setItem(relay.getContainerSize() - 1, starStack);

        assertTrue(chargeOutput(relay));

        assertEquals(KleinStarItem.MAX_EIN, KleinStarItem.getStoredEmc(starStack));
        assertEquals(968, relay.getStoredEmc());
    }

    @Test
    void chargingDoesNotSpendMoreEmcThanRelayStores() throws Exception {
        TestRelay relay = new TestRelay(3, 640);
        ItemStack starStack = kleinStarStack(0);
        relay.setStoredEmc(32);
        relay.setItem(relay.getContainerSize() - 1, starStack);

        assertTrue(chargeOutput(relay));

        assertEquals(32, KleinStarItem.getStoredEmc(starStack));
        assertEquals(0, relay.getStoredEmc());
    }

    @Test
    void doesNotChargeKleinStarFromInputSlot() throws Exception {
        TestRelay relay = new TestRelay(1, 64);
        ItemStack starStack = kleinStarStack(0);
        relay.setStoredEmc(1_000);
        relay.setItem(0, starStack);

        assertFalse(chargeOutput(relay));

        assertEquals(0, KleinStarItem.getStoredEmc(starStack));
        assertEquals(1_000, relay.getStoredEmc());
    }

    @Test
    void sendsEmcAtRelayTierRate() {
        TestRelay mk1 = relayWithEmc(1, 64, 1_000);
        TestRelay mk2 = relayWithEmc(2, 192, 1_000);
        TestRelay mk3 = relayWithEmc(3, 640, 1_000);
        TestCondenser mk1Target = acceptingCondenser();
        TestCondenser mk2Target = acceptingCondenser();
        TestCondenser mk3Target = acceptingCondenser();

        assertEquals(64, sendEmcToCondensers(mk1, List.of(mk1Target)));
        assertEquals(192, sendEmcToCondensers(mk2, List.of(mk2Target)));
        assertEquals(640, sendEmcToCondensers(mk3, List.of(mk3Target)));

        assertEquals(64, mk1Target.getStoredEmc());
        assertEquals(192, mk2Target.getStoredEmc());
        assertEquals(640, mk3Target.getStoredEmc());
    }

    @Test
    void transferCannotExceedRelayBalance() {
        TestRelay relay = relayWithEmc(1, 64, 32);
        TestCondenser condenser = acceptingCondenser();

        assertEquals(32, sendEmcToCondensers(relay, List.of(condenser)));

        assertEquals(0, relay.getStoredEmc());
        assertEquals(32, condenser.getStoredEmc());
    }

    @Test
    void splitsTransferEvenlyBetweenCondensers() {
        TestRelay relay = relayWithEmc(1, 64, 100);
        TestCondenser first = acceptingCondenser();
        TestCondenser second = acceptingCondenser();

        assertEquals(64, sendEmcToCondensers(relay, List.of(first, second)));

        assertEquals(36, relay.getStoredEmc());
        assertEquals(32, first.getStoredEmc());
        assertEquals(32, second.getStoredEmc());
    }

    @Test
    void undividedTransferRemainderStaysInRelay() {
        TestRelay relay = relayWithEmc(1, 64, 100);
        TestCondenser first = acceptingCondenser();
        TestCondenser second = acceptingCondenser();
        TestCondenser third = acceptingCondenser();

        assertEquals(63, sendEmcToCondensers(
              relay, List.of(first, second, third)));

        assertEquals(37, relay.getStoredEmc());
        assertEquals(21, first.getStoredEmc());
        assertEquals(21, second.getStoredEmc());
        assertEquals(21, third.getStoredEmc());
    }

    @Test
    void invalidTargetDoesNotReduceAcceptingCondenserShare() {
        TestRelay relay = relayWithEmc(1, 64, 100);
        TestCondenser invalid = new TestCondenser();
        TestCondenser accepting = acceptingCondenser();

        assertEquals(64, sendEmcToCondensers(
              relay, List.of(invalid, accepting)));

        assertEquals(0, invalid.getStoredEmc());
        assertEquals(64, accepting.getStoredEmc());
    }

    @Test
    void fullCondenserDoesNotReduceAcceptingCondenserShare() {
        TestRelay relay = relayWithEmc(1, 64, 100);
        TestCondenser full = acceptingCondenser();
        TestCondenser accepting = acceptingCondenser();
        full.setStoredEmc(Long.MAX_VALUE);

        assertEquals(64, sendEmcToCondensers(
              relay, List.of(full, accepting)));

        assertEquals(Long.MAX_VALUE, full.getStoredEmc());
        assertEquals(64, accepting.getStoredEmc());
    }

    @Test
    void transferDeductsOnlyWhatCondenserAccepts() {
        TestRelay relay = relayWithEmc(1, 64, 100);
        TestCondenser condenser = acceptingCondenser();
        condenser.setStoredEmc(Long.MAX_VALUE - 32);

        assertEquals(32, sendEmcToCondensers(relay, List.of(condenser)));

        assertEquals(68, relay.getStoredEmc());
        assertEquals(Long.MAX_VALUE, condenser.getStoredEmc());
    }

    @Test
    void collectorBonusMatchesRelayTier() {
        TestRelay mk1 = new TestRelay(1, 64);
        TestRelay mk2 = new TestRelay(2, 192);
        TestRelay mk3 = new TestRelay(3, 640);

        addCollectorBonus(mk1, 20);
        addCollectorBonus(mk2, 7);
        addCollectorBonus(mk3, 2);

        assertEquals(1, mk1.getStoredEmc());
        assertEquals(1, mk2.getStoredEmc());
        assertEquals(1, mk3.getStoredEmc());
    }

    @Test
    void saveAndLoadPreserveFractionalCollectorBonus() {
        TestRelay source = new TestRelay(1, 64);
        addCollectorBonus(source, 10);

        CompoundTag saved = source.saveCustomOnly(registries);
        TestRelay restored = new TestRelay(1, 64);
        restored.loadCustomOnly(TagValueInput.create(
              ProblemReporter.DISCARDING, registries, saved));
        addCollectorBonus(restored, 10);

        assertEquals(1, restored.getStoredEmc());
    }

    @Test
    void collectorBonusDoesNotAccumulateWhileRelayIsFull() {
        TestRelay relay = new TestRelay(1, 64);
        relay.setStoredEmc(relay.getMaximumEmc());
        addCollectorBonus(relay, 20);

        relay.setStoredEmc(relay.getMaximumEmc() - 1);
        addCollectorBonus(relay, 19);
        assertEquals(relay.getMaximumEmc() - 1, relay.getStoredEmc());

        addCollectorBonus(relay, 1);
        assertEquals(relay.getMaximumEmc(), relay.getStoredEmc());
    }

    @Test
    void collectorPassesBonusToAdjacentRelay() {
        TestRelay relay = new TestRelay(1, 64);

        for (int tick = 0; tick < 20; tick++) {
            sendRelayBonus(relay);
        }

        assertEquals(1, relay.getStoredEmc());
    }

    private static void addCollectorBonus(TestRelay relay, int times) {
        for (int count = 0; count < times; count++) {
            relay.addBonus();
        }
    }

    private static void sendRelayBonus(TestRelay relay) {
        CollectorBlockEntity.Base.sendRelayBonus(relay);
    }

    private static boolean burnOneInput(
          TestRelay relay, ToLongFunction<ItemStack> emcValue
    ) {
        return relay.burnOneInput(emcValue);
    }

    private static boolean chargeOutput(TestRelay relay) {
        return relay.chargeOutput();
    }

    private static long sendEmcToCondensers(
          TestRelay relay, List<CondenserBlockEntity.Base> condensers
    ) {
        return RelayBlockEntity.Base.sendEmcToCondensers(relay, condensers);
    }

    private static TestRelay relayWithEmc(
          int tier, int transferRate, long storedEmc
    ) {
        TestRelay relay = new TestRelay(tier, transferRate);
        relay.setStoredEmc(storedEmc);
        return relay;
    }

    private static TestCondenser acceptingCondenser() {
        TestCondenser condenser = new TestCondenser();
        condenser.setTarget(new ItemStack(Items.DIAMOND));
        condenser.refreshTargetEmc(ignored -> 8_192);
        return condenser;
    }

    private static KleinStarItem allocateKleinStar(String tier) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object unsafe = unsafeField.get(null);
        Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        KleinStarItem star = (KleinStarItem) allocateInstance.invoke(
              unsafe, KleinStarItem.class);
        Field tierField = KleinStarItem.class.getDeclaredField("tier");
        tierField.setAccessible(true);
        tierField.set(star, tier);
        return star;
    }

    private static ItemStack kleinStarStack(long storedEmc) throws Exception {
        KleinStarItem star = allocateKleinStar("ein");
        ItemStack stack = new ItemStack(Holder.direct(star, DataComponentMap.EMPTY));
        KleinStarItem.setStoredEmc(stack, storedEmc);
        return stack;
    }

    private static ItemStack stack(net.minecraft.world.item.Item item, int count) {
        ItemStack stack = new ItemStack(item);
        stack.setCount(count);
        return stack;
    }

    private static final class TestRelay extends RelayBlockEntity.Base {
        private TestRelay() {
            this(1, 64);
        }

        private TestRelay(int tier, int transferRate) {
            this(BlockPos.ZERO, Blocks.FURNACE.defaultBlockState(), tier, transferRate);
        }

        private TestRelay(
              BlockPos pos, BlockState state, int tier, int transferRate
        ) {
            super(relayType, pos, state, tier, transferRate);
        }
    }

    private static final class TestCondenser extends CondenserBlockEntity.Base {
        private TestCondenser() {
            super(relayType, BlockPos.ZERO, Blocks.FURNACE.defaultBlockState(), 1);
        }
    }
}
