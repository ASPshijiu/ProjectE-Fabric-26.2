package moze_intel.projecte.content.menu;

import java.util.Objects;
import java.util.function.LongSupplier;
import net.minecraft.world.inventory.DataSlot;

/** Synchronizes one non-negative long through vanilla's two integer menu data slots. */
final class SyncedLong {
    private final LongSupplier source;
    private long value;
    private final DataSlot lowSlot = new DataSlot() {
        @Override
        public int get() {
            return (int) source.getAsLong();
        }

        @Override
        public void set(int low) {
            value = (value & 0xFFFFFFFF00000000L) | Integer.toUnsignedLong(low);
        }
    };
    private final DataSlot highSlot = new DataSlot() {
        @Override
        public int get() {
            return (int) (source.getAsLong() >>> 32);
        }

        @Override
        public void set(int high) {
            value = ((long) high << 32) | (value & 0xFFFFFFFFL);
        }
    };

    SyncedLong(LongSupplier source) {
        this.source = Objects.requireNonNull(source, "source");
    }

    DataSlot lowSlot() {
        return lowSlot;
    }

    DataSlot highSlot() {
        return highSlot;
    }

    long value() {
        return value;
    }
}
