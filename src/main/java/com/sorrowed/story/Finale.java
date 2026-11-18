package com.sorrowed.story;

import com.sorrowed.SorrowedConfig;
import com.sorrowed.SorrowedData;
import com.sorrowed.cutscene.Cutscene;
import com.sorrowed.cutscene.CutsceneLibrary;
import com.sorrowed.cutscene.CutsceneServer;
import com.sorrowed.entity.FriendEntity;
import com.sorrowed.item.PapersItem;
import com.sorrowed.mind.Scheduler;
import com.sorrowed.mind.Sorrow;
import com.sorrowed.net.ModNetwork;
import com.sorrowed.registry.Dimensions;
import com.sorrowed.registry.ModEntities;
import com.sorrowed.worldgen.MemorySites;
import com.sorrowed.worldgen.Sites;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The end. Once every objective is done the Backup starts to move, and he is sitting at the end
 * of the dock. Walk up to him and he starts to say goodbye, and then turns ({@link BossFight}).
 * Beat what he turns into and he comes back down out of the sky, himself, with a halo; after that
 * the screen goes white, you wake up at home, and it has stopped raining.
 */
public final class Finale {
    private Finale() {}

    /** Where he sits: the end of the dock at the pond, facing the water. */
    public static final Vec3 SEAT = new Vec3(-23.5D, Sites.GROUND + 1, -33.5D);
    public static final float SEAT_YAW = -90.0F;
    /** Where the float sits on the water, a little way out from him. */
    private static final Vec3 FLOAT = new Vec3(-20.5D, Sites.GROUND + 0.95D, -33.0D);

    /** He speaks in the fallback (no cutscenes) and in the cutscene captions alike: himself again, after. */
    public static final String[] LINES = {
            "...look up.",
            "that wasn't me. that was everything i wouldn't let go of.",
            "you broke it. thank you.",
            "look. the float's moving. it's never done that.",
            "it's the day after now. it was always going to be.",
            "go home. it stopped raining.",
            "leave the door open.",
    };

    private static final Set<UUID> MEETING = new HashSet<>();
    private static final Set<UUID> HOMEWARD = new HashSet<>();

    /** Everything is done: tell them, and let his world start moving. */
    public static void ready(ServerPlayer p) {
        if (Progress.stage(p) != Progress.PLAYING) return;
        Progress.setStage(p, Progress.READY);
        Progress.sync(p);
        MinecraftServer server = p.getServer();
        ModNetwork.thought(p, "That's all of them. Every one.");
        if (server == null) return;
        Scheduler.after(server, 120, () -> {
            if (p.isRemoved()) return;
            ModNetwork.thought(p, "Something's different in his world. I can feel it from here.");
            ModNetwork.thought(p, Thoughts.f("{friend} is at the pond. He's waiting."));
        });
    }

    /** True if anyone in this level still has him to meet, or to say goodbye to. */
    public static boolean anyoneWaiting(ServerLevel level) {
        for (ServerPlayer p : level.players()) {
            int stage = Progress.stage(p);
            if (stage == Progress.READY || stage == Progress.FAREWELL) return true;
        }
        return false;
    }

    /** Called twice a second for players with everything done. */
    public static void tick(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        if (!level.dimension().equals(Dimensions.BACKUP)) return;
        if (p.distanceToSqr(SEAT) > 96.0D * 96.0D) return;
        FriendEntity him = ensureHim(level);
        if (him != null && p.distanceToSqr(him) < 4.5D * 4.5D) meet(p, him);
    }

    private static FriendEntity ensureHim(ServerLevel level) {
        if (!level.isLoaded(BlockPos.containing(SEAT))) return null;
        List<FriendEntity> found = level.getEntitiesOfClass(FriendEntity.class, new AABB(BlockPos.containing(SEAT)).inflate(8.0D));
        if (!found.isEmpty()) return found.get(0);
        FriendEntity him = ModEntities.FRIEND.get().create(level);
        if (him == null) return null;
        him.moveTo(SEAT.x, SEAT.y, SEAT.z, SEAT_YAW, 0.0F);
        him.setYHeadRot(SEAT_YAW);
        him.setYBodyRot(SEAT_YAW);
        level.addFreshEntity(him);
        return him;
    }

    /** You reach him. He starts to say goodbye, and then he will not let you finish it. */
    public static void meet(ServerPlayer p, FriendEntity him) {
        if (Progress.stage(p) != Progress.READY || MEETING.contains(p.getUUID())) return;
        if (CutsceneServer.isPlaying(p) || him.isHalo()) return;
        MEETING.add(p.getUUID());
        Sorrow.set(p, 0);
        BossFight.begin(p, him);
    }

