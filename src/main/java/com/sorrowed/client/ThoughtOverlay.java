package com.sorrowed.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Typewriter text above the hotbar: monospaced cells, a blinking cursor, soft key clicks.
 * Ordinary thoughts are slow and are dropped if two are already waiting, so they never pile up.
 * Cutscene captions are "fast" and always play.
 */
public class ThoughtOverlay {
    private record Entry(String text, boolean fast) {}

    private static final String GLITCH = "#/\\?!%&";
    private static final int CELL = 6;
    private static final int LINE_HEIGHT = 11;
    private static final Deque<Entry> QUEUE = new ArrayDeque<>();

    private static List<String> lines = new ArrayList<>();
    private static boolean active;
    private static boolean fastNow;
    private static int totalChars;
    private static int shown;
    private static int holdTicks;
    private static int fadeTicks;
    private static int gap;
    private static int step;

    public static void push(String text) {
        push(text, false);
    }

    public static void push(String text, boolean fast) {
        if (!fast && QUEUE.size() >= 2) return;
        if (fast) {
            if (!active) gap = 0;
        } else if (!active && QUEUE.isEmpty() && gap < 50) {
            gap = 50;
        }
        QUEUE.add(new Entry(text, fast));
    }

    public static void clear() {
        QUEUE.clear();
        active = false;
        gap = 0;
        lines = new ArrayList<>();
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;

        if (!active) {
            if (gap > 0) {
                gap--;
                return;
            }
            Entry next = QUEUE.poll();
            if (next != null) begin(mc, next);
            return;
        }

        step++;
        if (shown < totalChars) {
            if (step % (fastNow ? 1 : 2) == 0) {
                shown++;
                char c = charAt(shown - 1);
                if (c != ' ' && shown % (fastNow ? 3 : 2) == 0) {
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK,
                            1.5F + mc.level.random.nextFloat() * 0.4F));
                }
            }
        } else if (holdTicks > 0) {
            holdTicks--;
        } else if (fadeTicks > 0) {
            fadeTicks--;
        } else {
            active = false;
            gap = fastNow ? 5 : 30;
        }
    }

    private static void begin(Minecraft mc, Entry entry) {
        int guiWidth = mc.getWindow().getGuiScaledWidth();
        int perLine = Math.max(20, Math.min(52, (guiWidth - 24) / CELL));
        lines = wrap(entry.text(), perLine);
        totalChars = 0;
        for (String l : lines) totalChars += l.length();
        shown = 0;
        step = 0;
        fastNow = entry.fast();
        holdTicks = fastNow ? 24 + totalChars / 2 : 50 + totalChars;
        fadeTicks = 20;
        active = true;
    }

    private static char charAt(int index) {
        int i = index;
        for (String l : lines) {
            if (i < l.length()) return l.charAt(i);
            i -= l.length();
        }
        return ' ';
    }

    private static List<String> wrap(String text, int perLine) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            int extra = cur.length() > 0 ? 1 : 0;
            if (cur.length() > 0 && cur.length() + extra + word.length() > perLine) {
                out.add(cur.toString());
                cur.setLength(0);
            }
            if (cur.length() > 0) cur.append(' ');
            cur.append(word);
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    public static void render(GuiGraphics g, int width, int height) {
        if (!active || lines.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.level == null) return;
        Font font = mc.font;

        boolean fading = shown >= totalChars && holdTicks <= 0;
        float alpha = fading ? fadeTicks / 20.0F : 1.0F;
        int a = Math.max(4, (int) (alpha * 255.0F));
        int color = (a << 24) | 0xE6DCC0;

        int baseY = height - 74 - (lines.size() - 1) * LINE_HEIGHT;
        boolean blink = (mc.level.getGameTime() / 6L) % 2L == 0L;
        int remaining = shown;

        for (int li = 0; li < lines.size(); li++) {
            String line = lines.get(li);
            int startX = (width - line.length() * CELL) / 2;
            int y = baseY + li * LINE_HEIGHT;
            int n = Math.max(0, Math.min(remaining, line.length()));

            for (int i = 0; i < n; i++) {
                char ch = line.charAt(i);
                if (ch == ' ') continue;
                if (ClientState.sorrow >= 55) {
                    int hash = i * 31 + li * 17 + (int) (mc.level.getGameTime() / 5L);
                    if (Math.floorMod(hash, 29) == 0) {
                        ch = GLITCH.charAt(Math.floorMod(hash * 7, GLITCH.length()));
                    }
                }
                String s = String.valueOf(ch);
                int cw = font.width(s);
                int jitter = ((i * 7 + li * 3) % 5 == 0) ? 1 : 0;
                g.drawString(font, Component.literal(s).withStyle(ChatFormatting.ITALIC),
                        startX + i * CELL + (CELL - cw) / 2, y + jitter, color, true);
            }

            if (shown < totalChars && blink && remaining >= 0 && remaining <= line.length()) {
                g.drawString(font, "_", startX + n * CELL, y, color, true);
            }

            remaining -= line.length();
            if (remaining < 0) break;
        }
    }
}
