package moze_intel.projecte.content.items.tools;

import moze_intel.projecte.content.ModDataComponents;
import moze_intel.projecte.content.items.IItemMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MatterPickaxeItem extends MatterToolItem implements IItemMode {
    public MatterPickaxeItem(Properties properties, int maxCharge, float chargeSpeedModifier) {
        super(properties, maxCharge, chargeSpeedModifier);
    }

    public PickaxeMode getMode(ItemStack stack) {
        return PickaxeMode.byId(stack.getOrDefault(ModDataComponents.TOOL_MODE, 0));
    }

    @Override
    public void cycleMode(Player player, ItemStack stack) {
        PickaxeMode next = getMode(stack).next();
        stack.set(ModDataComponents.TOOL_MODE, next.ordinal());
        player.sendOverlayMessage(Component.translatable(
              "mode.projecte.switch", Component.translatable(next.translationKey())));
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos,
          LivingEntity miner) {
        ToolHelper.digBasedOnMode(level, miner, stack, pos, getMode(stack));
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null) {
            return InteractionResult.PASS;
        }
        if (!context.getLevel().getBlockState(context.getClickedPos()).is(MatterToolTags.ORES)) {
            return InteractionResult.PASS;
        }
        return ToolHelper.veinMine(
              context.getLevel(), context.getPlayer(), context.getItemInHand(),
              context.getClickedPos(), getCharge(context.getItemInHand()));
    }

    public enum PickaxeMode {
        STANDARD,
        TALLSHOT,
        WIDESHOT,
        LONGSHOT;

        public PickaxeMode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public static PickaxeMode byId(int id) {
            return values()[Math.floorMod(id, values().length)];
        }

        public String translationKey() {
            return "mode.projecte.pick." + (ordinal() + 1);
        }
    }
}
