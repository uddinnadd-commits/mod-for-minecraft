package com.sorrowed.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** A darkening, pulsing vignette that closes in as Sorrow rises, plus a little static at the very end. */
public class SorrowOverlay {
    public static void render(GuiGraphics g, int w, int h) {
        int s = ClientState.sorrow;
        if (s < 25) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.level == null) return;

        float t = (s - 25) / 75.0F;
        float pulse = 0.85F + 0.15F * (float) Math.sin(mc.level.getGameTime() * 0.12D);
        int maxAlpha = (int) (200.0F * t * t * pulse);

        int bands = 10;
        int bw = Math.max(2, w / 48);
        int bh = Math.max(2, h / 28);
        for (int i = 0; i < bands; i++) {
            double fall = (bands - i) / (double) bands;
            int a = (int) (maxAlpha * fall * fall);
            if (a < 4) continue;
            int color = (a << 24) | 0x05050C;
            g.fill(0, i * bh, w, (i + 1) * bh, color);
            g.fill(0, h - (i + 1) * bh, w, h - i * bh, color);
            g.fill(i * bw, 0, (i + 1) * bw, h, color);
            g.fill(w - (i + 1) * bw, 0, w - i * bw, h, color);
        }

        // Static: a few thin pale lines for two ticks out of every forty.
        if (s >= 80 && mc.level.getGameTime() % 40L < 2L) {
            for (int i = 0; i < 4; i++) {
                int y = mc.level.random.nextInt(Math.max(1, h));
                g.fill(0, y, w, y + 1, 0x30FFFFFF);
            }
        }
    }
}
