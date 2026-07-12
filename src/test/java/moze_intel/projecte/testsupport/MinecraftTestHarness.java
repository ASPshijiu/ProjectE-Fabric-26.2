package moze_intel.projecte.testsupport;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;

/**
 * Shared Minecraft bootstrap helper for unit tests that touch item registries or build item stacks.
 *
 * <p>In Minecraft 26.2, {@link Bootstrap#bootStrap()} registers items but does not bind their
 * component maps, so constructing an {@code ItemStack} throws "Components not bound yet". This
 * harness boots the registries and binds empty component maps so item-stack based logic can be
 * exercised without a running server.
 */
public final class MinecraftTestHarness {
    private MinecraftTestHarness() {
    }

    /**
     * Boots the builtin registries and binds empty component maps to every registered item.
     * Safe to call from multiple test classes; the underlying bootstrap is idempotent enough for
     * unit-test use within a single JVM.
     */
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        BuiltInRegistries.ITEM.forEach(MinecraftTestHarness::bindEmptyComponents);
    }

    private static void bindEmptyComponents(Item item) {
        int id = BuiltInRegistries.ITEM.getId(item);
        BuiltInRegistries.ITEM.get(id).ifPresent(reference -> {
            if (!reference.areComponentsBound()) {
                reference.bindComponents(DataComponentMap.EMPTY);
            }
        });
    }

    @SuppressWarnings("unused")
    private static void unusedHolderReference() {
        // Keeps the Holder import meaningful for documentation of the bound reference type.
        Holder.Reference<Item> ignored = null;
    }
}
