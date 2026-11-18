package com.sorrowed.event;

import com.sorrowed.Sorrowed;
import com.sorrowed.SorrowedData;
import com.sorrowed.block.LighthouseLampBlock;
import com.sorrowed.block.ScreenBlock;
import com.sorrowed.entity.FriendEntity;
import com.sorrowed.mind.Scheduler;
import com.sorrowed.net.ModNetwork;
import com.sorrowed.registry.Dimensions;
import com.sorrowed.registry.ModBlocks;
import com.sorrowed.story.Finale;
import com.sorrowed.story.Objectives;
import com.sorrowed.story.Progress;
import com.sorrowed.story.Thoughts;
import com.sorrowed.worldgen.MemorySites;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The objectives, as they happen: touching the stones, the switch in his house, the page, the bed,
 * the screens, the lamp; standing in the four places and keeping still by the television. Also
 * keeps each player's picture of their progress up to date.
 */
@Mod.EventBusSubscriber(modid = Sorrowed.MODID)
public class StoryEvents {
    private static final Map<UUID, Integer> HOLD = new HashMap<>();
    private static final Map<UUID, Vec3> LAST_POS = new HashMap<>();

    private static final String[] COUNT = {
            "One.", "Two.", "Three.", "Four.", "Five.", "Six.", "Seven.", "Eight. That's all of them."
    };
    private static final String[] PLACE_LINES = {
            "We never caught a single fish here. We never cared.",
            "Two of us in a hole in the ground. It was the best night.",
            "You can see every door from up here. They're all open.",
            "It's still burning. He never let it go out.",
    };

