package moze_intel.projecte.content.blocks;

import moze_intel.projecte.content.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Dark Matter Pedestal — accepts dark matter items for special activation effects.
 * Contains a single slot for dark matter or klein star items.
 */
public class PedestalBlockEntity extends BaseContainerBlockEntity {
    private static final Component NAME = Component.translatable("container.projecte.dm_pedestal");
    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

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
        // Pedestal activation logic: consume EMC from klein star to activate nearby dark matter blocks
    }
}
