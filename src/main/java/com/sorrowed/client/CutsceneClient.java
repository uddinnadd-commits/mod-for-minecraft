package com.sorrowed.client;

import com.sorrowed.cutscene.Cutscene;
import com.sorrowed.cutscene.CutsceneLibrary;
import com.sorrowed.story.Thoughts;
import net.minecraft.client.CameraType;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Plays a cutscene by swapping the camera to an invisible armor stand (never added to the world)
 * and moving it along the keyframes. The player stays where they are, frozen and untouchable.
 */
public final class CutsceneClient {
    private CutsceneClient() {}

    private record Pose(double x, double y, double z, float yaw, float pitch, float fov, float roll) {}

    private static Cutscene active;
    private static int tick;
    private static double ax;
    private static double ay;
    private static double az;
    private static float ayaw;
    private static float startPitch;
    private static ArmorStand cam;
    private static Entity savedCamera;
    private static CameraType savedType;
    private static int postFade;
    private static int postFadeTotal = 1;
    private static float curFov = 70.0F;
    private static float curRoll;
    private static float curYaw;
    private static float curPitch;
    private static Pose smooth;
    private static long lastFrameMs;

    public static boolean isActive() {
        return active != null;
    }

    public static float fov() {
        return curFov;
    }

    public static float roll() {
        return curRoll;
    }

    public static float yaw() {
        return curYaw;
    }

    public static float pitch() {
        return curPitch;
    }

    public static void start(String id, boolean anchored, double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        if (active != null || mc.level == null || mc.player == null) return;
        Cutscene c = CutsceneLibrary.get(id);
        if (c == null) return;

        active = c;
        tick = 0;
        startPitch = mc.player.getXRot();
        if (anchored) {
            ax = x;
            ay = y;
            az = z;
            ayaw = 0.0F;
        } else {
            ax = mc.player.getX();
            ay = mc.player.getY();
            az = mc.player.getZ();
            ayaw = mc.player.getYRot();
        }

        cam = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
        cam.setInvisible(true);
        cam.setNoGravity(true);
        savedCamera = mc.getCameraEntity();
        savedType = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        smooth = null;
        lastFrameMs = Util.getMillis();
        place(pose(mc, 0.0F), true);
        mc.setCameraEntity(cam);
    }

    /**
     * Called every frame before the world renders. The camera is moved to the pose at the
     * fractional time (tick + partial) and its "previous" position is set to the same spot,
     * so the renderer never interpolates between two tick positions and nothing judders.
     */
    public static void frame(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (active == null || cam == null || mc.level == null || mc.player == null) return;
        if (mc.isPaused()) partialTick = 0.0F;
        Pose target = pose(mc, Math.min(tick + partialTick, active.duration()));
        // A short, time-based follow on top of the spline soaks up any remaining stepping between
        // frames (uneven frame times, tick boundaries) without adding visible lag.
        long now = Util.getMillis();
        float dt = Math.min(0.1F, (now - lastFrameMs) / 1000.0F);
        lastFrameMs = now;
        if (smooth == null) {
            smooth = target;
        } else {
            float k = 1.0F - (float) Math.exp(-dt * 22.0D);
            smooth = new Pose(lerp(smooth.x(), target.x(), k), lerp(smooth.y(), target.y(), k), lerp(smooth.z(), target.z(), k),
                    smooth.yaw() + Mth.wrapDegrees(target.yaw() - smooth.yaw()) * k,
                    (float) lerp(smooth.pitch(), target.pitch(), k), (float) lerp(smooth.fov(), target.fov(), k),
                    (float) lerp(smooth.roll(), target.roll(), k));
        }
        place(smooth, true);
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (postFade > 0 && !mc.isPaused()) postFade--;
        if (active == null) return;
        if (mc.level == null || mc.player == null) {
            finish(mc, false);
            return;
        }
        if (mc.isPaused()) return;

        tick++;
        for (Cutscene.Caption caption : active.captions()) {
            if (caption.tick() == tick) ThoughtOverlay.push(Thoughts.f(caption.text()), true);
        }
        if (tick >= active.duration()) {
            finish(mc, true);
            return;
        }
    }

    public static void abort() {
        if (active != null) finish(Minecraft.getInstance(), false);
        postFade = 0;
    }

    private static void finish(Minecraft mc, boolean fadeAfter) {
        if (cam != null) {
            mc.setCameraEntity(savedCamera != null ? savedCamera : mc.player);
            if (savedType != null) mc.options.setCameraType(savedType);
        }
        if (fadeAfter && active != null && active.afterFade() > 0) {
            postFadeTotal = active.afterFade();
            postFade = postFadeTotal;
        }
        active = null;
        cam = null;
        savedCamera = null;
        curFov = 70.0F;
        curRoll = 0.0F;
    }

    private static void place(Pose p, boolean first) {
        if (cam == null) return;
        cam.setPos(p.x(), p.y() - cam.getEyeHeight(), p.z());
        cam.setYRot(p.yaw());
        cam.setXRot(p.pitch());
        cam.setYHeadRot(p.yaw());
        cam.setOldPosAndRot();
        curFov = p.fov();
        curRoll = p.roll();
        curYaw = p.yaw();
        curPitch = p.pitch();
    }

