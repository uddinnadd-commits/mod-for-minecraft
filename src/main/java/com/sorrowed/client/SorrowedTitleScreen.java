package com.sorrowed.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sorrowed.Sorrowed;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.ModListScreen;

import java.util.Random;

/**
 * The title screen. His house at night across a field, in the rain, one window lit. Somebody is
 * standing in the field. Every time you come back to this screen they are standing a little closer.
 */
public class SorrowedTitleScreen extends Screen {
    private static final ResourceLocation BG = new ResourceLocation(Sorrowed.MODID, "textures/gui/title_bg.png");
    private static final ResourceLocation FIGURE = new ResourceLocation(Sorrowed.MODID, "textures/gui/title_figure.png");
    private static final int BG_W = 480;
    private static final int BG_H = 270;

    private static final String[] LINES = {
            "i left the door open for you",
            "it's still raining here",
            "are you still there",
            "the backup is the day before. stay in the backup",
            "you took so long",
            "i saved it for you. every night",
            "don't go in the clinic one first",
            "someone is standing in the field. it isn't me",
    };

    /** How many times the menu has been opened this session. The figure walks closer each time. */
    private static int visits;

    private final long opened = Util.getMillis();
    private final String line;
    private long nextFlash;
    private long flashAt = -1L;
    private int ticks;

    public SorrowedTitleScreen() {
        super(Component.literal("sorrowed"));
        visits++;
        Random r = new Random(System.nanoTime());
        this.line = LINES[r.nextInt(LINES.length)];
        this.nextFlash = opened + 9000L + r.nextInt(14000);
    }

