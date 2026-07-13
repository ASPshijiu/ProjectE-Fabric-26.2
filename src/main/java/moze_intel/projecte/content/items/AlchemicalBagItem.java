package moze_intel.projecte.content.items;

import moze_intel.projecte.content.menu.AlchemicalBagMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Alchemical Bag — a color-coded portable storage container. Each of the 16 dye colors has its own
 * item but the same 27-slot inventory, persisted on the stack via the vanilla {@code CONTAINER}
 * component. Right-clicking opens the {@link AlchemicalBagMenu}.
 */
public class AlchemicalBagItem extends Item {
    public static final String TITLE_KEY = "container.projecte.alchemical_bag";
    private static final Component TITLE = Component.translatable(TITLE_KEY);
    private final DyeColor color;

    public AlchemicalBagItem(Properties properties, DyeColor color) {
        super(properties);
        this.color = color;
    }

    public DyeColor getColor() {
        return color;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            ItemStack stack = player.getItemInHand(hand);
            serverPlayer.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return TITLE;
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p) {
                    return new AlchemicalBagMenu(containerId, inventory, stack);
                }
            });
        }
        return InteractionResult.SUCCESS;
    }
}
