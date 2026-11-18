package com.sorrowed.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * The way between worlds. The picture you are looking at comes apart: it is sliced into bands
 * that slide and smear sideways, the colours split, the edges draw in, until there is only a
 * dark wash. The world changes underneath. Then it all settles back into place.
 *
 * The effect redraws the frame that was just rendered (copied out of the main framebuffer) in
 * displaced strips, so it is a true distortion of the view, not something painted over it.
 */
public final class DistortionOverlay {
    private DistortionOverlay() {}

    private static int in;
    private static int hold;
    private static int out;
    private static int tick = -1;   // -1: idle
    private static RenderTarget copy;

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
        if (tick == 1) mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BEACON_DEACTIVATE, 0.4F, 1.0F));
        if (tick == in + hold) mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BEACON_ACTIVATE, 1.2F, 0.8F));
        if (tick >= in + hold + out) tick = -1;
    }

    /** 0..1: how far the picture has come apart. Eased so it never jumps. */
    private static float strength(float partial) {
        float t = tick + partial;
        float s;
        if (t < in) s = t / in;
        else if (t < in + hold) s = 1.0F;
        else s = Math.max(0.0F, 1.0F - (t - in - hold) / out);
        return s * s * (3.0F - 2.0F * s);
    }

    private static void grab(Minecraft mc) {
        RenderTarget main = mc.getMainRenderTarget();
        if (copy == null || copy.width != main.width || copy.height != main.height) {
            if (copy != null) copy.destroyBuffers();
            copy = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
        }
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, copy.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, copy.width, copy.height,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        main.bindWrite(true);
    }

    public static void render(GuiGraphics g, int w, int h) {
        if (tick < 0) return;
        Minecraft mc = Minecraft.getInstance();
        float s = strength(mc.getPartialTick());
        if (s <= 0.0F) return;
        float time = (tick + mc.getPartialTick()) * 0.08F;

        grab(mc);
        Matrix4f m = g.pose().last().pose();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, copy.getColorTextureId());
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();

        int bands = 64;
        float bandH = h / (float) bands;
        float amp = w * 0.18F * s * s;
        float split = (3.0F + 18.0F * s) * s;
        // Pass one replaces the picture with its displaced self; passes two and three add faint,
        // offset red and blue copies so the colours fringe apart.
        for (int pass = 0; pass < 3; pass++) {
            if (pass == 0) {
                RenderSystem.disableBlend();
            } else {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            }
            float r = pass == 1 ? 0.35F : pass == 0 ? 1.0F : 0.0F;
            float gg = pass == 0 ? 1.0F : 0.0F;
            float b = pass == 2 ? 0.35F : pass == 0 ? 1.0F : 0.0F;
            float offset = pass == 1 ? -split : pass == 2 ? split : 0.0F;
            BufferBuilder buf = Tesselator.getInstance().getBuilder();
            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            for (int i = 0; i < bands; i++) {
                float y0 = i * bandH;
                float y1 = y0 + bandH;
                float f = i / (float) bands;
                // Each band drifts with a slow wave plus a faster wobble; the middle bands tear hardest.
                float wave = Mth.sin(f * 9.0F + time * 2.1F) * 0.55F + Mth.sin(f * 31.0F - time * 5.3F) * 0.3F
                        + Mth.sin(f * 73.0F + time * 11.0F) * 0.15F;
                float shift = wave * amp * (0.6F + 0.4F * Mth.sin(f * Mth.PI)) + offset;
                // Vertical smear: bands stretch a little as the picture lets go.
                float stretch = 1.0F + 0.5F * s * Mth.sin(f * 17.0F + time * 3.0F);
                float v0 = 1.0F - f;
                float v1 = v0 - (bandH / h) * stretch;
                band(buf, m, -w, w * 2, y0, y1, -0.5F, 1.5F, v0, v1, shift, r, gg, b, 1.0F);
            }
            BufferUploader.drawWithShader(buf.end());
        }
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();

        // Then the edges close in and the rest goes to a dark wash so the change of world is hidden.
        float dark = Mth.clamp((s - 0.55F) / 0.45F, 0.0F, 1.0F);
        int a = (int) (255.0F * dark);
        if (a > 0) g.fill(0, 0, w, h, (a << 24) | 0x07070B);
        int vig = (int) (170.0F * s);
        int bandsV = 12;
        int bw = Math.max(2, w / 40);
        int bh = Math.max(2, h / 24);
        for (int i = 0; i < bandsV; i++) {
            double fall = (bandsV - i) / (double) bandsV;
            int va = (int) (vig * fall * fall);
            if (va < 3) continue;
            int color = (va << 24) | 0x05050A;
            g.fill(0, i * bh, w, (i + 1) * bh, color);
            g.fill(0, h - (i + 1) * bh, w, h - i * bh, color);
            g.fill(i * bw, 0, (i + 1) * bw, h, color);
            g.fill(w - (i + 1) * bw, 0, w - i * bw, h, color);
        }
    }

    private static void band(BufferBuilder buf, Matrix4f m, float x0, float x1, float y0, float y1,
                             float u0, float u1, float v0, float v1, float shift, float r, float g, float b, float a) {
        buf.vertex(m, x0 + shift, y1, 0.0F).uv(u0, v1).color(r, g, b, a).endVertex();
        buf.vertex(m, x1 + shift, y1, 0.0F).uv(u1, v1).color(r, g, b, a).endVertex();
        buf.vertex(m, x1 + shift, y0, 0.0F).uv(u1, v0).color(r, g, b, a).endVertex();
        buf.vertex(m, x0 + shift, y0, 0.0F).uv(u0, v0).color(r, g, b, a).endVertex();
    }
}
