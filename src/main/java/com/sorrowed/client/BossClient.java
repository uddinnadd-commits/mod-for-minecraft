package com.sorrowed.client;

import com.sorrowed.Sorrowed;
import com.sorrowed.registry.Dimensions;
import com.sorrowed.registry.ModSounds;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The client half of the fight: its own health bar in place of the vanilla one, the stage titles,
 * the camera shaking, flashes, and the music, which loops for as long as the fight lasts and
 * fades when it is over.
 */
@Mod.EventBusSubscriber(modid = Sorrowed.MODID, value = Dist.CLIENT)
public final class BossClient {
    private BossClient() {}

    private static final String BOSS_KEY = "entity.sorrowed.memory_shatterer";
    private static final String[] ROMAN = {"I", "II", "III", "IV"};
    private static final String[] STAGES = {"remembering", "forgetting", "shattering", "the last memory"};
    private static final String GLITCH = "#/\\?!%&";

    private static float shake;
    private static long flashAt = -1L;
    private static int flashColor = 0xFFFFFF;
    private static float flashStrength;
    private static long titleAt = -1L;
    private static int titleStage;
    private static Music music;

    private static float trail = 1.0F;
    private static float lastProgress = 1.0F;
    private static long hitAt = -1L;

    /** "turn", "stage:N", "shake:S", "dying", "dead". */
    public static void command(String what) {
        if (what.equals("turn")) {
            StaticOverlay.push(0.9F);
            flash(0x8A0C18, 0.55F);
            shake(1.0F);
        } else if (what.startsWith("stage:")) {
            int s = Mth.clamp(parse(what.substring(6)), 0, 3);
            titleStage = s;
            titleAt = Util.getMillis();
            if (s > 0) {
                StaticOverlay.push(0.6F);
                flash(0xFFF2EA, 0.45F);
                shake(0.9F);
            }
        } else if (what.startsWith("shake:")) {
            try {
                shake(Float.parseFloat(what.substring(6)));
            } catch (NumberFormatException ignored) {
                shake(0.5F);
            }
        } else if (what.equals("dying")) {
            StaticOverlay.push(0.5F);
            shake(0.6F);
        } else if (what.equals("dead")) {
            flash(0xFFFFFF, 0.9F);
            shake(1.2F);
        }
    }

    private static int parse(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void shake(float s) {
        shake = Math.max(shake, s);
    }

    private static void flash(int rgb, float strength) {
        flashAt = Util.getMillis();
        flashColor = rgb;
        flashStrength = strength;
    }

    public static void clear() {
        shake = 0.0F;
        flashAt = -1L;
        titleAt = -1L;
        trail = 1.0F;
        lastProgress = 1.0F;
        if (music != null) music.stopNow();
        music = null;
    }

    private static boolean fighting() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && ClientStory.stage == ClientStory.FIGHTING && mc.level.dimension().equals(Dimensions.BACKUP);
    }

    public static void tick() {
        shake *= 0.86F;
        if (shake < 0.01F) shake = 0.0F;
        Minecraft mc = Minecraft.getInstance();
        boolean want = fighting();
        if (want && (music == null || music.isStopped())) {
            mc.getMusicManager().stopPlaying();
            music = new Music();
            mc.getSoundManager().play(music);
        } else if (!want && music != null) {
            music.fadeOut();
            if (music.isStopped()) music = null;
        }
    }

    /** No other music while it plays. */
    @SubscribeEvent
    public static void onSound(PlaySoundEvent event) {
        SoundInstance s = event.getSound();
        if (s != null && fighting() && s.getSource() == SoundSource.MUSIC && !(s instanceof Music)) {
            event.setSound(null);
        }
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        if (shake <= 0.0F || CutsceneClient.isActive()) return;
        RandomSource r = RandomSource.create(Util.getMillis() / 16L);
        float s = shake * shake;
        event.setYaw(event.getYaw() + (r.nextFloat() - 0.5F) * 3.0F * s);
        event.setPitch(event.getPitch() + (r.nextFloat() - 0.5F) * 3.0F * s);
        event.setRoll(event.getRoll() + (r.nextFloat() - 0.5F) * 4.0F * s);
    }

    // ------------------------------------------------------------------ the bar

    private static boolean ours(LerpingBossEvent e) {
        return e.getName().getContents() instanceof TranslatableContents t && BOSS_KEY.equals(t.getKey());
    }

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent e = event.getBossEvent();
        if (!ours(e)) return;
        event.setCanceled(true);
        event.setIncrement(36);
        GuiGraphics g = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        int sw = g.guiWidth();
        int w = Math.min(300, sw - 40);
        int x = (sw - w) / 2;
        int y = event.getY();
        float p = Mth.clamp(e.getProgress(), 0.0F, 1.0F);
        long now = Util.getMillis();
        if (p < lastProgress - 0.0005F) hitAt = now;
        lastProgress = p;
        trail = trail < p ? p : Math.max(p, trail - 0.0035F);

