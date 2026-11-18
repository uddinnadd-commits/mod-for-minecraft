package com.sorrowed.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.sorrowed.Sorrowed;
import com.sorrowed.SorrowedConfig;
import com.sorrowed.story.Objectives;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * The objective for the world you are in, top left, in his handwriting colours: where you are,
 * what is left to do here, and a pencil arrow toward it with the distance. Bright for a few
 * seconds whenever something changes, then it settles back to a faint note.
 */
public final class ObjectiveHud {
    private ObjectiveHud() {}

    private static final ResourceLocation ARROW = new ResourceLocation(Sorrowed.MODID, "textures/gui/arrow.png");
    private static final int PAPER = 0xE6DCC0;
    private static final int MUTED = 0x9AA0AC;

    private static long bumpedAt = Util.getMillis();

    public static void bump() {
        bumpedAt = Util.getMillis();
    }

    public static void render(GuiGraphics g, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null || mc.options.hideGui || !ClientStory.synced) return;
        if (!SorrowedConfig.OBJECTIVE_HUD.get()) return;
        if (CutsceneClient.isActive() || DistortionOverlay.active() || mc.screen instanceof JournalScreen) return;
        if (ClientStory.stage == ClientStory.ENDED) return;

        String place;
        String task;
        String progress = null;
        boolean done = false;
        if (ClientStory.stage == ClientStory.READY) {
            place = Objectives.FINALE_PLACE;
            task = Objectives.FINALE_TASK;
        } else {
            Objectives.Objective o = ClientStory.here();
            if (o == null) return;
            place = o.place();
            task = o.task();
            done = ClientStory.isDone(o.id());
            progress = progress(o);
        }

        float since = (Util.getMillis() - bumpedAt) / 1000.0F;
        float alpha;
        if (done) {
            alpha = since < 6.0F ? 1.0F : Math.max(0.0F, 1.0F - (since - 6.0F) / 2.0F);
        } else {
            alpha = since < 8.0F ? 1.0F : Math.max(0.42F, 1.0F - (since - 8.0F) / 2.5F);
        }
        if (alpha <= 0.02F) return;

        Font font = mc.font;
        int x = 10;
        int y = 10;
        int taskW = font.width(task);
        int boxW = Math.max(taskW + 18, font.width(place) + 8) + 46;
        int boxH = progress != null ? 40 : 30;

        // A soft dark strip behind the words, fading out to the right, and a pencil line on the left.
        RenderSystem.enableBlend();
        for (int i = 0; i < 8; i++) {
            int a = (int) (alpha * 90.0F * (1.0F - i / 8.0F));
            int x0 = x - 4 + i * boxW / 8;
            g.fill(x0, y - 4, x0 + boxW / 8, y + boxH - 4, a << 24);
        }
        g.fill(x - 4, y - 4, x - 3, y + boxH - 4, col(PAPER, alpha * 0.8F));

        g.drawString(font, place, x + 2, y, col(MUTED, alpha), false);
        int ty = y + 12;
        g.drawString(font, "- " + task, x + 2, ty, col(PAPER, alpha), true);
        if (done) {
            int sw = font.width("- " + task);
            g.fill(x + 1, ty + 4, x + 3 + sw, ty + 5, col(PAPER, alpha));
        }
        if (progress != null) {
            g.drawString(font, progress, x + 10, ty + 12, col(MUTED, alpha), false);
        }

        // The arrow, toward the nearest thing left to find in this world.
        BlockPos target = nearest(player);
        if (target != null && !done) {
            double dx = target.getX() + 0.5D - player.getX();
            double dy = target.getY() + 0.5D - player.getEyeY();
            double dz = target.getZ() + 0.5D - player.getZ();
            float bearing = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
            float rel = Mth.wrapDegrees(bearing - player.getYRot());
            int dist = (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
            int ax = x + 2 + Math.max(taskW, font.width(place)) + 20;
            int ay = y + 9;
            g.pose().pushPose();
            g.pose().translate(ax, ay, 0.0F);
            g.pose().mulPose(Axis.ZP.rotationDegrees(rel));
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            g.blit(ARROW, -8, -8, 0, 0, 16, 16, 16, 16);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            g.pose().popPose();
            String d = dist < 3 ? "here" : Integer.toString(dist);
            if (dist >= 3 && Math.abs(dy) > 5.0D && Math.abs(dy) > Math.sqrt(dx * dx + dz * dz) * 0.35D) {
                d += dy > 0 ? " up" : " down";
            }
            g.drawString(font, d, ax + 11, ay - 4, col(MUTED, alpha), false);
        }

        // For the first few seconds, how to open the journal.
        if (since < 8.0F && KeyBindings.JOURNAL != null) {
            String hint = "[" + KeyBindings.JOURNAL.getTranslatedKeyMessage().getString().toLowerCase() + "] journal";
            float ha = alpha * Math.min(1.0F, (8.0F - since) / 1.5F);
            if (ha > 0.04F) g.drawString(font, hint, x + 2, y + boxH - 2, col(MUTED, ha * 0.8F), false);
        }
    }

    /** "2 of 4", "5 of 8" and so on, for the objectives that come in parts. */
    static String progress(Objectives.Objective o) {
        if (o.steps() <= 1 || ClientStory.isDone(o.id())) return null;
        int mask = ClientStory.stepsOf(o.id());
        if ("hush".equals(o.id())) {
            int stones = Integer.bitCount(mask & 0xFF);
            return stones < 8 ? stones + " of 8 counted" : "8 of 8. and one more";
        }
        return Integer.bitCount(mask) + " of " + o.steps();
    }

    private static BlockPos nearest(Player player) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : ClientStory.targets) {
            double d = player.distanceToSqr(p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    private static int col(int rgb, float alpha) {
        return ((int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24) | rgb;
    }
}
