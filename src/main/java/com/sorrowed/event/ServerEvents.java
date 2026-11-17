package com.sorrowed.event;

import com.sorrowed.Sorrowed;
import com.sorrowed.SorrowedConfig;
import com.sorrowed.SorrowedData;
import com.sorrowed.cutscene.CutsceneServer;
import com.sorrowed.entity.EchoEntity;
import com.sorrowed.mind.PathHistory;
import com.sorrowed.mind.Scheduler;
import com.sorrowed.mind.Sorrow;
import com.sorrowed.mind.SorrowEvents;
import com.sorrowed.mind.SorrowSystem;
import com.sorrowed.mind.WeatherControl;
import com.sorrowed.net.ModNetwork;
import com.sorrowed.registry.Dimensions;
import com.sorrowed.registry.ModEntities;
import com.sorrowed.story.Letters;
import com.sorrowed.story.Thoughts;
import com.sorrowed.worldgen.GraveBuilder;
import com.sorrowed.worldgen.HubBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Sorrowed.MODID)
public class ServerEvents {
    private static final String PERSIST = "PlayerPersisted";
    private static final String WELCOMED = "sorrowed_welcomed";
    private static final int RAIN_TIME = 24000;
    private static final Map<UUID, Long> LAST_AMBIENT = new HashMap<>();

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;

        // It is raining the moment you join.
        if (SorrowedConfig.ETERNAL_RAIN.get()) {
            server.overworld().setWeatherParameters(0, RAIN_TIME, true, false);
        }

        CompoundTag root = player.getPersistentData();
        CompoundTag saved = root.getCompound(PERSIST);
        boolean first = !saved.getBoolean(WELCOMED);
        saved.putBoolean(WELCOMED, true);
        root.put(PERSIST, saved);

        Sorrow.resync(player);

        if (first) {
            if (SorrowedConfig.GIVE_BOOK.get()) {
                player.getInventory().add(Letters.welcomeBook());
            }
            placeGrave(player);
            if (SorrowedConfig.CUTSCENES.get()) {
                // The first three lines are the cutscene's captions; the last two are plain thoughts after it.
                Scheduler.after(server, 40, () -> {
                    if (player.isRemoved()) return;
                    boolean played = CutsceneServer.play(player, "awakening", null, () -> {
                        ModNetwork.thought(player, Thoughts.f(Thoughts.INTRO[3]));
                        ModNetwork.thought(player, Thoughts.f(Thoughts.INTRO[4]));
                    });
                    if (!played) {
                        for (String line : Thoughts.INTRO) ModNetwork.thought(player, Thoughts.f(line));
                    }
                });
            } else {
                for (String line : Thoughts.INTRO) {
                    ModNetwork.thought(player, Thoughts.f(line));
                }
            }
        } else if (player.serverLevel().random.nextInt(3) == 0) {
            ModNetwork.thought(player, Thoughts.pick(Thoughts.RETURNING, player.serverLevel().random));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PathHistory.clear(player.getUUID());
        CutsceneServer.forget(player.getUUID());
        LAST_AMBIENT.remove(player.getUUID());
        Sorrow.forget(player.getUUID());
        SorrowEvents.forget(player.getUUID());
    }

    /** The first time anyone joins, put a grave a few steps from them. */
    private static void placeGrave(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!level.dimension().equals(Level.OVERWORLD)) return;
        SorrowedData data = SorrowedData.get(level);
        if (data.gravePlaced) return;

