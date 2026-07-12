package moze_intel.projecte.content.items;

import moze_intel.projecte.transmutation.world.WorldTransmutationAction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/**
 * The Philosopher's Stone. Right-clicking a block performs the normal world transmutation;
 * shift-right-clicking performs the alternate result. The conversion is fully server-authoritative
 * through {@link WorldTransmutationAction}.
 */
public class PhilosophersStoneItem extends Item {
    public PhilosophersStoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        boolean applied = WorldTransmutationAction.apply(
              context.getLevel(), context.getClickedPos(), context.isSecondaryUseActive());
        return applied ? InteractionResult.CONSUME : InteractionResult.PASS;
    }
}
