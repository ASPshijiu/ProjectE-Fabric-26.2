package moze_intel.projecte.client.screen;

import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.content.menu.CondenserMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Client screen shared by Energy Condenser MK1 and MK2. */
public final class CondenserScreen extends AbstractContainerScreen<CondenserMenu> {
    private static final int PANEL_WIDTH = 255;
    private static final int PANEL_HEIGHT = 233;
    private static final Identifier MK1_TEXTURE = ProjectEAPI.id(
          "textures/gui/condenser.png");
    private static final Identifier MK2_TEXTURE = ProjectEAPI.id(
          "textures/gui/condenser_mk2.png");

    public CondenserScreen(
          CondenserMenu menu,
          Inventory playerInventory,
          Component title
    ) {
        super(menu, playerInventory, title, PANEL_WIDTH, PANEL_HEIGHT);
    }

    @Override
    public void extractBackground(
          GuiGraphicsExtractor extractor,
          int mouseX,
          int mouseY,
          float partialTick
    ) {
        super.extractBackground(extractor, mouseX, mouseY, partialTick);
        Identifier texture = texture();
        extractor.blit(RenderPipelines.GUI_TEXTURED, texture,
              leftPos, topPos, 0.0f, 0.0f,
              PANEL_WIDTH, PANEL_HEIGHT, 256, 256);
        int progress = menu.progressScaled();
        if (progress > 0) {
            extractor.blit(RenderPipelines.GUI_TEXTURED, texture,
                  leftPos + 33, topPos + 10, 0.0f, 235.0f,
                  progress, 10, 256, 256);
        }
    }

    @Override
    protected void extractLabels(
          GuiGraphicsExtractor extractor,
          int mouseX,
          int mouseY
    ) {
        long displayed = Math.min(menu.storedEmc(), menu.requiredEmc());
        extractor.text(font, Component.translatable(
              "item.projecte.emc_value", String.format("%,d", displayed)),
              140, 10, 0xFF404040);
    }

    private Identifier texture() {
        return menu.tier() == 1 ? MK1_TEXTURE : MK2_TEXTURE;
    }
}
