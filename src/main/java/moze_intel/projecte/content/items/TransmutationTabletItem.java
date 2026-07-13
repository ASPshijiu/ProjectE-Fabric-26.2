package moze_intel.projecte.content.items;

import moze_intel.projecte.content.menu.TransmutationTableMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Portable transmutation tablet. Right-clicking with the item opens the same transmutation menu as
 * the table block ( {@link TransmutationTableMenu}), letting the player learn, burn and extract
 * items on the go. Like the table, all state lives on the player, so the item itself carries no
 * saved data.
 */
public final class TransmutationTabletItem extends Item {
    public static final String TITLE_KEY = "container.projecte.transmutation_tablet";
    private static final Component TITLE = Component.translatable(TITLE_KEY);

    public TransmutationTabletItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return TITLE;
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                    return new TransmutationTableMenu(containerId, inventory);
                }
            });
        }
        return InteractionResult.SUCCESS;
    }
}
