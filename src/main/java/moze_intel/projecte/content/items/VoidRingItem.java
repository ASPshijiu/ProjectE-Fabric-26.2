package moze_intel.projecte.content.items;

import moze_intel.projecte.content.ModDataComponents;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.player.PlayerDataService;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.Consumer;

/**
 * Void Ring — combines the Black Hole Band's item attraction with the Repair Talisman's
 * passive repair while active.
 *
 * <p>上游的"按过滤器销毁拾取物"依赖尚未移植的过滤器界面，这里不做任何物品销毁，
 * 以免在没有过滤配置的情况下吞掉玩家物品；该缺口已在移植矩阵中如实标注。
 */
public class VoidRingItem extends ActiveEmcItem {
    private static final int RADIUS = 7;
    private static final long EMC_PER_ACTION = 1L;
    private static final int REPAIR_INTERVAL = 20;

    public VoidRingItem(Properties p) { super(p); }
    @Override public long getEmcPerTick() { return 0L; }
    @Override public boolean isActive(ItemStack s) { return s.getOrDefault(ModDataComponents.STORED_EMC, 0L) == 1L; }
    @Override public void setActive(ItemStack s, boolean a) { s.set(ModDataComponents.STORED_EMC, a ? 1L : 0L); }

    @Override
    public void onTick(Player player, ItemStack stack, PlayerDataService service) {
        if (!isActive(stack)) return;
        Level level = player.level();
        if (level.isClientSide()) return;

        // 与黑洞band 一致：只有真正做功的刻才扣费，扣不起立即关闭。
        boolean paid = false;
        AABB box = player.getBoundingBox().inflate(RADIUS);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
            if (!item.isAlive()) continue;
            double dx = player.getX() - item.getX();
            double dy = player.getY() + player.getEyeHeight() / 2.0 - item.getY();
            double dz = player.getZ() - item.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist < 1.5) continue;
            if (!paid) {
                if (!service.tryRemoveEmc(EmcValue.of(EMC_PER_ACTION))) {
                    setActive(stack, false);
                    return;
                }
                paid = true;
            }
            double speed = 0.15;
            item.setDeltaMovement(dx / dist * speed, dy / dist * speed + 0.05, dz / dist * speed);
        }

        if (level.getGameTime() % REPAIR_INTERVAL != 0) return;
        if (!paid && !service.tryRemoveEmc(EmcValue.of(EMC_PER_ACTION))) {
            setActive(stack, false);
            return;
        }
        RepairTalismanItem.tickRepair(player.getInventory(), true,
              player.swinging ? player.getMainHandItem() : ItemStack.EMPTY);
    }

    @Override public InteractionResult use(Level l, Player p, net.minecraft.world.InteractionHand h) {
        ItemStack s = p.getItemInHand(h); boolean was = isActive(s); setActive(s, !was);
        if (!l.isClientSide()) p.sendSystemMessage(Component.translatable("item.projecte.void_ring." + (was ? "off" : "on")));
        return InteractionResult.SUCCESS;
    }
    @Override public void appendHoverText(ItemStack s, Item.TooltipContext c, TooltipDisplay d, Consumer<Component> t, TooltipFlag f) {
        super.appendHoverText(s, c, d, t, f);
        t.accept(Component.translatable("item.projecte.active_" + (isActive(s) ? "on" : "off")));
    }
}
