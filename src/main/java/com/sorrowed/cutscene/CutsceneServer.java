package com.sorrowed.cutscene;

import com.sorrowed.SorrowedConfig;
import com.sorrowed.mind.Scheduler;
import com.sorrowed.net.ModNetwork;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server side of a cutscene: tell the client to start, make the player untouchable for the
 * duration, play the sound cues, and run a callback when it is over.
 */
public final class CutsceneServer {
    private CutsceneServer() {}

    private static final Map<UUID, Long> PLAYING = new HashMap<>();

    public static boolean isPlaying(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        if (server == null) return false;
        Long until = PLAYING.get(p.getUUID());
        return until != null && server.getTickCount() < until;
    }

    public static boolean play(ServerPlayer p, String id, Vec3 anchor) {
        return play(p, id, anchor, null);
    }

    /** Returns false (and does nothing) if cutscenes are off, the id is unknown, or one is already playing. */
    public static boolean play(ServerPlayer p, String id, Vec3 anchor, Runnable onEnd) {
        if (!SorrowedConfig.CUTSCENES.get()) return false;
        Cutscene c = CutsceneLibrary.get(id);
        MinecraftServer server = p.getServer();
        if (c == null || server == null || isPlaying(p)) return false;

        PLAYING.put(p.getUUID(), server.getTickCount() + c.duration() + 10L);
        ModNetwork.cutscene(p, id, anchor);
        // Resistance V: nothing short of the void can hurt a player who is busy watching.
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, c.duration() + 40, 4, false, false));

        for (Cutscene.Cue cue : c.cues()) {
            Scheduler.after(server, cue.tick(), () -> {
                ResourceLocation rl = ResourceLocation.tryParse(cue.sound());
                SoundEvent sound = rl == null ? null : BuiltInRegistries.SOUND_EVENT.get(rl);
                if (sound != null && !p.isRemoved()) {
                    p.playNotifySound(sound, SoundSource.AMBIENT, cue.volume(), cue.pitch());
                }
            });
        }
        if (onEnd != null) {
            Scheduler.after(server, c.duration(), () -> {
                if (!p.isRemoved()) onEnd.run();
            });
        }
        return true;
    }

    public static void forget(UUID id) {
        PLAYING.remove(id);
    }
}
