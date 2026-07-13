package moze_intel.projecte.content.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class SyncedLongTest {
    @Test
    void transfersTheFullNonNegativeLongThroughTwoDataSlots() {
        AtomicLong serverValue = new AtomicLong(Long.MAX_VALUE);
        SyncedLong server = new SyncedLong(serverValue::get);
        SyncedLong client = new SyncedLong(() -> 0L);

        client.lowSlot().set(server.lowSlot().get());
        client.highSlot().set(server.highSlot().get());

        assertEquals(Long.MAX_VALUE, client.value());
    }

    @Test
    void tracksChangesWithoutTruncatingToAnInt() {
        long value = (1L << 40) + 123;
        SyncedLong server = new SyncedLong(() -> value);
        SyncedLong client = new SyncedLong(() -> 0L);

        client.highSlot().set(server.highSlot().get());
        client.lowSlot().set(server.lowSlot().get());

        assertEquals(value, client.value());
    }
}
