package net.discyupgrade.core.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Locale;
import java.util.function.IntConsumer;

/**
 * DDS-inspired color picker panel (wheel, brightness, quick palette, hex).
 */
public class ColorPickerPanel {
    private static final int[] QUICK_PALETTE = {
            0xFF000000, 0xFFFFFFFF, 0xFF9E9E9E, 0xFFD32F2F,
            0xFFFF9800, 0xFF388E3C, 0xFF1976D2, 0xFF9C27B0
    };

    private final net.minecraft.client.gui.Font font;
    private final IntConsumer onColorChanged;

    private int panelX;
    private int panelY;
    private int panelW;
    private int wheelRadius = 34;
    private int wheelCx;
    private int wheelCy;
    private int brightnessY;

    private int selectedColor = 0xFF000000;
    private float colorHue;
    private float colorSat;
    private float colorVal = 1f;
    private boolean pickingWheel;
    private boolean pickingBrightness;
    private boolean syncingHex;

    private EditBox hexField;

    public ColorPickerPanel(net.minecraft.client.gui.Font font, IntConsumer onColorChanged) {
        this.font = font;
        this.onColorChanged = onColorChanged;
    }

    public void setBounds(int x, int y, int width) {
        panelX = x;
        panelY = y;
        panelW = width;
        wheelCx = panelX + panelW / 2;
        wheelCy = panelY + wheelRadius + 18;
        brightnessY = wheelCy + wheelRadius + 10;
        if (hexField != null) {
            hexField.setX(panelX + 4);
            hexField.setY(brightnessY + 16);
            hexField.setWidth(panelW - 8);
        }
    }

    public EditBox createHexField() {
        hexField = new EditBox(font, panelX + 4, brightnessY + 16, panelW - 8, 18, Component.literal("Hex"));
        hexField.setMaxLength(7);
        hexField.setHint(Component.literal("#RRGGBB"));
        hexField.setResponder(this::onHexChanged);
        syncHexField();
        return hexField;
    }

    public int getSelectedColor() {
        return selectedColor & 0xFFFFFF;
    }

    public void setSelectedColor(int rgb) {
        setColorFromArgb(0xFF000000 | (rgb & 0xFFFFFF));
    }

