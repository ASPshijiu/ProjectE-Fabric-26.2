package moze_intel.projecte.content;

import moze_intel.projecte.api.ProjectEAPI;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModCreativeTab {
    private ModCreativeTab() {}

    public static final ResourceKey<CreativeModeTab> TAB_KEY =
          ResourceKey.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(), ProjectEAPI.id("tab"));

    public static void init() {
        CreativeModeTab tab = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
              .title(Component.translatable("itemGroup.projecte"))
              .icon(() -> new ItemStack(ModItems.PHILOSOPHERS_STONE))
              .build();

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, tab);

        CreativeModeTabEvents.modifyOutputEvent(TAB_KEY).register(output -> {
            BuiltInRegistries.ITEM.stream()
                  .filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(ProjectEAPI.MOD_ID))
                  .forEach(output::accept);
        });
    }
}
