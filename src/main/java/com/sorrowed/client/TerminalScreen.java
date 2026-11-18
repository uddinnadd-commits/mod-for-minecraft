package com.sorrowed.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sorrowed.Sorrowed;
import com.sorrowed.net.ModNetwork;
import com.sorrowed.story.Thoughts;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * What is on the screens he left on. An old monitor's worth of glass, scanlines, a cursor.
 * "chat": the hallway; his last messages, and a line for you to answer him on.
 * "console": the server room; somebody types list.
 * "laptop": the orchard; the copy he started on a Sunday, finishing.
 */
public class TerminalScreen extends Screen {
    private static final ResourceLocation VIGNETTE = new ResourceLocation(Sorrowed.MODID, "textures/gui/vignette.png");
    private static final int CELL = 6;
    private static final int LINE = 11;

    private record Line(long at, String text, int color, boolean typed) {}

    private final String mode;
    private final long opened = Util.getMillis();
    private final List<Line> script = new ArrayList<>();
    private final StringBuilder input = new StringBuilder();
    private final String friend = Thoughts.f("{friend}").toLowerCase();
    private final String me;
    private final int text;
    private final int dim;
    private final int glass;
    private final int bezel;
    private long inputFrom = Long.MAX_VALUE;
    private long sentAt = -1L;
    private long reportAt = -1L;
    private boolean reported;
    private int lastShown;

    public TerminalScreen(String mode) {
        super(Component.literal(mode));
        this.mode = mode;
        Minecraft mc = Minecraft.getInstance();
        this.me = mc.player != null ? mc.player.getGameProfile().getName() : "you";
        switch (mode) {
            case "console" -> {
                text = 0xFF7CFF9A;
                dim = 0xFF3E8A52;
                glass = 0xFF040A06;
                bezel = 0xFF2A2C30;
            }
            case "laptop" -> {
                text = 0xFFD0D0D0;
                dim = 0xFF7A7A80;
                glass = 0xFF07070C;
                bezel = 0xFF1C1C20;
            }
            default -> {
                text = 0xFFCAD4E4;
                dim = 0xFF6E7C94;
                glass = 0xFF0C1220;
                bezel = 0xFFB8AE92;
            }
        }
        build();
    }

    private void add(long at, String s, int color, boolean typed) {
        script.add(new Line(opened + at, s, color, typed));
    }

