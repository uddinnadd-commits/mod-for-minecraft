package com.sorrowed.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.sorrowed.Sorrowed;
import com.sorrowed.story.Objectives;
import com.sorrowed.story.Thoughts;
import com.sorrowed.worldgen.MemorySites;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * The journal: two of his sheets side by side. On the left, his list, one line for every world,
 * crossed out in pencil as you finish them. On the right, whichever line you pick: what he asked,
 * how to get there, and what happened when you did it. Opens with J (or from the pause menu).
 */
public class JournalScreen extends Screen {
    private static final ResourceLocation PAPER = new ResourceLocation(Sorrowed.MODID, "textures/gui/paper.png");
    private static final int PW = 200;
    private static final int PH = 250;
    private static final int GAP = 8;
    private static final int TEXT_W = 160;
    private static final int INK = 0xFF2A2630;
    private static final int PENCIL = 0xFF6A6670;
    private static final int BLUE = 0xFF33456A;
    private static final int RED = 0xFF8C2C24;
    private static final int ROW_Y = 38;
    private static final int ROW_H = 16;
    private static final long GLIDE_MS = 600L;

    /** -1 is the extra line at the top once everything is done (the pond, or the end). */
    private record Row(int index) {}

    private final List<Row> rows = new ArrayList<>();
    private final long opened = Util.getMillis();
    private long closing = -1L;
    private int selected;
    private long selectedAt = Util.getMillis();
    private int hovered = -1;
    private float scale = 1.0F;

