package moze_intel.projecte.content.items;

import moze_intel.projecte.content.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Klein Star — a portable EMC battery with six tiers (Ein → Omega).
 * Each tier increases the maximum EMC capacity.
 *
 * <p>EMC is stored as a {@code stored_emc} data component (long).
 * The max capacity is determined by the star's tier.
 */
public class KleinStarItem extends Item {
    private final String tier;

    // Max EMC capacities for each tier (matches upstream ProjectE)
    public static final long MAX_EIN = 50_000L;
    public static final long MAX_ZWEI = 200_000L;
    public static final long MAX_DREI = 800_000L;
    public static final long MAX_VIER = 3_200_000L;
    public static final long MAX_SPHERE = 12_800_000L;
    public static final long MAX_OMEGA = 51_200_000L;

    public KleinStarItem(Properties properties, String tier) {
        super(properties);
        this.tier = tier;
    }

    public String getTier() {
        return tier;
    }

    /** @return the max EMC capacity for this star's tier. */
    public long getMaxEmc() {
        return maxEmcForTier(tier);
    }

    /** Read the stored EMC from an item stack of this star. */
    public static long getStoredEmc(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.STORED_EMC, 0L);
    }

    /** Write EMC to an item stack of this star (clamped to [0, max]). */
    public static void setStoredEmc(ItemStack stack, long emc) {
        long max = stack.getItem() instanceof KleinStarItem ks ? ks.getMaxEmc() : 0;
        stack.set(ModDataComponents.STORED_EMC, Math.max(0, Math.min(emc, max)));
    }

    /** Add EMC, returning any overflow that couldn't be stored. */
    public static long addEmc(ItemStack stack, long amount) {
        long current = getStoredEmc(stack);
        long max = stack.getItem() instanceof KleinStarItem ks ? ks.getMaxEmc() : 0;
        if (amount >= 0) {
            long accepted = Math.min(amount, Math.max(0, max - current));
            setStoredEmc(stack, current + accepted);
            return amount - accepted;
        }
        long newVal = current + amount;
        if (newVal > max) {
            setStoredEmc(stack, max);
            return newVal - max;
        }
        setStoredEmc(stack, newVal);
        return 0;
    }

    /** Remove EMC, returning the amount actually removed. */
    public static long removeEmc(ItemStack stack, long amount) {
        long current = getStoredEmc(stack);
        if (current <= 0 || amount <= 0) return 0;
        long toRemove = Math.min(current, amount);
        setStoredEmc(stack, current - toRemove);
        return toRemove;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                TooltipDisplay tooltipDisplay,
                                java.util.function.Consumer<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        long stored = getStoredEmc(stack);
        long max = getMaxEmc();
        tooltip.accept(Component.translatable("item.projecte.klein_star.emc",
              String.format("%,d", stored), String.format("%,d", max)));
    }

    // ── Tier capacity lookup ──────────────────────────────────────────────

    public static long maxEmcForTier(String tier) {
        return switch (tier) {
            case "ein" -> MAX_EIN;
            case "zwei" -> MAX_ZWEI;
            case "drei" -> MAX_DREI;
            case "vier" -> MAX_VIER;
            case "sphere" -> MAX_SPHERE;
            case "omega" -> MAX_OMEGA;
            default -> 0;
        };
    }
}
