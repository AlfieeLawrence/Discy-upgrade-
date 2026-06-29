package net.discyupgrade.core.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.discyupgrade.core.client.gui.ColorPickerPanel;
import net.discyupgrade.core.floor.FloorPattern;
import net.discyupgrade.core.network.DiscyUpgradeNetworking;
import net.discyupgrade.core.screen.LightControllerMenu;

import java.util.*;

public class LightControllerScreen extends AbstractContainerScreen<LightControllerMenu> {
    private enum Tab { FLOORS, LIGHTS, GROUPS }

    private static final int RIGHT_PANEL_W = 118;
    private Tab activeTab = Tab.FLOORS;
    private ColorPickerPanel colorPicker;
    private EditBox groupNameField;
    private EditBox presetNameField;
    private int gridOriginX, gridOriginY, tileSize = 18;
    private int selectedRelX = -1, selectedRelZ = -1;
    private UUID activeGroupId;
    private int patternIndex;
    private int patternSpeed = 5;
    private final Map<Long, Integer> localColors = new HashMap<>();

    public LightControllerScreen(LightControllerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 340;
        imageHeight = 236;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;
        activeGroupId = menu.getSelectedFloorGroup();
        if (activeGroupId == null && !menu.getFloorGroups().isEmpty()) {
            activeGroupId = menu.getFloorGroups().get(0).id();
        }
        syncAnimFromGroup();

        int tabY = topPos + 4;
        addRenderableWidget(Button.builder(Component.translatable("gui.discyupgrade.tab.floors"), b -> activeTab = Tab.FLOORS)
                .bounds(leftPos + 8, tabY, 52, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.discyupgrade.tab.lights"), b -> activeTab = Tab.LIGHTS)
                .bounds(leftPos + 62, tabY, 52, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.discyupgrade.tab.groups"), b -> activeTab = Tab.GROUPS)
                .bounds(leftPos + 116, tabY, 52, 18).build());

        colorPicker = new ColorPickerPanel(font, color -> {
            if (activeGroupId != null && selectedRelX >= 0) applyColor(selectedRelX, selectedRelZ, color);
        });
        colorPicker.setBounds(leftPos + imageWidth - RIGHT_PANEL_W - 6, topPos + 28, RIGHT_PANEL_W);
        addRenderableWidget(colorPicker.createHexField());

        groupNameField = new EditBox(font, leftPos + 8, topPos + imageHeight - 22, 100, 18, Component.literal("Group"));
        groupNameField.setMaxLength(32);
        addRenderableWidget(groupNameField);

        presetNameField = new EditBox(font, leftPos + 112, topPos + imageHeight - 22, 80, 18, Component.literal("Preset"));
        presetNameField.setMaxLength(24);
        addRenderableWidget(presetNameField);

        int by = topPos + imageHeight - 44;
        addRenderableWidget(Button.builder(Component.translatable("gui.discyupgrade.apply_tile"), b -> {
            if (activeGroupId != null && selectedRelX >= 0)
                DiscyUpgradeNetworking.sendSetTileColor(menu.getControllerPos(), activeGroupId, selectedRelX, selectedRelZ, colorPicker.getSelectedColor());
        }).bounds(leftPos + 8, by, 58, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.discyupgrade.apply_all"), b -> {
            if (activeGroupId != null)
                DiscyUpgradeNetworking.sendApplyAllColor(menu.getControllerPos(), activeGroupId, colorPicker.getSelectedColor());
        }).bounds(leftPos + 68, by, 58, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.discyupgrade.rename_group"), b -> renameGroup())
                .bounds(leftPos + 128, by, 54, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.discyupgrade.wrench_group"), b -> {
            if (activeGroupId != null) DiscyUpgradeNetworking.sendSetWrenchGroup(activeGroupId);
        }).bounds(leftPos + 184, by, 54, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.discyupgrade.unlink_group"), b -> {
            if (activeGroupId != null) {
                DiscyUpgradeNetworking.sendUnlinkFloorGroup(menu.getControllerPos(), activeGroupId);
                DiscyUpgradeNetworking.sendRefresh(menu.getControllerPos());
            }
        }).bounds(leftPos + 240, by, 54, 18).build());

        addRenderableWidget(Button.builder(Component.literal("<"), b -> cyclePattern(-1)).bounds(leftPos + 8, by - 22, 16, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.discyupgrade.pattern"), b -> applyPattern())
                .bounds(leftPos + 26, by - 22, 70, 18).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> cyclePattern(1)).bounds(leftPos + 98, by - 22, 16, 18).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> changeSpeed(-1)).bounds(leftPos + 118, by - 22, 16, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.discyupgrade.speed", patternSpeed), b -> {})
                .bounds(leftPos + 136, by - 22, 36, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> changeSpeed(1)).bounds(leftPos + 174, by - 22, 16, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.discyupgrade.play"), b -> togglePlay(true))
                .bounds(leftPos + 194, by - 22, 36, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.discyupgrade.pause"), b -> togglePlay(false))
                .bounds(leftPos + 232, by - 22, 40, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.discyupgrade.sync_disco"), b -> {
            if (activeGroupId != null) DiscyUpgradeNetworking.sendToggleSyncDisco(menu.getControllerPos(), activeGroupId, true);
        }).bounds(leftPos + 274, by - 22, 58, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.discyupgrade.save_preset"), b -> savePreset())
                .bounds(leftPos + 198, topPos + imageHeight - 22, 62, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.discyupgrade.apply_preset"), b -> applyPreset())
                .bounds(leftPos + 262, topPos + imageHeight - 22, 62, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.discyupgrade.refresh"), b ->
                DiscyUpgradeNetworking.sendRefresh(menu.getControllerPos())).bounds(leftPos + 8, topPos + imageHeight - 64, 54, 18).build());

        reloadColors();
        layoutGrid();
        syncGroupNameField();
    }

    private void syncAnimFromGroup() {
        for (var g : menu.getFloorGroups()) {
            if (g.id().equals(activeGroupId)) {
                patternIndex = g.anim().pattern().id();
                patternSpeed = g.anim().speed();
                return;
            }
        }
    }

    private void syncGroupNameField() {
        if (groupNameField == null || activeGroupId == null) return;
        for (var g : menu.getFloorGroups()) {
            if (g.id().equals(activeGroupId)) { groupNameField.setValue(g.name()); return; }
        }
    }

    private void renameGroup() {
        if (activeGroupId == null || groupNameField == null) return;
        String name = groupNameField.getValue().trim();
        if (!name.isEmpty()) DiscyUpgradeNetworking.sendRenameFloorGroup(activeGroupId, name);
    }

    private void cyclePattern(int delta) {
        FloorPattern[] values = FloorPattern.values();
        patternIndex = Math.floorMod(patternIndex + delta, values.length);
        applyPattern();
    }

    private void applyPattern() {
        if (activeGroupId == null) return;
        DiscyUpgradeNetworking.sendSetPattern(menu.getControllerPos(), activeGroupId, FloorPattern.fromId(patternIndex));
    }

    private void changeSpeed(int delta) {
        patternSpeed = Math.max(1, Math.min(10, patternSpeed + delta));
        if (activeGroupId != null)
            DiscyUpgradeNetworking.sendSetPatternSpeed(menu.getControllerPos(), activeGroupId, patternSpeed);
        init();
    }

    private void togglePlay(boolean playing) {
        if (activeGroupId != null)
            DiscyUpgradeNetworking.sendTogglePattern(menu.getControllerPos(), activeGroupId, playing);
    }

    private void savePreset() {
        if (activeGroupId == null || presetNameField == null) return;
        String name = presetNameField.getValue().trim();
        if (!name.isEmpty()) DiscyUpgradeNetworking.sendSavePreset(menu.getControllerPos(), name, activeGroupId);
    }

    private void applyPreset() {
        if (activeGroupId == null || presetNameField == null) return;
        String name = presetNameField.getValue().trim();
        if (!name.isEmpty()) {
            DiscyUpgradeNetworking.sendApplyPreset(menu.getControllerPos(), name, activeGroupId);
            DiscyUpgradeNetworking.sendRefresh(menu.getControllerPos());
        }
    }

    private void reloadColors() {
        localColors.clear();
        for (var g : menu.getFloorGroups()) {
            if (!g.id().equals(activeGroupId)) continue;
            g.tiles().forEach(t -> localColors.put(pack(t.relX(), t.relZ()), t.color()));
        }
    }

    private void layoutGrid() {
        int maxX = 0, maxZ = 0;
        for (var g : menu.getFloorGroups()) {
            if (!g.id().equals(activeGroupId)) continue;
            for (var t : g.tiles()) { maxX = Math.max(maxX, t.relX()); maxZ = Math.max(maxZ, t.relZ()); }
        }
        int gridW = (maxX + 1) * tileSize, gridH = (maxZ + 1) * tileSize;
        int availW = imageWidth - RIGHT_PANEL_W - 28, availH = imageHeight - 96;
        float scale = Math.min(1f, Math.min(availW / (float) Math.max(1, gridW), availH / (float) Math.max(1, gridH)));
        tileSize = Math.max(8, (int) (18 * scale));
        gridW = (maxX + 1) * tileSize; gridH = (maxZ + 1) * tileSize;
        gridOriginX = leftPos + 14 + (availW - gridW) / 2;
        gridOriginY = topPos + 40 + (availH - gridH) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xCC101010);
        g.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xEE1A1A1A);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        switch (activeTab) {
            case FLOORS -> renderFloors(g);
            case LIGHTS -> renderLights(g);
            case GROUPS -> renderGroups(g);
        }
        renderTooltip(g, mx, my);
    }

    private void renderFloors(GuiGraphics g) {
        int gy = topPos + 26, gx = leftPos + 10;
        g.drawString(font, Component.translatable("gui.discyupgrade.linked_floors"), gx, gy, 0xFFCCCCCC, false);
        int i = 0;
        for (var group : menu.getFloorGroups()) {
            int bx = gx + i * 62;
            boolean sel = group.id().equals(activeGroupId);
            g.fill(bx, gy + 12, bx + 60, gy + 28, sel ? 0xFF2A5A8A : 0xFF333333);
            g.drawString(font, truncate(group.name(), 8), bx + 4, gy + 17, 0xFFFFFFFF, false);
            i++;
        }
        var view = menu.getFloorGroups().stream().filter(gr -> gr.id().equals(activeGroupId)).findFirst();
        if (view.isEmpty()) {
            g.drawString(font, Component.translatable("gui.discyupgrade.no_floors"), leftPos + 20, topPos + 80, 0xFF888888, false);
            return;
        }
        g.drawString(font, Component.translatable("gui.discyupgrade.pattern_name",
                FloorPattern.fromId(patternIndex).name()), leftPos + 8, topPos + imageHeight - 78, 0xFFAAAAAA, false);
        for (var tile : view.get().tiles()) {
            int x = gridOriginX + tile.relX() * tileSize, y = gridOriginY + tile.relZ() * tileSize;
            int color = localColors.getOrDefault(pack(tile.relX(), tile.relZ()), tile.color());
            g.fill(x, y, x + tileSize - 1, y + tileSize - 1, 0xFF000000 | (color & 0xFFFFFF));
            if (tile.relX() == selectedRelX && tile.relZ() == selectedRelZ)
                g.renderOutline(x - 1, y - 1, tileSize + 1, tileSize + 1, 0xFFFFFFFF);
        }
        colorPicker.render(g);
    }

    private void renderLights(GuiGraphics g) {
        int y = topPos + 30;
        y = drawLightSection(g, y, "gui.discyupgrade.disco_balls", menu.getDiscoBalls(), true);
        y = drawLightSection(g, y, "gui.discyupgrade.lasers", menu.getLasers(), false);
        y = drawLightSection(g, y, "gui.discyupgrade.party_lights", menu.getPartyLights(), false);
        drawLightSection(g, y, "gui.discyupgrade.strobe_lights", menu.getStrobes(), false);
        g.drawString(font, Component.translatable("gui.discyupgrade.spin_state", menu.isDiscoSpinEnabled() ? "ON" : "OFF"),
                leftPos + 10, topPos + imageHeight - 70, 0xFFAAAAAA, false);
        addRenderableWidget(Button.builder(Component.translatable("gui.discyupgrade.disco_spin"),
                b -> DiscyUpgradeNetworking.sendToggleDiscoSpin(menu.getControllerPos(), !menu.isDiscoSpinEnabled()))
                .bounds(leftPos + 120, topPos + imageHeight - 74, 80, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.discyupgrade.toggle_discos"),
                b -> DiscyUpgradeNetworking.sendToggleAllDiscos(menu.getControllerPos()))
                .bounds(leftPos + 204, topPos + imageHeight - 74, 80, 18).build());
    }

    private int drawLightSection(GuiGraphics g, int y, String key, List<LightControllerMenu.LightView> lights, boolean disco) {
        g.drawString(font, Component.translatable(key), leftPos + 10, y, 0xFFCCCCCC, false);
        y += 14;
        for (var light : lights) {
            String line = light.pos().getX() + "," + light.pos().getY() + "," + light.pos().getZ()
                    + (light.active() ? " [ON]" : " [OFF]");
            g.drawString(font, line, leftPos + 14, y, light.active() ? 0xFF88FF88 : 0xFF888888, false);
            y += 12;
        }
        return y + 6;
    }

    private void renderGroups(GuiGraphics g) {
        int y = topPos + 30;
        g.drawString(font, Component.translatable("gui.discyupgrade.manage_groups"), leftPos + 10, y, 0xFFCCCCCC, false);
        y += 14;
        for (var group : menu.getFloorGroups()) {
            g.drawString(font, group.name() + " (" + group.tiles().size() + ")", leftPos + 14, y, 0xFFE0E0E0, false);
            y += 12;
        }
        if (!menu.getPresetNames().isEmpty()) {
            y += 6;
            g.drawString(font, Component.translatable("gui.discyupgrade.presets"), leftPos + 10, y, 0xFFCCCCCC, false);
            y += 12;
            for (String p : menu.getPresetNames()) {
                g.drawString(font, "- " + p, leftPos + 14, y, 0xFFBBBBBB, false);
                y += 11;
            }
        }
        y += 8;
        for (int i = 0; i < 3; i++) {
            g.drawString(font, Component.translatable("gui.discyupgrade.wrench_help_" + (i + 1)), leftPos + 10, y, 0xFF999999, false);
            y += 11;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (activeTab == Tab.FLOORS) {
            if (clickFloorTab(mx, my)) return true;
            if (colorPicker.mouseClicked(mx, my, button)) return true;
            if (button == 0 && selectTileAt(mx, my)) return true;
        }
        if (activeTab == Tab.LIGHTS && button == 0) {
            if (clickLightRow(mx, my, menu.getDiscoBalls(), true)) return true;
            if (clickLightRow(mx, my, menu.getLasers(), false)) return true;
            if (clickLightRow(mx, my, menu.getPartyLights(), false)) return true;
            if (clickLightRow(mx, my, menu.getStrobes(), false)) return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private boolean clickFloorTab(double mx, double my) {
        int gy = topPos + 26, gx = leftPos + 10, i = 0;
        for (var group : menu.getFloorGroups()) {
            int bx = gx + i * 62;
            if (mx >= bx && mx < bx + 60 && my >= gy + 12 && my < gy + 28) {
                activeGroupId = group.id();
                selectedRelX = selectedRelZ = -1;
                DiscyUpgradeNetworking.sendSelectFloorGroup(menu.getControllerPos(), activeGroupId);
                syncAnimFromGroup();
                reloadColors();
                layoutGrid();
                syncGroupNameField();
                return true;
            }
            i++;
        }
        return false;
    }

    private boolean clickLightRow(double mx, double my, List<LightControllerMenu.LightView> lights, boolean disco) {
        int y = topPos + 44;
        y += 14; // skip first header - approximate
        for (var light : lights) {
            if (my >= y && my < y + 12 && mx >= leftPos + 10 && mx < leftPos + 200) {
                if (disco) DiscyUpgradeNetworking.sendToggleDisco(menu.getControllerPos(), light.pos());
                else if (lights == menu.getLasers()) DiscyUpgradeNetworking.sendToggleLaser(menu.getControllerPos(), light.pos());
                else if (lights == menu.getPartyLights()) DiscyUpgradeNetworking.sendToggleParty(menu.getControllerPos(), light.pos());
                else DiscyUpgradeNetworking.sendToggleStrobe(menu.getControllerPos(), light.pos());
                DiscyUpgradeNetworking.sendRefresh(menu.getControllerPos());
                return true;
            }
            y += 12;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (activeTab == Tab.FLOORS && colorPicker.mouseDragged(mx, my)) return true;
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        colorPicker.mouseReleased();
        return super.mouseReleased(mx, my, button);
    }

    private boolean selectTileAt(double mx, double my) {
        for (var g : menu.getFloorGroups()) {
            if (!g.id().equals(activeGroupId)) continue;
            for (var tile : g.tiles()) {
                int x = gridOriginX + tile.relX() * tileSize, y = gridOriginY + tile.relZ() * tileSize;
                if (mx >= x && mx < x + tileSize - 1 && my >= y && my < y + tileSize - 1) {
                    selectedRelX = tile.relX();
                    selectedRelZ = tile.relZ();
                    colorPicker.setSelectedColor(localColors.getOrDefault(pack(selectedRelX, selectedRelZ), tile.color()));
                    return true;
                }
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

    private static long pack(int x, int z) { return ((long) x << 32) | (z & 0xFFFFFFFFL); }
    private static String truncate(String s, int max) { return s.length() <= max ? s : s.substring(0, max); }
}
