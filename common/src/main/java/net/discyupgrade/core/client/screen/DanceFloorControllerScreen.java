package net.discyupgrade.core.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.discyupgrade.core.client.gui.ColorPickerPanel;
import net.discyupgrade.core.network.DiscyUpgradeNetworking;
import net.discyupgrade.core.screen.DanceFloorControllerMenu;

import java.util.HashMap;
import java.util.Map;

public class DanceFloorControllerScreen extends AbstractContainerScreen<DanceFloorControllerMenu> {
    private static final int RIGHT_PANEL_W = 120;
    private static final int GRID_PADDING = 16;

    private ColorPickerPanel colorPicker;
    private int gridOriginX;
    private int gridOriginY;
    private int tileSize = 20;
    private int selectedRelX = -1;
    private int selectedRelZ = -1;
    private final Map<Long, Integer> localColors = new HashMap<>();

    public DanceFloorControllerScreen(DanceFloorControllerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 320;
        this.imageHeight = 220;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;

        colorPicker = new ColorPickerPanel(font, color -> {
            if (selectedRelX >= 0) {
                applyColorToTile(selectedRelX, selectedRelZ, color);
            }
        });
        colorPicker.setBounds(leftPos + imageWidth - RIGHT_PANEL_W - 8, topPos + 20, RIGHT_PANEL_W);
        addRenderableWidget(colorPicker.createHexField());

        addRenderableWidget(Button.builder(Component.translatable("gui.discyupgrade.apply_tile"), b -> {
            if (selectedRelX >= 0) {
                DiscyUpgradeNetworking.sendSetTileColor(
                        menu.getControllerPos(), selectedRelX, selectedRelZ, colorPicker.getSelectedColor());
            }
        }).bounds(leftPos + 8, topPos + imageHeight - 24, 72, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.discyupgrade.apply_all"), b ->
                DiscyUpgradeNetworking.sendApplyAllColor(menu.getControllerPos(), colorPicker.getSelectedColor())
        ).bounds(leftPos + 84, topPos + imageHeight - 24, 72, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.discyupgrade.refresh"), b ->
                DiscyUpgradeNetworking.sendRefresh(menu.getControllerPos())
        ).bounds(leftPos + 160, topPos + imageHeight - 24, 56, 18).build());

        reloadLocalColors();
        layoutGrid();
    }

    private void reloadLocalColors() {
        localColors.clear();
        for (DanceFloorControllerMenu.TileView tile : menu.getTiles()) {
            localColors.put(pack(tile.relX(), tile.relZ()), tile.color());
        }
    }

    private void layoutGrid() {
        int maxX = 0;
        int maxZ = 0;
        for (DanceFloorControllerMenu.TileView tile : menu.getTiles()) {
            maxX = Math.max(maxX, tile.relX());
            maxZ = Math.max(maxZ, tile.relZ());
        }

        int gridW = (maxX + 1) * tileSize;
        int gridH = (maxZ + 1) * tileSize;
        int availW = imageWidth - RIGHT_PANEL_W - GRID_PADDING * 2;
        int availH = imageHeight - GRID_PADDING * 2 - 30;
        if (gridW > availW || gridH > availH) {
            float scale = Math.min(availW / (float) Math.max(1, gridW), availH / (float) Math.max(1, gridH));
            tileSize = Math.max(8, (int) (tileSize * scale));
            gridW = (maxX + 1) * tileSize;
            gridH = (maxZ + 1) * tileSize;
        }

        gridOriginX = leftPos + GRID_PADDING + (availW - gridW) / 2;
        gridOriginY = topPos + GRID_PADDING + 12 + (availH - gridH) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xCC101010);
        g.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xEE1E1E1E);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderLabels(g);

        g.drawString(font, Component.translatable("gui.discyupgrade.floor_layout"),
                gridOriginX, topPos + 14, 0xFFCCCCCC, false);

        for (DanceFloorControllerMenu.TileView tile : menu.getTiles()) {
            int x = gridOriginX + tile.relX() * tileSize;
            int y = gridOriginY + tile.relZ() * tileSize;
            int color = localColors.getOrDefault(pack(tile.relX(), tile.relZ()), tile.color());
            int argb = 0xFF000000 | (color & 0xFFFFFF);
            g.fill(x, y, x + tileSize - 1, y + tileSize - 1, argb);
            if (tile.relX() == selectedRelX && tile.relZ() == selectedRelZ) {
                g.renderOutline(x - 1, y - 1, tileSize + 1, tileSize + 1, 0xFFFFFFFF);
            } else {
                g.renderOutline(x, y, tileSize - 1, tileSize - 1, 0xFF333333);
            }
        }

        colorPicker.render(g);
        renderTooltip(g, mouseX, mouseY);
    }

    protected void renderLabels(GuiGraphics g) {
        g.drawString(font, title, 8, 6, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (colorPicker.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0 && selectTileAt(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (colorPicker.mouseDragged(mouseX, mouseY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        colorPicker.mouseReleased();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean selectTileAt(double mouseX, double mouseY) {
        for (DanceFloorControllerMenu.TileView tile : menu.getTiles()) {
            int x = gridOriginX + tile.relX() * tileSize;
            int y = gridOriginY + tile.relZ() * tileSize;
            if (mouseX >= x && mouseX < x + tileSize - 1 && mouseY >= y && mouseY < y + tileSize - 1) {
                selectedRelX = tile.relX();
                selectedRelZ = tile.relZ();
                int color = localColors.getOrDefault(pack(selectedRelX, selectedRelZ), tile.color());
                colorPicker.setSelectedColor(color);
                return true;
            }
        }
        return false;
    }

    private void applyColorToTile(int relX, int relZ, int color) {
        localColors.put(pack(relX, relZ), color & 0xFFFFFF);
        menu.updateTileColor(relX, relZ, color);
        DiscyUpgradeNetworking.sendSetTileColor(menu.getControllerPos(), relX, relZ, color);
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
}
