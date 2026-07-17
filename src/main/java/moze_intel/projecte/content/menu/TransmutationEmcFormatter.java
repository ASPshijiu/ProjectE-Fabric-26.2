package moze_intel.projecte.content.menu;

import java.util.Locale;
import net.minecraft.network.chat.Component;

/** Formats the transmutation-table balance without overflowing its narrow EMC display area. */
public final class TransmutationEmcFormatter {
    private static final long MAX_EXACT_DISPLAY = 1_000_000_000_000L;

    private TransmutationEmcFormatter() {
    }

    public static Component format(long emc) {
        if (emc < 0) {
            throw new IllegalArgumentException("EMC cannot be negative: " + emc);
        }
        if (emc < MAX_EXACT_DISPLAY) {
            return Component.literal(String.format(Locale.ROOT, "%,d", emc));
        }
        int groups = (Long.toString(emc).length() - 1) / 3;
        int postfix = groups - 4;
        double divisor = Math.pow(1_000.0, groups);
        String shortened = String.format(Locale.ROOT, "%.2f", emc / divisor);
        return Component.translatable("emc.projecte.postfix." + postfix, shortened);
    }
}
