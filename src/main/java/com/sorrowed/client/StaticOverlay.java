package com.sorrowed.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Random;

/** Television static over the view. The Carrier pushes it; it fades on its own. */
public final class StaticOverlay {
    private StaticOverlay() {}

    private static float strength;

    public static void push(float s) {
        strength = Math.max(strength, Math.min(1.0F, s));
    }

    public static void tick() {
        strength = Math.max(0.0F, strength - 0.02F);
    }

    public static void clear() {
        strength = 0.0F;
    }

    public static void render(GuiGraphics g, int w, int h) {
        if (strength <= 0.01F) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        int cell = 3;
        int cols = w / cell + 1;
        int rows = h / cell + 1;
        Random rnd = new Random(mc.level.getGameTime() * 31L + (long) (mc.getPartialTick() * 7.0F));
        int alpha = (int) (170.0F * strength);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (rnd.nextFloat() > strength * 0.6F) continue;
                int v = rnd.nextInt(256);
                g.fill(c * cell, r * cell, c * cell + cell, r * cell + cell, (alpha << 24) | (v << 16) | (v << 8) | v);
            }
        }
        // Rolling bars, the way an old set loses sync.
        int bar = (int) ((mc.level.getGameTime() * 6L) % (h + 40L)) - 40;
        g.fill(0, bar, w, bar + 6, ((int) (90 * strength) << 24) | 0xFFFFFF);
    }
}
