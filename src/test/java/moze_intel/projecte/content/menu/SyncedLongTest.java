package moze_intel.projecte.content.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.world.inventory.DataSlot;
import org.junit.jupiter.api.Test;

class SyncedLongTest {
    @Test
    void transfersTheFullNonNegativeLongThroughFourUnsignedShortSlots() {
        AtomicLong serverValue = new AtomicLong(Long.MAX_VALUE);
        SyncedLong server = new SyncedLong(serverValue::get);
        SyncedLong client = new SyncedLong(() -> 0L);

        transfer(server, client);

        assertEquals(Long.MAX_VALUE, client.value());
    }

    @Test
    void tracksChangesWithoutTruncatingToAnInt() {
        long value = (1L << 40) + 123;
        SyncedLong server = new SyncedLong(() -> value);
        SyncedLong client = new SyncedLong(() -> 0L);

        transfer(server, client);

        assertEquals(value, client.value());
    }

    private static void transfer(SyncedLong server, SyncedLong client) {
        for (int index = server.slots().size() - 1; index >= 0; index--) {
            DataSlot source = server.slots().get(index);
            client.slots().get(index).set((short) source.get());
        }
    }
}
