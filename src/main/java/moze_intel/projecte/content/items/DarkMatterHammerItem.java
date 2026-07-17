package moze_intel.projecte.content.items;

import moze_intel.projecte.content.items.tools.ToolHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Dark Matter Hammer — a chargeable tool that breaks a flat plane of blocks on right-click. The
 * plane size grows with charge: charge 0 = single block, charge n = a (2n+1)×(2n+1) flat plane
 * perpendicular to the hit face. The hammer is also a serviceable pickaxe via its tier (configured
 * at registration).
 */
public class DarkMatterHammerItem extends ChargeableItem {
    private static final int MAX_CHARGE = 2;

    public DarkMatterHammerItem(Properties properties) {
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
        return mineAt(level, player, stack,
              blockHit.getBlockPos(), blockHit.getDirection());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        return mineAt(context.getLevel(), player, context.getItemInHand(),
              context.getClickedPos(), context.getClickedFace());
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float speed = super.getDestroySpeed(stack, state);
        return speed <= 1.0F ? speed : speed + 12.0F * getCharge(stack);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos,
          net.minecraft.world.entity.LivingEntity miner) {
        return true;
    }

    private InteractionResult mineAt(Level level, Player player, ItemStack stack,
          BlockPos target, Direction face) {
        return ToolHelper.digAOE(
              level, player, stack, target, face, getCharge(stack), true);
    }
}
