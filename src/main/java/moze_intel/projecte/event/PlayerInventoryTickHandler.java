package moze_intel.projecte.event;

import moze_intel.projecte.content.items.ActiveEmcItem;
import moze_intel.projecte.content.items.RepairTalismanItem;
import moze_intel.projecte.content.items.SwiftwolfRendingGaleItem;
import moze_intel.projecte.content.items.armor.GemArmorItem;
import moze_intel.projecte.player.PlayerAttachmentKeys;
import moze_intel.projecte.player.PlayerDataService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Central server-side tick handler for ProjectE passive item effects.
 *
 * <p>Registers a per-tick callback that iterates every online player's
 * inventory and dispatches to:
 * <ul>
 *   <li>{@link RepairTalismanItem} – item repair</li>
 *   <li>{@link ActiveEmcItem} subclasses – active ring/amulet effects</li>
 * </ul>
 */
public final class PlayerInventoryTickHandler {
    private static int tickCounter = 0;

    private PlayerInventoryTickHandler() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            // Run heavy checks once per second
            boolean doSecondTick = (tickCounter % 20 == 0);

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.isSpectator() || !player.isAlive()) continue;

                var access = PlayerAttachmentKeys.fabricAdapter(player);
                var service = new PlayerDataService(access);

                boolean hasTalisman = false;
                boolean hasActiveSwiftwolf = false;

                var inventory = player.getInventory();
                // Check all slots: main, armor, offhand
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    ItemStack stack = inventory.getItem(i);
                    if (stack.isEmpty()) continue;

                    // Repair Talisman: repair items once per second
                    if (RepairTalismanItem.isTalisman(stack)) {
                        hasTalisman = true;
                    }

                    // Active items (rings, amulets, stones): onTick every tick
                    if (stack.getItem() instanceof ActiveEmcItem activeItem) {
                        activeItem.onTick(player, stack, service);
                        if (activeItem instanceof SwiftwolfRendingGaleItem
                              && activeItem.isActive(stack)) {
                            hasActiveSwiftwolf = true;
                        }
                    }
                }

                SwiftwolfRendingGaleItem.updateFlight(player, service, hasActiveSwiftwolf);

                if (doSecondTick) {
                    // Repair once per second if talisman present
                    if (hasTalisman) {
                        ItemStack activeMainHand = player.swinging
                              ? player.getMainHandItem()
                              : ItemStack.EMPTY;
                        RepairTalismanItem.tickRepair(inventory, true, activeMainHand);
                    }
                }

                GemArmorItem.tickPlayer(player, doSecondTick);
            }
        });
    }
}
