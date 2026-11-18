package com.sorrowed.mind;

import com.sorrowed.Sorrowed;
import com.sorrowed.SorrowedConfig;
import com.sorrowed.cutscene.CutsceneServer;
import com.sorrowed.entity.HollowEntity;
import com.sorrowed.entity.ListenerEntity;
import com.sorrowed.entity.MasonEntity;
import com.sorrowed.entity.ReflectionEntity;
import com.sorrowed.net.ModNetwork;
import com.sorrowed.registry.Dimensions;
import com.sorrowed.registry.ModBlocks;
import com.sorrowed.registry.ModEntities;
import com.sorrowed.story.Thoughts;
import com.sorrowed.worldgen.GateBuilder;
import com.sorrowed.worldgen.LiminalGenerator;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Things that happen to you. Every ten seconds each player rolls for one; the higher their Sorrow,
 * the likelier it is and the worse the pool gets. /sorrowed event <name> fires one by hand.
 */
public final class SorrowEvents {
    private SorrowEvents() {}

    private record Ev(String name, int minTier, Set<ResourceKey<Level>> dims, Predicate<ServerPlayer> run) {}

    private static final Set<ResourceKey<Level>> OVERWORLD = Set.of(Level.OVERWORLD);
    private static final Set<ResourceKey<Level>> WILD = Set.of(Level.OVERWORLD, Dimensions.HUSH);
    private static final Set<ResourceKey<Level>> LONELY = Set.of(Level.OVERWORLD, Dimensions.HALLWAY, Dimensions.ARCHIVE, Dimensions.STREET, Dimensions.WARD);
    private static final Set<ResourceKey<Level>> HOME = Set.of(Dimensions.BACKUP);

    private static final List<Ev> EVENTS = List.of(
            new Ev("footsteps", 1, null, SorrowEvents::footsteps),
            new Ev("silence", 1, OVERWORLD, SorrowEvents::silence),
            new Ev("whisper", 2, null, SorrowEvents::whisper),
            new Ev("thunder", 2, OVERWORLD, SorrowEvents::thunder),
            new Ev("ghostgrave", 2, WILD, SorrowEvents::ghostGrave),
            new Ev("reflection", 2, WILD, SorrowEvents::reflection),
            new Ev("listener", 2, LONELY, SorrowEvents::listener),
            new Ev("fakejoin", 3, null, SorrowEvents::fakeJoin),
            new Ev("doorbehind", 3, OVERWORLD, SorrowEvents::doorBehind),
            new Ev("hollowcircle", 3, WILD, SorrowEvents::hollowCircle),
            new Ev("echoclose", 3, WILD, SorrowEvents::echoClose),
            new Ev("mason", 0, HOME, SorrowEvents::mason)
    );

    private static final Map<UUID, Long> NEXT_ALLOWED = new HashMap<>();

    public static String names() {
        List<String> n = new ArrayList<>();
        for (Ev e : EVENTS) n.add(e.name());
        return String.join(", ", n);
    }

    /** Called every ten seconds per player. */
    public static void roll(ServerPlayer p) {
        if (!SorrowedConfig.EVENTS.get()) return;
        if (p.isCreative() || p.isSpectator() || CutsceneServer.isPlaying(p)) return;
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        Long next = NEXT_ALLOWED.get(p.getUUID());
        if (next != null && now < next) return;

        float sorrow = Sorrow.get(p);
        float chance = 0.05F + (sorrow / 100.0F) * 0.30F;
        if (level.random.nextFloat() > chance) return;

        int tier = Sorrow.tier(p);
        ResourceKey<Level> dim = level.dimension();
        List<Ev> pool = new ArrayList<>();
        for (Ev e : EVENTS) {
            if (tier >= e.minTier() && (e.dims() == null || e.dims().contains(dim))) pool.add(e);
        }
        if (pool.isEmpty()) return;

        Ev chosen = pool.get(level.random.nextInt(pool.size()));
        if (chosen.run().test(p)) {
            NEXT_ALLOWED.put(p.getUUID(), now + 1200L + level.random.nextInt(1200));
            Sorrowed.LOGGER.debug("Event {} fired for {}", chosen.name(), p.getName().getString());
        }
    }

    /** Fire one event by name, ignoring tier and cooldown. Used by the command. */
    public static boolean fire(String name, ServerPlayer p) {
        for (Ev e : EVENTS) {
            if (e.name().equalsIgnoreCase(name)) return e.run().test(p);
        }
        return false;
    }

    public static void forget(UUID id) {
        NEXT_ALLOWED.remove(id);
    }

    // ------------------------------------------------------------------ helpers

    private static Vec3 behind(ServerPlayer p) {
        Vec3 look = p.getLookAngle();
        Vec3 back = new Vec3(-look.x, 0.0D, -look.z);
        if (back.lengthSqr() < 1.0E-4D) back = new Vec3(0.0D, 0.0D, 1.0D);
        return back.normalize();
    }

