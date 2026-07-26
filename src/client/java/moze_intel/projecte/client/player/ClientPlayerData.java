package moze_intel.projecte.client.player;

import java.util.Optional;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.player.PlayerAttachments;
import moze_intel.projecte.player.PlayerInputLocks;
import moze_intel.projecte.player.PlayerKnowledge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Read-only access to the locally synced player ProjectE data.
 *
 * <p>Every value here originates from the server's authoritative attachment sync; clients must never
 * mutate through this facade. Returns empty/zero defaults when no local player exists (e.g. during
 * early login or on the title screen) so callers can render safely.
 */
public final class ClientPlayerData {
    private ClientPlayerData() {
    }

    private static LocalPlayer player() {
        return Minecraft.getInstance().player;
    }

    public static EmcValue emc() {
        LocalPlayer player = player();
        // getAttached 在同步包到达前返回 null；用 OrElse 保证空默认值契约。
        return player == null ? EmcValue.ZERO
              : player.getAttachedOrElse(PlayerAttachments.EMC, EmcValue.ZERO);
    }

    public static PlayerKnowledge knowledge() {
        LocalPlayer player = player();
        return player == null ? PlayerKnowledge.empty()
              : player.getAttachedOrElse(PlayerAttachments.KNOWLEDGE, PlayerKnowledge.empty());
    }

    public static boolean hasKnowledge(NormalizedStackKey key) {
        return knowledge().has(key);
    }

    public static PlayerInputLocks inputLocks() {
        LocalPlayer player = player();
        return player == null ? PlayerInputLocks.empty()
              : player.getAttachedOrElse(PlayerAttachments.INPUT_LOCKS, PlayerInputLocks.empty());
    }

    public static Optional<NormalizedStackKey> inputLock(int slot) {
        NormalizedStackKey key = inputLocks().get(slot);
        return Optional.ofNullable(key);
    }
}