    private static double lerp(double a, double b, float f) {
        return a + (b - a) * f;
    }

    /** Cubic Hermite with Catmull-Rom tangents, non-uniform in time; still at both ends. */
    private static double spline(List<Cutscene.Key> keys, int i, float s, java.util.function.ToDoubleFunction<Cutscene.Key> v) {
        int n = keys.size();
        Cutscene.Key k1 = keys.get(i);
        Cutscene.Key k2 = keys.get(Math.min(i + 1, n - 1));
        double t1 = k1.tick();
        double t2 = k2.tick();
        double span = Math.max(1.0D, t2 - t1);
        double p1 = v.applyAsDouble(k1);
        double p2 = v.applyAsDouble(k2);
        double m1 = 0.0D;
        double m2 = 0.0D;
        if (i > 0) {
            Cutscene.Key k0 = keys.get(i - 1);
            m1 = (p2 - v.applyAsDouble(k0)) / Math.max(1.0D, t2 - k0.tick()) * span;
        }
        if (i + 2 < n) {
            Cutscene.Key k3 = keys.get(i + 2);
            m2 = (v.applyAsDouble(k3) - p1) / Math.max(1.0D, k3.tick() - t1) * span;
        }
        double s2 = s * s;
        double s3 = s2 * s;
        double h00 = 2.0D * s3 - 3.0D * s2 + 1.0D;
        double h10 = s3 - 2.0D * s2 + s;
        double h01 = -2.0D * s3 + 3.0D * s2;
        double h11 = s3 - s2;
        return h00 * p1 + h10 * m1 + h01 * p2 + h11 * m2;
    }

    private static Pose pose(Minecraft mc, float t) {
        List<Cutscene.Key> keys = active.keys();
        int n = keys.size();
        int i = 0;
        while (i < n - 2 && t >= keys.get(i + 1).tick()) i++;
        Cutscene.Key a = keys.get(i);
        Cutscene.Key b = keys.get(Math.min(i + 1, n - 1));
        float s = 0.0F;
        int span = b.tick() - a.tick();
        if (span > 0) s = Mth.clamp((t - a.tick()) / (float) span, 0.0F, 1.0F);
        if (t >= keys.get(n - 1).tick()) {
            i = n - 1;
            s = 0.0F;
        }

        double right = spline(keys, i, s, Cutscene.Key::right);
        double fwd = spline(keys, i, s, Cutscene.Key::fwd);
        double up = spline(keys, i, s, Cutscene.Key::up);
        float yaw = ayaw + (float) spline(keys, i, s, Cutscene.Key::yaw);
        float pitch = (float) spline(keys, i, s, k -> Float.isNaN(k.pitch()) ? startPitch : k.pitch());
        float fov = (float) spline(keys, i, s, Cutscene.Key::fov);
        float roll = (float) spline(keys, i, s, Cutscene.Key::roll);

        double rad = Math.toRadians(ayaw);
        double fx = -Math.sin(rad);
        double fz = Math.cos(rad);
        double rx = -Math.cos(rad);
        double rz = -Math.sin(rad);
        double x = ax + fwd * fx + right * rx;
        double y = ay + up;
        double z = az + fwd * fz + right * rz;

        int d = active.duration();
        int bo = active.blendOut();
        Player pl = mc.player;
        if (bo > 0 && t > d - bo && pl != null) {
            float bf = (t - (d - bo)) / (float) bo;
            bf = bf * bf * (3.0F - 2.0F * bf);
            x = lerp(x, pl.getX(), bf);
            y = lerp(y, pl.getEyeY(), bf);
            z = lerp(z, pl.getZ(), bf);
            yaw = yaw + Mth.wrapDegrees(pl.getYRot() - yaw) * bf;
            pitch = (float) lerp(pitch, pl.getXRot(), bf);
            fov = (float) lerp(fov, 70.0F, bf);
            roll = roll * (1.0F - bf);
        }
        return new Pose(x, y, z, yaw, pitch, fov, roll);
    }

    /** 0..1: how far the letterbox bars have closed. */
    public static float bars() {
        if (active == null) return 0.0F;
        float in = Math.min(1.0F, tick / 15.0F);
        float out = Math.min(1.0F, (active.duration() - tick) / 15.0F);
        float v = Math.max(0.0F, Math.min(in, out));
        return v * v * (3.0F - 2.0F * v);
    }

    /** 0..1: black-screen amount. */
    public static float fade() {
        if (active != null) {
            float a = 0.0F;
            int d = active.duration();
            if (active.fadeIn() > 0 && tick < active.fadeIn()) a = 1.0F - tick / (float) active.fadeIn();
            if (active.fadeOut() > 0 && tick > d - active.fadeOut()) {
                a = Math.max(a, (tick - (d - active.fadeOut())) / (float) active.fadeOut());
            }
            return Math.max(0.0F, Math.min(1.0F, a));
        }
        if (postFade > 0) return postFade / (float) postFadeTotal;
        return 0.0F;
    }

    public static boolean overlayNeeded() {
        return active != null || postFade > 0;
    }
}
