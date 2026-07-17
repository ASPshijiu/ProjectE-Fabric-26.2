package moze_intel.projecte.content.items;

import moze_intel.projecte.content.ModDataComponents;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.player.PlayerDataService;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/** Mind Stone — grants XP using player EMC. */
public class MindStoneItem extends ActiveEmcItem {
    public MindStoneItem(Properties p) { super(p); }
    @Override public long getEmcPerTick() { return 10L; }
    @Override public boolean isActive(ItemStack s) { return s.getOrDefault(ModDataComponents.STORED_EMC, 0L) == 1L; }
    @Override public void setActive(ItemStack s, boolean a) { s.set(ModDataComponents.STORED_EMC, a ? 1L : 0L); }

    @Override public void onTick(Player player, ItemStack stack, PlayerDataService service) {
        if (!isActive(stack)) return;
        if (player.level().getGameTime() % 20 == 0) {
            if (!service.tryRemoveEmc(EmcValue.of(getEmcPerTick()))) {
                setActive(stack, false);
                return;
            }
            player.giveExperiencePoints(5);
        }
    }

    @Override public InteractionResult use(Level l, Player p, net.minecraft.world.InteractionHand h) {
        ItemStack s = p.getItemInHand(h); boolean was = isActive(s); setActive(s, !was);
        if (!l.isClientSide()) p.sendSystemMessage(Component.translatable("item.projecte.mind_stone." + (was ? "off" : "on")));
        return InteractionResult.SUCCESS;
    }
    @Override public void appendHoverText(ItemStack s, Item.TooltipContext c, TooltipDisplay d, Consumer<Component> t, TooltipFlag f) {
        super.appendHoverText(s, c, d, t, f);
        t.accept(Component.translatable("item.projecte.active_" + (isActive(s) ? "on" : "off")));
    }
}