    @Override
    protected void init() {
        int x = 34;
        int y = this.height / 2 - 18;
        Minecraft mc = Minecraft.getInstance();
        addRenderableWidget(new Plain(x, y, "singleplayer", b -> mc.setScreen(new SelectWorldScreen(this))));
        addRenderableWidget(new Plain(x, y + 18, "multiplayer", b -> mc.setScreen(new JoinMultiplayerScreen(this))));
        addRenderableWidget(new Plain(x, y + 36, "mods", b -> mc.setScreen(new ModListScreen(this))));
        addRenderableWidget(new Plain(x, y + 54, "options", b -> mc.setScreen(new OptionsScreen(this, mc.options))));
        addRenderableWidget(new Plain(x, y + 80, "quit", b -> mc.stop()));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        ticks++;
        Minecraft mc = Minecraft.getInstance();
        // The rain, quietly, always.
        if (ticks % 6 == 0) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.WEATHER_RAIN, 0.8F + (ticks % 5) * 0.05F, 0.10F));
        }
        long now = Util.getMillis();
        if (now >= nextFlash) {
            flashAt = now;
            nextFlash = now + 12000L + (long) (Math.random() * 16000.0D);
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6F + (float) Math.random() * 0.2F, 0.35F));
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long now = Util.getMillis();
        float t = (now - opened) / 1000.0F;
        int w = this.width;
        int h = this.height;

        // The painting, slowly panning, a little larger than the screen.
        float cover = Math.max(w / (float) BG_W, h / (float) BG_H) * 1.12F;
        float dw = BG_W * cover;
        float dh = BG_H * cover;
        float pan = Mth.sin(t * 0.03F) * 0.5F + 0.5F;
        float ox = -(dw - w) * pan;
        float oy = -(dh - h) * 0.5F;
        RenderSystem.enableBlend();
        g.pose().pushPose();
        g.pose().translate(ox, oy, 0.0F);
        g.pose().scale(cover, cover, 1.0F);
        g.blit(BG, 0, 0, 0, 0, BG_W, BG_H, BG_W, BG_H);

        // The lit window flickers.
        float flick = 0.75F + 0.25F * Mth.sin(t * 7.3F) * Mth.sin(t * 2.1F);
        int wa = (int) (110 * flick);
        g.fill(311, 158, 317, 164, (wa << 24) | 0xFFD070);

        // The figure in the field. Further away on the first visit; closer every time after.
        int step = Math.min(visits - 1, 6);
        float fs = 0.55F + step * 0.22F;
        float fx = 140.0F + step * 18.0F;
        float fy = 196.0F + step * 9.0F;
        g.pose().pushPose();
        g.pose().translate(fx, fy - 32.0F * fs, 0.0F);
        g.pose().scale(fs, fs, 1.0F);
        g.blit(FIGURE, 0, 0, 0, 0, 16, 32, 16, 32);
        g.pose().popPose();
        g.pose().popPose();

        // Darken toward the left, where the words are.
        for (int i = 0; i < 24; i++) {
            int a = (int) (150 * (1.0F - i / 24.0F));
            g.fill(i * (w / 48), 0, (i + 1) * (w / 48), h, (a << 24));
        }

        rain(g, w, h, t);

        // Lightning: the whole screen goes white and fades.
        if (flashAt > 0L) {
            float since = (now - flashAt) / 1000.0F;
            if (since < 0.9F) {
                float a = since < 0.08F ? 0.8F : since < 0.16F ? 0.15F : since < 0.24F ? 0.6F : 0.6F * (1.0F - (since - 0.24F) / 0.66F);
                g.fill(0, 0, w, h, ((int) (Mth.clamp(a, 0.0F, 1.0F) * 200) << 24) | 0xE8EEFF);
            }
        }

        title(g, t);

        // His line at the bottom, typed out, lowercase, the way he wrote.
        int shown = Math.min(line.length(), (int) Math.max(0.0F, (t - 1.5F) * 14.0F));
        String typed = line.substring(0, shown) + ((int) (t * 2) % 2 == 0 ? "_" : " ");
        g.drawString(this.font, typed, 34, h - 34, 0xFF9AA4B4, false);
        g.drawString(this.font, "Minecraft " + SharedConstants.getCurrentVersion().getName() + " / Forge", 2, h - 10, 0x66FFFFFF, false);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private static void rain(GuiGraphics g, int w, int h, float t) {
        Random r = new Random(77L);
        int drops = (w * h) / 900;
        for (int i = 0; i < drops; i++) {
            float speed = 220.0F + r.nextFloat() * 260.0F;
            float x0 = r.nextFloat() * (w + 60) - 30;
            float y0 = r.nextFloat() * h;
            float len = 6.0F + speed / 60.0F;
            float y = (y0 + t * speed) % (h + len) - len;
            float x = x0 - (y / h) * 18.0F;   // slanted by the wind
            int a = 40 + r.nextInt(60);
            g.fill((int) x, (int) y, (int) x + 1, (int) (y + len), (a << 24) | 0xA8B8D0);
        }
    }

    private void title(GuiGraphics g, float t) {
        String word = "sorrowed";
        Font f = this.font;
        float scale = 4.0F;
        int x = 32;
        int y = this.height / 2 - 70;
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        int cx = 0;
        for (int i = 0; i < word.length(); i++) {
            String ch = String.valueOf(word.charAt(i));
            // Each letter sits a little crooked, and now and then one slips.
            float jitter = Mth.sin(t * 1.3F + i * 1.7F) * 0.4F;
            boolean slip = Mth.sin(t * 0.9F + i * 3.1F) > 0.985F;
            int color = slip ? 0xFF6A1A1A : 0xFFD8D4CC;
            g.drawString(f, ch, cx, (int) jitter + (slip ? 1 : 0), color, false);
            cx += f.width(ch);
        }
        g.pose().popPose();
        g.drawString(f, "a world someone left open", x + 2, y + (int) (f.lineHeight * scale) + 4, 0xFF6E7684, false);
    }

    /** A button that is just a word, which brightens and gains a dash when you point at it. */
    private static final class Plain extends Button {
        Plain(int x, int y, String label, OnPress press) {
            super(x, y, 120, 14, Component.literal(label), press, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            Font font = Minecraft.getInstance().font;
            boolean hot = this.isHoveredOrFocused();
            String text = (hot ? "- " : "") + this.getMessage().getString();
            g.drawString(font, text, this.getX(), this.getY() + 3, hot ? 0xFFF2EEE6 : 0xFF8C93A0, false);
        }
    }
}
