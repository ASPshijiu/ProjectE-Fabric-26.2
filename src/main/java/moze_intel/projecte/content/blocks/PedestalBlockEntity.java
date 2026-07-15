package moze_intel.projecte.content.blocks;

import moze_intel.projecte.content.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Dark Matter Pedestal — stores one item and applies its pedestal effect while active.
 */
public class PedestalBlockEntity extends BaseContainerBlockEntity {
    private static final Component NAME = Component.translatable("container.projecte.dm_pedestal");
    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private boolean active;

    public PedestalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DM_PEDESTAL, pos, state);
    }

    @Override protected Component getDefaultName() { return NAME; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> list) { this.items = list; }
    @Override public int getContainerSize() { return 1; }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inv) { return null; }

    public static void tick(Level level, BlockPos pos, BlockState state, PedestalBlockEntity entity) {
        if (level.isClientSide()) return;
    }

    static boolean insertItem(Container pedestal, ItemStack held) {
        if (held.isEmpty() || !pedestal.isEmpty()) return false;
        pedestal.setItem(0, held.split(1));
        return true;
    }

    static ItemStack takeItem(Container pedestal) {
        return pedestal.removeItem(0, pedestal.getItem(0).getCount());
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        if (this.active != active) {
            this.active = active;
            setChanged();
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        active = input.getBooleanOr("active", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putBoolean("active", active);
    }
}
