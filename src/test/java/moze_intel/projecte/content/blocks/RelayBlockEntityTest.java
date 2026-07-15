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
}
