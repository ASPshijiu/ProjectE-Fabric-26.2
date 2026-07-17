package moze_intel.projecte.content.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MatterShearsItem extends ShearsItem implements MatterTool {
    private final int maxCharge;
    private final float chargeSpeedModifier;

    public MatterShearsItem(Properties properties, int maxCharge, float chargeSpeedModifier) {
        super(properties);
        this.maxCharge = maxCharge;
        this.chargeSpeedModifier = chargeSpeedModifier;
    }

    @Override public int getMaxCharge(ItemStack stack) { return maxCharge; }
    @Override public float getChargeSpeedModifier() { return chargeSpeedModifier; }
    @Override public float getDestroySpeed(ItemStack stack, BlockState state) {
        return chargedDestroySpeed(super.getDestroySpeed(stack, state), stack);
    }
    @Override public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos,
          LivingEntity miner) { return true; }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return ToolHelper.shearAOE(player, player.getItemInHand(hand));
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null) return InteractionResult.PASS;
        if (context.getLevel().getBlockState(context.getClickedPos()).is(BlockTags.LEAVES)) {
            return ToolHelper.clearConnected(
                  context.getLevel(), context.getPlayer(), context.getItemInHand(),
                  context.getClickedPos(), BlockTags.LEAVES,
                  5 * getCharge(context.getItemInHand()));
        }
        return super.useOn(context);
    }
    @Override public InteractionResult interactLivingEntity(
          ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if (!(entity instanceof Shearable shearable) || !shearable.readyForShearing()) {
            return InteractionResult.PASS;
        }
        if (entity.level() instanceof ServerLevel serverLevel) {
            shearable.shear(serverLevel, SoundSource.PLAYERS, stack);
        }
        return InteractionResult.SUCCESS;
    }
    @Override public boolean isBarVisible(ItemStack stack) { return chargeBarVisible(stack); }
    @Override public int getBarWidth(ItemStack stack) { return chargeBarWidth(stack); }
    @Override public int getBarColor(ItemStack stack) { return chargeBarColor(stack); }
}
