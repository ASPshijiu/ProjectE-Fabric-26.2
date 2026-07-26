package moze_intel.projecte.content.items;

import moze_intel.projecte.content.ModDataComponents;
import moze_intel.projecte.player.PlayerDataService;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.Consumer;

/** Zero Ring — freezes nearby water, extinguishes fire and chills hostile entities. */
public class ZeroRingItem extends ActiveEmcItem {
    private static final int RADIUS = 5;
    private static final int EFFECT_INTERVAL = 20;
    private static final int SLOWNESS_DURATION = 60;

    public ZeroRingItem(Properties p) { super(p); }
    @Override public long getEmcPerTick() { return 2L; }
    @Override public boolean isActive(ItemStack s) { return s.getOrDefault(ModDataComponents.STORED_EMC, 0L) == 1L; }
    @Override public void setActive(ItemStack s, boolean a) { s.set(ModDataComponents.STORED_EMC, a ? 1L : 0L); }

    @Override
    public void onTick(Player player, ItemStack stack, PlayerDataService service) {
        if (!isActive(stack)) return;
        super.onTick(player, stack, service);
        if (!isActive(stack)) return;
        Level level = player.level();
        if (level.isClientSide()) return;

        // 佩戴者自身灭火（上游同款"零度"语义）。
        player.clearFire();
        if (level.getGameTime() % EFFECT_INTERVAL != 0) return;

        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-RADIUS, -RADIUS, -RADIUS),
              center.offset(RADIUS, RADIUS, RADIUS))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
                level.setBlockAndUpdate(pos.immutable(), Blocks.AIR.defaultBlockState());
            } else if (state.getFluidState().is(Fluids.WATER)
                  && state.getFluidState().isSource()
                  && state.getCollisionShape(level, pos).isEmpty()) {
                level.setBlockAndUpdate(pos.immutable(), Blocks.ICE.defaultBlockState());
            }
        }

        AABB box = player.getBoundingBox().inflate(RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(
              LivingEntity.class, box, target -> target != player && !(target instanceof Player));
        for (LivingEntity target : targets) {
            target.clearFire();
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SLOWNESS_DURATION, 1, true, false));
        }
    }

    @Override public InteractionResult use(Level l, Player p, net.minecraft.world.InteractionHand h) {
        ItemStack s = p.getItemInHand(h); boolean was = isActive(s); setActive(s, !was);
        if (!l.isClientSide()) p.sendSystemMessage(Component.translatable("item.projecte.zero_ring." + (was ? "off" : "on")));
        return InteractionResult.SUCCESS;
    }
    @Override public void appendHoverText(ItemStack s, Item.TooltipContext c, TooltipDisplay d, Consumer<Component> t, TooltipFlag f) {
        super.appendHoverText(s, c, d, t, f);
        t.accept(Component.translatable("item.projecte.active_" + (isActive(s) ? "on" : "off")));
    }
}
