package moze_intel.projecte.content.items;

import moze_intel.projecte.content.ModDataComponents;
import moze_intel.projecte.player.PlayerDataService;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.Consumer;

/** Ring of Ignition — sets nearby hostile entities on fire and grants the wearer fire immunity. */
public class IgnitionRingItem extends ActiveEmcItem {
    private static final int RADIUS = 5;
    private static final int IGNITE_INTERVAL = 20;
    private static final int IGNITE_SECONDS = 5;

    public IgnitionRingItem(Properties p) { super(p); }
    @Override public long getEmcPerTick() { return 2L; }
    @Override public boolean isActive(ItemStack s) { return s.getOrDefault(ModDataComponents.STORED_EMC, 0L) == 1L; }
    @Override public void setActive(ItemStack s, boolean a) { s.set(ModDataComponents.STORED_EMC, a ? 1L : 0L); }

    @Override
    public void onTick(Player player, ItemStack stack, PlayerDataService service) {
        if (!isActive(stack)) return;
        super.onTick(player, stack, service);
        // 付不起 EMC 时 super 已关闭戒指，效果同刻停止。
        if (!isActive(stack)) return;
        Level level = player.level();
        if (level.isClientSide()) return;

        // 佩戴者持续免疫火焰（无粒子，时长略长于刷新间隔以免闪断）。
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, true, false));
        player.clearFire();

        if (level.getGameTime() % IGNITE_INTERVAL != 0) return;
        AABB box = player.getBoundingBox().inflate(RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(
              LivingEntity.class, box, target -> target != player && !(target instanceof Player));
        for (LivingEntity target : targets) {
            target.igniteForSeconds(IGNITE_SECONDS);
        }
    }

    @Override public InteractionResult use(Level l, Player p, net.minecraft.world.InteractionHand h) {
        ItemStack s = p.getItemInHand(h); boolean was = isActive(s); setActive(s, !was);
        if (!l.isClientSide()) p.sendSystemMessage(Component.translatable("item.projecte.ignition_ring." + (was ? "off" : "on")));
        return InteractionResult.SUCCESS;
    }
    @Override public void appendHoverText(ItemStack s, Item.TooltipContext c, TooltipDisplay d, Consumer<Component> t, TooltipFlag f) {
        super.appendHoverText(s, c, d, t, f);
        t.accept(Component.translatable("item.projecte.active_" + (isActive(s) ? "on" : "off")));
    }
}
