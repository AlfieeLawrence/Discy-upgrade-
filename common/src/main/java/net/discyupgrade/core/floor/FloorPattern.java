package net.discyupgrade.core.floor;

import net.minecraft.util.Mth;

public enum FloorPattern {
    STATIC,
    RAINBOW_CHASE,
    PULSE,
    WAVE,
    SPARKLE;

    public static FloorPattern fromId(int id) {
        FloorPattern[] values = values();
        return values[Mth.clamp(id, 0, values.length - 1)];
    }

    public int id() {
        return ordinal();
    }

    public int computeColor(int baseColor, int relX, int relZ, int tick, int speed) {
        if (this == STATIC) return baseColor;
        int br = (baseColor >> 16) & 0xFF;
        int bg = (baseColor >> 8) & 0xFF;
        int bb = baseColor & 0xFF;
        int phase = tick * Math.max(1, speed);
        return switch (this) {
            case RAINBOW_CHASE -> hsv((phase + relX * 18 + relZ * 18) % 360, 0.85f, 0.95f);
            case PULSE -> {
                float wave = (Mth.sin(phase * 0.12f) + 1f) * 0.5f;
                yield scale(baseColor, 0.35f + wave * 0.65f);
            }
            case WAVE -> {
                float wave = (Mth.sin(phase * 0.15f + relX * 0.55f + relZ * 0.55f) + 1f) * 0.5f;
                yield blend(baseColor, hsv((phase * 3) % 360, 0.7f, 0.9f), wave * 0.65f);
            }
            case SPARKLE -> {
                int hash = (relX * 734287 ^ relZ * 912931 ^ phase) & 0xFF;
                if (hash > 245 - speed * 8) yield 0xFFFFFF;
                yield scale(baseColor, 0.55f + (hash / 255f) * 0.45f);
            }
            default -> baseColor;
        };
    }

    private static int scale(int rgb, float factor) {
        int r = Mth.clamp((int) (((rgb >> 16) & 0xFF) * factor), 0, 255);
        int g = Mth.clamp((int) (((rgb >> 8) & 0xFF) * factor), 0, 255);
        int b = Mth.clamp((int) ((rgb & 0xFF) * factor), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    private static int blend(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static int hsv(int hue, float sat, float val) {
        float h = (hue % 360) / 360f;
        int i = (int) (h * 6f);
        float f = h * 6f - i;
        float p = val * (1f - sat);
        float q = val * (1f - sat * f);
        float t = val * (1f - sat * (1f - f));
        float r, g, b;
        switch (i % 6) {
            case 0 -> { r = val; g = t; b = p; }
            case 1 -> { r = q; g = val; b = p; }
            case 2 -> { r = p; g = val; b = t; }
            case 3 -> { r = p; g = q; b = val; }
            case 4 -> { r = t; g = p; b = val; }
            default -> { r = val; g = p; b = q; }
        }
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }
}