        for (int i = 0; i < 8; i++) {
            BlockPos guess = player.blockPosition().offset(5 + i * 2, 0, 4 + i);
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, guess);
            if (!level.getBlockState(top.below()).getFluidState().isEmpty()) continue;
            GraveBuilder.build(level, top, level.random);
            data.gravePlaced = true;
            data.setDirty();
            return;
        }
    }

    /** Keep the overworld raining (with room for silences and thunder). */
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel level)) return;
        if (!SorrowedConfig.ETERNAL_RAIN.get()) return;
        if (!level.dimension().equals(Level.OVERWORLD)) return;
        if (level.getGameTime() % 20L != 0L) return;
        WeatherControl.tick(level);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Scheduler.tick(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        Scheduler.clear();
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PathHistory.clear(player.getUUID());
        ResourceKey<Level> to = event.getTo();
        MinecraftServer server = player.getServer();
        if (server == null || to.equals(Level.OVERWORLD)) return;

        // The first arrival in each dimension is a cutscene. After that, the place speaks for itself.
        String path = to.location().getPath();
        if (!to.location().getNamespace().equals(Sorrowed.MODID)) return;
        CompoundTag root = player.getPersistentData();
        CompoundTag saved = root.getCompound(PERSIST);
        String flag = "sorrowed_seen_" + path;
        if (saved.getBoolean(flag)) return;
        saved.putBoolean(flag, true);
        root.put(PERSIST, saved);

        Scheduler.after(server, 30, () -> {
            if (player.isRemoved()) return;
            if (!CutsceneServer.play(player, "arrive_" + path, null)) {
                sendArrivalThoughts(player, to);
            }
        });
    }

    private static void sendArrivalThoughts(ServerPlayer player, ResourceKey<Level> to) {
        if (to.equals(Dimensions.UNFINISHED)) {
            ModNetwork.thought(player, Thoughts.ENTER_UNFINISHED_1);
            ModNetwork.thought(player, Thoughts.ENTER_UNFINISHED_2);
        } else if (to.equals(Dimensions.HUSH)) {
            ModNetwork.thought(player, Thoughts.ENTER_HUSH_1);
            ModNetwork.thought(player, Thoughts.ENTER_HUSH_2);
        } else if (to.equals(Dimensions.DROWNED)) {
            ModNetwork.thought(player, Thoughts.ENTER_DROWNED_1);
            ModNetwork.thought(player, Thoughts.ENTER_DROWNED_2);
        } else if (to.equals(Dimensions.HALLWAY)) {
            ModNetwork.thought(player, Thoughts.ENTER_HALLWAY_1);
            ModNetwork.thought(player, Thoughts.ENTER_HALLWAY_2);
        } else if (to.equals(Dimensions.BACKUP)) {
            ModNetwork.thought(player, Thoughts.ENTER_BACKUP_1);
            ModNetwork.thought(player, Thoughts.ENTER_BACKUP_2);
        } else if (to.equals(Dimensions.ARCHIVE)) {
            ModNetwork.thought(player, Thoughts.ENTER_ARCHIVE_1);
            ModNetwork.thought(player, Thoughts.ENTER_ARCHIVE_2);
        } else if (to.equals(Dimensions.STREET)) {
            ModNetwork.thought(player, Thoughts.f(Thoughts.ENTER_STREET_1));
            ModNetwork.thought(player, Thoughts.ENTER_STREET_2);
        } else if (to.equals(Dimensions.STATIC)) {
            ModNetwork.thought(player, Thoughts.ENTER_STATIC_1);
            ModNetwork.thought(player, Thoughts.ENTER_STATIC_2);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        PathHistory.record(player);
        int t = player.tickCount;
        ServerLevel level = player.serverLevel();
        ResourceKey<Level> dim = level.dimension();

        // Falling off the floating chunks never kills you. You just land back at the start.
        if (t % 10 == 0 && dim.equals(Dimensions.UNFINISHED) && player.getY() < -20.0D) {
            Vec3 hub = HubBuilder.arrival(level, Dimensions.UNFINISHED);
            player.fallDistance = 0.0F;
            player.connection.teleport(hub.x, hub.y, hub.z, 180.0F, 0.0F);
            ModNetwork.thought(player, Thoughts.FALL);
            Sorrow.add(player, 6.0F);
        }

        if (t % 20 == 0) {
            SorrowSystem.tick(player);
        }

        if (t % 200 == 0) {
            RandomSource random = level.random;
            long now = level.getGameTime();
            long gap = SorrowedConfig.AMBIENT_MINUTES.get() * 1200L;
            Long last = LAST_AMBIENT.get(player.getUUID());
            boolean quiet = CutsceneServer.isPlaying(player) || (last != null && now - last < gap);
            if (!quiet && random.nextInt(8) == 0) {
                LAST_AMBIENT.put(player.getUUID(), now);
                ModNetwork.thought(player, Thoughts.f(ambient(player, level, random)));
            }
            SorrowEvents.roll(player);
            if (SorrowedConfig.ECHOES.get()
                    && (dim.equals(Level.OVERWORLD) || dim.equals(Dimensions.HUSH))
                    && random.nextInt(12) == 0) {
                trySpawnEcho(player, level, random);
            }
        }
    }

    private static String ambient(ServerPlayer player, ServerLevel level, RandomSource random) {
        int tier = Sorrow.tier(player);
        // The worse it gets, the more of what you think is not about the world at all.
        if (tier >= 1 && random.nextInt(3) != 0) {
            String[] pool = tier >= 3 ? Thoughts.TIER_3 : tier == 2 ? Thoughts.TIER_2 : Thoughts.TIER_1;
            return pool[random.nextInt(pool.length)];
        }
        ResourceKey<Level> dim = level.dimension();
        String[] pool;
        if (dim.equals(Dimensions.UNFINISHED)) pool = Thoughts.AMBIENT_UNFINISHED;
        else if (dim.equals(Dimensions.HUSH)) pool = Thoughts.AMBIENT_HUSH;
        else if (dim.equals(Dimensions.DROWNED)) pool = Thoughts.AMBIENT_DROWNED;
        else if (dim.equals(Dimensions.HALLWAY)) pool = Thoughts.AMBIENT_HALLWAY;
        else if (dim.equals(Dimensions.BACKUP)) pool = Thoughts.AMBIENT_BACKUP;
        else if (dim.equals(Dimensions.ARCHIVE)) pool = Thoughts.AMBIENT_ARCHIVE;
        else if (dim.equals(Dimensions.STREET)) pool = Thoughts.AMBIENT_STREET;
        else if (dim.equals(Dimensions.STATIC)) pool = Thoughts.AMBIENT_STATIC;
        else if (level.isNight() && random.nextBoolean()) pool = Thoughts.AMBIENT_NIGHT;
        else pool = Thoughts.AMBIENT_OVERWORLD;
        return pool[random.nextInt(pool.length)];
    }

    private static void trySpawnEcho(ServerPlayer player, ServerLevel level, RandomSource random) {
        List<EchoEntity> nearby = level.getEntitiesOfClass(EchoEntity.class, player.getBoundingBox().inflate(64.0D));
        if (!nearby.isEmpty()) return;

        double angle = random.nextDouble() * Math.PI * 2.0D;
        double dist = 18.0D + random.nextDouble() * 10.0D;
        BlockPos guess = BlockPos.containing(player.getX() + Math.cos(angle) * dist, player.getY(),
                player.getZ() + Math.sin(angle) * dist);
        if (!level.hasChunkAt(guess)) return;

        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, guess);
        if (!level.getBlockState(top.below()).getFluidState().isEmpty()) return;

        ModEntities.ECHO.get().spawn(level, top, MobSpawnType.EVENT);
    }
}