    private void build() {
        switch (mode) {
            case "console" -> {
                add(0, "backup-01 :: last login: the day before", dim, false);
                add(500, "> list", text, true);
                add(1700, "There are 2 of a max of 2 players online: " + me + ", " + friend, text, false);
                add(2400, "> _", text, false);
                reportAt = opened + 2600;
            }
            case "laptop" -> {
                add(0, "C:\\BACKUP> copy world e:\\backup", text, true);
                add(1500, "copying world...", dim, false);
                add(1700, "@progress", text, false);
                add(5600, "        1 file(s) copied.", text, false);
                add(6100, "backup complete. sunday, 18:02", dim, false);
                add(6600, "C:\\BACKUP> _", text, false);
                reportAt = opened + 5800;
            }
            default -> {
                add(0, "#us - 2 members", dim, false);
                add(250, "[19:02:11] " + friend + " joined the game", dim, false);
                add(500, "[19:02:40] <" + friend + "> testing", text, false);
                add(750, "[19:40:03] <" + friend + "> ok it works", text, false);
                add(1000, "[23:58:12] <" + friend + "> are you on", text, false);
                add(1250, "[23:58:50] <" + friend + "> its fine", text, false);
                add(1500, "[00:14:31] " + friend + " left the game", dim, false);
                if (ClientStory.isDone("hallway")) {
                    String said = ClientStory.reply.isEmpty() ? "..." : ClientStory.reply;
                    add(1750, "[--:--:--] <" + me + "> " + said, text, false);
                    add(2000, "* delivered", dim, false);
                } else {
                    inputFrom = opened + 1900;
                }
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        long now = Util.getMillis();
        if (!reported && reportAt > 0 && now >= reportAt) {
            reported = true;
            ModNetwork.reply(mode, mode.equals("console") ? "list" : "");
        }
        // Key clicks while lines are being typed out.
        int shown = 0;
        for (Line l : script) {
            if (now >= l.at()) shown++;
        }
        if (shown != lastShown) {
            lastShown = shown;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.8F, 0.25F));
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long now = Util.getMillis();
        float open = Mth.clamp((now - opened) / 260.0F, 0.0F, 1.0F);
        g.fill(0, 0, width, height, ((int) (170 * open) << 24));

        int pw = Math.min(380, width - 30);
        int ph = Math.min(210, height - 40);
        int x0 = (width - pw) / 2;
        int y0 = (height - ph) / 2;
        // The set warming up: a line that opens into the picture.
        int visibleH = (int) (ph * easeOut(open));
        int cy = y0 + ph / 2;
        int top = cy - visibleH / 2;
        int bottom = cy + visibleH / 2;

        g.fill(x0 - 8, y0 - 8, x0 + pw + 8, y0 + ph + 8, bezel);
        g.fill(x0 - 8, y0 + ph + 4, x0 + pw + 8, y0 + ph + 8, darker(bezel));
        g.fill(x0 - 2, y0 - 2, x0 + pw + 2, y0 + ph + 2, 0xFF000000);
        g.fill(x0, top, x0 + pw, bottom, glass);
        if (visibleH < ph) {
            g.fill(x0, cy - 1, x0 + pw, cy + 1, 0xFFFFFFFF);
            return;
        }

        g.enableScissor(x0, y0, x0 + pw, y0 + ph);
        Font f = this.font;
        int cols = (pw - 16) / CELL;
        int maxLines = (ph - 16) / LINE;
        List<String[]> out = new ArrayList<>();   // {text, color}
        for (Line l : script) {
            if (now < l.at()) break;
            String s = l.text();
            if (s.equals("@progress")) {
                float pr = Mth.clamp((now - l.at()) / 3800.0F, 0.0F, 1.0F);
                int bars = 24;
                int fill = (int) (bars * pr);
                s = "[" + "#".repeat(fill) + ".".repeat(bars - fill) + "] " + (int) (pr * 100) + "%";
            } else if (l.typed()) {
                int n = (int) Mth.clamp((now - l.at()) / 70L, 0, s.length());
                s = s.substring(0, n);
            }
            if (s.endsWith("_")) s = s.substring(0, s.length() - 1) + (((now / 450L) & 1L) == 0L ? "_" : " ");
            for (String w : wrap(s, cols)) out.add(new String[] {w, Integer.toString(l.color())});
        }
        // The chat: what you are typing, and what happens after.
        if ("chat".equals(mode) && now >= inputFrom) {
            if (sentAt < 0L) {
                String cur = "> " + input + (((now / 450L) & 1L) == 0L ? "_" : " ");
                for (String w : wrap(cur, cols)) out.add(new String[] {w, Integer.toString(text)});
            } else {
                String stamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                out.add(new String[] {"[" + stamp + "] <" + me + "> " + input, Integer.toString(text)});
                long since = now - sentAt;
                if (since > 900L && since < 4600L) {
                    String dots = ".".repeat((int) ((since / 400L) % 4L));
                    out.add(new String[] {"* " + friend + " is typing" + dots, Integer.toString(dim)});
                } else if (since >= 4600L) {
                    out.add(new String[] {"* delivered", Integer.toString(dim)});
                }
            }
        }
        int start = Math.max(0, out.size() - maxLines);
        int y = y0 + 8;
        float flicker = 0.92F + 0.08F * Mth.sin(now / 37.0F) * Mth.sin(now / 113.0F);
        for (int i = start; i < out.size(); i++) {
            String s = out.get(i)[0];
            int c = Integer.parseInt(out.get(i)[1]);
            int col = ((int) (((c >>> 24) & 0xFF) * flicker) << 24) | (c & 0xFFFFFF);
            for (int k = 0; k < s.length(); k++) {
                String ch = String.valueOf(s.charAt(k));
                if (ch.equals(" ")) continue;
                g.drawString(f, ch, x0 + 8 + k * CELL + (CELL - f.width(ch)) / 2, y, col, false);
            }
            y += LINE;
        }
        // Scanlines, a slow bright band rolling down, and the glass darkening at its edges.
        for (int sy = y0; sy < y0 + ph; sy += 2) g.fill(x0, sy, x0 + pw, sy + 1, 0x28000000);
        int band = (int) ((now / 12L) % (ph + 60)) - 30;
        g.fill(x0, y0 + band, x0 + pw, y0 + band + 18, 0x0CFFFFFF);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 0.75F);
        g.blit(VIGNETTE, x0, y0, pw, ph, 0.0F, 0.0F, 256, 256, 256, 256);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.disableScissor();

        String hint = "chat".equals(mode) && now >= inputFrom && sentAt < 0L ? "type, then enter  -  esc to leave" : "esc to leave";
        g.drawString(f, hint, (width - f.width(hint)) / 2, y0 + ph + 14, 0x99B8B2A6, false);
    }

    private static List<String> wrap(String s, int cols) {
        List<String> out = new ArrayList<>();
        while (s.length() > cols) {
            int cut = s.lastIndexOf(' ', cols);
            if (cut <= 0) cut = cols;
            out.add(s.substring(0, cut));
            s = s.substring(cut).stripLeading();
        }
        out.add(s);
        return out;
    }

    private static float easeOut(float t) {
        float u = 1.0F - t;
        return 1.0F - u * u * u;
    }

    private static int darker(int argb) {
        int r = ((argb >> 16) & 0xFF) * 3 / 4;
        int gg = ((argb >> 8) & 0xFF) * 3 / 4;
        int b = (argb & 0xFF) * 3 / 4;
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (!"chat".equals(mode) || sentAt >= 0L || Util.getMillis() < inputFrom) return false;
        if (c >= ' ' && c != 127 && input.length() < 100) {
            input.append(c);
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(),
                    1.6F + (float) Math.random() * 0.4F, 0.3F));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        boolean typing = "chat".equals(mode) && sentAt < 0L && Util.getMillis() >= inputFrom;
        if (typing && key == 259 && input.length() > 0) {   // backspace
            input.setLength(input.length() - 1);
            return true;
        }
        if (typing && (key == 257 || key == 335) && !input.toString().isBlank()) {   // enter
            sentAt = Util.getMillis();
            ModNetwork.reply("chat", input.toString());
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT.value(), 1.6F, 0.6F));
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }
}
