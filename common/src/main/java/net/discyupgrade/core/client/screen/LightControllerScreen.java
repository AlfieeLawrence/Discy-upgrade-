package net.discyupgrade.core.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.discyupgrade.core.client.gui.ColorPickerPanel;
import net.discyupgrade.core.network.DiscyUpgradeNetworking;
import net.discyupgrade.core.screen.LightControllerMenu;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class LightControllerScreen extends AbstractContainerScreen<LightControllerMenu> {
    private enum Tab { FLOORS, LIGHTS, GROUPS }

    private static final int RIGHT_PANEL_W = 118;

    private Tab activeTab = Tab.FLOORS;
    private ColorPickerPanel colorPicker;
    private EditBox groupNameField;
    private int gridOriginX, gridOriginY, tileSize = 18;
    private int selectedRelX = -1, selectedRelZ = -1;
    private UUID activeGroupId;
    private final Map<Long, Integer> localColors = new HashMap<>();

    public LightControllerScreen(LightControllerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 340;
        imageHeight = 230;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;
        activeGroupId = menu.getSelectedFloorGroup();
        if (activeGroupId == null) menu.getSelectedFloorView().ifPresent(g -> activeGroupId = g.id());

        int tabY = topPos + 4;
        int tabX = leftPos + 8;
        addRenderableWidget(Button.builder(Component.translatable("gui.discyupgrade.tab.floors"), b -> activeTab = Tab.FLOORS)
                .bounds(tabX, tabY, 52, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.discyupgrade.tab.lights"), b -> activeTab = Tab.LIGHTS)
                .bounds(tabX + 54, tabY, 52, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.discyupgrade.tab.groups"), b -> activeTab = Tab.GROUPS)
                .bounds(tabX + 108, tabY, 52, 18).build());

        colorPicker = new ColorPickerPanel(font, color -> {
            if (activeGroupId != null && selectedRelX >= 0) {
                applyColor(selectedRelX, selectedRelZ, color);
            }
        });
        colorPicker.setBounds(leftPos + imageWidth - RIGHT_PANEL_W - 6, topPos + 28, RIGHT_PANEL_W);
        addRenderableWidget(colorPicker.createHexField());

        groupNameField = new EditBox(font, leftPos + 10, topPos + imageHeight - 22, 120, 18, Component.literal("Group"));
        groupNameField.setMaxLength(32);
        addRenderableWidget(groupNameField);

        addRenderableWidget(Button.builder(Component.translatable("gui.discyupgrade.apply_tile"), b -> {
            if (activeGroupId != null && selectedRelX >= 0) {
                DiscyUpgradeNetworking.sendSetTileColor(menu.getControllerPos(), activeGroupId,
                        selectedRelX, selectedRelZ, colorPicker.getSelectedColor());
            }
        }).bounds(leftPos + 8, topPos + imageHeight - 44, 70, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.discyupgrade.apply_all"), b -> {
            if (activeGroupId != null) {
                DiscyUpgradeNetworking.sendApplyAllColor(menu.getControllerPos(), activeGroupId, colorPicker.getSelectedColor());
            }
        }).bounds(leftPos + 82, topPos + imageHeight - 44, 70, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.discyupgrade.rename_group"), b -> renameGroup())
                .bounds(leftPos + 156, topPos + imageHeight - 44, 72, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.discyupgrade.wrench_group"), b -> {
            if (activeGroupId != null) DiscyUpgradeNetworking.sendSetWrenchGroup(activeGroupId);
        }).bounds(leftPos + 232, topPos + imageHeight - 44, 72, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.discyupgrade.disco_spin"),
                b -> DiscyUpgradeNetworking.sendToggleDiscoSpin(menu.getControllerPos(), !menu.isDiscoSpinEnabled()))
                .bounds(leftPos + 8, topPos + imageHeight - 44, 90, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.discyupgrade.toggle_discos"),
                b -> DiscyUpgradeNetworking.sendToggleAllDiscos(menu.getControllerPos()))
                .bounds(leftPos + 102, topPos + imageHeight - 44, 90, 18).build());

        reloadColors();
        layoutGrid();
        syncGroupNameField();
    }

    private void syncGroupNameField() {
        if (groupNameField == null || activeGroupId == null) return;
        for (var g : menu.getFloorGroups()) {
            if (g.id().equals(activeGroupId)) {
                groupNameField.setValue(g.name());
                return;
            }
        }
    }

    private void renameGroup() {
        if (activeGroupId == null || groupNameField == null) return;
        String name = groupNameField.getValue().trim();
        if (!name.isEmpty()) DiscyUpgradeNetworking.sendRenameFloorGroup(activeGroupId, name);
    }

    private void reloadColors() {
        localColors.clear();
        Optional<LightControllerMenu.FloorGroupView> view = menu.getFloorGroups().stream()
                .filter(g -> g.id().equals(activeGroupId)).findFirst();
        view.ifPresent(g -> g.tiles().forEach(t -> localColors.put(pack(t.relX(), t.relZ()), t.color())));
    }

    private void layoutGrid() {
        Optional<LightControllerMenu.FloorGroupView> view = menu.getFloorGroups().stream()
                .filter(g -> g.id().equals(activeGroupId)).findFirst();
        int maxX = 0, maxZ = 0;
        if (view.isPresent()) {
            for (var t : view.get().tiles()) {
                maxX = Math.max(maxX, t.relX());
                maxZ = Math.max(maxZ, t.relZ());
            }
        }
        int gridW = (maxX + 1) * tileSize;
        int gridH = (maxZ + 1) * tileSize;
        int availW = imageWidth - RIGHT_PANEL_W - 28;
        int availH = imageHeight - 80;
        float scale = Math.min(1f, Math.min(availW / (float) Math.max(1, gridW), availH / (float) Math.max(1, gridH)));
        tileSize = Math.max(8, (int) (18 * scale));
        gridW = (maxX + 1) * tileSize;
        gridH = (maxZ + 1) * tileSize;
        gridOriginX = leftPos + 14 + (availW - gridW) / 2;
        gridOriginY = topPos + 36 + (availH - gridH) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xCC101010);
        g.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xEE1A1A1A);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawString(font, title, leftPos + 8, topPos + imageHeight - 58, 0xFFAAAAAA, false);

        switch (activeTab) {
            case FLOORS -> renderFloorsTab(g);
            case LIGHTS -> renderLightsTab(g);
            case GROUPS -> renderGroupsTab(g);
        }
        renderTooltip(g, mouseX, mouseY);
    }

    private void renderFloorsTab(GuiGraphics g) {
        int gy = topPos + 26;
        int gx = leftPos + 10;
        g.drawString(font, Component.translatable("gui.discyupgrade.linked_floors"), gx, gy, 0xFFCCCCCC, false);
        int i = 0;
        for (var group : menu.getFloorGroups()) {
            int bx = gx + i * 62;
            boolean selected = group.id().equals(activeGroupId);
            g.fill(bx, gy + 12, bx + 60, gy + 28, selected ? 0xFF2A5A8A : 0xFF333333);
            String label = group.name().length() > 8 ? group.name().substring(0, 8) : group.name();
            g.drawString(font, label, bx + 4, gy + 17, 0xFFFFFFFF, false);
            i++;
        }

        Optional<LightControllerMenu.FloorGroupView> view = menu.getFloorGroups().stream()
                .filter(gr -> gr.id().equals(activeGroupId)).findFirst();
        if (view.isEmpty()) {
            g.drawString(font, Component.translatable("gui.discyupgrade.no_floors"),
                    leftPos + 20, topPos + 80, 0xFF888888, false);
            return;
        }

        for (var tile : view.get().tiles()) {
            int x = gridOriginX + tile.relX() * tileSize;
            int y = gridOriginY + tile.relZ() * tileSize;
            int color = localColors.getOrDefault(pack(tile.relX(), tile.relZ()), tile.color());
            g.fill(x, y, x + tileSize - 1, y + tileSize - 1, 0xFF000000 | (color & 0xFFFFFF));
            if (tile.relX() == selectedRelX && tile.relZ() == selectedRelZ) {
                g.renderOutline(x - 1, y - 1, tileSize + 1, tileSize + 1, 0xFFFFFFFF);
            }
        }
        colorPicker.render(g);
    }

    private void renderLightsTab(GuiGraphics g) {
        int y = topPos + 30;
        g.drawString(font, Component.translatable("gui.discyupgrade.disco_balls"), leftPos + 10, y, 0xFFCCCCCC, false);
        y += 14;
        for (var disco : menu.getDiscoBalls()) {
            String line = disco.pos().getX() + ", " + disco.pos().getY() + ", " + disco.pos().getZ()
                    + (disco.active() ? " [ON]" : " [OFF]");
            g.drawString(font, line, leftPos + 14, y, disco.active() ? 0xFF88FF88 : 0xFF888888, false);
            y += 12;
        }
        y += 8;
        g.drawString(font, Component.translatable("gui.discyupgrade.lasers"), leftPos + 10, y, 0xFFCCCCCC, false);
        y += 14;
        for (var laser : menu.getLasers()) {
            String line = laser.pos().getX() + ", " + laser.pos().getY() + ", " + laser.pos().getZ()
                    + (laser.active() ? " [ON]" : " [OFF]");
            g.drawString(font, line, leftPos + 14, y, laser.active() ? 0xFFFF8888 : 0xFF888888, false);
            y += 12;
        }
        g.drawString(font, Component.translatable("gui.discyupgrade.spin_state",
                menu.isDiscoSpinEnabled() ? "ON" : "OFF"), leftPos + 10, topPos + imageHeight - 70, 0xFFAAAAAA, false);
    }

    private void renderGroupsTab(GuiGraphics g) {
        int y = topPos + 30;
        g.drawString(font, Component.translatable("gui.discyupgrade.manage_groups"), leftPos + 10, y, 0xFFCCCCCC, false);
        y += 16;
        for (var group : menu.getFloorGroups()) {
            g.drawString(font, group.name() + " (" + group.tiles().size() + " tiles)", leftPos + 14, y, 0xFFE0E0E0, false);
            y += 12;
        }
        y += 8;
        g.drawString(font, Component.translatable("gui.discyupgrade.wrench_help_1"), leftPos + 10, y, 0xFF999999, false);
        y += 11;
        g.drawString(font, Component.translatable("gui.discyupgrade.wrench_help_2"), leftPos + 10, y, 0xFF999999, false);
        y += 11;
        g.drawString(font, Component.translatable("gui.discyupgrade.wrench_help_3"), leftPos + 10, y, 0xFF999999, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (activeTab == Tab.FLOORS) {
            int gy = topPos + 26;
            int gx = leftPos + 10;
            int i = 0;
            for (var group : menu.getFloorGroups()) {
                int bx = gx + i * 62;
                if (mouseX >= bx && mouseX < bx + 60 && mouseY >= gy + 12 && mouseY < gy + 28) {
                    activeGroupId = group.id();
                    selectedRelX = selectedRelZ = -1;
                    DiscyUpgradeNetworking.sendSelectFloorGroup(menu.getControllerPos(), activeGroupId);
                    reloadColors();
                    layoutGrid();
                    syncGroupNameField();
                    return true;
                }
                i++;
            }
            if (colorPicker.mouseClicked(mouseX, mouseY, button)) return true;
            if (button == 0 && selectTileAt(mouseX, mouseY)) return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (activeTab == Tab.FLOORS && colorPicker.mouseDragged(mouseX, mouseY)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        colorPicker.mouseReleased();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean selectTileAt(double mouseX, double mouseY) {
        Optional<LightControllerMenu.FloorGroupView> view = menu.getFloorGroups().stream()
                .filter(gr -> gr.id().equals(activeGroupId)).findFirst();
        if (view.isEmpty()) return false;
        for (var tile : view.get().tiles()) {
            int x = gridOriginX + tile.relX() * tileSize;
            int y = gridOriginY + tile.relZ() * tileSize;
            if (mouseX >= x && mouseX < x + tileSize - 1 && mouseY >= y && mouseY < y + tileSize - 1) {
                selectedRelX = tile.relX();
                selectedRelZ = tile.relZ();
                colorPicker.setSelectedColor(localColors.getOrDefault(pack(selectedRelX, selectedRelZ), tile.color()));
                return true;
            }
        }
        return false;
    }

    private void applyColor(int relX, int relZ, int color) {
        localColors.put(pack(relX, relZ), color & 0xFFFFFF);
        if (activeGroupId != null) {
            menu.updateTileColor(activeGroupId, relX, relZ, color);
            DiscyUpgradeNetworking.sendSetTileColor(menu.getControllerPos(), activeGroupId, relX, relZ, color);
        }
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
}
