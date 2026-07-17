package moze_intel.projecte.content.items.tools;

import moze_intel.projecte.content.ModDataComponents;
import moze_intel.projecte.content.items.IItemMode;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class RedMatterSwordItem extends MatterSwordItem implements IItemMode {
    public RedMatterSwordItem(Properties properties, int maxCharge, float chargeSpeedModifier) {
        super(properties, maxCharge, chargeSpeedModifier);
    }

    public KatarMode getMode(ItemStack stack) {
        return KatarMode.byId(stack.getOrDefault(ModDataComponents.TOOL_MODE, 0));
    }

    @Override
    protected boolean slayAll(ItemStack stack) {
        return getMode(stack) == KatarMode.SLAY_ALL;
    }

    @Override
    public void cycleMode(Player player, ItemStack stack) {
        KatarMode next = getMode(stack).next();
        stack.set(ModDataComponents.TOOL_MODE, next.ordinal());
        player.sendOverlayMessage(Component.translatable(
              "mode.projecte.switch", Component.translatable(next.translationKey())));
    }

    public enum KatarMode {
        SLAY_HOSTILE,
        SLAY_ALL;

        public KatarMode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public static KatarMode byId(int id) {
            return values()[Math.floorMod(id, values().length)];
        }

        public String translationKey() {
            return "mode.projecte.katar." + (ordinal() + 1);
        }
    }
}
