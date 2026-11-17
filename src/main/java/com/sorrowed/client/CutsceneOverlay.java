package com.sorrowed.client;

import net.minecraft.client.gui.GuiGraphics;

/** Letterbox bars and fades for cutscenes. */
public class CutsceneOverlay {
    public static void render(GuiGraphics g, int w, int h) {
        if (!CutsceneClient.overlayNeeded()) return;

        int bar = (int) (h * 0.12F * CutsceneClient.bars());
        if (bar > 0) {
            g.fill(0, 0, w, bar, 0xFF000000);
            g.fill(0, h - bar, w, h, 0xFF000000);
        }
        int a = (int) (CutsceneClient.fade() * 255.0F);
        if (a > 0) {
            g.fill(0, 0, w, h, (Math.min(255, a) << 24));
        }
    }
}
