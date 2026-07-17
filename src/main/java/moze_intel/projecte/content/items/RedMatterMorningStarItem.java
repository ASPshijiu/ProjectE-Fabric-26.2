package moze_intel.projecte.content.items;

import moze_intel.projecte.content.ModDataComponents;
import moze_intel.projecte.content.items.tools.MatterPickaxeItem.PickaxeMode;
import moze_intel.projecte.content.items.tools.ToolHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Red Matter Morning Star — combines charged block AoE with pickaxe mining modes and charged
 * single-target damage.
 */
public class RedMatterMorningStarItem extends ChargeableItem implements IItemMode {
    private static final int MAX_CHARGE = 4;

    public RedMatterMorningStarItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getMaxCharge(ItemStack stack) {
        return MAX_CHARGE;
    }

    public PickaxeMode getMode(ItemStack stack) {
        return PickaxeMode.byId(stack.getOrDefault(ModDataComponents.TOOL_MODE, 0));
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
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos,
          LivingEntity miner) {
        ToolHelper.digBasedOnMode(level, miner, stack, pos, getMode(stack));
        return true;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = player.pick(5.0, 0.0f, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }
        BlockPos target = blockHit.getBlockPos();
        Direction face = blockHit.getDirection();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        int radius = getCharge(stack);
        BlockState state = level.getBlockState(target);
        if (!state.isAir() && stack.isCorrectToolForDrops(state)) {
            level.destroyBlock(target, true, player);
        }
        ToolHelper.digAOE(level, player, stack, hand, target, face, radius, false);
        return InteractionResult.CONSUME;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        ToolHelper.attackWithCharge(stack, target, attacker);
    }
}
