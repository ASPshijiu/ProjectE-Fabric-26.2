package moze_intel.projecte.content.items;

import moze_intel.projecte.content.items.tools.ToolHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Red Matter Hammer — a chargeable tool that breaks a cube of blocks on right-click, larger than
 * the Dark Matter variant. Charge 0 = single block, charge n = a (2n+1)³ cube around the target.
 */
public class RedMatterHammerItem extends ChargeableItem {
    private static final int MAX_CHARGE = 4;

    public RedMatterHammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getMaxCharge(ItemStack stack) {
        return MAX_CHARGE;
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
        // Red Matter hammer mines a cube (not a flat plane).
        ToolHelper.digAOE(level, player, stack, hand, target, face, radius, false);
        if (stack.isDamageableItem()) {
            stack.hurtAndBreak(1, player, hand);
        }
        return InteractionResult.CONSUME;
    }
}
