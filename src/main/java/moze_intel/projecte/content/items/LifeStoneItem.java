package moze_intel.projecte.content.items;

import moze_intel.projecte.content.ModDataComponents;
import moze_intel.projecte.player.PlayerDataService;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/** Life Stone — grants passive beneficial effects using player EMC. */
public class LifeStoneItem extends ActiveEmcItem {
    public LifeStoneItem(Properties p) { super(p); }
    @Override public long getEmcPerTick() { return 4L; }
    @Override public boolean isActive(ItemStack s) { return s.getOrDefault(ModDataComponents.STORED_EMC, 0L) == 1L; }
    @Override public void setActive(ItemStack s, boolean a) { s.set(ModDataComponents.STORED_EMC, a ? 1L : 0L); }

    @Override
    public void onTick(Player player, ItemStack stack, PlayerDataService service) {
        if (!isActive(stack)) return;
        if (player.level().getGameTime() % 100 == 0) {
            super.onTick(player, stack, service);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0, false, false));
        }
    }

    @Override public InteractionResult use(Level l, Player p, net.minecraft.world.InteractionHand h) {
        ItemStack s = p.getItemInHand(h); boolean was = isActive(s); setActive(s, !was);
        if (!l.isClientSide()) p.sendSystemMessage(Component.translatable("item.projecte.life_stone." + (was ? "off" : "on")));
        return InteractionResult.SUCCESS;
    }
    @Override public void appendHoverText(ItemStack s, Item.TooltipContext c, TooltipDisplay d, Consumer<Component> t, TooltipFlag f) {
        super.appendHoverText(s, c, d, t, f);
        t.accept(Component.translatable("item.projecte.active_" + (isActive(s) ? "on" : "off")));
    }
}
