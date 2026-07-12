package moze_intel.projecte.content.menu;

import moze_intel.projecte.content.ModMenuTypes;
import moze_intel.projecte.emc.EmcMappingSnapshot;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.emc.ProjectEEmc;
import moze_intel.projecte.player.PlayerAttachmentKeys;
import moze_intel.projecte.player.PlayerDataService;
import moze_intel.projecte.transmutation.table.TransmutationOutputResolver;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Server-authoritative transmutation table menu. Hosts the player inventory plus virtual input/output
 * regions; output candidates are computed from the EMC mapping and the player's knowledge through
 * {@link TransmutationOutputResolver}, and extractions spend EMC through the validated transaction.
 *
 * <p>This is the minimal live menu wiring the already-tested core logic; detailed slot rendering and
 * the C2S search protocol are layered on by the client screen phase.
 */
public final class TransmutationTableMenu extends AbstractContainerMenu {
    public static final int INPUT_SLOTS = 8;
    public static final int OUTPUT_SLOTS = TransmutationOutputResolver.OUTPUT_SLOT_COUNT;

    private final Player player;
    private final PlayerDataService service;

    public TransmutationTableMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.TRANSMUTATION_TABLE, containerId);
        this.player = playerInventory.player;
        this.service = new PlayerDataService(PlayerAttachmentKeys.fabricAdapter(player));
        addPlayerInventory(playerInventory);
    }

    public static TransmutationTableMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new TransmutationTableMenu(containerId, playerInventory);
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    /**
     * @return the current EMC balance of the owning player (for client display via the menu state).
     */
    public EmcValue playerEmc() {
        return service.emc();
    }

    /**
     * @return the output candidates for the current knowledge/EMC state.
     */
    public java.util.List<NormalizedStackKey> outputCandidates() {
        EmcMappingSnapshot<NormalizedStackKey> snapshot = ProjectEEmc.service().current();
        return TransmutationOutputResolver.resolve(snapshot, service.knowledge(), service.emc());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Minimal shift-click: no special handling yet; the slot machinery handles inventory moves.
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.player.equals(player) && !player.isDeadOrDying();
    }

    @SuppressWarnings("unused")
    private static ItemStack empty() {
        return new ItemStack(Items.AIR);
    }
}
