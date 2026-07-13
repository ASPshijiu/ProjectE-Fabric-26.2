package moze_intel.projecte.client.screen;

import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.content.menu.TransmutationTableMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client screen for the transmutation table. Renders the {@code transmute.png} panel background
 * (a 256×256 sheet with a 228×196 usable area) and overlays the player's current EMC balance.
 *
 * <p>The 26.2 GUI pipeline uses an extract-render-state model: {@link #extractBackground} draws the
 * complete textured panel in screen space (before the (leftPos, topPos) translate), while
 * {@link #extractLabels} draws text in panel-relative space (after the translate).
 */
public final class TransmutationTableScreen extends AbstractContainerScreen<TransmutationTableMenu> {
    private static final int PANEL_WIDTH = 228;
    private static final int PANEL_HEIGHT = 196;
    private static final Identifier TEXTURE =
          Identifier.fromNamespaceAndPath(ProjectEAPI.MOD_ID, "textures/gui/transmute.png");

    public TransmutationTableScreen(TransmutationTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, PANEL_WIDTH, PANEL_HEIGHT);
        this.titleLabelX = 6;
        this.titleLabelY = 8;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(extractor, mouseX, mouseY, partialTick);
        // Draw the textured panel. The PNG is a 256×256 sheet; the usable panel is 228×196 at UV (0,0).
        extractor.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
              leftPos, topPos, 0.0f, 0.0f, PANEL_WIDTH, PANEL_HEIGHT, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        extractor.text(font, title, titleLabelX, titleLabelY, 0xFF404040);
        long emc = menu.playerEmc().longValue();
        extractor.text(font, Component.translatable("item.projecte.emc_value", String.format("%,d", emc)),
              6, PANEL_HEIGHT - 94, 0xFF404040);
    }
}
