package com.sorrowed.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.sorrowed.Sorrowed;
import com.sorrowed.story.Papers;
import com.sorrowed.story.Thoughts;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * A sheet of paper that glides up from the bottom of the screen, settles with a slight tilt,
 * and shows his handwriting or a pencil drawing. Click or press Escape and it slips away again.
 */
public class PaperScreen extends Screen {
    private static final ResourceLocation PAPER = new ResourceLocation(Sorrowed.MODID, "textures/gui/paper.png");
    private static final int PW = 200;
    private static final int PH = 250;
    private static final int TEXT_W = 160;
    private static final int INK = 0xFF2A2630;
    private static final long GLIDE_MS = 650L;

    private final Papers.Page page;
    private final long opened;
    private long closing = -1L;
    private List<FormattedCharSequence> lines;

    private PaperScreen(Papers.Page page) {
        super(Component.literal(page.title()));
        this.page = page;
        this.opened = Util.getMillis();
    }

    public static void open(Papers.Page page) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new PaperScreen(page));
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 0.8F));
    }

    @Override
    protected void init() {
        lines = font.split(Component.literal(Thoughts.f(page.text())), TEXT_W);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static float easeOut(float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        float u = 1.0F - t;
        return 1.0F - u * u * u;
    }

    /** 0 = off the bottom of the screen, 1 = settled in place. */
    private float progress() {
        long now = Util.getMillis();
        if (closing >= 0L) return 1.0F - easeOut((now - closing) / 420.0F);
        return easeOut((now - opened) / (float) GLIDE_MS);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float p = progress();
        if (closing >= 0L && p <= 0.0F) {
            onClose();
            return;
        }
        // Dim the world behind, in step with the paper.
        g.fill(0, 0, width, height, ((int) (120 * p) << 24));

        float scale = Math.min(1.0F, Math.min((height - 20) / (float) PH, (width - 20) / (float) PW));
        float cx = width / 2.0F;
        float restY = (height - PH * scale) / 2.0F;
        float y = restY + (1.0F - p) * (height - restY + 10.0F);
        // A little settle at the end: overshoot the tilt and come back.
        float tilt = -2.5F + (1.0F - p) * 10.0F;

        g.pose().pushPose();
        g.pose().translate(cx, y + PH * scale / 2.0F, 0.0F);
        g.pose().mulPose(Axis.ZP.rotationDegrees(tilt));
        g.pose().scale(scale, scale, 1.0F);
        g.pose().translate(-PW / 2.0F, -PH / 2.0F, 0.0F);

        RenderSystem.enableBlend();
        g.blit(PAPER, 0, 0, 0, 0, PW, PH, 256, 256);

        int ty = 18;
        int tx = (PW - TEXT_W) / 2;
        if (!page.title().isEmpty()) {
            g.drawString(font, Component.literal(page.title()), tx, ty, INK, false);
            ty += 16;
        }
        if (page.drawing() != null) {
            ResourceLocation drawing = new ResourceLocation(Sorrowed.MODID, "textures/gui/drawing_" + page.drawing() + ".png");
            g.blit(drawing, (PW - 160) / 2, ty, 0, 0, 160, 160, 160, 160);
            ty += 166;
        }
        if (lines != null) {
            int i = 0;
            for (FormattedCharSequence line : lines) {
                int wobble = ((i * 7) % 3) - 1;   // his hand was never quite steady
                g.drawString(font, line, tx + wobble, ty, INK, false);
                ty += 11;
                i++;
                if (ty > PH - 14) break;
            }
        }
        g.pose().popPose();
    }

    private void dismiss() {
        if (closing < 0L) {
            closing = Util.getMillis();
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.1F));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        dismiss();
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256 || key == 257 || key == 32 || key == 69) {   // esc, enter, space, E
            dismiss();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