    /** After the fight: he comes down out of the sky, himself again, with a halo, and says it properly. */
    public static void farewell(ServerPlayer p) {
        if (Progress.stage(p) != Progress.FAREWELL) return;
        MinecraftServer server = p.getServer();
        if (server == null) return;
        ServerLevel level = p.serverLevel();
        if (level.dimension().equals(Dimensions.BACKUP)) {
            for (FriendEntity f : level.getEntitiesOfClass(FriendEntity.class, new AABB(BlockPos.containing(SEAT)).inflate(8.0D))) {
                f.discard();
            }
            FriendEntity angel = ModEntities.FRIEND.get().create(level);
            if (angel != null) {
                angel.setHalo(true);
                angel.moveTo(SEAT.x, SEAT.y, SEAT.z, 90.0F, 0.0F);
                angel.setYHeadRot(90.0F);
                angel.setYBodyRot(90.0F);
                level.addFreshEntity(angel);
            }
            level.playSound(null, BlockPos.containing(SEAT), SoundEvents.BEACON_ACTIVATE, SoundSource.AMBIENT, 2.0F, 0.6F);
        }
        Cutscene scene = CutsceneLibrary.get("farewell");
        if (SorrowedConfig.CUTSCENES.get() && scene != null && CutsceneServer.play(p, "farewell", SEAT, null)) {
            // The float, moving for the first time.
            for (int t : new int[] {430, 470, 520}) {
                Scheduler.after(server, t, () -> level.sendParticles(ParticleTypes.SPLASH,
                        FLOAT.x, FLOAT.y, FLOAT.z, 14, 0.15D, 0.05D, 0.15D, 0.1D));
            }
            // The white comes up over the last shot; the cutscene ends underneath it.
            Scheduler.after(server, scene.duration() - 50, () -> {
                if (!p.isRemoved()) end(p);
            });
            return;
        }
        // No cutscenes: the same words, one after another, where you stand.
        for (int i = 0; i < LINES.length; i++) {
            String line = Thoughts.f(LINES[i]);
            Scheduler.after(server, 60 + i * 110, () -> {
                if (!p.isRemoved()) ModNetwork.thought(p, line);
            });
        }
        Scheduler.after(server, 80 + LINES.length * 110, () -> {
            if (!p.isRemoved()) end(p);
        });
    }

    /** The screen goes white and the rain stops. You go home once you have read his letter. */
    public static void end(ServerPlayer p) {
        MEETING.remove(p.getUUID());
        if (Progress.stage(p) != Progress.FAREWELL) return;
        MinecraftServer server = p.getServer();
        if (server == null) return;
        Progress.setStage(p, Progress.ENDED);
        ModNetwork.ending(p);

        ServerLevel home = server.overworld();
        SorrowedData data = SorrowedData.get(home);
        data.setEnded();
        home.setWeatherParameters(24000 * 5, 0, false, false);
        long day = home.getDayTime() % 24000L;
        if (day > 12800L && day < 23200L) home.setDayTime(home.getDayTime() + (23400L - day));

        UUID id = p.getUUID();
        HOMEWARD.add(id);
        // If the letter is never closed, take them home anyway.
        Scheduler.after(server, 20 * 150, () -> {
            ServerPlayer again = server.getPlayerList().getPlayer(id);
            if (again != null) goHome(again);
        });
    }

    /** Wake up by his grave, in the overworld, with his last note in your pocket. */
    public static void goHome(ServerPlayer p) {
        if (!HOMEWARD.remove(p.getUUID())) return;
        MinecraftServer server = p.getServer();
        if (server == null) return;
        ServerLevel home = server.overworld();
        BlockPos grave = MemorySites.grave(home);
        Vec3 where = wakeSpot(home, grave);
        float yaw = grave == null ? 180.0F
                : (float) Math.toDegrees(Math.atan2(grave.getZ() + 0.5D - where.z, grave.getX() + 0.5D - where.x)) - 90.0F;
        p.teleportTo(home, where.x, where.y, where.z, yaw, 0.0F);
        p.getInventory().add(PapersItem.of("goodbye"));
        Progress.sync(p);
    }

    /** Where to stand around the grave, nearest first; the first one with dry ground wins. */
    private static final int[][] AROUND = {
            {0, 3}, {3, 0}, {-3, 0}, {0, -3}, {2, 2}, {-2, 2}, {2, -2}, {-2, -2},
            {0, 5}, {5, 0}, {-5, 0}, {0, -5}, {4, 4}, {-4, 4}, {4, -4}, {-4, -4}, {0, 0},
    };

    /** On dry ground by his grave, if there is one; otherwise the world spawn. Never in water. */
    private static Vec3 wakeSpot(ServerLevel home, BlockPos grave) {
        if (grave != null) {
            for (int[] o : AROUND) {
                BlockPos top = dryTop(home, grave.offset(o[0], 0, o[1]));
                if (top != null) return Vec3.atBottomCenterOf(top);
            }
        }
        BlockPos spawn = home.getSharedSpawnPos();
        BlockPos top = dryTop(home, spawn);
        return Vec3.atBottomCenterOf(top != null ? top : home.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn));
    }

    /** The open block above solid ground in this column, or null if the ground there is water or lava. */
    private static BlockPos dryTop(ServerLevel level, BlockPos at) {
        level.getChunkAt(at);   // an unloaded chunk reports the bottom of the world as its height
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at);
        return level.getFluidState(top.below()).isEmpty() && level.getFluidState(top).isEmpty() ? top : null;
    }

    public static void forget(UUID id) {
        MEETING.remove(id);
    }
}
