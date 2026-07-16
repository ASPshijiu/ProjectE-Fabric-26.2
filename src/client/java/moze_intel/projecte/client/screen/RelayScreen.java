package moze_intel.projecte.client.screen;

import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.content.menu.RelayMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Client screen shared by all three Anti-Matter Relay tiers. */
public final class RelayScreen extends AbstractContainerScreen<RelayMenu> {
    private static final Identifier MK1_TEXTURE = ProjectEAPI.id("textures/gui/relay1.png");
    private static final Identifier MK2_TEXTURE = ProjectEAPI.id("textures/gui/relay2.png");
    private static final Identifier MK3_TEXTURE = ProjectEAPI.id("textures/gui/relay3.png");

    public RelayScreen(RelayMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, width(menu.tier()), height(menu.tier()));
        titleLabelX = titleX(menu.tier());
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
              imageWidth, imageHeight, 256, 256);

        drawProgress(extractor, texture,
              leftPos + emcBarX(), topPos + 6,
              30, vOffset(), 102, 10, menu.storageProgress());
        drawProgress(extractor, texture,
              leftPos + 116 + shiftX(), topPos + 67 + shiftY(),
              0, vOffset(), 30, 10, menu.chargeProgress());
        drawProgress(extractor, texture,
              leftPos + 64 + shiftX(), topPos + 67 + shiftY(),
              0, vOffset(), 30, 10, menu.burnProgress());
    }

    @Override
    protected void extractLabels(
          GuiGraphicsExtractor extractor,
          int mouseX,
          int mouseY
    ) {
        extractor.text(font, title, titleLabelX, titleLabelY, 0xFF404040);
        extractor.text(font, String.format("%,d", menu.storedEmc()),
              emcX(), emcY(), 0xFF404040);
    }

    private void drawProgress(
          GuiGraphicsExtractor extractor,
          Identifier texture,
          int x,
          int y,
          int sourceX,
          int sourceY,
          int maximumWidth,
          int height,
          double progress
    ) {
        int width = Math.clamp((int) (progress * maximumWidth), 0, maximumWidth);
        if (width > 0) {
            extractor.blit(RenderPipelines.GUI_TEXTURED, texture,
                  x, y, sourceX, (float) sourceY,
                  width, height, 256, 256);
        }
    }

    private Identifier texture() {
        return switch (menu.tier()) {
            case 1 -> MK1_TEXTURE;
            case 2 -> MK2_TEXTURE;
            case 3 -> MK3_TEXTURE;
            default -> throw new IllegalStateException();
        };
    }

    private int emcX() {
        return switch (menu.tier()) {
            case 1 -> 88;
            case 2 -> 107;
            case 3 -> 125;
            default -> throw new IllegalStateException();
        };
    }

    private int emcY() {
        return switch (menu.tier()) {
            case 1 -> 24;
            case 2 -> 25;
            case 3 -> 39;
            default -> throw new IllegalStateException();
        };
    }

    private int vOffset() {
        return switch (menu.tier()) {
            case 1 -> 177;
            case 2 -> 183;
            case 3 -> 195;
            default -> throw new IllegalStateException();
        };
    }

    private int emcBarX() {
        return switch (menu.tier()) {
            case 1 -> 64;
            case 2 -> 86;
            case 3 -> 105;
            default -> throw new IllegalStateException();
        };
    }

    private int shiftX() {
        return switch (menu.tier()) {
            case 1 -> 0;
            case 2 -> 17;
            case 3 -> 37;
            default -> throw new IllegalStateException();
        };
    }

    private int shiftY() {
        return switch (menu.tier()) {
            case 1 -> 0;
            case 2 -> 1;
            case 3 -> 15;
            default -> throw new IllegalStateException();
        };
    }

    private static int width(int tier) {
        return switch (tier) {
            case 1 -> 175;
            case 2 -> 193;
            case 3 -> 212;
            default -> throw new IllegalArgumentException();
        };
    }

    private static int height(int tier) {
        return switch (tier) {
            case 1 -> 176;
            case 2 -> 182;
            case 3 -> 194;
            default -> throw new IllegalArgumentException();
        };
    }

    private static int titleX(int tier) {
        return switch (tier) {
            case 1 -> 10;
            case 2 -> 28;
            case 3 -> 38;
            default -> throw new IllegalArgumentException();
        };
    }
}
