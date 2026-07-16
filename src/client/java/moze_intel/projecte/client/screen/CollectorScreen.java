package moze_intel.projecte.client.screen;

import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.content.menu.CollectorMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Client screen shared by all three Energy Collector tiers. */
public final class CollectorScreen extends AbstractContainerScreen<CollectorMenu> {
    private static final Identifier MK1_TEXTURE = ProjectEAPI.id(
          "textures/gui/collector1.png");
    private static final Identifier MK2_TEXTURE = ProjectEAPI.id(
          "textures/gui/collector2.png");
    private static final Identifier MK3_TEXTURE = ProjectEAPI.id(
          "textures/gui/collector3.png");

    public CollectorScreen(
          CollectorMenu menu,
          Inventory playerInventory,
          Component title
    ) {
        super(menu, playerInventory, title, width(menu.tier()), height(menu.tier()));
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

        int xShift = xShift();
        int textureShift = textureShift();
        int sunlight = Math.clamp(menu.sunLevel() * 12 / 16, 0, 12);
        if (sunlight > 0) {
            extractor.blit(RenderPipelines.GUI_TEXTURED, texture,
                  leftPos + 126 + xShift, topPos + 49 - sunlight,
                  177.0f + textureShift, 13.0f - sunlight,
                  12, sunlight, 256, 256);
        }

        drawHorizontalProgress(extractor, texture,
              leftPos + 64 + xShift, topPos + 18, menu.storageProgress());
        drawHorizontalProgress(extractor, texture,
              leftPos + 64 + xShift, topPos + 58, menu.kleinProgress());

        int fuel = Math.clamp((int) (menu.fuelProgress() * 24), 0, 24);
        extractor.blit(RenderPipelines.GUI_TEXTURED, texture,
              leftPos + 138 + xShift, topPos + 55 - fuel,
              176.0f + textureShift, 38.0f - fuel,
              10, fuel + 1, 256, 256);
    }

    @Override
    protected void extractLabels(
          GuiGraphicsExtractor extractor,
          int mouseX,
          int mouseY
    ) {
        int x = 60 + xShift();
        extractor.text(font, Long.toString(menu.storedEmc()), x, 32, 0xFF404040);
        if (menu.kleinEmc() > 0) {
            extractor.text(font, String.format("%,d", menu.kleinEmc()),
                  x, 44, 0xFF404040);
        }
    }

    private void drawHorizontalProgress(
          GuiGraphicsExtractor extractor,
          Identifier texture,
          int x,
          int y,
          double progress
    ) {
        int width = Math.clamp((int) (progress * 48), 0, 48);
        if (width > 0) {
            extractor.blit(RenderPipelines.GUI_TEXTURED, texture,
                  x, y, 0.0f, 166.0f, width, 10, 256, 256);
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

    private int xShift() {
        return switch (menu.tier()) {
            case 1 -> 0;
            case 2 -> 16;
            case 3 -> 34;
            default -> throw new IllegalStateException();
        };
    }

    private int textureShift() {
        return switch (menu.tier()) {
            case 1 -> 0;
            case 2 -> 25;
            case 3 -> 43;
            default -> throw new IllegalStateException();
        };
    }

    private static int width(int tier) {
        return switch (tier) {
            case 1 -> 176;
            case 2 -> 200;
            case 3 -> 218;
            default -> throw new IllegalArgumentException();
        };
    }

    private static int height(int tier) {
        return tier == 1 ? 166 : 165;
    }
}
