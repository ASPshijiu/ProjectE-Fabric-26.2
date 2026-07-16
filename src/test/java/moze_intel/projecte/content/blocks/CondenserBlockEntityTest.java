package moze_intel.projecte.content.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;
import java.util.function.ToLongFunction;
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

class CondenserBlockEntityTest {
    private static HolderLookup.Provider registries;
    private static BlockEntityType<?> condenserType;

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        condenserType = Objects.requireNonNull(BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(
              Identifier.withDefaultNamespace("furnace")));
    }

    @Test
    void saveAndLoadPreserveStoredEmcAndFullInventory() {
        TestCondenser source = new TestCondenser();
        source.setStoredEmc(987_654);
        source.setItem(0, new ItemStack(Items.DIAMOND));
        source.setItem(101, new ItemStack(Items.EMERALD));

        CompoundTag saved = source.saveCustomOnly(registries);
        TestCondenser restored = new TestCondenser();
        restored.loadCustomOnly(TagValueInput.create(
              ProblemReporter.DISCARDING, registries, saved));

        assertEquals(987_654, restored.getStoredEmc());
        assertTrue(restored.getItem(0).is(Items.DIAMOND));
        assertTrue(restored.getItem(101).is(Items.EMERALD));
        assertEquals(102, restored.getContainerSize());
    }

    @Test
    void targetIsStoredAsSingleGhostCopy() {
        TestCondenser condenser = new TestCondenser();
        ItemStack source = new ItemStack(Items.DIAMOND);
        source.setCount(32);

        condenser.setTarget(source);

        assertTrue(condenser.getTarget().is(Items.DIAMOND));
        assertEquals(1, condenser.getTarget().getCount());
        assertEquals(32, source.getCount());
        assertTrue(condenser.isEmpty());
    }

    @Test
    void saveAndLoadPreserveTarget() {
        TestCondenser source = new TestCondenser();
        source.setTarget(new ItemStack(Items.EMERALD));

        CompoundTag saved = source.saveCustomOnly(registries);
        TestCondenser restored = new TestCondenser();
        restored.loadCustomOnly(TagValueInput.create(
              ProblemReporter.DISCARDING, registries, saved));

        assertTrue(restored.getTarget().is(Items.EMERALD));
        assertEquals(1, restored.getTarget().getCount());
    }

    @Test
    void targetRequirementTracksCurrentEmcMapping() {
        TestCondenser condenser = new TestCondenser();
        condenser.setTarget(new ItemStack(Items.DIAMOND));

        condenser.refreshTargetEmc(ignored -> 8_192);
        assertEquals(8_192, condenser.getRequiredEmc());

        condenser.refreshTargetEmc(ignored -> 0);
        assertEquals(0, condenser.getRequiredEmc());
    }

    @Test
    void clearingTargetClearsRequirement() {
        TestCondenser condenser = new TestCondenser();
        condenser.setTarget(new ItemStack(Items.DIAMOND));
        condenser.refreshTargetEmc(ignored -> 8_192);

        condenser.setTarget(ItemStack.EMPTY);

        assertTrue(condenser.getTarget().isEmpty());
        assertEquals(0, condenser.getRequiredEmc());
    }

    @Test
    void mk1ConsumesOneInputItemPerTick() {
        TestCondenser condenser = new TestCondenser(1);
        prepareTarget(condenser);
        ItemStack input = stack(Items.REDSTONE, 5);
        condenser.items.set(0, input);

        assertEquals(64, consumeInput(condenser, ignored -> 64));

        assertEquals(64, condenser.getStoredEmc());
        assertEquals(4, input.getCount());
    }

    @Test
    void mk2ConsumesOneWholeInputStackPerTick() {
        TestCondenser condenser = new TestCondenser(2);
        prepareTarget(condenser);
        ItemStack input = stack(Items.REDSTONE, 5);
        condenser.items.set(0, input);

        assertEquals(320, consumeInput(condenser, ignored -> 64));

        assertEquals(320, condenser.getStoredEmc());
        assertTrue(input.isEmpty());
    }

    @Test
    void doesNotConsumeStackMatchingTarget() {
        TestCondenser condenser = new TestCondenser();
        prepareTarget(condenser);
        ItemStack input = stack(Items.DIAMOND, 3);
        condenser.items.set(0, input);

        assertEquals(0, consumeInput(condenser, ignored -> 8_192));

        assertEquals(0, condenser.getStoredEmc());
        assertEquals(3, input.getCount());
    }

    @Test
    void doesNotConsumeInputWithoutEmc() {
        TestCondenser condenser = new TestCondenser();
        prepareTarget(condenser);
        ItemStack input = stack(Items.DIRT, 1);
        condenser.items.set(0, input);

        assertEquals(0, consumeInput(condenser, ignored -> 0));

        assertEquals(0, condenser.getStoredEmc());
        assertEquals(1, input.getCount());
    }

    @Test
    void doesNotConsumeInputWithoutValidTarget() {
        TestCondenser condenser = new TestCondenser();
        condenser.setTarget(new ItemStack(Items.DIAMOND));
        condenser.refreshTargetEmc(ignored -> 0);
        ItemStack input = stack(Items.REDSTONE, 1);
        condenser.items.set(0, input);

        assertEquals(0, consumeInput(condenser, ignored -> 64));

        assertEquals(0, condenser.getStoredEmc());
        assertEquals(1, input.getCount());
    }

    @Test
    void doesNotConsumeInputWhenStoredEmcWouldOverflow() {
        TestCondenser condenser = new TestCondenser();
        prepareTarget(condenser);
        condenser.setStoredEmc(Long.MAX_VALUE - 32);
        ItemStack input = stack(Items.REDSTONE, 1);
        condenser.items.set(0, input);

        assertEquals(0, consumeInput(condenser, ignored -> 64));

        assertEquals(Long.MAX_VALUE - 32, condenser.getStoredEmc());
        assertEquals(1, input.getCount());
    }

    @Test
    void mk2DoesNotConsumeStackWhenEmcMultiplicationOverflows() {
        TestCondenser condenser = new TestCondenser(2);
        prepareTarget(condenser);
        ItemStack input = stack(Items.REDSTONE, 2);
        condenser.items.set(0, input);

        assertEquals(0, consumeInput(condenser, ignored -> Long.MAX_VALUE));

        assertEquals(0, condenser.getStoredEmc());
        assertEquals(2, input.getCount());
    }

    private static void prepareTarget(TestCondenser condenser) {
        condenser.setTarget(new ItemStack(Items.DIAMOND));
        condenser.refreshTargetEmc(ignored -> 8_192);
    }

    private static long consumeInput(
          TestCondenser condenser, ToLongFunction<ItemStack> emcValue
    ) {
        return condenser.consumeInput(emcValue);
    }

    private static ItemStack stack(net.minecraft.world.item.Item item, int count) {
        ItemStack stack = new ItemStack(item);
        stack.setCount(count);
        return stack;
    }

    private static final class TestCondenser extends CondenserBlockEntity.Base {
        private TestCondenser() {
            this(1);
        }

        private TestCondenser(int tier) {
            this(BlockPos.ZERO, Blocks.FURNACE.defaultBlockState(), tier);
        }

        private TestCondenser(BlockPos pos, BlockState state, int tier) {
            super(condenserType, pos, state, tier);
        }
    }
}