    public JournalScreen() {
        super(Component.literal("journal"));
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new JournalScreen());
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 0.9F));
    }

    @Override
    protected void init() {
        rows.clear();
        if (ClientStory.stage != ClientStory.PLAYING) rows.add(new Row(-1));
        for (int i = 0; i < Objectives.ALL.size(); i++) rows.add(new Row(i));
        // Start on what matters now: the pond, or this world's line, or the first thing not done.
        selected = 0;
        if (ClientStory.stage == ClientStory.PLAYING) {
            Objectives.Objective here = ClientStory.here();
            int pick = -1;
            for (int r = 0; r < rows.size(); r++) {
                Objectives.Objective o = Objectives.ALL.get(rows.get(r).index());
                if (here != null && o.id().equals(here.id())) pick = r;
            }
            if (pick < 0) {
                for (int r = 0; r < rows.size(); r++) {
                    if (!ClientStory.done[rows.get(r).index()]) {
                        pick = r;
                        break;
                    }
                }
            }
            selected = Math.max(0, pick);
        }
        scale = Math.min(1.0F, Math.min((height - 30) / (float) PH, (width - 20) / (float) (PW * 2 + GAP)));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private static float easeOut(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        float u = 1.0F - t;
        return 1.0F - u * u * u;
    }

    private float progress() {
        long now = Util.getMillis();
        if (closing >= 0L) return 1.0F - easeOut((now - closing) / 380.0F);
        return easeOut((now - opened) / (float) GLIDE_MS);
    }

    private float leftX() {
        return width / 2.0F - (PW + GAP / 2.0F) * scale;
    }

    private float rightX() {
        return width / 2.0F + (GAP / 2.0F) * scale;
    }

    private float restY() {
        return (height - PH * scale) / 2.0F;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float p = progress();
        if (closing >= 0L && p <= 0.0F) {
            onClose();
            return;
        }
        g.fill(0, 0, width, height, ((int) (150 * p) << 24) | 0x05060A);

        float drop = (1.0F - p) * (height - restY() + 10.0F);
        float y = restY() + drop;
        hovered = rowAt(mouseX, mouseY);

        page(g, leftX(), y + (1.0F - p) * 30.0F, -1.3F + (1.0F - p) * 8.0F, true);
        page(g, rightX(), y, 0.9F - (1.0F - p) * 6.0F, false);

        if (p > 0.95F && closing < 0L) {
            String help = "click a line to read it  -  esc to close";
            int hw = font.width(help);
            g.drawString(font, help, (width - hw) / 2, (int) (restY() + PH * scale + 8), 0x88B8B2A6, false);
        }
    }

    private void page(GuiGraphics g, float x, float y, float tilt, boolean left) {
        g.pose().pushPose();
        g.pose().translate(x + PW * scale / 2.0F, y + PH * scale / 2.0F, 0.0F);
        g.pose().mulPose(Axis.ZP.rotationDegrees(tilt));
        g.pose().scale(scale, scale, 1.0F);
        g.pose().translate(-PW / 2.0F, -PH / 2.0F, 0.0F);
        g.fill(4, 5, PW + 4, PH + 5, 0x55000000);   // its shadow on the desk
        RenderSystem.enableBlend();
        g.blit(PAPER, 0, 0, 0, 0, PW, PH, 256, 256);
        if (left) list(g);
        else detail(g);
        g.pose().popPose();
    }

    // ------------------------------------------------------------------ the list

    private void list(GuiGraphics g) {
        g.drawString(font, "things left for you", 20, 16, INK, false);
        squiggle(g, 18, 28, 140, PENCIL);
        Objectives.Objective here = ClientStory.here();
        for (int r = 0; r < rows.size(); r++) {
            int ry = ROW_Y + r * ROW_H;
            int idx = rows.get(r).index();
            boolean sel = r == selected;
            if (r == hovered && !sel) g.fill(30, ry + 9, 30 + 120, ry + 10, 0x30000000);
            if (sel) g.drawString(font, ">", 10, ry, RED, false);
            if (idx < 0) {
                boolean ended = ClientStory.stage == ClientStory.ENDED;
                String label = ended ? "the end" : "the pond";
                g.drawString(font, label, 32, ry, ended ? BLUE : RED, false);
                box(g, 20, ry + 1, ended, ended ? BLUE : RED);
                continue;
            }
            Objectives.Objective o = Objectives.ALL.get(idx);
            boolean done = ClientStory.done[idx];
            int ink = done ? PENCIL : INK;
            g.drawString(font, o.place(), 32, ry, ink, false);
            int tw = font.width(o.place());
            if (done) strike(g, 30, ry + 4, tw + 4, r);
            box(g, 20, ry + 1, done, ink);
            if (here != null && o.id().equals(here.id()) && !done) {
                g.drawString(font, "(here)", 36 + tw, ry, PENCIL, false);
            }
        }
        int n = ClientStory.count();
        String count = n + " of " + Objectives.ALL.size();
        int fy = PH - 26;
        g.drawString(font, count, PW - 20 - font.width(count), fy, INK, false);
        // A pencil bar, filled as far as you have got.
        int bw = 90;
        g.fill(22, fy + 3, 22 + bw, fy + 4, 0x40000000);
        g.fill(22, fy + 2, 22 + (int) (bw * n / (float) Objectives.ALL.size()), fy + 5, PENCIL);
    }

    private void box(GuiGraphics g, int x, int y, boolean ticked, int ink) {
        g.fill(x, y, x + 7, y + 1, ink);
        g.fill(x, y + 6, x + 7, y + 7, ink);
        g.fill(x, y, x + 1, y + 7, ink);
        g.fill(x + 6, y, x + 7, y + 7, ink);
        if (ticked) {
            // A tick that overshoots the box, the way you do when you are pleased.
            g.fill(x + 1, y + 3, x + 2, y + 4, ink);
            g.fill(x + 2, y + 4, x + 3, y + 5, ink);
            g.fill(x + 3, y + 5, x + 4, y + 6, ink);
            for (int i = 0; i < 6; i++) g.fill(x + 4 + i, y + 4 - i, x + 5 + i, y + 5 - i, ink);
        }
    }

    private void strike(GuiGraphics g, int x, int y, int w, int seed) {
        for (int i = 0; i < w; i++) {
            int wob = (int) Math.round(Math.sin((i + seed * 7) * 0.31D) * 0.7D);
            g.fill(x + i, y + wob, x + i + 1, y + wob + 1, 0xCC3A3640);
        }
    }

    private void squiggle(GuiGraphics g, int x, int y, int w, int ink) {
        for (int i = 0; i < w; i++) {
            int wob = (int) Math.round(Math.sin(i * 0.22D) * 0.8D);
            g.fill(x + i, y + wob, x + i + 1, y + wob + 1, ink);
        }
    }

    // ------------------------------------------------------------------ the page on the right

    private void detail(GuiGraphics g) {
        if (rows.isEmpty()) return;
        int idx = rows.get(Mth.clamp(selected, 0, rows.size() - 1)).index();
        float fade = Mth.clamp((Util.getMillis() - selectedAt) / 220.0F, 0.0F, 1.0F);
        int a = (int) (fade * 255.0F);
        if (a < 8) return;

        String heading;
        String task;
        String hint;
        String way = null;
        boolean done = false;
        Objectives.Objective o = null;
        if (idx < 0) {
            boolean ended = ClientStory.stage == ClientStory.ENDED;
            heading = ended ? "the end" : Objectives.FINALE_PLACE;
            task = ended ? "thank you for coming back" : Objectives.FINALE_TASK;
            hint = ended ? Objectives.ENDED_HINT : Objectives.FINALE_HINT;
        } else {
            o = Objectives.ALL.get(idx);
            heading = o.place();
            task = o.task();
            hint = o.hint();
            done = ClientStory.done[idx];
            Objectives.Objective here = ClientStory.here();
            if (!done && (here == null || !here.id().equals(o.id()))) way = o.way();
        }

        g.pose().pushPose();
        g.pose().translate(20.0F, 14.0F, 0.0F);
        g.pose().scale(1.5F, 1.5F, 1.0F);
        g.drawString(font, heading, 0, 0, alpha(INK, a), false);
        g.pose().popPose();
        int ty = 32;
        for (FormattedCharSequence line : font.split(Component.literal("- " + task), TEXT_W)) {
            g.drawString(font, line, 20, ty, alpha(done ? PENCIL : RED, a), false);
            if (done) strike(g, 26, ty + 4, Math.max(4, font.width(line) - 6), 3);
            ty += 10;
        }
        squiggle(g, 18, ty + 3, 150, alpha(PENCIL, a));
        ty += 10;

        int limit = PH - 16;
        ty = paragraph(g, Thoughts.f(hint), ty, alpha(INK, a), limit);
        if (way != null && ty < limit - 24) {
            ty += 4;
            g.drawString(font, "the way there:", 20, ty, alpha(PENCIL, a), false);
            ty = paragraph(g, way, ty + 11, alpha(PENCIL, a), limit);
        }
        if (o != null) ty = parts(g, o, ty + 4, a, limit);
        if (o != null && done && ty < limit - 12) {
            ty += 4;
            ty = paragraph(g, Thoughts.f(o.done()), ty, alpha(BLUE, a), limit);
            if ("hallway".equals(o.id()) && !ClientStory.reply.isEmpty() && ty < limit - 12) {
                paragraph(g, "you wrote: \"" + ClientStory.reply + "\"", ty + 3, alpha(BLUE, a), limit);
            }
        }
    }

    /** The checklist for the objectives that come in parts. */
    private int parts(GuiGraphics g, Objectives.Objective o, int ty, int a, int limit) {
        if (o.steps() <= 1 || ClientStory.isDone(o.id())) return ty;
        int mask = ClientStory.stepsOf(o.id());
        if ("backup".equals(o.id())) {
            for (int i = 0; i < MemorySites.PLACE_NAMES.length && ty < limit - 10; i++) {
                boolean got = (mask & (1 << i)) != 0;
                box(g, 22, ty + 1, got, alpha(got ? PENCIL : INK, a));
                g.drawString(font, MemorySites.PLACE_NAMES[i], 34, ty, alpha(got ? PENCIL : INK, a), false);
                if (got) strike(g, 32, ty + 4, font.width(MemorySites.PLACE_NAMES[i]) + 4, i);
                ty += 12;
            }
            return ty;
        }
        String line = ObjectiveHud.progress(o);
        if (line != null && ty < limit - 10) {
            g.drawString(font, line, 20, ty, alpha(INK, a), false);
            ty += 12;
        }
        return ty;
    }

    /** Wrapped, a little unsteady, the way his hand was. Returns the y below it. */
    private int paragraph(GuiGraphics g, String text, int y, int color, int limit) {
        List<FormattedCharSequence> lines = font.split(Component.literal(text), TEXT_W);
        int i = 0;
        for (FormattedCharSequence line : lines) {
            if (y > limit - 9) break;
            int wobble = ((i * 7) % 3) - 1;
            g.drawString(font, line, 20 + wobble, y, color, false);
            y += 10;
            i++;
        }
        return y;
    }

    private static int alpha(int argb, int a) {
        return (Math.min(a, (argb >>> 24) & 0xFF) << 24) | (argb & 0xFFFFFF);
    }

    // ------------------------------------------------------------------ input

    /** Which line of the list the mouse is over, or -1. Ignores the page's slight tilt. */
    private int rowAt(double mx, double my) {
        double lx = (mx - leftX()) / scale;
        double ly = (my - restY()) / scale;
        if (lx < 8 || lx > PW - 16) return -1;
        int r = (int) Math.floor((ly - ROW_Y + 3) / ROW_H);
        if (ly < ROW_Y - 3 || r < 0 || r >= rows.size()) return -1;
        return r;
    }

    private void select(int r) {
        if (r < 0 || r >= rows.size() || r == selected) return;
        selected = r;
        selectedAt = Util.getMillis();
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.3F, 0.5F));
    }

    private void dismiss() {
        if (closing < 0L) {
            closing = Util.getMillis();
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.1F));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int r = rowAt(mouseX, mouseY);
        if (r >= 0) {
            select(r);
            return true;
        }
        float top = restY();
        boolean onPages = mouseY >= top && mouseY <= top + PH * scale
                && mouseX >= leftX() && mouseX <= rightX() + PW * scale;
        if (!onPages) dismiss();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        select(Mth.clamp(selected - (int) Math.signum(delta), 0, rows.size() - 1));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256 || key == 69 || KeyBindings.JOURNAL.matches(key, scan)) {   // esc, E, or the journal key
            dismiss();
            return true;
        }
        if (key == 265 || key == 87) {   // up, W
            select(Math.max(0, selected - 1));
            return true;
        }
        if (key == 264 || key == 83) {   // down, S
            select(Math.min(rows.size() - 1, selected + 1));
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }
}
