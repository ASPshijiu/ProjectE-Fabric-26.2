package moze_intel.projecte.content.menu;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import moze_intel.projecte.content.ModBlocks;
import moze_intel.projecte.content.ModMenuTypes;
import moze_intel.projecte.content.menu.slots.TransmuteConsumeSlot;
import moze_intel.projecte.content.menu.slots.TransmuteInputSlot;
import moze_intel.projecte.content.menu.slots.TransmuteLockSlot;
import moze_intel.projecte.content.menu.slots.TransmuteOutputSlot;
import moze_intel.projecte.content.menu.slots.TransmuteUnlearnSlot;
import moze_intel.projecte.emc.EmcMappingSnapshot;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.ItemStackKey;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.emc.ProjectEEmc;
import moze_intel.projecte.emc.recipe.MinecraftStackKeyFactory;
import moze_intel.projecte.player.PlayerAttachmentKeys;
import moze_intel.projecte.player.PlayerDataService;
import moze_intel.projecte.player.PlayerKnowledge;
import moze_intel.projecte.transmutation.table.TransmutationOutputResolver;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

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
    public static final int PREVIOUS_PAGE_BUTTON = 0;
    public static final int NEXT_PAGE_BUTTON = 1;

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
    private final ContainerLevelAccess levelAccess;
    private final PlayerDataService service;
    private final SimpleContainer inputLocksContainer;
    private final SimpleContainer unlearnContainer;
    private final SimpleContainer consumeContainer;
    private final SimpleContainer outputContainer;
    private final MinecraftStackKeyFactory keyFactory;
    private final SyncedLong syncedEmc;
    private final DataSlot currentPage;
    private final DataSlot pageCount;
    private boolean loadingInputLocks;
    private long renderedMappingVersion = -1;
    private PlayerKnowledge renderedKnowledge;
    private EmcValue renderedAvailable;
    private Optional<EmcValue> renderedLockLimit;
    private int renderedPage = -1;

    public TransmutationTableMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public TransmutationTableMenu(
          int containerId, Inventory playerInventory, ContainerLevelAccess levelAccess
    ) {
        super(ModMenuTypes.TRANSMUTATION_TABLE, containerId);
        this.player = playerInventory.player;
        this.levelAccess = Objects.requireNonNull(levelAccess, "levelAccess");
        this.service = new PlayerDataService(PlayerAttachmentKeys.fabricAdapter(player));
        this.keyFactory = new MinecraftStackKeyFactory(player.level().registryAccess());
        this.syncedEmc = new SyncedLong(() -> service.emc().longValue());
        syncedEmc.slots().forEach(this::addDataSlot);
        this.currentPage = DataSlot.standalone();
        this.pageCount = DataSlot.standalone();
        this.pageCount.set(1);
        addDataSlot(currentPage);
        addDataSlot(pageCount);

        this.inputLocksContainer = new SimpleContainer(INPUT_SLOTS + 1) {
            @Override
            public void setChanged() {
                super.setChanged();
                if (!loadingInputLocks && !player.level().isClientSide()) {
                    persistInputLocks();
                }
            }
        }; // 8 inputs + 1 lock
        this.unlearnContainer = new SimpleContainer(1);
        this.consumeContainer = new SimpleContainer(1);
        this.outputContainer = new SimpleContainer(OUTPUT_SLOTS) {
            @Override
            public int getMaxStackSize() {
                return 64;
              }
        };

        loadInputLocks();
        addTableSlots();
        addPlayerInventory(playerInventory);
        // Populate outputs server-side; the client receives page metadata and slots through sync.
        if (!player.level().isClientSide()) {
            refreshOutputs();
        }
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
        PlayerKnowledge knowledge = service.knowledge();
        Optional<EmcValue> lockLimit = keyFactory.optionalKey(inputLocksContainer.getItem(INPUT_SLOTS))
              .flatMap(snapshot::valueFor);
        int requestedPage = currentPage.get();
        if (snapshot.version() == renderedMappingVersion
              && knowledge.equals(renderedKnowledge)
              && available.equals(renderedAvailable)
              && lockLimit.equals(renderedLockLimit)
              && requestedPage == renderedPage) {
            return;
        }
        TransmutationOutputResolver.Page page = TransmutationOutputResolver.resolvePage(
              snapshot, knowledge, available, lockLimit, requestedPage);
        currentPage.set(page.pageIndex());
        pageCount.set(page.pageCount());
        List<NormalizedStackKey> candidates = page.outputs();
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
        renderedMappingVersion = snapshot.version();
        renderedKnowledge = knowledge;
        renderedAvailable = available;
        renderedLockLimit = lockLimit;
        renderedPage = page.pageIndex();
    }

    private ItemStack displayStackFor(NormalizedStackKey key) {
        if (key instanceof ItemStackKey itemKey) {
            return keyFactory.stack(itemKey);
        }
        // Tag/Fake keys have no concrete item to display.
        return ItemStack.EMPTY;
    }

    /**
     * @return the current EMC balance of the owning player (for client display).
     */
    public EmcValue playerEmc() {
        return player.level().isClientSide() ? EmcValue.of(syncedEmc.value()) : service.emc();
    }

    public int currentPage() {
        return currentPage.get();
    }

    public int pageCount() {
        return Math.max(1, pageCount.get());
    }

    public boolean hasPreviousPage() {
        return currentPage() > 0;
    }

    public boolean hasNextPage() {
        return currentPage() + 1 < pageCount();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!this.player.equals(player)) {
            return false;
        }
        if (id == PREVIOUS_PAGE_BUTTON && hasPreviousPage()) {
            currentPage.set(currentPage() - 1);
        } else if (id == NEXT_PAGE_BUTTON && hasNextPage()) {
            currentPage.set(currentPage() + 1);
        } else {
            return false;
        }
        if (!player.level().isClientSide()) {
            refreshOutputs();
            broadcastChanges();
        }
        return true;
    }

    private void loadInputLocks() {
        loadingInputLocks = true;
        try {
            for (int slot = 0; slot < INPUT_SLOTS + 1; slot++) {
                ItemStack stack = service.inputLock(slot)
                      .map(this::displayStackFor)
                      .orElse(ItemStack.EMPTY);
                inputLocksContainer.setItem(slot, stack);
            }
        } finally {
            loadingInputLocks = false;
        }
    }

    private void persistInputLocks() {
        var persisted = service.inputLocks();
        for (int slot = 0; slot < INPUT_SLOTS + 1; slot++) {
            ItemStack stack = inputLocksContainer.getItem(slot);
            NormalizedStackKey key = stack.isEmpty() ? null : keyFactory.key(stack);
            if (!Objects.equals(persisted.get(slot), key)) {
                service.setInputLock(slot, key);
            }
        }
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
            // Work out the real destination capacity before the output slot charges EMC.
            int room = MenuQuickMove.roomForOneStack(
                  slot.getItem(), slots.subList(firstPlayer, slots.size()));
            if (room <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack taken = slot.remove(room);
            if (taken.isEmpty()) {
                return ItemStack.EMPTY;
            }
            moveItemStackTo(taken, firstPlayer, slots.size(), false);
            if (!taken.isEmpty()) {
                refundOutputEmc(taken);
            }
            // The output slot is virtual and remains populated, so stop the vanilla repeat loop.
            return ItemStack.EMPTY;
        }

        return MenuQuickMove.move(slot, stack -> index < INPUT_SLOTS + 3
              // Table slots -> player inventory.
              ? moveItemStackTo(stack, firstPlayer, slots.size(), false)
              // Player inventory -> consume slot (burn for EMC).
              : moveItemStackTo(stack, INPUT_SLOTS + 2, INPUT_SLOTS + 3, false));
    }

    @Override
    public boolean stillValid(Player player) {
        return this.player.equals(player) && !player.isDeadOrDying()
              && stillValid(levelAccess, player, ModBlocks.TRANSMUTATION_TABLE);
    }

    @Override
    public void removed(Player player) {
        ItemStack unlearn = unlearnContainer.removeItemNoUpdate(0);
        super.removed(player);
        if (unlearn.isEmpty()) {
            return;
        }
        if (player.isDeadOrDying()
              || player instanceof ServerPlayer serverPlayer && serverPlayer.hasDisconnected()) {
            player.drop(unlearn, false);
        } else {
            player.getInventory().placeItemBackInInventory(unlearn);
        }
    }

    private void refundOutputEmc(ItemStack stack) {
        keyFactory.optionalKey(stack)
              .flatMap(ProjectEEmc.service().current()::valueFor)
              .ifPresent(value -> service.addEmc(value.multiply(stack.getCount())));
    }

    @Override
    public void broadcastChanges() {
        // Refresh outputs before super so the container snapshot the client receives is current.
        if (!player.level().isClientSide()) {
            refreshOutputs();
        }
        super.broadcastChanges();
    }

    /** Index helpers for slot categories. */
    public static int firstOutputSlot() {
        return INPUT_SLOTS + 3;
    }

    public static int firstPlayerSlot() {
        return INPUT_SLOTS + 3 + OUTPUT_SLOTS;
    }
}
