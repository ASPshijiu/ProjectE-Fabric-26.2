package moze_intel.projecte.content.blocks;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class FurnaceBurnTimePersistence {
    private static final String LIT_TIME_REMAINING = "lit_time_remaining";
    private static final String LIT_TOTAL_TIME = "lit_total_time";

    private FurnaceBurnTimePersistence() {
    }

    public static int readLitTimeRemaining(ValueInput input) {
        return input.getIntOr(LIT_TIME_REMAINING, 0);
    }

    public static int readLitTotalTime(ValueInput input) {
        return input.getIntOr(LIT_TOTAL_TIME, 0);
    }

    public static void writeBurnTimes(ValueOutput output, int remaining, int total) {
        output.putInt(LIT_TIME_REMAINING, remaining);
        output.putInt(LIT_TOTAL_TIME, total);
    }
}
