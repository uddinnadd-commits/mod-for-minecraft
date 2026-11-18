package com.sorrowed.story;

import com.sorrowed.SorrowedConfig;
import com.sorrowed.SorrowedData;
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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The end. Once every objective is done the Backup starts to move, and he is sitting at the end
 * of the dock. Walk up to him and you get to say goodbye; after that the screen goes white, you
 * wake up at home, and it has stopped raining.
 */
public final class Finale {
    private Finale() {}

    /** Where he sits: the end of the dock at the pond, facing the water. */
    public static final Vec3 SEAT = new Vec3(-23.5D, Sites.GROUND + 1, -33.5D);
    public static final float SEAT_YAW = -90.0F;
    /** Where the float sits on the water, a little way out from him. */
    private static final Vec3 FLOAT = new Vec3(-20.5D, Sites.GROUND + 0.95D, -33.0D);

    /** He speaks in the fallback (no cutscenes) and in the cutscene captions alike. */
    public static final String[] LINES = {
            "...{friend}?",
            "hey. you took so long.",
            "i'm sorry i made you look in all of them. i couldn't say it, so i built it.",
            "look. the float's moving. it's never done that.",
            "it's the day after. it was always going to be.",
            "go home. it stopped raining.",
            "leave the door open for whoever comes next.",
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

    /** True if anyone in this level still has him to meet. */
    public static boolean anyoneWaiting(ServerLevel level) {
        for (ServerPlayer p : level.players()) {
            if (Progress.stage(p) == Progress.READY) return true;
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

    /** You reach him. */
    public static void meet(ServerPlayer p, FriendEntity him) {
        if (Progress.stage(p) != Progress.READY || MEETING.contains(p.getUUID())) return;
        if (CutsceneServer.isPlaying(p)) return;
        MinecraftServer server = p.getServer();
        if (server == null) return;
        MEETING.add(p.getUUID());
        Sorrow.set(p, 0);
        if (SorrowedConfig.CUTSCENES.get() && CutsceneLibrary.get("goodbye") != null) {
            int duration = CutsceneLibrary.get("goodbye").duration();
            boolean played = CutsceneServer.play(p, "goodbye", SEAT, null);
            if (played) {
                // The float, moving for the first time.
                ServerLevel level = p.serverLevel();
                for (int t : new int[] {470, 520, 600}) {
                    Scheduler.after(server, t, () -> level.sendParticles(ParticleTypes.SPLASH,
                            FLOAT.x, FLOAT.y, FLOAT.z, 14, 0.15D, 0.05D, 0.15D, 0.1D));
                }
                // The white comes up over the last shot; the cutscene ends underneath it.
                Scheduler.after(server, duration - 50, () -> {
                    if (!p.isRemoved()) end(p);
                });
                return;
            }
        }
        // No cutscenes: the same words, one after another, where you stand.
        for (int i = 0; i < LINES.length; i++) {
            String line = Thoughts.f(LINES[i]);
            Scheduler.after(server, 20 + i * 110, () -> {
                if (!p.isRemoved()) ModNetwork.thought(p, line);
            });
        }
        Scheduler.after(server, 40 + LINES.length * 110, () -> {
            if (!p.isRemoved()) end(p);
        });
    }

    /** The screen goes white and the rain stops. You go home once you have read his letter. */
    private static void end(ServerPlayer p) {
        MEETING.remove(p.getUUID());
        if (Progress.stage(p) != Progress.READY) return;
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
        Vec3 where = wakeSpot(home);
        p.teleportTo(home, where.x, where.y, where.z, 180.0F, 0.0F);
        p.getInventory().add(PapersItem.of("goodbye"));
        Progress.sync(p);
    }

    /** By his grave, if there is one; otherwise the world spawn. Facing the grave. */
    private static Vec3 wakeSpot(ServerLevel home) {
        BlockPos grave = MemorySites.grave(home);
        BlockPos base = grave != null ? grave.offset(0, 0, 3) : home.getSharedSpawnPos();
        BlockPos top = home.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, base);
        return Vec3.atBottomCenterOf(top);
    }

    public static void forget(UUID id) {
        MEETING.remove(id);
    }
}
