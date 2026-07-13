package moze_intel.projecte.content.blocks;

import moze_intel.projecte.content.menu.TransmutationTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the transmutation table. It is a thin menu provider; the table's persistent
 * state (knowledge, EMC, input locks) lives on the player, not the block, so this entity carries no
 * extra saved data.
 */
public final class TransmutationTableBlockEntity extends BlockEntity {
    public static final String TITLE_KEY = "container.projecte.transmutation_table";
    public static final Component TITLE = Component.translatable(TITLE_KEY);

    public TransmutationTableBlockEntity(BlockPos pos, BlockState state) {
        super(moze_intel.projecte.content.ModBlocks.TRANSMUTATION_TABLE_ENTITY, pos, state);
    }

    public MenuProvider menuProvider() {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return TITLE;
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                return new TransmutationTableMenu(containerId, playerInventory);
            }
        };
    }

    /**
     * Used by the menu to read the player's current EMC and knowledge. Currently delegated to the
     * menu's own service; kept for future per-block extensions.
     */
    @SuppressWarnings("unused")
    private Level levelReference() {
        return getLevel();
    }
}
