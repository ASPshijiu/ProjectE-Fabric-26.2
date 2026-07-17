package moze_intel.projecte.content.menu;

import moze_intel.projecte.content.ModMenuTypes;
import moze_intel.projecte.content.blocks.AlchemicalChestBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The 104-slot, 13-by-8 inventory exposed by an Alchemical Chest. */
public final class AlchemicalChestMenu extends AbstractContainerMenu {
    private static final int COLUMNS = 13;
    private static final int ROWS = 8;
    private static final int CHEST_X = 12;
    private static final int CHEST_Y = 5;
    private static final int PLAYER_X = 48;
    private static final int PLAYER_Y = 152;
    private static final int HOTBAR_Y = 210;

    private final Container chest;

    public AlchemicalChestMenu(int containerId, Inventory playerInventory, AlchemicalChestBlockEntity chest) {
        this(containerId, playerInventory, (Container) chest);
    }

    private AlchemicalChestMenu(int containerId, Inventory playerInventory, Container chest) {
        super(ModMenuTypes.ALCHEMICAL_CHEST, containerId);
        this.chest = chest;
        chest.startOpen(playerInventory.player);

        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                addSlot(new Slot(chest, column + row * COLUMNS,
                      CHEST_X + column * 18, CHEST_Y + row * 18));
            }
        }
        addPlayerInventory(playerInventory);
    }

    public static AlchemicalChestMenu fromNetwork(int containerId, Inventory playerInventory) {
        return new AlchemicalChestMenu(containerId, playerInventory,
              new SimpleContainer(AlchemicalChestBlockEntity.SLOTS));
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                      PLAYER_X + column * 18, PLAYER_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, PLAYER_X + column * 18, HOTBAR_Y));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        return MenuQuickMove.move(slot, stack -> index < AlchemicalChestBlockEntity.SLOTS
              ? moveItemStackTo(stack, AlchemicalChestBlockEntity.SLOTS, slots.size(), false)
              : moveItemStackTo(stack, 0, AlchemicalChestBlockEntity.SLOTS, false));
    }

    @Override
    public boolean stillValid(Player player) {
        return chest.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        chest.stopOpen(player);
    }
}
