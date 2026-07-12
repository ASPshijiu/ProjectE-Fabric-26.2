package moze_intel.projecte.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import moze_intel.projecte.emc.FakeStackKey;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class PlayerInputLocksTest {
    private static FakeStackKey key(String path) {
        return new FakeStackKey(Identifier.fromNamespaceAndPath("projecte", path));
    }

    @Test
    void emptyHasNineSlotsAllEmpty() {
        PlayerInputLocks locks = PlayerInputLocks.empty();
        assertEquals(PlayerInputLocks.LOCK_SLOTS, locks.size());
        for (int slot = 0; slot < locks.size(); slot++) {
            assertTrue(locks.isEmpty(slot), "slot " + slot + " should be empty");
            assertNull(locks.get(slot));
        }
    }

    @Test
    void setAndGetRoundTrips() {
        FakeStackKey gem = key("gem");
        PlayerInputLocks locks = PlayerInputLocks.empty().set(3, gem);
        assertNull(locks.get(2));
        assertEquals(gem, locks.get(3));
        assertFalse(locks.isEmpty(3));
    }

    @Test
    void settingNullClearsSlot() {
        FakeStackKey gem = key("gem");
        PlayerInputLocks locks = PlayerInputLocks.empty().set(0, gem).set(0, null);
        assertNull(locks.get(0));
        assertTrue(locks.isEmpty(0));
    }

    @Test
    void rejectsSlotOutOfBounds() {
        PlayerInputLocks locks = PlayerInputLocks.empty();
        assertThrows(IndexOutOfBoundsException.class, () -> locks.set(-1, key("gem")));
        assertThrows(IndexOutOfBoundsException.class, () -> locks.set(locks.size(), key("gem")));
        assertThrows(IndexOutOfBoundsException.class, () -> locks.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> locks.get(locks.size()));
    }

    @Test
    void copyIsIndependentDeepCopy() {
        FakeStackKey gem = key("gem");
        PlayerInputLocks locks = PlayerInputLocks.empty().set(0, gem);
        PlayerInputLocks copy = locks.copy();
        assertNotSame(locks, copy);
        assertEquals(gem, copy.get(0));
        PlayerInputLocks mutated = copy.set(0, key("ring"));
        assertEquals(gem, locks.get(0));
        assertEquals(key("ring"), mutated.get(0));
    }
}
