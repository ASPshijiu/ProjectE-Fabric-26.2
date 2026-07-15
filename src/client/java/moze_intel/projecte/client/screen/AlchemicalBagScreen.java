package moze_intel.projecte.client.screen;

import moze_intel.projecte.content.menu.AlchemicalBagMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client screen for the Alchemical Bag. Renders the upstream 104-slot inventory layout.
 */
public final class AlchemicalBagScreen extends AbstractContainerScreen<AlchemicalBagMenu> {
    private static final Identifier TEXTURE =
          Identifier.parse("projecte:textures/gui/alchchest.png");
    private static final int PANEL_WIDTH = 255;
    private static final int PANEL_HEIGHT = 230;

    public AlchemicalBagScreen(AlchemicalBagMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, PANEL_WIDTH, PANEL_HEIGHT);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(extractor, mouseX, mouseY, partialTick);
        extractor.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
              leftPos, topPos, 0.0f, 0.0f, PANEL_WIDTH, PANEL_HEIGHT, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        // The upstream texture has no space for title or inventory labels.
    }
}
