package moze_intel.projecte.content.items;

import moze_intel.projecte.content.ModDataComponents;
import moze_intel.projecte.content.items.tools.RedMatterSwordItem.KatarMode;
import moze_intel.projecte.content.items.tools.ToolHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

/**
 * Red Matter Katar — a chargeable melee weapon with a key-triggered attack aura and configurable
 * hostile-only/all-target modes.
 */
public class RedMatterKatarItem extends ChargeableItem implements IItemMode, IExtraFunction {
    private static final int MAX_CHARGE = 4;

    public RedMatterKatarItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getMaxCharge(ItemStack stack) {
        return MAX_CHARGE;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        ToolHelper.attackWithCharge(stack, target, attacker);
    }

    public KatarMode getMode(ItemStack stack) {
        return KatarMode.byId(stack.getOrDefault(ModDataComponents.TOOL_MODE, 0));
    }

    @Override
    public void cycleMode(Player player, ItemStack stack) {
        var next = getMode(stack).next();
        stack.set(ModDataComponents.TOOL_MODE, next.ordinal());
        player.sendOverlayMessage(Component.translatable(
              "mode.projecte.switch",
              Component.translatable(next.translationKey())));
    }

    @Override
    public void doExtraFunction(Player player, ItemStack stack, InteractionHand hand) {
        if (player.getAttackStrengthScale(0.0F) < 1.0F) {
            return;
        }
        ToolHelper.attackAOE(
              stack, player, getMode(stack) == KatarMode.SLAY_ALL,
              1000.0F);
        player.resetAttackStrengthTicker();
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return ToolHelper.shearAOE(player, player.getItemInHand(hand));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        int charge = getCharge(context.getItemInHand());
        for (var behavior : java.util.List.of(
              Items.NETHERITE_AXE, Items.NETHERITE_HOE, Items.SHEARS)) {
            InteractionResult result = ToolHelper.useAOE(
                  context, behavior, charge, behavior != Items.NETHERITE_AXE);
            if (result.consumesAction()) {
                return result;
            }
        }
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (state.is(BlockTags.LOGS)) {
            return ToolHelper.clearConnected(
                  context.getLevel(), player, context.getItemInHand(), context.getClickedPos(),
                  BlockTags.LOGS, 5 * charge);
        }
        if (state.is(BlockTags.LEAVES)) {
            return ToolHelper.clearConnected(
                  context.getLevel(), player, context.getItemInHand(), context.getClickedPos(),
                  BlockTags.LEAVES, 5 * charge);
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult interactLivingEntity(
          ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if (!(entity instanceof Shearable shearable) || !shearable.readyForShearing()) {
            return InteractionResult.PASS;
        }
        if (entity.level() instanceof ServerLevel serverLevel) {
            shearable.shear(serverLevel, SoundSource.PLAYERS, stack);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float speed = super.getDestroySpeed(stack, state);
        return speed <= 1.0F ? speed : speed + 14.0F * getCharge(stack);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos,
          LivingEntity miner) {
        return true;
    }
}
