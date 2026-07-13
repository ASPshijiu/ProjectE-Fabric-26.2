package moze_intel.projecte.client;

import moze_intel.projecte.emc.EmcMappingSnapshot;
import moze_intel.projecte.emc.ItemStackKey;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.emc.ProjectEEmc;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.network.chat.Component;

/**
 * Registers an {@link ItemTooltipCallback} that appends each item's EMC
 * value to its tooltip when the client has a resolved mapping snapshot.
 */
public final class EmcTooltipHandler {
    private EmcTooltipHandler() {}

    private static volatile boolean snapshotLogged = false;

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            EmcMappingSnapshot<NormalizedStackKey> snapshot = ProjectEEmc.service().current();
            if (snapshot.values().isEmpty() || stack.isEmpty()) {
                if (snapshot.values().isEmpty() && !snapshotLogged) {
                    snapshotLogged = true;
                    org.slf4j.LoggerFactory.getLogger("projecte/client")
                          .warn("EMC tooltip: client snapshot is empty ({} values) — EMC tooltips will not show. Check the 'Received EMC mapping sync payload' log line.", snapshot.values().size());
                }
                return;
            }

            // Build a simple key from the item's registry id (no component data)
            var itemKey = stack.typeHolder().unwrapKey();
            if (itemKey.isEmpty()) return;
            ItemStackKey key = new ItemStackKey(itemKey.get().identifier(), java.util.Map.of());

            snapshot.valueFor(key).ifPresent(emc -> {
                if (emc.longValue() > 0) {
                    lines.add(Component.translatable("item.projecte.emc_value",
                          String.format("%,d", emc.longValue())));
                }
            });
        });
    }
}
