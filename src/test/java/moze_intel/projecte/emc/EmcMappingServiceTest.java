package moze_intel.projecte.emc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class EmcMappingServiceTest {
    @Test
    void snapshotsAreImmutableDefensiveCopiesWithMonotonicVersions() {
        EmcMappingService<String> service = new EmcMappingService<>();
        Map<String, EmcValue> mutable = new HashMap<>(Map.of("a", EmcValue.of(1)));
        EmcMappingSnapshot<String> first = service.replace(mutable);
        mutable.put("b", EmcValue.of(2));

        assertEquals(1, first.version());
        assertEquals(Map.of("a", EmcValue.of(1)), first.values());
        assertThrows(UnsupportedOperationException.class, () -> first.values().put("x", EmcValue.of(3)));

        EmcMappingSnapshot<String> second = service.replace(Map.of("b", EmcValue.of(2)));
        assertEquals(2, second.version());
        assertNotSame(first, second);
    }

    @Test
    void rebuildFailurePreservesPreviousSnapshot() {
        EmcMappingService<String> service = new EmcMappingService<>();
        EmcMappingSnapshot<String> previous = service.replace(Map.of("a", EmcValue.of(1)));
        EmcMappingService.RebuildResult<String> result = service.rebuild(() -> {
            throw new IllegalStateException("broken data");
        });

        assertFalse(result.success());
        assertTrue(result.exception().isPresent());
        assertSame(previous, result.snapshot());
        assertSame(previous, service.current());
    }

    @Test
    void concurrentReadersObserveOnlyCompleteSnapshots() throws Exception {
        EmcMappingService<String> service = new EmcMappingService<>();
        Map<String, EmcValue> left = Map.of("a", EmcValue.of(1), "b", EmcValue.of(2));
        Map<String, EmcValue> right = Map.of("c", EmcValue.of(3), "d", EmcValue.of(4));
        service.replace(left);
        AtomicBoolean mixed = new AtomicBoolean(false);
        AtomicBoolean done = new AtomicBoolean(false);
        CountDownLatch started = new CountDownLatch(1);

        Thread reader = Thread.ofVirtual().start(() -> {
            started.countDown();
            while (!done.get()) {
                Map<String, EmcValue> values = service.current().values();
                if (!values.equals(left) && !values.equals(right)) {
                    mixed.set(true);
                    return;
                }
            }
        });
        started.await();
        for (int index = 0; index < 1_000; index++) {
            service.replace((index & 1) == 0 ? right : left);
        }
        done.set(true);
        reader.join();

        assertFalse(mixed.get());
    }
}
