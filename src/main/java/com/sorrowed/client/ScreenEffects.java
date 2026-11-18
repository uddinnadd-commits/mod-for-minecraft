package com.sorrowed.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sorrowed.Sorrowed;
import com.sorrowed.SorrowedConfig;
import com.sorrowed.registry.Dimensions;
import com.sorrowed.story.Objectives;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/**
 * The look of each world on top of the picture: a coloured vignette that closes in as Sorrow
 * rises (and beats with your heart near the end), a little film grain, and the flourish when you
 * finish something: a soft white bloom, and what you did written large and struck through.
 */
public final class ScreenEffects {
    private ScreenEffects() {}

    private static final ResourceLocation VIGNETTE = new ResourceLocation(Sorrowed.MODID, "textures/gui/vignette.png");
    private static final ResourceLocation GRAIN = new ResourceLocation(Sorrowed.MODID, "textures/gui/grain.png");

    private static Objectives.Objective memory;
    private static long memoryAt = -1L;
    private static boolean scratched;

    /** An objective was just finished. */
    public static void memory(Objectives.Objective o) {
        memory = o;
        memoryAt = Util.getMillis();
        scratched = false;
    }

    public static void clear() {
        memory = null;
        memoryAt = -1L;
    }

    /** r, g, b, base vignette strength, grain strength. */
    private static float[] look(ResourceKey<Level> dim) {
        boolean after = ClientStory.stage != ClientStory.PLAYING;
        if (dim.equals(Dimensions.BACKUP)) {
            if (ClientStory.stage == ClientStory.FIGHTING) return new float[] {0.30F, 0.0F, 0.04F, 0.62F, 0.09F};
            return after ? new float[] {1.0F, 0.80F, 0.55F, 0.30F, 0.025F} : new float[] {1.0F, 0.96F, 0.86F, 0.22F, 0.02F};
        }
        if (dim.equals(Dimensions.HUSH)) return new float[] {0.0F, 0.02F, 0.06F, 0.62F, 0.07F};
        if (dim.equals(Dimensions.DROWNED)) return new float[] {0.0F, 0.10F, 0.12F, 0.55F, 0.05F};
        if (dim.equals(Dimensions.HALLWAY)) return new float[] {0.20F, 0.15F, 0.02F, 0.48F, 0.05F};
        if (dim.equals(Dimensions.ARCHIVE)) return new float[] {0.10F, 0.06F, 0.02F, 0.55F, 0.05F};
        if (dim.equals(Dimensions.STREET)) return new float[] {0.02F, 0.02F, 0.09F, 0.55F, 0.07F};
        if (dim.equals(Dimensions.STATIC)) return new float[] {0.45F, 0.45F, 0.45F, 0.32F, 0.16F};
        if (dim.equals(Dimensions.ORCHARD)) return new float[] {0.20F, 0.22F, 0.20F, 0.45F, 0.05F};
        if (dim.equals(Dimensions.WARD)) return new float[] {0.22F, 0.30F, 0.26F, 0.38F, 0.04F};
        if (dim.equals(Dimensions.SERVER)) return new float[] {0.0F, 0.08F, 0.04F, 0.52F, 0.05F};
        // Home: cold at the edges in the rain, almost nothing once it has stopped.
        return ClientStory.stage == ClientStory.ENDED
                ? new float[] {0.15F, 0.12F, 0.08F, 0.16F, 0.015F}
                : new float[] {0.03F, 0.05F, 0.08F, 0.34F, 0.035F};
    }

    public static void render(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (SorrowedConfig.SCREEN_EFFECTS.get()) {
            float[] look = look(mc.level.dimension());
            float sorrow = ClientState.sorrow / 100.0F;
            float t = (mc.level.getGameTime() + mc.getFrameTime()) / 20.0F;
            float strength = look[3] + 0.32F * sorrow;
            if (ClientState.sorrow >= 80) {
                // Lub-dub, a little over once a second.
                float phase = (t * 1.15F) % 1.0F;
                float beat = (float) (Math.exp(-Math.pow((phase - 0.08F) * 22.0F, 2)) + 0.6D * Math.exp(-Math.pow((phase - 0.28F) * 22.0F, 2)));
                strength += 0.16F * beat;
            }
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(look[0], look[1], look[2], Mth.clamp(strength, 0.0F, 0.95F));
            g.blit(VIGNETTE, 0, 0, w, h, 0.0F, 0.0F, 256, 256, 256, 256);

            float grain = look[4] + 0.05F * sorrow;
            if (grain > 0.005F) {
                long frame = Util.getMillis() / 42L;
                int ox = (int) Math.floorMod(frame * 37L, 64L);
                int oy = (int) Math.floorMod(frame * 53L, 64L);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, grain);
                g.blit(GRAIN, 0, 0, ox, oy, w, h, 64, 64);
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
        renderMemory(g, w, h, mc);
    }

    private static void renderMemory(GuiGraphics g, int w, int h, Minecraft mc) {
        if (memory == null || memoryAt < 0L) return;
        float s = (Util.getMillis() - memoryAt) / 1000.0F;
        if (s > 5.0F) {
            memory = null;
            return;
        }
        // The bloom.
        if (s < 1.4F) {
            float b = 1.0F - s / 1.4F;
            int a = (int) (150.0F * b * b);
            if (a > 3) g.fill(0, 0, w, h, (a << 24) | 0xFFF6E8);
        }
        if (CutsceneClient.isActive()) return;
        Font font = mc.font;
        String text = memory.task();
        String sub = memory.place();
        float in = Mth.clamp(s / 0.35F, 0.0F, 1.0F);
        float out = s > 3.6F ? Mth.clamp(1.0F - (s - 3.6F) / 1.2F, 0.0F, 1.0F) : 1.0F;
        float a = in * out;
        if (a <= 0.03F) return;
        float scale = 2.0F;
        int tw = font.width(text);
        int cy = (int) (h * 0.30F);
        g.pose().pushPose();
        g.pose().translate(w / 2.0F, cy, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        int color = ((int) (a * 255.0F) << 24) | 0xF2EADA;
        g.drawString(font, text, -tw / 2, 0, color, true);
        // The pencil, crossing it out, left to right.
        float strike = Mth.clamp((s - 0.55F) / 0.7F, 0.0F, 1.0F);
        if (strike > 0.0F) {
            if (!scratched) {
                scratched = true;
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.0F, 0.9F));
            }
            int x0 = -tw / 2 - 3;
            int len = (int) ((tw + 6) * strike);
            for (int i = 0; i < len; i++) {
                int wob = (int) Math.round(Math.sin((x0 + i) * 0.35D) * 0.6D);
                g.fill(x0 + i, 4 + wob, x0 + i + 1, 5 + wob, color);
            }
        }
        g.pose().popPose();
        int sw = font.width(sub);
        g.drawString(font, sub, (w - sw) / 2, cy + 22, ((int) (a * 200.0F) << 24) | 0xB8B2A6, true);
    }
}
