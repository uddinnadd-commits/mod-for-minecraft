package com.sorrowed.client;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.util.Random;

/**
 * A scribbled mess that covers the screen: pencil strokes pile up until nothing is left, the
 * world changes underneath, and then the strokes are scratched away again.
 */
public final class ScribbleOverlay {
    private ScribbleOverlay() {}

    private static final int MAX_STROKES = 900;
    private static int in;
    private static int hold;
    private static int out;
    private static int tick = -1;   // -1: idle

    public static void start(int inTicks, int holdTicks, int outTicks) {
        in = Math.max(1, inTicks);
        hold = Math.max(0, holdTicks);
        out = Math.max(1, outTicks);
        tick = 0;
    }

    public static boolean active() {
        return tick >= 0;
    }

    public static void clear() {
        tick = -1;
    }

    public static void tick() {
        if (tick < 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) return;
        tick++;
        if (tick < in && tick % 4 == 0) {
            float pitch = 1.3F + (tick % 12) * 0.05F;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, pitch, 0.6F));
        }
        if (tick >= in + hold + out) tick = -1;
    }

    /** 0..1: how much of the screen is covered. */
    private static float cover(float partial) {
        float t = tick + partial;
        if (t < in) return t / in;
        if (t < in + hold) return 1.0F;
        return Math.max(0.0F, 1.0F - (t - in - hold) / out);
    }

    public static void render(GuiGraphics g, int w, int h) {
        if (tick < 0) return;
        Minecraft mc = Minecraft.getInstance();
        float c = cover(mc.getPartialTick());
        if (c <= 0.0F) return;

        // Strokes arrive fast at first and crowd in toward the end; each one is the same every frame.
        int strokes = (int) (MAX_STROKES * Math.pow(c, 1.6D));
        Random rnd = new Random(1337L);
        for (int i = 0; i < strokes; i++) {
            float x = rnd.nextFloat() * w;
            float y = rnd.nextFloat() * h;
            float len = 30.0F + rnd.nextFloat() * Math.max(w, h) * 0.45F;
            float angle = rnd.nextFloat() * 360.0F;
            int thick = 1 + rnd.nextInt(3);
            int shade = 0x10 + rnd.nextInt(0x18);
            int color = 0xFF000000 | (shade << 16) | (shade << 8) | (shade + 6);
            g.pose().pushPose();
            g.pose().translate(x, y, 0.0F);
            g.pose().mulPose(Axis.ZP.rotationDegrees(angle));
            // A stroke is a few short segments that drift, like a hand that cannot keep a line.
            float px = 0.0F;
            float py = 0.0F;
            int segs = 3 + rnd.nextInt(4);
            for (int s = 0; s < segs; s++) {
                float seg = len / segs;
                float drift = (rnd.nextFloat() - 0.5F) * 10.0F;
                g.pose().pushPose();
                g.pose().translate(px, py, 0.0F);
                g.pose().mulPose(Axis.ZP.rotationDegrees(drift));
                g.fill(0, -thick / 2, (int) seg, thick - thick / 2, color);
                g.pose().popPose();
                px += seg * (float) Math.cos(Math.toRadians(drift));
                py += seg * (float) Math.sin(Math.toRadians(drift));
            }
            g.pose().popPose();
        }
        // The last of the white goes under a flat wash of graphite.
        if (c > 0.7F) {
            int a = (int) (255.0F * Math.min(1.0F, (c - 0.7F) / 0.25F));
            g.fill(0, 0, w, h, (a << 24) | 0x14141A);
        }
    }
}
