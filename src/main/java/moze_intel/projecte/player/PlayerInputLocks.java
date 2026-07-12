package moze_intel.projecte.player;

import java.util.Arrays;
import java.util.Objects;
import moze_intel.projecte.emc.NormalizedStackKey;

/**
 * Immutable fixed-size inventory of transmutation-table input locks (9 slots), matching ProjectE's
 * {@code LOCK_SLOTS}. Empty slots hold {@code null}. Every mutator returns a new instance.
 */
public final class PlayerInputLocks {
    public static final int LOCK_SLOTS = 9;

    private final NormalizedStackKey[] slots;

    private PlayerInputLocks(NormalizedStackKey[] slots) {
        this.slots = slots;
    }

    public static PlayerInputLocks empty() {
        return new PlayerInputLocks(new NormalizedStackKey[LOCK_SLOTS]);
    }

    /**
     * Reconstruction point used by the codec. The array is copied and padded/truncated to
     * {@link #LOCK_SLOTS}.
     */
    public static PlayerInputLocks of(NormalizedStackKey[] slots) {
        Objects.requireNonNull(slots, "slots");
        NormalizedStackKey[] copy = new NormalizedStackKey[LOCK_SLOTS];
        System.arraycopy(slots, 0, copy, 0, Math.min(slots.length, LOCK_SLOTS));
        return new PlayerInputLocks(copy);
    }

    public int size() {
        return LOCK_SLOTS;
    }

    public NormalizedStackKey get(int slot) {
        checkSlot(slot);
        return slots[slot];
    }

    public boolean isEmpty(int slot) {
        checkSlot(slot);
        return slots[slot] == null;
    }

    public PlayerInputLocks set(int slot, NormalizedStackKey key) {
        checkSlot(slot);
        NormalizedStackKey[] copy = slots.clone();
        copy[slot] = key;
        return new PlayerInputLocks(copy);
    }

    public PlayerInputLocks copy() {
        return new PlayerInputLocks(slots.clone());
    }

    /**
     * @return a defensive snapshot of the slots for serialization.
     */
    public NormalizedStackKey[] toArray() {
        return slots.clone();
    }

    private static void checkSlot(int slot) {
        if (slot < 0 || slot >= LOCK_SLOTS) {
            throw new IndexOutOfBoundsException("input-lock slot out of range: " + slot);
        }
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof PlayerInputLocks that && Arrays.equals(slots, that.slots);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(slots);
    }
}