        // The name, letters slipping.
        String name = "The Memory Shatterer";
        float t = now / 1000.0F;
        int nx = (sw - font.width(name)) / 2;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean slip = Mth.sin(t * 1.7F + i * 2.3F) > 0.97F;
            String ch = String.valueOf(slip && c != ' ' ? GLITCH.charAt(i % GLITCH.length()) : c);
            int jit = (int) (Mth.sin(t * 9.0F + i * 1.3F) * (slip ? 1.5F : 0.4F));
            g.drawString(font, ch, nx, y + jit, slip ? 0xFFB01828 : 0xFFE6DCC0, true);
            nx += font.width(String.valueOf(c));
        }

        int by = y + 12;
        int hx = now - hitAt < 160L ? (int) ((Math.random() - 0.5D) * 3.0D) : 0;
        g.fill(x - 2 + hx, by - 2, x + w + 2 + hx, by + 8, 0xE0000000);
        g.fill(x - 1 + hx, by - 1, x + w + 1 + hx, by + 7, 0xFF3A0A12);
        g.fill(x + hx, by, x + w + hx, by + 6, 0xFF12050A);
        g.fill(x + hx, by, x + hx + (int) (w * trail), by + 6, 0xFFE8D8D0);
        int fw = (int) (w * p);
        g.fillGradient(x + hx, by, x + hx + fw, by + 6, 0xFFD8303E, 0xFF7A0E1E);
        // A glint running along it.
        int gx = (int) ((now / 6L) % (w + 40L)) - 20;
        if (gx > 0 && gx < fw) g.fill(x + hx + gx, by, x + hx + Math.min(fw, gx + 6), by + 6, 0x40FFFFFF);
        if (now - hitAt < 120L) g.fill(x + hx, by, x + hx + fw, by + 6, 0x70FFFFFF);
        for (int k = 1; k <= 3; k++) {
            int kx = x + hx + w * k / 4;
            g.fill(kx, by - 2, kx + 1, by + 8, 0xC0E6DCC0);
        }
        int stage = stageOf(p);
        String label = ROMAN[stage] + "  " + STAGES[stage];
        g.drawString(font, label, x + w - font.width(label), by + 10, 0xFFB8A8A8, false);
    }

    private static int stageOf(float p) {
        int s = 0;
        if (p <= 0.7501F) s++;
        if (p <= 0.5001F) s++;
        if (p <= 0.2501F) s++;
        return s;
    }

    // ------------------------------------------------------------------ titles and flashes

    public static void render(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        long now = Util.getMillis();
        if (flashAt >= 0L) {
            float s = (now - flashAt) / 1000.0F;
            float a = flashStrength * Math.max(0.0F, 1.0F - s / 0.9F);
            if (a > 0.01F) g.fill(0, 0, w, h, ((int) (a * 255.0F) << 24) | flashColor);
            else flashAt = -1L;
        }
        if (titleAt < 0L || CutsceneClient.isActive() || mc.options.hideGui) return;
        float s = (now - titleAt) / 1000.0F;
        if (s > 4.0F) {
            titleAt = -1L;
            return;
        }
        float a = Math.min(Mth.clamp(s / 0.4F, 0.0F, 1.0F), Mth.clamp((4.0F - s) / 0.9F, 0.0F, 1.0F));
        if (a <= 0.03F) return;
        Font font = mc.font;
        String big = ROMAN[titleStage];
        float scale = 4.0F;
        int cy = (int) (h * 0.32F);
        g.pose().pushPose();
        g.pose().translate(w / 2.0F, cy, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        int jit = s < 0.5F ? (int) ((Math.random() - 0.5D) * 2.0D) : 0;
        g.drawString(font, big, -font.width(big) / 2 + jit, 0, ((int) (a * 255.0F) << 24) | 0xE6DCC0, true);
        g.pose().popPose();
        String sub = STAGES[titleStage];
        g.drawString(font, sub, (w - font.width(sub)) / 2, cy + 40, ((int) (a * 230.0F) << 24) | 0xC23040, true);
        if (titleStage == 0) {
            String name = "the memory shatterer";
            g.drawString(font, name, (w - font.width(name)) / 2, cy - 14, ((int) (a * 200.0F) << 24) | 0xB8B2A6, true);
        }
    }

    /** The fight music: looped, not positional, and faded out rather than cut. */
    private static final class Music extends AbstractTickableSoundInstance {
        private boolean fading;

        Music() {
            super(ModSounds.BOSS_MUSIC.get(), SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.volume = 1.0F;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
        }

        void fadeOut() {
            fading = true;
        }

        void stopNow() {
            this.stop();
        }

        @Override
        public void tick() {
            if (!fading) return;
            this.volume -= 0.025F;
            if (this.volume <= 0.0F) this.stop();
        }
    }
}
