package moze_intel.projecte.client.screen;

import moze_intel.projecte.content.menu.AlchemicalBagMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client screen for the Alchemical Bag. Renders the 27-slot bag inventory plus the player inventory
 * over a generic 6-row container background (176×166, matching the standard chest/shulker GUI).
 */
public final class AlchemicalBagScreen extends AbstractContainerScreen<AlchemicalBagMenu> {
    // The vanilla generic_54 texture sheet: rows 0..6 occupy 14 + row*18 pixels from the top.
    // A 3-row layout (bag) + 3-row player inventory = the 6-row sheet, using the 0..6 portion.
    private static final Identifier TEXTURE = Identifier.parse("minecraft:textures/gui/container/generic_54.png");
    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 168;
    private static final int BAG_ROWS = 3;
    private static final int TOP_HEIGHT = 17 + BAG_ROWS * 18;
    private static final int PLAYER_INVENTORY_TEXTURE_Y = 126;
    private static final int PLAYER_INVENTORY_HEIGHT = 96;

    public AlchemicalBagScreen(AlchemicalBagMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, PANEL_WIDTH, PANEL_HEIGHT);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 74;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(extractor, mouseX, mouseY, partialTick);
        extractor.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
              leftPos, topPos, 0.0f, 0.0f, PANEL_WIDTH, TOP_HEIGHT, 256, 256);
        extractor.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
              leftPos, topPos + TOP_HEIGHT, 0.0f, PLAYER_INVENTORY_TEXTURE_Y,
              PANEL_WIDTH, PLAYER_INVENTORY_HEIGHT, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        super.extractLabels(extractor, mouseX, mouseY);
    }
}
