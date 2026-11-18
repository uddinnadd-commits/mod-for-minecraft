package com.sorrowed.story;

import com.sorrowed.mind.Scheduler;
import com.sorrowed.mind.Sorrow;
import com.sorrowed.net.ModNetwork;
import com.sorrowed.net.ObjectivesPacket;
import com.sorrowed.worldgen.MemorySites;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Where each player has got to: which objectives are done, which parts of the bigger ones, what
 * they typed back to him, and whether the story has ended for them. Kept in the persisted part of
 * the player's data, so it survives death.
 */
public final class Progress {
    private Progress() {}

    public static final int PLAYING = 0;
    /** Everything is done. He is waiting at the pond. */
    public static final int READY = 1;
    public static final int ENDED = 2;
    /** He turned into the Memory Shatterer and you are fighting it. Never saved: a fight that is cut off goes back to READY. */
    public static final int FIGHTING = 3;
    /** You won. He comes back down, with a halo, to say goodbye properly. */
    public static final int FAREWELL = 4;

    private static final String PERSIST = "PlayerPersisted";
    private static final String KEY = "sorrowed_story";

    private static CompoundTag read(ServerPlayer p) {
        return p.getPersistentData().getCompound(PERSIST).getCompound(KEY);
    }

    private static void write(ServerPlayer p, CompoundTag story) {
        CompoundTag root = p.getPersistentData();
        CompoundTag saved = root.getCompound(PERSIST);
        saved.put(KEY, story);
        root.put(PERSIST, saved);
    }

    public static boolean done(ServerPlayer p, String id) {
        return read(p).getCompound("done").getBoolean(id);
    }

    public static int steps(ServerPlayer p, String id) {
        return read(p).getCompound("steps").getInt(id);
    }

    public static int stage(ServerPlayer p) {
        return read(p).getInt("stage");
    }

    public static String reply(ServerPlayer p) {
        return read(p).getString("reply");
    }

    public static void setReply(ServerPlayer p, String text) {
        CompoundTag s = read(p);
        s.putString("reply", text);
        write(p, s);
    }

    public static void setStage(ServerPlayer p, int stage) {
        CompoundTag s = read(p);
        s.putInt("stage", stage);
        write(p, s);
    }

    /** One-off story flags, such as having seen him turn once already. */
    public static boolean flag(ServerPlayer p, String name) {
        return read(p).getCompound("flags").getBoolean(name);
    }

    public static void setFlag(ServerPlayer p, String name) {
        CompoundTag s = read(p);
        CompoundTag flags = s.getCompound("flags");
        flags.putBoolean(name, true);
        s.put("flags", flags);
        write(p, s);
    }

    public static int count(ServerPlayer p) {
        CompoundTag done = read(p).getCompound("done");
        int n = 0;
        for (Objectives.Objective o : Objectives.ALL) {
            if (done.getBoolean(o.id())) n++;
        }
        return n;
    }

    public static boolean allDone(ServerPlayer p) {
        return count(p) >= Objectives.ALL.size();
    }

    /** Records one part of an objective; finishing the last part finishes the objective. Returns true if it was new. */
    public static boolean step(ServerPlayer p, String id, int bit) {
        Objectives.Objective o = Objectives.get(id);
        if (o == null || done(p, id)) return false;
        CompoundTag s = read(p);
        CompoundTag steps = s.getCompound("steps");
        int mask = steps.getInt(id);
        if ((mask & (1 << bit)) != 0) return false;
        mask |= 1 << bit;
        steps.putInt(id, mask);
        s.put("steps", steps);
        write(p, s);
        if ((mask & o.fullMask()) == o.fullMask()) {
            complete(p, id);
        } else {
            p.playNotifySound(SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.7F, 1.2F);
            sync(p, "");
        }
        return true;
    }

    /** Finishes an objective: the line, the chime, a little of the weight lifted. Returns true if it was new. */
    public static boolean complete(ServerPlayer p, String id) {
        Objectives.Objective o = Objectives.get(id);
        if (o == null || done(p, id)) return false;
        CompoundTag s = read(p);
        CompoundTag done = s.getCompound("done");
        done.putBoolean(id, true);
        s.put("done", done);
        CompoundTag steps = s.getCompound("steps");
        steps.putInt(id, o.fullMask());
        s.put("steps", steps);
        write(p, s);

        Sorrow.add(p, -12.0F);
        p.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.5F);
        ModNetwork.thought(p, Thoughts.f(o.done()));
        sync(p, id);
        if (allDone(p) && stage(p) == PLAYING) {
            MinecraftServer server = p.getServer();
            if (server != null) {
                Scheduler.after(server, 160, () -> {
                    if (!p.isRemoved() && stage(p) == PLAYING) Finale.ready(p);
                });
            }
        }
        return true;
    }

    public static void reset(ServerPlayer p) {
        write(p, new CompoundTag());
        sync(p, "");
    }

    public static void sync(ServerPlayer p) {
        sync(p, "");
    }

    /** Sends the whole picture to the player. justDone names an objective that was finished just now, for the flourish. */
    public static void sync(ServerPlayer p, String justDone) {
        CompoundTag s = read(p);
        CompoundTag done = s.getCompound("done");
        CompoundTag steps = s.getCompound("steps");
        int n = Objectives.ALL.size();
        boolean[] d = new boolean[n];
        int[] st = new int[n];
        for (int i = 0; i < n; i++) {
            String id = Objectives.ALL.get(i).id();
            d[i] = done.getBoolean(id);
            st[i] = steps.getInt(id);
        }
        ModNetwork.objectives(p, new ObjectivesPacket(s.getInt("stage"), d, st, s.getString("reply"),
                MemorySites.targets(p), justDone == null ? "" : justDone));
    }
}
