package moze_intel.projecte.content.items;

import java.util.function.Consumer;
import moze_intel.projecte.player.PlayerDataService;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Tome of Knowledge — permanently grants full transmutation knowledge when sold to a
 * transmutation table. EMC-based item creation is handled by the table.
 */
public class TomeOfKnowledgeItem extends Item {
    public TomeOfKnowledgeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
          Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.projecte.tome"));
    }

    /**
     * Grants the owning player full transmutation knowledge.
     */
    public static void learnAll(PlayerDataService service) {
        if (!service.knowledge().fullKnowledge()) {
            service.setFullKnowledge(true);
        }
    }

    /**
     * Check whether an ItemStack is a Tome of Knowledge.
     */
    public static boolean isTome(ItemStack stack) {
        return stack.getItem() instanceof TomeOfKnowledgeItem;
    }
}