    /** A standable spot near (x,z) at roughly the player's height, or null. */
    private static BlockPos groundAt(ServerLevel level, double x, double z, double refY) {
        BlockPos guess = BlockPos.containing(x, refY, z);
        if (!level.hasChunkAt(guess)) return null;
        if (level.dimension().equals(Dimensions.HALLWAY) || level.dimension().equals(Dimensions.ARCHIVE) || level.dimension().equals(Dimensions.WARD)) {
            int floorY = level.dimension().equals(Dimensions.HALLWAY) ? LiminalGenerator.HALL_FLOOR
                    : level.dimension().equals(Dimensions.WARD) ? LiminalGenerator.WARD_FLOOR : LiminalGenerator.ARCHIVE_FLOOR;
            BlockPos floor = new BlockPos(guess.getX(), floorY + 1, guess.getZ());
            return level.getBlockState(floor).isAir() && level.getBlockState(floor.above()).isAir() ? floor : null;
        }
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, guess);
        if (top.getY() <= level.getMinBuildHeight() + 2) return null;
        if (Math.abs(top.getY() - refY) > 24.0D) return null;
        if (!level.getBlockState(top.below()).getFluidState().isEmpty()) return null;
        return top;
    }

    private static BlockPos ring(ServerLevel level, ServerPlayer p, double minDist, double maxDist) {
        RandomSource r = level.random;
        for (int i = 0; i < 8; i++) {
            double a = r.nextDouble() * Math.PI * 2.0D;
            double d = minDist + r.nextDouble() * (maxDist - minDist);
            BlockPos pos = groundAt(level, p.getX() + Math.cos(a) * d, p.getZ() + Math.sin(a) * d, p.getY());
            if (pos != null) return pos;
        }
        return null;
    }

    private static void sound(ServerPlayer p, SoundEvent sound, Vec3 at, float volume, float pitch) {
        p.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
                SoundSource.AMBIENT, at.x, at.y, at.z, volume, pitch, p.getRandom().nextLong()));
    }

    private static SoundEvent stepSound(ServerLevel level) {
        ResourceKey<Level> dim = level.dimension();
        if (dim.equals(Dimensions.HALLWAY)) return SoundEvents.WOOL_STEP;
        if (dim.equals(Dimensions.DROWNED)) return SoundEvents.PLAYER_SWIM;
        return SoundEvents.GRASS_STEP;
    }

    // ------------------------------------------------------------------ events

    /** Steps coming up behind you, then nothing. */
    private static boolean footsteps(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        MinecraftServer server = level.getServer();
        Vec3 back = behind(p);
        SoundEvent step = stepSound(level);
        for (int i = 0; i < 7; i++) {
            final int n = i;
            Scheduler.after(server, i * 9, () -> {
                if (p.isRemoved()) return;
                Vec3 at = p.position().add(back.scale(7.0D - n * 0.9D));
                sound(p, step, at, 0.9F, 0.85F + (n % 2) * 0.1F);
            });
        }
        return true;
    }

    /** The rain stops. Dead stop. Then it comes back with thunder. */
    private static boolean silence(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        if (!level.dimension().equals(Level.OVERWORLD) || !level.isRaining()) return false;
        WeatherControl.silence(level, 400);
        ModNetwork.thought(p, Thoughts.f(Thoughts.SILENCE));
        Scheduler.after(level.getServer(), 440, () -> {
            if (!p.isRemoved()) ModNetwork.thought(p, Thoughts.f(Thoughts.SILENCE_END));
        });
        return true;
    }

    private static boolean thunder(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        if (!level.dimension().equals(Level.OVERWORLD)) return false;
        WeatherControl.thunder(level, 1400);
        return true;
    }

    /** The "friend" speaks in chat. */
    private static boolean whisper(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        String name = com.sorrowed.SorrowedConfig.friend();
        p.sendSystemMessage(Component.literal("<" + name + "> " + Thoughts.FRIEND_CHAT[level.random.nextInt(Thoughts.FRIEND_CHAT.length)]));
        if (level.random.nextBoolean()) {
            Scheduler.after(level.getServer(), 70, () -> {
                if (!p.isRemoved()) {
                    p.sendSystemMessage(Component.literal("<" + name + "> " + Thoughts.FRIEND_CHAT[level.random.nextInt(Thoughts.FRIEND_CHAT.length)]));
                }
            });
        }
        return true;
    }

    /** "<friend> joined the game", then "<friend> left the game". */
    private static boolean fakeJoin(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Component name = Component.literal(com.sorrowed.SorrowedConfig.friend());
        p.sendSystemMessage(Component.translatable("multiplayer.player.joined", name).withStyle(ChatFormatting.YELLOW));
        Scheduler.after(level.getServer(), 100, () -> {
            if (p.isRemoved()) return;
            p.sendSystemMessage(Component.translatable("multiplayer.player.left", name).withStyle(ChatFormatting.YELLOW));
            ModNetwork.thought(p, Thoughts.FAKE_JOIN);
        });
        return true;
    }

    /** A grave marker that only you can see, a few steps ahead. It is gone after six seconds. */
    private static boolean ghostGrave(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getLookAngle();
        Vec3 fwd = new Vec3(look.x, 0.0D, look.z);
        if (fwd.lengthSqr() < 1.0E-4D) return false;
        fwd = fwd.normalize();
        BlockPos pos = groundAt(level, p.getX() + fwd.x * 7.0D, p.getZ() + fwd.z * 7.0D, p.getY());
        if (pos == null || !level.getBlockState(pos).isAir()) return false;

        p.connection.send(new ClientboundBlockUpdatePacket(pos, ModBlocks.GRAVE_MARKER.get().defaultBlockState()));
        ModNetwork.thought(p, Thoughts.f(Thoughts.GHOST_GRAVE));
        Scheduler.after(level.getServer(), 120, () -> {
            if (!p.isRemoved()) p.connection.send(new ClientboundBlockUpdatePacket(pos, level.getBlockState(pos)));
        });
        return true;
    }

    /** You, ten seconds behind you. Needs a long enough walk to have a path. */
    private static boolean reflection(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        PathHistory.Sample old = PathHistory.get(p.getUUID(), ReflectionEntity.DELAY_TICKS);
        if (old == null) return false;
        double dx = old.x() - p.getX();
        double dz = old.z() - p.getZ();
        if (dx * dx + dz * dz < 100.0D) return false;
        if (!level.getEntitiesOfClass(ReflectionEntity.class, p.getBoundingBox().inflate(200.0D)).isEmpty()) return false;

        ReflectionEntity r = ModEntities.REFLECTION.get().create(level);
        if (r == null) return false;
        r.setOwner(p.getUUID());
        r.moveTo(old.x(), old.y(), old.z(), old.yaw(), 0.0F);
        level.addFreshEntity(r);
        return true;
    }

    private static boolean listener(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        if (!level.getEntitiesOfClass(ListenerEntity.class, p.getBoundingBox().inflate(80.0D)).isEmpty()) return false;
        BlockPos pos = ring(level, p, 26.0D, 36.0D);
        if (pos == null) return false;
        ListenerEntity e = ModEntities.LISTENER.get().spawn(level, pos, MobSpawnType.EVENT);
        return e != null;
    }

    /** A stone gate stands where there was nothing, behind you. */
    private static boolean doorBehind(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 back = behind(p);
        BlockPos base = groundAt(level, p.getX() + back.x * 13.0D, p.getZ() + back.z * 13.0D, p.getY());
        if (base == null) return false;
        Direction.Axis axis = Math.abs(back.x) > Math.abs(back.z) ? Direction.Axis.Z : Direction.Axis.X;
        GateBuilder.build(level, base, axis, ModBlocks.BACKUP_DOOR.get(), level.random);
        if (!CutsceneServer.play(p, "door_behind", null)) {
            Scheduler.after(level.getServer(), 30, () -> {
                if (!p.isRemoved()) ModNetwork.thought(p, Thoughts.f(Thoughts.DOOR_BEHIND));
            });
        }
        return true;
    }

    /** Four of them in a ring, all facing you. (On Peaceful, four Echoes instead.) */
    private static boolean hollowCircle(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        boolean peaceful = level.getDifficulty() == Difficulty.PEACEFUL;
        int spawned = 0;
        for (int i = 0; i < 4; i++) {
            double a = i * (Math.PI / 2.0D) + level.random.nextDouble() * 0.4D;
            BlockPos pos = groundAt(level, p.getX() + Math.cos(a) * 21.0D, p.getZ() + Math.sin(a) * 21.0D, p.getY());
            if (pos == null) continue;
            if (peaceful) {
                if (ModEntities.ECHO.get().spawn(level, pos, MobSpawnType.EVENT) != null) spawned++;
            } else {
                HollowEntity h = ModEntities.HOLLOW.get().spawn(level, pos, MobSpawnType.EVENT);
                if (h != null) {
                    h.setTarget(p);
                    spawned++;
                }
            }
        }
        if (spawned == 0) return false;
        ModNetwork.thought(p, Thoughts.f(Thoughts.CIRCLE));
        return true;
    }

    private static boolean echoClose(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 back = behind(p);
        BlockPos pos = groundAt(level, p.getX() + back.x * 7.5D, p.getZ() + back.z * 7.5D, p.getY());
        if (pos == null) return false;
        if (ModEntities.ECHO.get().spawn(level, pos, MobSpawnType.EVENT) == null) return false;
        ModNetwork.thought(p, Thoughts.f(Thoughts.ECHO_CLOSE));
        return true;
    }

    /** In Alex's world: someone building in the distance. */
    private static boolean mason(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        if (!level.getEntitiesOfClass(MasonEntity.class, p.getBoundingBox().inflate(120.0D)).isEmpty()) return false;
        BlockPos pos = ring(level, p, 22.0D, 32.0D);
        if (pos == null) return false;
        return ModEntities.MASON.get().spawn(level, pos, MobSpawnType.EVENT) != null;
    }
}
