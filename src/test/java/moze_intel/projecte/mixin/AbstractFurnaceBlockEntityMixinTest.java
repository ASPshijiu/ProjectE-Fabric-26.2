package moze_intel.projecte.mixin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import moze_intel.projecte.content.blocks.FurnaceBurnTimePersistence;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AbstractFurnaceBlockEntityMixinTest {
    private static HolderLookup.Provider registries;

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    @Test
    void preservesBurnTimesBeyondShortRange() {
        TagValueOutput output = TagValueOutput.createWithContext(
              ProblemReporter.DISCARDING, registries);

        FurnaceBurnTimePersistence.writeBurnTimes(output, 230_399, 230_400);

        CompoundTag saved = output.buildResult();
        assertInstanceOf(IntTag.class, saved.get("lit_time_remaining"));
        assertInstanceOf(IntTag.class, saved.get("lit_total_time"));
        ValueInput input = TagValueInput.create(
              ProblemReporter.DISCARDING, registries, saved);
        assertEquals(230_399,
              FurnaceBurnTimePersistence.readLitTimeRemaining(input));
        assertEquals(230_400,
              FurnaceBurnTimePersistence.readLitTotalTime(input));
    }

    @Test
    void readsLegacyShortBurnTimes() {
        CompoundTag legacy = new CompoundTag();
        legacy.putShort("lit_time_remaining", (short) 31_999);
        legacy.putShort("lit_total_time", (short) 32_000);
        ValueInput input = TagValueInput.create(
              ProblemReporter.DISCARDING, registries, legacy);

        assertEquals(31_999,
              FurnaceBurnTimePersistence.readLitTimeRemaining(input));
        assertEquals(32_000,
              FurnaceBurnTimePersistence.readLitTotalTime(input));
    }
}
