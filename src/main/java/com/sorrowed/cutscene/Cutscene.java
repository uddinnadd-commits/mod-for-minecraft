package com.sorrowed.cutscene;

import java.util.ArrayList;
import java.util.List;

/**
 * A camera sequence. Camera positions are relative to an anchor (usually the player) in the
 * anchor's own frame: right, forward, up in blocks, yaw/pitch/roll in degrees, fov in degrees.
 * A pitch of NaN means "whatever the player was looking at when it started".
 */
public final class Cutscene {
    public record Key(int tick, double right, double fwd, double up, float yaw, float pitch, float fov, float roll) {}

    public record Caption(int tick, String text) {}

    public record Cue(int tick, String sound, float volume, float pitch) {}

    private final String id;
    private final int duration;
    private final int blendOut;
    private final int fadeIn;
    private final int fadeOut;
    private final int afterFade;
    private final List<Key> keys;
    private final List<Caption> captions;
    private final List<Cue> cues;

    private Cutscene(Builder b) {
        this.id = b.id;
        this.duration = b.duration;
        this.blendOut = b.blendOut;
        this.fadeIn = b.fadeIn;
        this.fadeOut = b.fadeOut;
        this.afterFade = b.afterFade;
        this.keys = List.copyOf(b.keys);
        this.captions = List.copyOf(b.captions);
        this.cues = List.copyOf(b.cues);
    }

    public String id() { return id; }
    public int duration() { return duration; }
    public int blendOut() { return blendOut; }
    public int fadeIn() { return fadeIn; }
    public int fadeOut() { return fadeOut; }
    public int afterFade() { return afterFade; }
    public List<Key> keys() { return keys; }
    public List<Caption> captions() { return captions; }
    public List<Cue> cues() { return cues; }

    public static Builder builder(String id, int duration) {
        return new Builder(id, duration);
    }

    public static final class Builder {
        private final String id;
        private final int duration;
        private int blendOut = 30;
        private int fadeIn;
        private int fadeOut;
        private int afterFade;
        private final List<Key> keys = new ArrayList<>();
        private final List<Caption> captions = new ArrayList<>();
        private final List<Cue> cues = new ArrayList<>();

        private Builder(String id, int duration) {
            this.id = id;
            this.duration = duration;
        }

        public Builder blendOut(int ticks) { this.blendOut = ticks; return this; }

        public Builder fade(int in, int out, int after) {
            this.fadeIn = in;
            this.fadeOut = out;
            this.afterFade = after;
            return this;
        }

        public Builder key(int tick, double right, double fwd, double up, float yaw, float pitch, float fov, float roll) {
            keys.add(new Key(tick, right, fwd, up, yaw, pitch, fov, roll));
            return this;
        }

        public Builder key(int tick, double right, double fwd, double up, float yaw, float pitch, float fov) {
            return key(tick, right, fwd, up, yaw, pitch, fov, 0.0F);
        }

        public Builder caption(int tick, String text) {
            captions.add(new Caption(tick, text));
            return this;
        }

        public Builder cue(int tick, String sound, float volume, float pitch) {
            cues.add(new Cue(tick, sound, volume, pitch));
            return this;
        }

        /** Camera circling the anchor, always looking at it. Angle 0 is straight in front. */
        public Builder orbit(int t0, int t1, double radius, double height, double a0, double a1,
                             double targetUp, float fov, int steps) {
            float pitch = (float) Math.toDegrees(Math.atan2(height - targetUp, radius));
            for (int i = 0; i <= steps; i++) {
                double f = i / (double) steps;
                double ang = a0 + (a1 - a0) * f;
                double rad = Math.toRadians(ang);
                key(t0 + (int) Math.round((t1 - t0) * f), radius * Math.sin(rad), radius * Math.cos(rad),
                        height, (float) (ang + 180.0D), pitch, fov, 0.0F);
            }
            return this;
        }

        public Cutscene build() {
            return new Cutscene(this);
        }
    }
}
