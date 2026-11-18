package com.sorrowed.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sorrowed.Sorrowed;
import com.sorrowed.story.Thoughts;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * The end. Everything goes white, the colour of his paper, and his last letter writes itself out
 * a line at a time while the music plays. Then the name of the thing, and thank you. While the
 * page is up, you are taken home.
 */
public class EndingScreen extends Screen {
    private static final ResourceLocation GRAIN = new ResourceLocation(Sorrowed.MODID, "textures/gui/grain.png");
    private static final int PAPER = 0xF2EEE6;
    private static final int INK = 0x2A2630;
    private static final float WHITE_IN = 2.5F;
    private static final float FIRST = 3.2F;
    private static final float EACH = 5.2F;
    private static final float TYPE_RATE = 26.0F;

    private static final String[] LETTER = {
            "you went into every one of them.",
            "the hallway. the water. the trees that weren't mine. the books, my street, the noise.",
            "grandad's orchard. the ward. the room where they kept it all running.",
            "i didn't want you to have to. i'm glad it was you.",
            "it's the day after now. it was always going to be.",
            "go home. it's stopped raining.",
            "- {friend}",
    };

    private final long opened = Util.getMillis();
    private SoundInstance music;
    private boolean closed;

    public EndingScreen() {
        super(Component.literal("the end"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private float t() {
        return (Util.getMillis() - opened) / 1000.0F;
    }

    private float letterEnd() {
        return FIRST + LETTER.length * EACH + 2.0F;
    }

    private float titleAt() {
        return letterEnd() + 1.6F;
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (music == null && t() > 1.4F) {
            mc.getMusicManager().stopPlaying();
            music = SimpleSoundInstance.forMusic(SoundEvents.MUSIC_CREDITS.value());
            mc.getSoundManager().play(music);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float t = t();
        int w = this.width;
        int h = this.height;
        float white = Mth.clamp(t / WHITE_IN, 0.0F, 1.0F);
        white = white * white * (3.0F - 2.0F * white);
        g.fill(0, 0, w, h, ((int) (white * 255.0F) << 24) | PAPER);
        if (white >= 1.0F) {
            // The tooth of the paper.
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(0.55F, 0.5F, 0.45F, 0.10F);
            g.blit(GRAIN, 0, 0, 0, 0, w, h, 64, 64);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        Font f = this.font;
        if (t < titleAt()) {
            float fadeOut = t > letterEnd() ? Mth.clamp(1.0F - (t - letterEnd()) / 1.4F, 0.0F, 1.0F) : 1.0F;
            int lineH = 16;
            int total = LETTER.length * lineH;
            int y = (h - total) / 2;
            // The long lines would run off narrow or tall windows: shrink the whole letter to fit.
            int widest = 1;
            for (String l : LETTER) widest = Math.max(widest, f.width(Thoughts.f(l)));
            float fit = Math.min(1.0F, (w - 16) / (float) widest);
            g.pose().pushPose();
            g.pose().translate(w / 2.0F, h / 2.0F, 0.0F);
            g.pose().scale(fit, fit, 1.0F);
            g.pose().translate(-w / 2.0F, -h / 2.0F, 0.0F);
            for (int i = 0; i < LETTER.length; i++) {
                float start = FIRST + i * EACH;
                if (t < start) break;
                String line = Thoughts.f(LETTER[i]);
                int n = (int) Mth.clamp((t - start) * TYPE_RATE, 0, line.length());
                String shown = line.substring(0, n);
                // Older lines go a little grey, the way pencil does.
                boolean newest = i == LETTER.length - 1 || t < FIRST + (i + 1) * EACH;
                float a = (newest ? 1.0F : 0.62F) * fadeOut;
                if (a > 0.03F) {
                    int lw = f.width(line);
                    int wobble = ((i * 7) % 3) - 1;
                    g.drawString(f, shown, (w - lw) / 2 + wobble, y + i * lineH, ((int) (a * 255.0F) << 24) | INK, false);
                }
            }
            g.pose().popPose();
            return;
        }

        float s = t - titleAt();
        float a = Mth.clamp(s / 2.2F, 0.0F, 1.0F);
        if (a > 0.03F) {
            String word = "sorrowed";
            float scale = 4.0F;
            int ww = (int) (f.width(word) * scale);
            g.pose().pushPose();
            g.pose().translate((w - ww) / 2.0F, h / 2.0F - 40.0F, 0.0F);
            g.pose().scale(scale, scale, 1.0F);
            g.drawString(f, word, 0, 0, ((int) (a * 255.0F) << 24) | INK, false);
            g.pose().popPose();
        }
        float b = Mth.clamp((s - 1.6F) / 2.0F, 0.0F, 1.0F);
        if (b > 0.03F) {
            String thanks = "thank you for coming back.";
            g.drawString(f, thanks, (w - f.width(thanks)) / 2, h / 2 + 4, ((int) (b * 255.0F) << 24) | 0x5A5660, false);
        }
        float c = Mth.clamp((s - 5.0F) / 1.5F, 0.0F, 1.0F) * (0.55F + 0.45F * Mth.sin(t * 2.2F));
        if (c > 0.05F) {
            String any = "press any key";
            g.drawString(f, any, (w - f.width(any)) / 2, h - 30, ((int) (c * 200.0F) << 24) | 0x8A8690, false);
        }
    }

    private void finish() {
        if (closed) return;
        closed = true;
        EndingMemory.remember();
        Minecraft mc = Minecraft.getInstance();
        if (music != null) mc.getSoundManager().stop(music);
        com.sorrowed.net.ModNetwork.reply("ending", "");
        mc.setScreen(null);
    }

    private boolean input() {
        float t = t();
        if (t >= titleAt() + 2.5F) {
            finish();
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256 && t() > 3.0F) {   // esc skips the letter, then leaves
            if (t() < titleAt()) {
                finish();
                return true;
            }
        }
        return input();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return input();
    }

    @Override
    public void removed() {
        // However it closes, remember it ended, let the music go, and go home.
        if (!closed) {
            closed = true;
            EndingMemory.remember();
            if (music != null) Minecraft.getInstance().getSoundManager().stop(music);
            com.sorrowed.net.ModNetwork.reply("ending", "");
        }
    }
}
