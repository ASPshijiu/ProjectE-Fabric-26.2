package moze_intel.projecte.content.menu;

import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.content.ModMenuTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Container menu for the Alchemical Bag. Hosts a 27-slot bag inventory backed by the held stack's
 * {@link DataComponents#CONTAINER} component, plus the player's main inventory and hotbar. Every
 * bag-slot change is written back to the component so the bag's contents persist on the item.
 *
 * <p>The bag inventory is transient ({@link SimpleContainer}); it is loaded from the component when
 * the menu opens and flushed back through a {@link ContainerListener} on every change.
 */
public final class AlchemicalBagMenu extends AbstractContainerMenu {
    public static final int BAG_SLOTS = 27;
    private static final int BAG_COLUMNS = 9;
    private static final int BAG_X = 8;
    private static final int BAG_Y = 18;
    private static final int PLAYER_INV_X = 8;
    private static final int PLAYER_INV_Y = 58;
    private static final int PLAYER_HOTBAR_Y = 116;

    private final Player player;
    private final ItemStack bagStack;
    private final SimpleContainer bagInventory;

    public AlchemicalBagMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, findBagStack(playerInventory.player));
    }

    public AlchemicalBagMenu(int containerId, Inventory playerInventory, ItemStack bagStack) {
        super(ModMenuTypes.ALCHEMICAL_BAG, containerId);
        this.player = playerInventory.player;
        this.bagStack = bagStack;
        this.bagInventory = new SimpleContainer(BAG_SLOTS);
        // Load persisted contents from the component.
        ItemContainerContents contents = bagStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        net.minecraft.core.NonNullList<ItemStack> loaded = net.minecraft.core.NonNullList.withSize(BAG_SLOTS, ItemStack.EMPTY);
        contents.copyInto(loaded);
        for (int i = 0; i < BAG_SLOTS && i < loaded.size(); i++) {
            bagInventory.setItem(i, loaded.get(i));
        }
        // Write back on every change so the item carries the inventory.
        addSlotListener(new BagPersistenceListener());

        // Bag slots (3 rows x 9).
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < BAG_COLUMNS; col++) {
                addSlot(new Slot(bagInventory, col + row * BAG_COLUMNS,
                      BAG_X + col * 18, BAG_Y + row * 18));
            }
        }
        addPlayerInventory(playerInventory);
    }

    public static AlchemicalBagMenu fromNetwork(int containerId, Inventory playerInventory) {
        return new AlchemicalBagMenu(containerId, playerInventory);
    }

    /** Locate the held bag so the network constructor opens the right stack. */
    private static ItemStack findBagStack(Player player) {
        for (net.minecraft.world.InteractionHand hand : net.minecraft.world.InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof moze_intel.projecte.content.items.AlchemicalBagItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
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

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index < BAG_SLOTS) {
            // Bag -> player inventory.
            if (!moveItemStackTo(copy, BAG_SLOTS, slots.size(), false)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Player inventory -> bag.
            if (!moveItemStackTo(copy, 0, BAG_SLOTS, false)) {
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
        return this.player.equals(player) && !player.isDeadOrDying()
              && bagStack.getItem() instanceof moze_intel.projecte.content.items.AlchemicalBagItem;
    }

    /**
     * Flushes the bag inventory back to the held stack's CONTAINER component on every slot change.
     */
    private final class BagPersistenceListener implements ContainerListener {
        @Override
        public void slotChanged(AbstractContainerMenu menu, int slotIndex, ItemStack stack) {
            if (slotIndex < BAG_SLOTS) {
                persistBag();
            }
        }

        @Override
        public void dataChanged(AbstractContainerMenu menu, int dataIndex, int value) {
            // no data slots
        }
    }

    private void persistBag() {
        if (bagStack.isEmpty()) return;
        List<ItemStack> items = new ArrayList<>(BAG_SLOTS);
        for (int i = 0; i < BAG_SLOTS; i++) {
            items.add(bagInventory.getItem(i));
        }
        bagStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    @Override
    public void removed(Player player) {
        // Final flush in case a listener missed a change.
        persistBag();
        super.removed(player);
    }
}
