package moze_intel.projecte.client.mixin;

import java.util.function.Consumer;
import moze_intel.projecte.emc.ProjectEEmc;
import moze_intel.projecte.emc.StackEmcResolver;
import moze_intel.projecte.emc.recipe.MinecraftStackKeyFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects into {@link Item#appendHoverText} to append the EMC value to every item's tooltip. This
 * replaces the Fabric {@code ItemTooltipCallback} event, which is dead in Fabric API 14.2.0
 * (MC 26.2) — the client {@code ItemStackMixin} that fired it is empty, so the event never fires.
 *
 * <p>The injection runs at the tail of {@code appendHoverText} (after vanilla + other mods have
 * added their lines), reads the item's EMC from the client's synced snapshot, and appends a
 * translatable "EMC: N" line when the value is positive.
 */
@Mixin(Item.class)
public abstract class ItemMixin {

    private static volatile boolean logged = false;

    @Inject(method = "appendHoverText", at = @At("RETURN"))
    private void projecte$appendEmcTooltip(ItemStack stack, Item.TooltipContext context,
          TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag flag,
          CallbackInfo ci) {
        if (!logged) {
            logged = true;
            org.slf4j.LoggerFactory.getLogger("projecte/client")
                  .info("ItemMixin.appendHoverText injection fired (tooltip mixin is active)");
        }
        if (stack.isEmpty()) return;
        var snapshot = ProjectEEmc.service().current();
        if (snapshot.values().isEmpty()) return;
        if (stack.typeHolder().unwrapKey().isEmpty()) return;
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var key = new MinecraftStackKeyFactory(level.registryAccess()).key(stack);
        StackEmcResolver.resolve(stack, key, snapshot).ifPresent(resolved -> {
            var emc = resolved.value();
            if (emc.longValue() > 0) {
                tooltipAdder.accept(Component.translatable("item.projecte.emc_value",
                      String.format("%,d", emc.longValue())));
            }
        });
    }
}
