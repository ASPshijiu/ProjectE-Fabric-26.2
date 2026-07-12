package moze_intel.projecte.player;

import java.util.Objects;
import java.util.Optional;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.NormalizedStackKey;

/**
 * Server-side facade over a single player's ProjectE data attachments.
 *
 * <p>All mutators compute the new immutable value through checked {@link EmcValue} arithmetic and
 * the immutable {@link PlayerKnowledge}/{@link PlayerInputLocks} models, then write it back through
 * the {@link PlayerAttachmentAccess} boundary. Fabric auto-syncs the new value to the owning client.
 * Clients must never call these methods; this is the server-authoritative surface.
 */
public final class PlayerDataService {
    private final PlayerAttachmentAccess access;

    public PlayerDataService(PlayerAttachmentAccess access) {
        this.access = Objects.requireNonNull(access, "access");
    }

    public PlayerKnowledge knowledge() {
        return access.get(PlayerAttachmentKeys.KNOWLEDGE, PlayerKnowledge.class);
    }

    public EmcValue emc() {
        return access.get(PlayerAttachmentKeys.EMC, EmcValue.class);
    }

    public void setEmc(EmcValue value) {
        Objects.requireNonNull(value, "value");
        access.modify(PlayerAttachmentKeys.EMC, EmcValue.class, current -> value);
    }

    public void addEmc(EmcValue delta) {
        Objects.requireNonNull(delta, "delta");
        access.modify(PlayerAttachmentKeys.EMC, EmcValue.class, current -> current.add(delta));
    }

    public void removeEmc(EmcValue delta) {
        Objects.requireNonNull(delta, "delta");
        access.modify(PlayerAttachmentKeys.EMC, EmcValue.class, current -> current.subtract(delta));
    }

    /**
     * @return true if the removal succeeded, false if the player lacked the EMC (balance unchanged).
     */
    public boolean tryRemoveEmc(EmcValue delta) {
        Objects.requireNonNull(delta, "delta");
        EmcValue current = emc();
        if (current.compareTo(delta) < 0) {
            return false;
        }
        setEmc(current.subtract(delta));
        return true;
    }

    public boolean learn(NormalizedStackKey key) {
        Objects.requireNonNull(key, "key");
        PlayerKnowledge before = knowledge();
        PlayerKnowledge after = before.learn(key);
        if (before == after) {
            return false;
        }
        access.modify(PlayerAttachmentKeys.KNOWLEDGE, PlayerKnowledge.class, current -> after);
        return true;
    }

    public boolean unlearn(NormalizedStackKey key) {
        Objects.requireNonNull(key, "key");
        PlayerKnowledge before = knowledge();
        PlayerKnowledge after = before.unlearn(key);
        if (before == after) {
            return false;
        }
        access.modify(PlayerAttachmentKeys.KNOWLEDGE, PlayerKnowledge.class, current -> after);
        return true;
    }

    public boolean hasKnowledge(NormalizedStackKey key) {
        return knowledge().has(key);
    }

    public void setFullKnowledge(boolean full) {
        access.modify(PlayerAttachmentKeys.KNOWLEDGE, PlayerKnowledge.class, current -> current.withFullKnowledge(full));
    }

    public PlayerInputLocks inputLocks() {
        return access.get(PlayerAttachmentKeys.INPUT_LOCKS, PlayerInputLocks.class);
    }

    public void setInputLock(int slot, NormalizedStackKey key) {
        access.modify(PlayerAttachmentKeys.INPUT_LOCKS, PlayerInputLocks.class, current -> current.set(slot, key));
    }

    public Optional<NormalizedStackKey> inputLock(int slot) {
        NormalizedStackKey key = inputLocks().get(slot);
        return Optional.ofNullable(key);
    }

    public boolean gemArmorEnabled() {
        return access.get(PlayerAttachmentKeys.GEM_ARMOR, Boolean.class);
    }

    public void setGemArmor(boolean enabled) {
        access.modify(PlayerAttachmentKeys.GEM_ARMOR, Boolean.class, current -> enabled);
    }
}
