package com.sorrowed.client;

import com.sorrowed.cutscene.Cutscene;
import com.sorrowed.cutscene.CutsceneLibrary;
import com.sorrowed.story.Thoughts;
import net.minecraft.client.CameraType;
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

    public static boolean isActive() {
        return active != null;
    }

    public static float fov() {
        return curFov;
    }

    public static float roll() {
        return curRoll;
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
        place(pose(mc, 0), true);
        mc.setCameraEntity(cam);
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
        place(pose(mc, tick), false);
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
        if (!first) {
            cam.xo = cam.getX();
            cam.yo = cam.getY();
            cam.zo = cam.getZ();
            cam.xOld = cam.getX();
            cam.yOld = cam.getY();
            cam.zOld = cam.getZ();
            cam.xRotO = cam.getXRot();
            cam.yRotO = cam.getYRot();
        }
        cam.setPos(p.x(), p.y() - cam.getEyeHeight(), p.z());
        cam.setYRot(p.yaw());
        cam.setXRot(p.pitch());
        if (first) {
            cam.xo = cam.getX();
            cam.yo = cam.getY();
            cam.zo = cam.getZ();
            cam.xOld = cam.getX();
            cam.yOld = cam.getY();
            cam.zOld = cam.getZ();
            cam.xRotO = p.pitch();
            cam.yRotO = p.yaw();
        }
        curFov = p.fov();
        curRoll = p.roll();
    }

    private static double lerp(double a, double b, float f) {
        return a + (b - a) * f;
    }

    private static Pose pose(Minecraft mc, int t) {
        List<Cutscene.Key> keys = active.keys();
        Cutscene.Key a = keys.get(0);
        Cutscene.Key b = a;
        Cutscene.Key last = keys.get(keys.size() - 1);
        if (t >= last.tick()) {
            a = last;
            b = last;
        } else if (t > a.tick()) {
            for (int i = 0; i < keys.size() - 1; i++) {
                if (t >= keys.get(i).tick() && t < keys.get(i + 1).tick()) {
                    a = keys.get(i);
                    b = keys.get(i + 1);
                    break;
                }
            }
        }

        float f = 0.0F;
        int span = b.tick() - a.tick();
        if (span > 0) {
            f = (t - a.tick()) / (float) span;
            if (span > 30) f = f * f * (3.0F - 2.0F * f);
        }

        double right = lerp(a.right(), b.right(), f);
        double fwd = lerp(a.fwd(), b.fwd(), f);
        double up = lerp(a.up(), b.up(), f);
        float pa = Float.isNaN(a.pitch()) ? startPitch : a.pitch();
        float pb = Float.isNaN(b.pitch()) ? startPitch : b.pitch();
        float yaw = ayaw + (float) lerp(a.yaw(), b.yaw(), f);
        float pitch = (float) lerp(pa, pb, f);
        float fov = (float) lerp(a.fov(), b.fov(), f);
        float roll = (float) lerp(a.roll(), b.roll(), f);

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
