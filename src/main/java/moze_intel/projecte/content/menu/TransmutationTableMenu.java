package moze_intel.projecte.content.menu;

import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.content.ModMenuTypes;
import moze_intel.projecte.content.menu.slots.TransmuteConsumeSlot;
import moze_intel.projecte.content.menu.slots.TransmuteInputSlot;
import moze_intel.projecte.content.menu.slots.TransmuteLockSlot;
import moze_intel.projecte.content.menu.slots.TransmuteOutputSlot;
import moze_intel.projecte.content.menu.slots.TransmuteUnlearnSlot;
import moze_intel.projecte.emc.EmcMappingSnapshot;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.emc.ProjectEEmc;
import moze_intel.projecte.emc.recipe.MinecraftStackKeyFactory;
import moze_intel.projecte.player.PlayerAttachmentKeys;
import moze_intel.projecte.player.PlayerDataService;
import moze_intel.projecte.transmutation.table.TransmutationOutputResolver;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Server-authoritative transmutation-table menu. Hosts the player inventory plus the
 * learning/lock/consume/unlearn slots and a 16-slot output ring. Output candidates are computed
 * from the EMC mapping and the player's knowledge via {@link TransmutationOutputResolver};
 * extractions spend EMC through the validated {@link PlayerDataService#tryRemoveEmc} transaction.
 *
 * <p>Slot layout (relative to the panel top-left, panel size 228×196):
 * <ul>
 *   <li>0–7: input slots (diamond around the center)</li>
 *   <li>8: lock slot (EMC filter cap)</li>
 *   <li>9: unlearn slot</li>
 *   <li>10: consume (burn-for-EMC) slot</li>
 *   <li>11–26: 16 output ring slots (read-only, resolver-populated)</li>
 *   <li>27–62: player main inventory + hotbar</li>
 * </ul>
 */
public final class TransmutationTableMenu extends AbstractContainerMenu {
    public static final int INPUT_SLOTS = 8;
    public static final int OUTPUT_SLOTS = TransmutationOutputResolver.OUTPUT_SLOT_COUNT;

    // Slot x/y coordinates relative to the panel top-left (matches the original ProjectE layout).
    private static final int[][] INPUT_SLOT_POS = {
          {43, 23}, {34, 41}, {52, 41}, {16, 50}, {70, 50}, {34, 59}, {52, 59}, {43, 77}
    };
    private static final int LOCK_X = 158;
    private static final int LOCK_Y = 50;
    private static final int UNLEARN_X = 89;
    private static final int UNLEARN_Y = 97;
    private static final int CONSUME_X = 107;
    private static final int CONSUME_Y = 97;
    // Outer ring of 12 (clockwise from top) + inner ring of 4.
    private static final int[][] OUTPUT_SLOT_POS = {
          {158, 9}, {176, 13}, {193, 30}, {199, 50}, {193, 70}, {176, 87},
          {158, 91}, {140, 87}, {123, 70}, {116, 50}, {123, 30}, {140, 13},
          {158, 31}, {177, 50}, {158, 69}, {139, 50}
    };
    private static final int PLAYER_INV_X = 35;
    private static final int PLAYER_INV_Y = 117;
    private static final int PLAYER_HOTBAR_Y = 175;

    private final Player player;
    private final PlayerDataService service;
    private final SimpleContainer inputLocksContainer;
    private final SimpleContainer unlearnContainer;
    private final SimpleContainer consumeContainer;
    private final SimpleContainer outputContainer;
    private final MinecraftStackKeyFactory keyFactory;

    public TransmutationTableMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.TRANSMUTATION_TABLE, containerId);
        this.player = playerInventory.player;
        this.service = new PlayerDataService(PlayerAttachmentKeys.fabricAdapter(player));
        this.keyFactory = new MinecraftStackKeyFactory(player.level().registryAccess());

        this.inputLocksContainer = new SimpleContainer(INPUT_SLOTS + 1); // 8 inputs + 1 lock
        this.unlearnContainer = new SimpleContainer(1);
        this.consumeContainer = new SimpleContainer(1);
        this.outputContainer = new SimpleContainer(OUTPUT_SLOTS) {
            @Override
            public int getMaxStackSize() {
                return 64;
            }
        };

        addTableSlots();
        addPlayerInventory(playerInventory);
        // Populate outputs from the resolver initially (server side; client gets them via slot sync).
        refreshOutputs();
    }

    public static TransmutationTableMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new TransmutationTableMenu(containerId, playerInventory);
    }

    private void addTableSlots() {
        for (int i = 0; i < INPUT_SLOTS; i++) {
            addSlot(new TransmuteInputSlot(inputLocksContainer, i,
                  INPUT_SLOT_POS[i][0], INPUT_SLOT_POS[i][1], player, service, keyFactory));
        }
        addSlot(new TransmuteLockSlot(inputLocksContainer, INPUT_SLOTS,
              LOCK_X, LOCK_Y, player, service, keyFactory));
        addSlot(new TransmuteUnlearnSlot(unlearnContainer, 0,
              UNLEARN_X, UNLEARN_Y, player, service, keyFactory));
        addSlot(new TransmuteConsumeSlot(consumeContainer, 0,
              CONSUME_X, CONSUME_Y, player, service, keyFactory));
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            addSlot(new TransmuteOutputSlot(outputContainer, i,
                  OUTPUT_SLOT_POS[i][0], OUTPUT_SLOT_POS[i][1], player, service, keyFactory));
        }
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9,
                      PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, PLAYER_INV_X + col * 18, PLAYER_HOTBAR_Y));
        }
    }

    /**
     * Recomputes the 16 output slots from the player's knowledge and EMC. Called server-side after
     * any state change (learn/unlearn/extract) and once per tick from {@link #broadcastChanges()}.
     * Output slots are virtual: they display the resolver candidates at count 1. This method must
     * not call {@link #broadcastChanges()} (the caller drives that) to avoid recursion.
     */
    public void refreshOutputs() {
        EmcMappingSnapshot<NormalizedStackKey> snapshot = ProjectEEmc.service().current();
        EmcValue available = service.emc();
        List<NormalizedStackKey> candidates = TransmutationOutputResolver.resolve(
              snapshot, service.knowledge(), available);
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            ItemStack display = ItemStack.EMPTY;
            if (i < candidates.size()) {
                display = displayStackFor(candidates.get(i));
            }
            // Only write if changed to avoid needless container churn.
            if (!ItemStack.matches(outputContainer.getItem(i), display)) {
                outputContainer.setItem(i, display);
            }
        }
    }

    private ItemStack displayStackFor(NormalizedStackKey key) {
        if (key instanceof moze_intel.projecte.emc.ItemStackKey itemKey) {
            var optionalItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(itemKey.identifier());
            if (optionalItem.isEmpty() || optionalItem.get().equals(Items.AIR)) {
                return ItemStack.EMPTY;
            }
            return new ItemStack(optionalItem.get());
        }
        // Tag/Fake keys have no concrete item to display.
        return ItemStack.EMPTY;
    }

    /**
     * @return the current EMC balance of the owning player (for client display).
     */
    public EmcValue playerEmc() {
        return service.emc();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        int firstOutput = firstOutputSlot();
        int firstPlayer = firstPlayerSlot();

        if (index >= firstOutput && index < firstPlayer) {
            // Output slot shift-click: route through the slot's EMC-charging remove() so the
            // extraction is paid for. Pull the full stack out, insert into the player inventory,
            // and return whatever was moved.
            ItemStack taken = slot.remove(slot.getItem().getCount());
            if (taken.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack leftover = taken.copy();
            for (int i = firstPlayer; i < slots.size() && !leftover.isEmpty(); i++) {
                leftover = slots.get(i).safeInsert(leftover);
            }
            return leftover.isEmpty() ? ItemStack.EMPTY : leftover;
        }

        ItemStack copy = slot.getItem().copy();
        if (index < INPUT_SLOTS + 3) {
            // Table slots -> player inventory.
            if (!moveItemStackTo(copy, firstPlayer, slots.size(), false)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Player inventory -> consume slot (burn for EMC).
            if (!moveItemStackTo(copy, INPUT_SLOTS + 2, INPUT_SLOTS + 3, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (copy.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.player.equals(player) && !player.isDeadOrDying();
    }

    @Override
    public void broadcastChanges() {
        // Refresh outputs before super so the container snapshot the client receives is current.
        if (!player.level().isClientSide()) {
            refreshOutputs();
        }
        super.broadcastChanges();
    }

    @SuppressWarnings("unused")
    private static ItemStack empty() {
        return new ItemStack(Items.AIR);
    }

    /** Index helpers for slot categories. */
    public static int firstOutputSlot() {
        return INPUT_SLOTS + 3;
    }

    public static int firstPlayerSlot() {
        return INPUT_SLOTS + 3 + OUTPUT_SLOTS;
    }
}
