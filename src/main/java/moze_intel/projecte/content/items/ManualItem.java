package moze_intel.projecte.content.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * ProjectE manual book item.
 * <p>
 * This port currently does not include a dedicated manual screen, so right-clicking this item
 * surfaces a status tooltip so the item is no longer a silent no-op.
 */
public class ManualItem extends Item {
    public ManualItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            player.sendSystemMessage(Component.translatable("item.projecte.manual.used"));
        }
        return InteractionResult.SUCCESS;
    }
}
