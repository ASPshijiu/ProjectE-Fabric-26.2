package moze_intel.projecte.content.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import moze_intel.projecte.content.ModMenuTypes;
import moze_intel.projecte.content.items.AlchemicalBagSession;
import moze_intel.projecte.player.AlchemicalBagData;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Container menu for the Alchemical Bag. Hosts a 104-slot inventory backed by the player's
 * color-linked attachment, plus the player's main inventory and hotbar. Every bag-slot change is
 * written back to the shared player storage.
 *
 * <p>The bag inventory is transient ({@link SimpleContainer}); it is loaded from the player's
 * attachment when the menu opens and flushed back through a {@link ContainerListener}.
 */
public final class AlchemicalBagMenu extends AbstractContainerMenu {
    public static final int BAG_SLOTS = AlchemicalBagData.SLOTS;
    private static final int BAG_COLUMNS = 13;
    private static final int BAG_ROWS = 8;
    private static final int BAG_X = 12;
    private static final int BAG_Y = 5;
    private static final int PLAYER_INV_X = 48;
    private static final int PLAYER_INV_Y = 152;
    private static final int PLAYER_HOTBAR_Y = 210;

    private final Player player;
    private final ItemStack bagStack;
    private final SimpleContainer bagInventory;
    private final Consumer<ItemContainerContents> persistence;

    public AlchemicalBagMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, findBagStack(playerInventory.player),
              ItemContainerContents.EMPTY, ignored -> { });
    }

    public AlchemicalBagMenu(
          int containerId, Inventory playerInventory, ItemStack bagStack,
          AlchemicalBagSession session
    ) {
        this(containerId, playerInventory, bagStack, session.contents(), session::save);
    }

    private AlchemicalBagMenu(
          int containerId, Inventory playerInventory, ItemStack bagStack,
          ItemContainerContents initialContents,
          Consumer<ItemContainerContents> persistence
    ) {
        super(ModMenuTypes.ALCHEMICAL_BAG, containerId);
        this.player = playerInventory.player;
        this.bagStack = bagStack;
        this.persistence = persistence;
        this.bagInventory = new SimpleContainer(BAG_SLOTS);
        net.minecraft.core.NonNullList<ItemStack> loaded = net.minecraft.core.NonNullList.withSize(BAG_SLOTS, ItemStack.EMPTY);
        initialContents.copyInto(loaded);
        for (int i = 0; i < BAG_SLOTS && i < loaded.size(); i++) {
            bagInventory.setItem(i, loaded.get(i));
        }
        // Write back on every change so the player attachment remains authoritative.
        addSlotListener(new BagPersistenceListener());

        // Bag slots (8 rows x 13), matching the upstream ProjectE layout.
        for (int row = 0; row < BAG_ROWS; row++) {
            for (int col = 0; col < BAG_COLUMNS; col++) {
                addSlot(new Slot(bagInventory, col + row * BAG_COLUMNS,
                      BAG_X + col * 18, BAG_Y + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return !(stack.getItem() instanceof moze_intel.projecte.content.items.AlchemicalBagItem);
                    }
                });
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
                addPlayerSlot(inventory, col + row * 9 + 9,
                      PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            addPlayerSlot(inventory, col, PLAYER_INV_X + col * 18, PLAYER_HOTBAR_Y);
        }
    }

    private void addPlayerSlot(Inventory inventory, int index, int x, int y) {
        addSlot(new Slot(inventory, index, x, y) {
            @Override
            public boolean mayPickup(Player player) {
                return getItem() != bagStack;
            }
        });
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        return MenuQuickMove.move(slot, stack -> index < BAG_SLOTS
              // Bag -> player inventory.
              ? moveItemStackTo(stack, BAG_SLOTS, slots.size(), false)
              // Player inventory -> bag.
              : moveItemStackTo(stack, 0, BAG_SLOTS, false));
    }

    @Override
    public boolean stillValid(Player player) {
        return this.player.equals(player) && !player.isDeadOrDying()
              && bagStack.getItem() instanceof moze_intel.projecte.content.items.AlchemicalBagItem;
    }

    /** Flushes the menu inventory back to the player's color-linked bag attachment. */
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
        List<ItemStack> items = new ArrayList<>(BAG_SLOTS);
        for (int i = 0; i < BAG_SLOTS; i++) {
            items.add(bagInventory.getItem(i));
        }
        persistence.accept(ItemContainerContents.fromItems(items));
    }

    @Override
    public void removed(Player player) {
        // Final flush in case a listener missed a change.
        persistBag();
        super.removed(player);
    }
}