    public void render(GuiGraphics g) {
        g.drawString(font, Component.translatable("gui.discyupgrade.colors"), panelX + 4, panelY + 4, 0xFFCCCCCC, false);
        renderColorWheel(g);
        renderBrightness(g);

        int swatch = 12;
        int swatchY = brightnessY + 38;
        for (int i = 0; i < QUICK_PALETTE.length; i++) {
            int sx = panelX + 4 + i * (swatch + 2);
            int color = QUICK_PALETTE[i];
            g.fill(sx - 1, swatchY - 1, sx + swatch + 1, swatchY + swatch + 1, 0xFF555555);
            g.fill(sx, swatchY, sx + swatch, swatchY + swatch, color | 0xFF000000);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (handleWheelClick(mouseX, mouseY)) return true;
        if (handleBrightnessClick(mouseX, mouseY)) return true;
        return handleSwatchClick(mouseX, mouseY);
    }

    public boolean mouseDragged(double mouseX, double mouseY) {
        if (pickingWheel) {
            pickFromWheel(mouseX, mouseY);
            return true;
        }
        if (pickingBrightness) {
            pickBrightness(mouseX, mouseY);
            return true;
        }
        return false;
    }

    public void mouseReleased() {
        pickingWheel = false;
        pickingBrightness = false;
    }

    private void renderColorWheel(GuiGraphics g) {
        int r = wheelRadius;
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                if (dx * dx + dy * dy > r * r) continue;
                float hue = (float) ((Math.atan2(dy, dx) / (Math.PI * 2) + 1.0) % 1.0);
                float sat = (float) (Math.sqrt(dx * dx + dy * dy) / r);
                int color = hsvToArgb(hue, sat, 1f);
                g.fill(wheelCx + dx, wheelCy + dy, wheelCx + dx + 1, wheelCy + dy + 1, color);
            }
        }
        if (colorSat > 0.01f) {
            int mx = wheelCx + (int) (Math.cos(colorHue * Math.PI * 2) * colorSat * r);
            int my = wheelCy + (int) (Math.sin(colorHue * Math.PI * 2) * colorSat * r);
            g.fill(mx - 2, my - 2, mx + 3, my + 3, 0xFFFFFFFF);
            g.fill(mx - 1, my - 1, mx + 2, my + 2, 0xFF000000);
        }
    }

    private void renderBrightness(GuiGraphics g) {
        int x0 = panelX + 4;
        int x1 = panelX + panelW - 4;
        int steps = x1 - x0;
        for (int i = 0; i < steps; i++) {
            float v = i / (float) (steps - 1);
            g.fill(x0 + i, brightnessY, x0 + i + 1, brightnessY + 10, hsvToArgb(colorHue, colorSat, v));
        }
        int markerX = x0 + (int) (colorVal * (steps - 1));
        g.fill(markerX - 1, brightnessY - 2, markerX + 2, brightnessY + 12, 0xFFFFFFFF);
    }

    private boolean handleWheelClick(double mouseX, double mouseY) {
        double dx = mouseX - wheelCx;
        double dy = mouseY - wheelCy;
        if (dx * dx + dy * dy > wheelRadius * wheelRadius) return false;
        pickingWheel = true;
        pickFromWheel(mouseX, mouseY);
        return true;
    }

    private boolean handleBrightnessClick(double mouseX, double mouseY) {
        int x0 = panelX + 4;
        int x1 = panelX + panelW - 4;
        if (mouseY < brightnessY || mouseY > brightnessY + 10 || mouseX < x0 || mouseX > x1) return false;
        pickingBrightness = true;
        pickBrightness(mouseX, mouseY);
        return true;
    }

    private boolean handleSwatchClick(double mouseX, double mouseY) {
        int swatch = 12;
        int swatchY = brightnessY + 38;
        for (int i = 0; i < QUICK_PALETTE.length; i++) {
            int sx = panelX + 4 + i * (swatch + 2);
            if (mouseX >= sx && mouseX < sx + swatch && mouseY >= swatchY && mouseY < swatchY + swatch) {
                setColorFromArgb(QUICK_PALETTE[i]);
                return true;
            }
        }
        return false;
    }

    private void pickFromWheel(double mouseX, double mouseY) {
        double dx = mouseX - wheelCx;
        double dy = mouseY - wheelCy;
        colorHue = (float) ((Math.atan2(dy, dx) / (Math.PI * 2) + 1.0) % 1.0);
        colorSat = (float) (Math.min(1.0, Math.sqrt(dx * dx + dy * dy) / wheelRadius));
        applyHsv();
    }

    private void pickBrightness(double mouseX, double mouseY) {
        int x0 = panelX + 4;
        int x1 = panelX + panelW - 4;
        colorVal = Mth.clamp((float) ((mouseX - x0) / (x1 - x0)), 0f, 1f);
        applyHsv();
    }

    private void applyHsv() {
        selectedColor = hsvToArgb(colorHue, colorSat, colorVal);
        syncHexField();
        onColorChanged.accept(getSelectedColor());
    }

    private void setColorFromArgb(int argb) {
        selectedColor = argb;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        float max = Math.max(r, Math.max(g, b)) / 255f;
        float min = Math.min(r, Math.min(g, b)) / 255f;
        colorVal = max;
        if (max <= 0.001f) {
            colorHue = 0f;
            colorSat = 0f;
        } else {
            colorSat = (max - min) / max;
            if (colorSat <= 0.001f) {
                colorHue = 0f;
            } else if (max == r / 255f) {
                colorHue = ((g - b) / 255f / (max - min)) / 6f;
            } else if (max == g / 255f) {
                colorHue = (2f + (b - r) / 255f / (max - min)) / 6f;
            } else {
                colorHue = (4f + (r - g) / 255f / (max - min)) / 6f;
            }
            if (colorHue < 0f) colorHue += 1f;
        }
        syncHexField();
        onColorChanged.accept(getSelectedColor());
    }

    private void onHexChanged(String text) {
        if (syncingHex) return;
        String cleaned = text.trim();
        if (!cleaned.startsWith("#")) cleaned = "#" + cleaned;
        if (cleaned.length() != 7) return;
        try {
            int rgb = Integer.parseInt(cleaned.substring(1), 16);
            setColorFromArgb(0xFF000000 | rgb);
        } catch (NumberFormatException ignored) {
        }
    }

    private void syncHexField() {
        if (hexField == null) return;
        syncingHex = true;
        hexField.setValue(String.format("#%06X", getSelectedColor()));
        syncingHex = false;
    }

    private static int hsvToArgb(float h, float s, float v) {
        int rgb = hsvToRgb(h, s, v);
        return 0xFF000000 | rgb;
    }

    private static int hsvToRgb(float h, float s, float v) {
        if (s <= 0.001f) {
            int c = (int) (v * 255);
            return (c << 16) | (c << 8) | c;
        }
        float hf = h * 6f;
        int i = (int) hf;
        float f = hf - i;
        float p = v * (1f - s);
        float q = v * (1f - s * f);
        float t = v * (1f - s * (1f - f));
        float r, g, b;
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }
}