    // ------------------------------------------------------------------ arriving

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        arrived(p);
        if (Progress.allDone(p) && Progress.stage(p) == Progress.PLAYING) Finale.ready(p);
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) arrived(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) arrived(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        HOLD.remove(id);
        LAST_POS.remove(id);
        Finale.forget(id);
    }

    private static void arrived(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        MemorySites.ensure(level);
        if (level.dimension().equals(Level.OVERWORLD)) ServerEvents.placeGrave(p);
        Progress.sync(p);
    }

    // ------------------------------------------------------------------ every half second

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p)) return;
        if (p.tickCount % 10 != 0 || p.isSpectator()) return;
        int stage = Progress.stage(p);
        if (stage == Progress.READY) {
            Finale.tick(p);
            return;
        }
        if (stage != Progress.PLAYING) return;
        ServerLevel level = p.serverLevel();
        Objectives.Objective o = Objectives.in(level.dimension());
        if (o == null || Progress.done(p, o.id())) return;
        switch (o.id()) {
            case "overworld" -> {
                BlockPos grave = MemorySites.grave(level);
                if (grave != null && p.distanceToSqr(Vec3.atCenterOf(grave)) < 3.5D * 3.5D) visitGrave(p, grave);
            }
            case "backup" -> places(p);
            case "static" -> holdStill(p, level);
            default -> { }
        }
    }

    /** The four places in his world. */
    private static void places(ServerPlayer p) {
        int mask = Progress.steps(p, "backup");
        for (int i = 0; i < MemorySites.PLACES.length; i++) {
            if ((mask & (1 << i)) != 0) continue;
            BlockPos at = MemorySites.PLACES[i];
            double dx = p.getX() - (at.getX() + 0.5D);
            double dz = p.getZ() - (at.getZ() + 0.5D);
            double flat = dx * dx + dz * dz;
            boolean there = switch (i) {
                case 0 -> flat < 6.0D * 6.0D;                                     // anywhere around the pond
                case 1 -> flat < 1.7D * 1.7D && p.getY() < at.getY() + 2.0D;       // inside the dirt house
                case 2 -> flat < 2.8D * 2.8D && p.getY() > at.getY() - 0.6D;       // up on the platform
                default -> flat < 3.5D * 3.5D;                                     // by the fire
            };
            if (!there) continue;
            if (Progress.step(p, "backup", i) && !Progress.done(p, "backup")) {
                ModNetwork.thought(p, PLACE_LINES[i]);
            }
            return;
        }
    }

    /** By the television in the static: keep still and let the picture load. */
    private static void holdStill(ServerPlayer p, ServerLevel level) {
        BlockPos tv = MemorySites.target(level, "static");
        if (tv == null) return;
        UUID id = p.getUUID();
        Vec3 now = p.position();
        Vec3 last = LAST_POS.put(id, now);
        if (p.distanceToSqr(Vec3.atCenterOf(tv)) > 4.5D * 4.5D) {
            HOLD.remove(id);
            return;
        }
        boolean still = last != null && last.distanceToSqr(now) < 0.04D;
        int held = HOLD.getOrDefault(id, 0);
        if (!still) {
            if (held >= 40) ModNetwork.thought(p, "It slipped. Keep still.");
            HOLD.put(id, 0);
            return;
        }
        held += 10;
        HOLD.put(id, held);
        ModNetwork.statik(p, Math.min(0.9F, 0.15F + held / 190.0F));
        if (held == 40) ModNetwork.thought(p, "It's getting louder.");
        if (held == 100) ModNetwork.thought(p, "There's something under the noise.");
        if (held >= 160) {
            HOLD.remove(id);
            BlockState s = level.getBlockState(tv);
            if (s.getBlock() instanceof ScreenBlock) {
                level.setBlock(tv, ModBlocks.CLEAR_SCREEN.get().defaultBlockState()
                        .setValue(ScreenBlock.FACING, s.getValue(ScreenBlock.FACING)), 3);
            }
            level.playSound(null, tv, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8F, 1.4F);
            Progress.complete(p, "static");
        }
    }

    // ------------------------------------------------------------------ things you use

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer p)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        ServerLevel level = p.serverLevel();
        BlockPos pos = event.getPos();
        ResourceKey<Level> dim = level.dimension();
        boolean used = false;
        if (dim.equals(Dimensions.HUSH)) {
            used = touchStone(p, level, pos);
        } else if (dim.equals(Dimensions.STREET)) {
            if (pos.equals(MemorySites.target(level, "street"))) {
                switchOff(p, level, pos);
                used = true;
            }
        } else if (dim.equals(Dimensions.ARCHIVE)) {
            if (pos.equals(MemorySites.target(level, "archive"))) {
                turnPage(p, level, pos);
                used = true;
            }
        } else if (dim.equals(Dimensions.WARD)) {
            BlockPos head = MemorySites.target(level, "ward");
            if (head != null && (pos.equals(head) || pos.equals(head.north()))) {
                bedFour(p, level);
                used = true;
            }
        }
        if (used) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** The stones in the Hush. Returns true if the block was one of them. */
    private static boolean touchStone(ServerPlayer p, ServerLevel level, BlockPos pos) {
        BlockPos c = MemorySites.stonesCenter(level);
        if (c == null || pos.getY() < c.getY() - 1 || pos.getY() > c.getY() + 8) return false;
        if (level.getBlockState(pos).isAir()) return false;
        int dx = pos.getX() - c.getX();
        int dz = pos.getZ() - c.getZ();
        int index = -1;
        for (int i = 0; i < MemorySites.STONES.length; i++) {
            if (MemorySites.STONES[i][0] == dx && MemorySites.STONES[i][1] == dz) index = i;
        }
        boolean ninth = dx == MemorySites.NINTH[0] && dz == MemorySites.NINTH[1];
        if (index < 0 && !ninth) return false;

        if (Progress.done(p, "hush")) {
            ModNetwork.thought(p, "Nine. I counted twice.");
            return true;
        }
        int mask = Progress.steps(p, "hush");
        if (ninth) {
            if ((mask & 0xFF) != 0xFF) {
                ModNetwork.thought(p, "Not that one. Not yet. Count the others first.");
                return true;
            }
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.getX() + 0.5D, c.getY() + 0.6D, c.getZ() + 0.5D,
                    60, 0.4D, 0.8D, 0.4D, 0.04D);
            level.playSound(null, c, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 1.0F, 0.6F);
            Progress.step(p, "hush", 8);
            return true;
        }
        if (!Progress.step(p, "hush", index)) return true;
        lightStone(level, pos.getX(), pos.getZ(), c.getY());
        level.sendParticles(ParticleTypes.SOUL, pos.getX() + 0.5D, c.getY() + 2.5D, pos.getZ() + 0.5D,
                12, 0.4D, 1.2D, 0.4D, 0.02D);
        level.playSound(null, pos, SoundEvents.SOUL_ESCAPE, SoundSource.BLOCKS, 1.0F, 0.7F + Integer.bitCount(mask) * 0.06F);
        int counted = Integer.bitCount((mask | (1 << index)) & 0xFF);
        ModNetwork.thought(p, COUNT[counted - 1]);
        if (counted == 8) {
            MinecraftServer server = level.getServer();
            Scheduler.after(server, 60, () -> {
                BlockPos ninthBase = c.offset(MemorySites.NINTH[0], 0, MemorySites.NINTH[1]);
                if (level.getBlockState(ninthBase).isAir()) MemorySites.raiseNinth(level);
                level.playSound(null, ninthBase, SoundEvents.DEEPSLATE_BREAK, SoundSource.BLOCKS, 1.4F, 0.5F);
                level.playSound(null, ninthBase, SoundEvents.WARDEN_HEARTBEAT, SoundSource.BLOCKS, 1.0F, 0.8F);
                if (!p.isRemoved()) {
                    ModNetwork.thought(p, "...No. There's another one.");
                    Progress.sync(p);
                }
            });
        }
        return true;
    }

    /** A soul lantern on top of the stone you touched, so you can see which ones you've counted. */
    private static void lightStone(ServerLevel level, int x, int z, int base) {
        for (int y = base + 7; y >= base; y--) {
            BlockPos at = new BlockPos(x, y, z);
            BlockState s = level.getBlockState(at);
            if (s.isAir()) continue;
            if (!s.is(Blocks.SOUL_LANTERN) && !s.is(Blocks.LANTERN) && level.getBlockState(at.above()).isAir()) {
                level.setBlock(at.above(), Blocks.SOUL_LANTERN.defaultBlockState(), 3);
            }
            return;
        }
    }

    /** The switch inside his front door. */
    private static void switchOff(ServerPlayer p, ServerLevel level, BlockPos lever) {
        if (Progress.done(p, "street")) {
            ModNetwork.thought(p, "It's off. It can stay off.");
            return;
        }
        BlockState s = level.getBlockState(lever);
        if (s.getBlock() instanceof LeverBlock) level.setBlock(lever, s.setValue(LeverBlock.POWERED, true), 3);
        level.playSound(null, lever, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, 0.5F);
        for (BlockPos light : new BlockPos[] {lever.offset(-1, 2, -3), lever.offset(-4, 2, -1)}) {
            if (level.getBlockState(light).is(Blocks.GLOWSTONE)) level.setBlock(light, Blocks.OAK_PLANKS.defaultBlockState(), 3);
        }
        Progress.complete(p, "street");
    }

    /** The book in the archive: turn the page, and there is something written on the next one. */
    private static void turnPage(ServerPlayer p, ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 0.8F);
        Progress.complete(p, "archive");
        ModNetwork.open(p, "page:archive_page");
    }

    /** Room four. Sit down, and the lamp that never worked comes on. */
    private static void bedFour(ServerPlayer p, ServerLevel level) {
        if (Progress.done(p, "ward")) {
            ModNetwork.thought(p, "The lamp works now.");
            return;
        }
        BlockPos lamp = MemorySites.WARD_LAMP;
        if (level.getBlockState(lamp).is(Blocks.REDSTONE_LAMP)) {
            level.setBlock(lamp.above(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        }
        level.playSound(null, lamp, SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.BLOCKS, 0.8F, 0.6F);
        Progress.complete(p, "ward");
    }

    /** His grave in the rain. A candle is left lit beside it when you go. */
    public static void visitGrave(ServerPlayer p, BlockPos marker) {
        ServerLevel level = p.serverLevel();
        if (!level.dimension().equals(Level.OVERWORLD)) return;
        SorrowedData data = SorrowedData.get(level);
        if (data.getPos("grave") == null) data.setPos("grave", marker);
        if (Progress.done(p, "overworld")) return;
        BlockPos candle = marker.offset(-1, 0, 1);
        if (level.getBlockState(candle).isAir() || level.getBlockState(candle).is(Blocks.LILY_OF_THE_VALLEY)) {
            level.setBlock(candle, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true), 3);
        }
        Progress.complete(p, "overworld");
    }

    /** One of the screens. What happens depends on which one, and where. */
    public static void useScreen(ServerPlayer p, String kind, BlockPos pos) {
        ResourceKey<Level> dim = p.serverLevel().dimension();
        switch (kind) {
            case "chat" -> {
                if (dim.equals(Dimensions.HALLWAY)) ModNetwork.open(p, "chat");
                else ModNetwork.thought(p, "The chat's open on this one too. Nobody's typed anything.");
            }
            case "console" -> {
                if (dim.equals(Dimensions.SERVER)) ModNetwork.open(p, "console");
            }
            case "laptop" -> {
                if (dim.equals(Dimensions.ORCHARD)) ModNetwork.open(p, "laptop");
            }
            case "snow" -> ModNetwork.thought(p, "Just snow. Stand still. Let it load.");
            case "clear" -> ModNetwork.thought(p, Thoughts.f("{friend}'s world. The sun's out. Nobody's in it."));
            default -> { }
        }
    }

    /** What the player did in a screen: typed back to him, watched the copy finish, read the list. */
    public static void reply(ServerPlayer p, String what, String text) {
        if ("ending".equals(what)) {
            Finale.goHome(p);
            return;
        }
        ServerLevel level = p.serverLevel();
        String id;
        ResourceKey<Level> dim;
        switch (what) {
            case "chat" -> { id = "hallway"; dim = Dimensions.HALLWAY; }
            case "laptop" -> { id = "orchard"; dim = Dimensions.ORCHARD; }
            case "console" -> { id = "server"; dim = Dimensions.SERVER; }
            default -> { return; }
        }
        if (!level.dimension().equals(dim)) return;
        BlockPos at = MemorySites.target(level, id);
        if (at != null && p.distanceToSqr(Vec3.atCenterOf(at)) > 10.0D * 10.0D) return;
        if ("chat".equals(what)) {
            String clean = text == null ? "" : text.strip();
            if (clean.isEmpty()) return;
            if (clean.length() > 120) clean = clean.substring(0, 120);
            if (!Progress.done(p, id)) Progress.setReply(p, clean);
        }
        Progress.complete(p, id);
    }

    /** The lamp at the top of the lighthouse. */
    public static void useLamp(ServerPlayer p, BlockPos pos, BlockState state) {
        ServerLevel level = p.serverLevel();
        if (!state.getValue(LighthouseLampBlock.LIT)) {
            level.setBlock(pos, state.setValue(LighthouseLampBlock.LIT, true), 3);
            level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.2F, 0.7F);
            level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    40, 0.6D, 0.6D, 0.6D, 0.08D);
        } else {
            ModNetwork.thought(p, "It's turning. Somebody will see it.");
        }
        if (level.dimension().equals(Dimensions.DROWNED)) Progress.complete(p, "drowned");
    }

    /** You walked up to him, or put a hand on his shoulder. */
    public static void meet(ServerPlayer p, FriendEntity him) {
        if (Progress.stage(p) == Progress.READY) {
            Finale.meet(p, him);
        } else {
            ModNetwork.thought(p, "He doesn't look up. Not yet. There's still something I haven't done.");
        }
    }
}
