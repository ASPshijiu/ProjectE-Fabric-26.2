package moze_intel.projecte.content.menu;

import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;
import net.minecraft.world.inventory.DataSlot;

/** Synchronizes one non-negative long through vanilla's four unsigned-short menu data slots. */
final class SyncedLong {
    private final LongSupplier source;
    private long value;
    private final List<DataSlot> slots;

    SyncedLong(LongSupplier source) {
        this.source = Objects.requireNonNull(source, "source");
        this.slots = List.of(slot(0), slot(16), slot(32), slot(48));
    }

    private DataSlot slot(int shift) {
        return new DataSlot() {
            @Override
            public int get() {
                return (int) (source.getAsLong() >>> shift) & 0xFFFF;
            }

            @Override
            public void set(int part) {
                long mask = 0xFFFFL << shift;
                value = (value & ~mask) | (((long) part & 0xFFFFL) << shift);
            }
        };
    }

    List<DataSlot> slots() {
        return slots;
    }

    long value() {
        return value;
    }
}
