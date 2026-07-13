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
 * (a 256×256 sheet with a 228×196 usable area), draws the input/lock/output slot squares that are
 * not baked into the texture, and overlays the player's current EMC balance in the lower-left.
 *
 * <p>The 26.2 GUI pipeline uses an extract-render-state model: {@link #extractBackground} draws the
 * textured panel and slot squares in screen space (before the (leftPos, topPos) translate), while
 * {@link #extractLabels} draws text in panel-relative space (after the translate).
 */
public final class TransmutationTableScreen extends AbstractContainerScreen<TransmutationTableMenu> {
    private static final int PANEL_WIDTH = 228;
    private static final int PANEL_HEIGHT = 196;
    private static final Identifier TEXTURE =
          Identifier.fromNamespaceAndPath(ProjectEAPI.MOD_ID, "textures/gui/transmute.png");

    // Slot square color (dark grey, the classic vanilla slot background tone).
    private static final int SLOT_COLOR = 0xFF373737;

    public TransmutationTableScreen(TransmutationTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, PANEL_WIDTH, PANEL_HEIGHT);
        // Position the inventory/title labels to match the texture's inventory grid + title area.
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 35;
        this.inventoryLabelY = 107;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(extractor, mouseX, mouseY, partialTick);
        // Draw the textured panel. The PNG is a 256×256 sheet; the usable panel is 228×196 at UV (0,0).
        extractor.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
              leftPos, topPos, 0.0f, 0.0f, PANEL_WIDTH, PANEL_HEIGHT, 256, 256);
        // The texture only bakes the consume/unlearn slots and the player inventory grid; the input
        // diamond, lock slot and output ring are rendered here as dark squares so every slot is visible.
        drawSlotSquares(extractor);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        super.extractLabels(extractor, mouseX, mouseY);
        // Draw the player's current EMC balance in the lower-left of the panel (panel-relative coords).
        long emc = menu.playerEmc().longValue();
        extractor.text(font, Component.translatable("item.projecte.emc_value", String.format("%,d", emc)),
              8, PANEL_HEIGHT - 92, 0xFF404040);
    }

    /**
     * Draws the slot squares for the input/lock/output slots that are not part of the texture.
     * Coordinates are in screen space (offset by leftPos/topPos). Each slot is 18×18 with a 1px
     * border, matching vanilla's slot rendering.
     */
    private void drawSlotSquares(GuiGraphicsExtractor extractor) {
        // Input slots (8, diamond) — read the same coordinates the menu used to add them.
        int[][] inputPos = {
              {43, 23}, {34, 41}, {52, 41}, {16, 50}, {70, 50}, {34, 59}, {52, 59}, {43, 77}
        };
        for (int[] pos : inputPos) {
            drawSlotSquare(extractor, pos[0], pos[1]);
        }
        // Lock slot.
        drawSlotSquare(extractor, 158, 50);
        // Output ring (12 outer + 4 inner).
        int[][] outputPos = {
              {158, 9}, {176, 13}, {193, 30}, {199, 50}, {193, 70}, {176, 87},
              {158, 91}, {140, 87}, {123, 70}, {116, 50}, {123, 30}, {140, 13},
              {158, 31}, {177, 50}, {158, 69}, {139, 50}
        };
        for (int[] pos : outputPos) {
            drawSlotSquare(extractor, pos[0], pos[1]);
        }
    }

    private void drawSlotSquare(GuiGraphicsExtractor extractor, int slotX, int slotY) {
        // Slot coords in the menu are the item's top-left; a slot is 18×18 (1px border + 16 interior).
        extractor.fill(leftPos + slotX - 1, topPos + slotY - 1,
              leftPos + slotX + 17, topPos + slotY + 17, SLOT_COLOR);
    }
}
