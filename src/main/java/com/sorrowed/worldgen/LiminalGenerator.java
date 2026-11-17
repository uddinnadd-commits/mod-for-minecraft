package com.sorrowed.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * One generator, many worlds. Everything is a pure function of (x, y, z) and a few hashes, so
 * every chunk is the same every time and nothing needs to know about its neighbours.
 *
 * "unfinished": chunk-aligned pieces floating in nothing. A big hub platform around the origin,
 * tall 16x16 monoliths with half-built walls on top, flat slabs, scaffold frames, and very long
 * 16-wide ribbons running along X and Z at different heights. Everything between them is void.
 *
 * "hallway": a sealed, carpeted, yellow-walled maze of rooms. Every 16x16 chunk is a room with
 * walls on its west/north edges that may or may not have a doorway. Some rooms are furnished:
 * a living room with a television showing static, a bedroom, a kitchen, and the odd room
 * where the lights are off and the carpet is black.
 *
 * "drowned": a sea 100 blocks deep with a rolling seabed, kelp forests, sea pickles, and the
 * occasional rock spire breaking the surface. The streetlights from his road are down here too.
 *
 * "backup": rolling, perfectly smooth meadow. Every chunk has the same single tree at its
 * centre and flowers on a rigid grid, with a gravel path grid and lamp posts that nobody lit.
 *
 * "archive": an endless library. Aisles of bookshelves under a low beamed ceiling, candles that
 * never burn down, ladders to nowhere, and reading rooms where the chairs are pulled out.
 *
 * "street": the road he grew up on, at night, in the rain. Identical houses on identical lots,
 * sodium streetlights, and a light on in one window in seven.
 *
 * "static": a flat plain of black, white and grey that goes on forever, like a television tuned
 * to nothing. Antenna masts with one red light each. Dead pixels standing up out of the ground.
 */
public class LiminalGenerator extends ChunkGenerator {
    public static final Codec<LiminalGenerator> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.getBiomeSource()),
            Codec.STRING.fieldOf("mode").forGetter(g -> g.mode)
    ).apply(inst, inst.stable(LiminalGenerator::new)));

    public static final int MIN_Y = -64;
    public static final int HEIGHT = 384;
    /** Top block of the hub platform in the Unfinished; you stand at 65. */
    public static final int PLATFORM_TOP = 64;
    public static final int HALL_FLOOR = 64;
    public static final int HALL_CEIL = 73;
    public static final int ARCHIVE_FLOOR = 64;
    public static final int ARCHIVE_CEIL = 76;
    public static final int STREET_GROUND = 64;
    public static final int STATIC_GROUND = 64;
    public static final int DROWN_SEA = 40;

    private enum Mode {
        UNFINISHED, HALLWAY, DROWNED, BACKUP, ARCHIVE, STREET, STATIC;

        static Mode of(String name) {
            for (Mode m : values()) {
                if (m.name().equalsIgnoreCase(name)) return m;
            }
            return UNFINISHED;
        }
    }

    private final String mode;
    private final Mode kind;

    public LiminalGenerator(BiomeSource source, String mode) {
        super(source);
        this.mode = mode;
        this.kind = Mode.of(mode);
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState random, BiomeManager biomes,
                             StructureManager structures, ChunkAccess chunk, GenerationStep.Carving step) {
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState random, ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    @Override
    public int getGenDepth() {
        return HEIGHT;
    }

    @Override
    public int getSeaLevel() {
        return kind == Mode.DROWNED ? DROWN_SEA : -63;
    }

    @Override
    public int getMinY() {
        return MIN_Y;
    }

    /** Highest y that stateAt can return a block for in this column. */
    private int columnTop(int x, int z) {
        switch (kind) {
            case HALLWAY: return HALL_CEIL;
            case DROWNED: return Math.max(drownedFloor(x, z) + 20, DROWN_SEA);
            case BACKUP: return backupHeight(x, z) + 7;
            case ARCHIVE: return ARCHIVE_CEIL;
            case STREET: return STREET_GROUND + 14;
            case STATIC: return STATIC_GROUND + 90;
            default: {
                Span span = unfinishedSpan(x, z);
                return span == null ? MIN_Y - 1 : span.top() + 6;
            }
        }
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState random,
                                                        StructureManager structures, ChunkAccess chunk) {
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = minX + lx;
                int z = minZ + lz;
                int lo = MIN_Y;
                int hi = columnTop(x, z);
                if (kind == Mode.UNFINISHED) {
                    Span span = unfinishedSpan(x, z);
                    if (span == null) continue;
                    lo = span.bottom();
                }
                for (int y = lo; y <= hi; y++) {
                    BlockState state = stateAt(x, y, z);
                    if (state == null || state.isAir()) continue;
                    chunk.setBlockState(pos.set(x, y, z), state, false);
                    oceanFloor.update(lx, y, lz, state);
                    surface.update(lx, y, lz, state);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    /** The walkable top of the column: what a heightmap would report before decoration. */
    private int topAt(int x, int z) {
        switch (kind) {
            case HALLWAY: return HALL_CEIL;
            case DROWNED: return Math.max(drownedFloor(x, z), DROWN_SEA);
            case BACKUP: return backupHeight(x, z);
            case ARCHIVE: return ARCHIVE_CEIL;
            case STREET: return STREET_GROUND;
            case STATIC: return STATIC_GROUND;
            default: {
                Span span = unfinishedSpan(x, z);
                return span == null ? MIN_Y - 1 : span.top();
            }
        }
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return Math.max(level.getMinBuildHeight(), topAt(x, z) + 1);
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        BlockState[] states = new BlockState[level.getHeight()];
        for (int i = 0; i < states.length; i++) {
            BlockState s = stateAt(x, level.getMinBuildHeight() + i, z);
            states[i] = s == null ? Blocks.AIR.defaultBlockState() : s;
        }
        return new NoiseColumn(level.getMinBuildHeight(), states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
    }

    // ------------------------------------------------------------------ blocks

    private BlockState stateAt(int x, int y, int z) {
        switch (kind) {
            case HALLWAY: return hallwayState(x, y, z);
            case DROWNED: return drownedState(x, y, z);
            case BACKUP: return backupState(x, y, z);
            case ARCHIVE: return archiveState(x, y, z);
            case STREET: return streetState(x, y, z);
            case STATIC: return staticState(x, y, z);
            default: return unfinishedState(x, y, z);
        }
    }

    private record Span(int bottom, int top, int palette, boolean monolith, boolean slab) {}

    private static long mix(long a, long b, long c) {
        long h = a * 0x9E3779B97F4A7C15L ^ b * 0xC2B2AE3D27D4EB4FL ^ c * 0x165667B19E3779F9L;
        h ^= h >>> 32;
        h *= 0xD6E8FEB86659FD93L;
        h ^= h >>> 32;
        return h;
    }

    /** True once in 'n' for this (a, b, salt). */
    private static boolean chance(long a, long b, long salt, int n) {
        return Math.floorMod(mix(a, b, salt), (long) n) == 0L;
    }

    // ------------------------------------------------------------- unfinished

    private static Span unfinishedSpan(int x, int z) {
        int cx = x >> 4;
        int cz = z >> 4;

        // The hub: a 7x5 chunk platform around the origin. The gates stand here.
        if (cx >= -3 && cx <= 3 && cz >= -2 && cz <= 2) {
            return new Span(PLATFORM_TOP - 8, PLATFORM_TOP, 0, false, false);
        }

        long h = mix(cx, cz, 1);
        int palette = (int) ((h >>> 40) % 3);

        // Monoliths: a whole chunk standing on end, up to y 220.
        if ((h & 15) == 0) {
            int top = 90 + (int) ((h >>> 8) & 127);
            return new Span(-40, top, palette, true, false);
        }

        // Long ribbons along X: 16 wide, 160 long, gently waving.
        long rowH = mix(cz, 77, 2);
        if ((rowH & 3) == 0) {
            long segH = mix(Math.floorDiv(cx, 10), cz, 3);
            if ((segH & 3) != 0) {
                int base = 30 + (int) ((rowH >>> 8) & 63);
                int top = base + (int) Math.round(5.0D * Math.sin(x / 37.0D + (rowH & 255)));
                return new Span(top - 3, top, (int) ((segH >>> 40) % 3), false, false);
            }
        }

        // Long ribbons along Z.
        long colH = mix(cx, 88, 4);
        if ((colH & 3) == 0) {
            long segH = mix(cx, Math.floorDiv(cz, 10), 5);
            if ((segH & 3) != 0) {
                int base = 50 + (int) ((colH >>> 8) & 63);
                int top = base + (int) Math.round(5.0D * Math.sin(z / 37.0D + (colH & 255)));
                return new Span(top - 3, top, (int) ((segH >>> 40) % 3), false, false);
            }
        }

        // Single floating slabs.
        if (((h >>> 4) & 7) == 0) {
            int top = 20 + (int) ((h >>> 12) & 127);
            return new Span(top - 4, top, palette, false, true);
        }
        return null;
    }

    private static BlockState unfinishedState(int x, int y, int z) {
        Span span = unfinishedSpan(x, z);
        if (span == null || y < span.bottom()) return null;
        if (y > span.top()) return unfinishedDecoration(span, x, y, z);
        int depth = span.top() - y;
        switch (span.palette()) {
            case 1: // scaffold: planks over old stone brick
                return (depth < 2 ? Blocks.SPRUCE_PLANKS : Blocks.STONE_BRICKS).defaultBlockState();
            case 2: // blueprint: pale and unfinished
                if (depth < 1) return Blocks.SMOOTH_QUARTZ.defaultBlockState();
                return (depth < 3 ? Blocks.WHITE_CONCRETE : Blocks.STONE).defaultBlockState();
            default: // ordinary ground
                if (depth == 0) return Blocks.GRASS_BLOCK.defaultBlockState();
                return (depth < 4 ? Blocks.DIRT : Blocks.STONE).defaultBlockState();
        }
    }

    /** What he left on top of the pieces: walls he never finished, scaffold, tools still out. */
    private static BlockState unfinishedDecoration(Span span, int x, int y, int z) {
        int cx = x >> 4;
        int cz = z >> 4;
        int lx = x & 15;
        int lz = z & 15;
        int up = y - span.top();
        boolean edge = lx == 0 || lx == 15 || lz == 0 || lz == 15;
        boolean corner = (lx == 0 || lx == 15) && (lz == 0 || lz == 15);

        if (span.monolith()) {
            // A wall ring of uneven height with gaps, as if he stopped mid-row.
            if (corner) {
                if (up <= 4) return Blocks.OAK_FENCE.defaultBlockState();
                if (up == 5) return Blocks.TORCH.defaultBlockState();
                return null;
            }
            if (edge) {
                int seg = (lx == 0 || lx == 15) ? lz / 4 : lx / 4;
                int side = lx == 0 ? 0 : lx == 15 ? 1 : lz == 0 ? 2 : 3;
                long h = mix(cx * 4L + side, cz * 4L + seg, 41);
                int height = (int) (h & 3);
                if (chance(cx * 4L + side, cz * 4L + seg, 42, 5)) height = 0;
                if (up <= height) {
                    return (span.palette() == 2 ? Blocks.QUARTZ_BRICKS : Blocks.STONE_BRICKS).defaultBlockState();
                }
            }
            return null;
        }

        if (span.slab()) {
            // Tools still out on the slabs: a crafting table and a ladder up to nothing.
            if (up == 1 && lx == 8 && lz == 8 && chance(cx, cz, 43, 3)) {
                return Blocks.CRAFTING_TABLE.defaultBlockState();
            }
            if (lx == 3 && lz == 3 && chance(cx, cz, 44, 4)) {
                if (up <= 3) return Blocks.OAK_FENCE.defaultBlockState();
                if (up == 4) return Blocks.OAK_SLAB.defaultBlockState();
            }
            return null;
        }

        // Ribbons: scaffold rails along the edges of the planked ones.
        if (span.palette() == 1 && edge && up == 1 && (x + z) % 3 == 0) {
            return Blocks.SPRUCE_FENCE.defaultBlockState();
        }
        return null;
    }

    // ---------------------------------------------------------------- hallway

    private static boolean hasWall(int cx, int cz, int salt) {
        return Math.floorMod(mix(cx, cz, salt), 3L) != 0L;
    }

    private enum Room { PLAIN, LIVING, BEDROOM, KITCHEN, WRONG }

    private static Room roomOf(int cx, int cz) {
        if (chance(cx, cz, 31, 17)) return Room.LIVING;
        if (chance(cx, cz, 32, 13)) return Room.BEDROOM;
        if (chance(cx, cz, 33, 11)) return Room.KITCHEN;
        if (chance(cx, cz, 34, 41)) return Room.WRONG;
        return Room.PLAIN;
    }

    private static BlockState hallwayState(int x, int y, int z) {
        if (y == MIN_Y) return Blocks.BEDROCK.defaultBlockState();
        if (y < HALL_FLOOR - 1) return Blocks.STONE.defaultBlockState();
        int cx = x >> 4;
        int cz = z >> 4;
        Room room = roomOf(cx, cz);
        if (y <= HALL_FLOOR) {
            return (room == Room.WRONG ? Blocks.BLACK_CONCRETE : Blocks.BROWN_TERRACOTTA).defaultBlockState();
        }

        int lx = x & 15;
        int lz = z & 15;
        if (y == HALL_CEIL) {
            boolean light = (lx % 8 == 3 || lx % 8 == 4) && (lz % 8 == 3 || lz % 8 == 4);
            if (room == Room.WRONG) return (light ? Blocks.BLACK_CONCRETE : Blocks.SMOOTH_SANDSTONE).defaultBlockState();
            return (light ? Blocks.GLOWSTONE : Blocks.SMOOTH_SANDSTONE).defaultBlockState();
        }
        if (y > HALL_CEIL) return null;

        boolean pillar = lx < 2 && lz < 2;
        boolean wallW = lx == 0 && hasWall(cx, cz, 11) && !(lz >= 5 && lz <= 10);
        boolean wallN = lz == 0 && hasWall(cx, cz, 12) && !(lx >= 5 && lx <= 10);
        if (pillar) return Blocks.CUT_SANDSTONE.defaultBlockState();
        if (wallW || wallN) {
            if (room == Room.WRONG) return Blocks.BLACK_CONCRETE.defaultBlockState();
            return (y == HALL_FLOOR + 1 ? Blocks.SPRUCE_PLANKS : Blocks.SMOOTH_SANDSTONE).defaultBlockState();
        }
        switch (room) {
            case LIVING: return livingRoom(cx, cz, lx, y, lz);
            case BEDROOM: return bedroom(lx, y, lz);
            case KITCHEN: return kitchen(lx, y, lz);
            default: return null;
        }
    }

    /** One room in seventeen: a living room. The television is showing static. */
    private static BlockState livingRoom(int cx, int cz, int lx, int y, int lz) {
        if (lz == 2 && lx >= 5 && lx <= 10 && y >= 66 && y <= 68) {
            boolean white = Math.floorMod(mix(lx, y, cx * 7L + cz), 2L) == 0L;
            return (white ? Blocks.WHITE_CONCRETE : Blocks.BLACK_CONCRETE).defaultBlockState();
        }
        if (y == HALL_FLOOR + 1) {
            if (lz == 12 && lx >= 5 && lx <= 10) {
                return Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
            }
            if (lx == 7 && lz == 8) return Blocks.SPRUCE_FENCE.defaultBlockState();
            if (lx == 11 && lz == 4) return Blocks.OAK_FENCE.defaultBlockState();
            if (lx >= 3 && lx <= 12 && lz >= 3 && lz <= 11) return Blocks.RED_CARPET.defaultBlockState();
        }
        if (y == HALL_FLOOR + 2) {
            if (lx == 7 && lz == 8) return Blocks.SPRUCE_PRESSURE_PLATE.defaultBlockState();
            if (lx == 11 && lz == 4) return Blocks.LANTERN.defaultBlockState();
        }
        return null;
    }

    /** A bedroom. The bed is made. The wardrobe is shut. The lamp is on. */
    private static BlockState bedroom(int lx, int y, int lz) {
        int up = y - HALL_FLOOR;
        // The bed against the north wall, mattress and quilt, headboard behind it.
        if (lx >= 6 && lx <= 7 && lz == 2 && up <= 2) return Blocks.SPRUCE_PLANKS.defaultBlockState();
        if (lx >= 6 && lx <= 7 && lz >= 3 && lz <= 5 && up == 1) {
            return (lz == 3 ? Blocks.WHITE_WOOL : Blocks.BLUE_WOOL).defaultBlockState();
        }
        // Bedside table with a lamp.
        if (lx == 9 && lz == 3) {
            if (up == 1) return Blocks.SPRUCE_FENCE.defaultBlockState();
            if (up == 2) return Blocks.LANTERN.defaultBlockState();
        }
        // The wardrobe: a dark block two high in the far corner.
        if (lx >= 12 && lx <= 13 && lz == 13 && up <= 2) return Blocks.DARK_OAK_PLANKS.defaultBlockState();
        // A rug.
        if (up == 1 && lx >= 4 && lx <= 10 && lz >= 7 && lz <= 11) return Blocks.LIGHT_BLUE_CARPET.defaultBlockState();
        return null;
    }

    /** A kitchen. Nothing has been cooked here in a long time. */
    private static BlockState kitchen(int lx, int y, int lz) {
        int up = y - HALL_FLOOR;
        // Counters along the north wall with a sink in them.
        if (lz == 2 && lx >= 3 && lx <= 12) {
            if (up == 1) return Blocks.SMOOTH_STONE.defaultBlockState();
            if (up == 2 && lx == 7) return Blocks.WATER_CAULDRON.defaultBlockState();
        }
        // The fridge, two high, humming.
        if (lx == 13 && lz == 2 && up <= 2) return Blocks.IRON_BLOCK.defaultBlockState();
        // Cupboards on the wall.
        if (lz == 2 && lx >= 3 && lx <= 12 && up == 4) return Blocks.SPRUCE_PLANKS.defaultBlockState();
        // A table with chairs, one pulled out.
        if (lx >= 6 && lx <= 9 && lz == 9) {
            if (up == 1 && (lx == 6 || lx == 9)) return Blocks.OAK_FENCE.defaultBlockState();
            if (up == 2) return Blocks.OAK_SLAB.defaultBlockState();
        }
        if (up == 1 && lz == 8 && (lx == 7 || lx == 8)) {
            return Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
        }
        if (up == 1 && lz == 11 && lx == 7) {
            return Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
        }
        // Chequered floor.
        if (up == 1 && lx >= 3 && lx <= 12 && lz >= 3 && lz <= 12 && (lx + lz) % 2 == 0) {
            return Blocks.WHITE_CARPET.defaultBlockState();
        }
        return null;
    }

    // ---------------------------------------------------------------- drowned

    private static int drownedFloor(int x, int z) {
        double h = -12.0D
                + 12.0D * Math.sin(x * 0.031D) * Math.sin(z * 0.027D)
                + 6.0D * Math.sin(x * 0.07D + z * 0.05D)
                + 4.0D * Math.cos(z * 0.11D - x * 0.03D);
        // Trenches: a few long cuts where the seabed drops away.
        double t = Math.sin(x * 0.013D + 1.7D) * Math.cos(z * 0.009D);
        if (t > 0.86D) h -= (t - 0.86D) * 180.0D;
        int floor = (int) Math.round(h);

        long hh = mix(x >> 4, z >> 4, 21);
        if ((hh & 31) == 0) {
            int dx = (x & 15) - 8;
            int dz = (z & 15) - 8;
            int r2 = dx * dx + dz * dz;
            if (r2 <= 16 + (int) ((hh >>> 8) & 15)) {
                floor = Math.max(floor, DROWN_SEA + 4 + (int) ((hh >>> 16) & 7) - r2 / 4);
            }
        }
        return floor;
    }

    private static BlockState drownedState(int x, int y, int z) {
        if (y == MIN_Y) return Blocks.BEDROCK.defaultBlockState();
        int floor = drownedFloor(x, z);
        if (y <= floor) {
            int depth = floor - y;
            if (y < -40) return Blocks.DEEPSLATE.defaultBlockState();
            if (depth <= 2) {
                if (floor > DROWN_SEA) return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
                if (floor > 10) return Blocks.SAND.defaultBlockState();
                if (floor < -30) return Blocks.TUFF.defaultBlockState();
                return (chance(x >> 2, z >> 2, 22, 3) ? Blocks.CLAY : Blocks.GRAVEL).defaultBlockState();
            }
            return Blocks.STONE.defaultBlockState();
        }
        if (floor >= DROWN_SEA) return null;

        int up = y - floor;
        int lx = x & 15;
        int lz = z & 15;

        // The streetlights from his road, standing on the seabed, still lit.
        if (lx == 8 && lz == 8 && chance(x >> 4, z >> 4, 23, 12) && floor < DROWN_SEA - 8) {
            if (up <= 5) return Blocks.DARK_OAK_FENCE.defaultBlockState();
            if (up == 6) return Blocks.SEA_LANTERN.defaultBlockState();
        }

        // Kelp forests in clusters, each stalk its own height.
        if (chance(x >> 3, z >> 3, 24, 4) && chance(x, z, 25, 3)) {
            int tall = 3 + (int) (mix(x, z, 26) & 11);
            int cap = DROWN_SEA - 2 - floor;
            if (tall > cap) tall = cap;
            if (up < tall) return Blocks.KELP_PLANT.defaultBlockState();
            if (up == tall && tall > 0) return Blocks.KELP.defaultBlockState();
        }

        if (up == 1) {
            if (chance(x, z, 27, 9)) return Blocks.SEAGRASS.defaultBlockState();
            if (chance(x, z, 28, 70)) return Blocks.SEA_PICKLE.defaultBlockState();
        }

        if (y <= DROWN_SEA) return Blocks.WATER.defaultBlockState();
        return null;
    }

    // ----------------------------------------------------------------- backup

    private static int backupHeight(int x, int z) {
        return 62 + (int) Math.round(3.0D * Math.sin(x * 0.02D) + 3.0D * Math.cos(z * 0.02D)
                + 1.5D * Math.sin(x * 0.006D + z * 0.004D));
    }

    private static boolean backupPath(int x, int z) {
        int px = Math.floorMod(x, 64);
        int pz = Math.floorMod(z, 64);
        return px <= 1 || pz <= 1;
    }

    private static BlockState backupState(int x, int y, int z) {
        if (y == MIN_Y) return Blocks.BEDROCK.defaultBlockState();
        int h = backupHeight(x, z);
        if (y < h - 3) return Blocks.STONE.defaultBlockState();
        if (y < h) return Blocks.DIRT.defaultBlockState();
        boolean path = backupPath(x, z);
        if (y == h) return (path ? Blocks.DIRT_PATH : Blocks.GRASS_BLOCK).defaultBlockState();

        int lx = x & 15;
        int lz = z & 15;
        int dx = lx - 8;
        int dz = lz - 8;
        int up = y - h;
        int px = Math.floorMod(x, 64);
        int pz = Math.floorMod(z, 64);

        // Lamp posts at every crossing, lit, with nobody to light them.
        if (px == 3 && pz == 3) {
            if (up <= 3) return Blocks.OAK_FENCE.defaultBlockState();
            if (up == 4) return Blocks.LANTERN.defaultBlockState();
        }
        // A white fence along the paths, with a gap every so often.
        if (up == 1 && ((px == 2 && pz > 3) || (pz == 2 && px > 3)) && (x + z) % 7 != 0) {
            return Blocks.BIRCH_FENCE.defaultBlockState();
        }
        if (path) return null;

        // The same tree in every chunk, dead centre.
        if (dx == 0 && dz == 0 && up >= 1 && up <= 4) return Blocks.OAK_LOG.defaultBlockState();
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        if ((up == 3 || up == 4) && Math.abs(dx) <= 2 && Math.abs(dz) <= 2
                && !(Math.abs(dx) == 2 && Math.abs(dz) == 2) && !(dx == 0 && dz == 0)) return leaves;
        if (up == 5 && Math.abs(dx) <= 1 && Math.abs(dz) <= 1) return leaves;
        if (up == 6 && dx == 0 && dz == 0) return leaves;

        // Flowers and grass on a rigid grid, clear of the tree.
        if (up == 1 && !(Math.abs(dx) <= 2 && Math.abs(dz) <= 2)) {
            if (lx % 4 == 2 && lz % 4 == 0) {
                int pick = Math.floorMod((x >> 4) + (z >> 4), 6);
                switch (pick) {
                    case 0: return Blocks.POPPY.defaultBlockState();
                    case 1: return Blocks.DANDELION.defaultBlockState();
                    case 2: return Blocks.AZURE_BLUET.defaultBlockState();
                    case 3: return Blocks.OXEYE_DAISY.defaultBlockState();
                    case 4: return Blocks.CORNFLOWER.defaultBlockState();
                    default: return Blocks.WHITE_TULIP.defaultBlockState();
                }
            }
            if (lx % 4 == 0 && lz % 4 == 2) return Blocks.GRASS.defaultBlockState();
        }
        return null;
    }

    // ---------------------------------------------------------------- archive

    private static BlockState archiveState(int x, int y, int z) {
        if (y == MIN_Y) return Blocks.BEDROCK.defaultBlockState();
        if (y < ARCHIVE_FLOOR - 1) return Blocks.STONE.defaultBlockState();
        if (y < ARCHIVE_FLOOR) return Blocks.DARK_OAK_PLANKS.defaultBlockState();

        int lx = x & 15;
        int lz = z & 15;
        int cx = x >> 4;
        int cz = z >> 4;
        boolean reading = chance(cx, cz, 51, 7);

        if (y == ARCHIVE_FLOOR) {
            // Parquet with a runner down each aisle.
            if (!reading && (lx == 6 || lx == 7 || lx == 14 || lx == 15)) return Blocks.RED_CARPET.defaultBlockState();
            return ((lx + lz) % 2 == 0 ? Blocks.DARK_OAK_PLANKS : Blocks.SPRUCE_PLANKS).defaultBlockState();
        }
        if (y == ARCHIVE_CEIL) {
            return (lz % 4 == 0 ? Blocks.DARK_OAK_LOG : Blocks.DARK_OAK_PLANKS).defaultBlockState();
        }
        if (y > ARCHIVE_CEIL) return null;

        int up = y - ARCHIVE_FLOOR;
        // Hanging lanterns, few and far between. The light is the colour of old paper.
        if (y == ARCHIVE_CEIL - 1 && lx % 8 == 6 && lz % 8 == 2) {
            return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        }
        // Pillars at the chunk corners, floor to ceiling.
        if (lx == 0 && lz == 0) return Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState();

        if (reading) {
            // A long table, chairs pulled out, books left open. Someone was just here.
            if (lz >= 7 && lz <= 8 && lx >= 4 && lx <= 11) {
                if (up == 1 && (lx == 4 || lx == 11)) return Blocks.DARK_OAK_FENCE.defaultBlockState();
                if (up == 2) return Blocks.DARK_OAK_SLAB.defaultBlockState();
            }
            if (up == 1 && lx >= 5 && lx <= 10 && (lz == 6 || lz == 9) && lx % 2 == 1) {
                return Blocks.DARK_OAK_STAIRS.defaultBlockState()
                        .setValue(StairBlock.FACING, lz == 6 ? Direction.SOUTH : Direction.NORTH);
            }
            // Stacks of books left on the floor, a candle burning on some of them.
            if (chance(x, z, 52, 23) && (lz < 5 || lz > 10)) {
                if (up == 1) return Blocks.BOOKSHELF.defaultBlockState();
                if (up == 2 && chance(x, z, 57, 2)) {
                    return Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true);
                }
            }
            return null;
        }

        // Shelf walls across the west edge, one chunk in five, with a doorway through them.
        if (lx == 0 && chance(cx, cz, 53, 5) && !(lz >= 6 && lz <= 8) && up <= ARCHIVE_CEIL - ARCHIVE_FLOOR - 1) {
            return Blocks.BOOKSHELF.defaultBlockState();
        }

        // Two aisles of shelves per chunk running north-south, six high, with gaps to slip through.
        boolean shelfRow = lx == 3 || lx == 11;
        if (shelfRow) {
            boolean gap = (lz % 8 == 6 || lz % 8 == 7) && chance(cx * 2L + (lx == 3 ? 0 : 1), cz * 2L + lz / 8, 54, 2);
            if (!gap) {
                if (up <= 6) return Blocks.BOOKSHELF.defaultBlockState();
                if (up == 7) {
                    if (lz % 8 == 2 && chance(x, z, 55, 2)) {
                        return Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true);
                    }
                    return Blocks.DARK_OAK_SLAB.defaultBlockState();
                }
            }
            return null;
        }
        // Ladders leaning on the shelves. None of them reach anything.
        if ((lx == 4 || lx == 12) && lz % 8 == 4 && up <= 6 && chance(cx, cz * 16L + lz, 56, 3)) {
            return Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST);
        }
        return null;
    }

    // ----------------------------------------------------------------- street

    private static final int BLOCK = 48;

    private static BlockState streetState(int x, int y, int z) {
        if (y == MIN_Y) return Blocks.BEDROCK.defaultBlockState();
        if (y < STREET_GROUND - 3) return Blocks.STONE.defaultBlockState();
        if (y < STREET_GROUND) return Blocks.DIRT.defaultBlockState();

        int pz = Math.floorMod(z, BLOCK);
        int lx = x & 15;
        int up = y - STREET_GROUND;

        // The road runs east-west, with a dashed line and a kerb either side.
        if (pz <= 6) {
            if (up > 0) return null;
            if (pz == 3 && (x & 7) < 4) return Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
            return Blocks.GRAY_CONCRETE.defaultBlockState();
        }
        if (pz == 7 || pz == BLOCK - 1) {
            if (up == 0) return Blocks.SMOOTH_STONE.defaultBlockState();
            // Sodium streetlights every other lot, on the kerb.
            if (lx == 8 && ((x >> 4) & 1) == 0) {
                if (up <= 4) return Blocks.DARK_OAK_FENCE.defaultBlockState();
                if (up == 5) return Blocks.SHROOMLIGHT.defaultBlockState();
            }
            return null;
        }
        // Lots: one house per chunk on each side of the road, facing it.
        boolean north = pz < BLOCK / 2;
        int hz = north ? pz - 12 : 42 - pz;   // depth into the house, 0 at the front door
        int cx = x >> 4;
        int cz = Math.floorDiv(z, BLOCK) * 2 + (north ? 0 : 1);
        BlockState house = house(cx, cz, lx, hz, up, north);
        if (house != null) return house;

        // A flagged path from the pavement to the front door; grass everywhere else.
        if (up == 0) {
            return (lx == 7 && hz < 0 ? Blocks.SMOOTH_STONE : Blocks.GRASS_BLOCK).defaultBlockState();
        }
        return lawn(cx, cz, lx, hz, up);
    }

    private static BlockState lawn(int cx, int cz, int lx, int hz, int up) {
        // The lawn stays grass; one lot in three has a spruce near the fence.
        if (lx == 1 && hz == 4 && chance(cx, cz, 61, 3)) {
            if (up <= 5) return Blocks.SPRUCE_LOG.defaultBlockState();
        }
        if (chance(cx, cz, 61, 3) && hz >= 2 && hz <= 6 && lx <= 3) {
            int dx = Math.abs(lx - 1);
            int dz = Math.abs(hz - 4);
            int r = Math.max(dx, dz);
            BlockState leaves = Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
            if (up == 3 && r <= 2) return leaves;
            if ((up == 4 || up == 5) && r <= 1) return leaves;
            if (up == 6 && r == 0) return leaves;
        }
        // A letterbox at the kerb.
        if (lx == 10 && hz == -3 && up == 1) return Blocks.OAK_FENCE.defaultBlockState();
        if (lx == 10 && hz == -3 && up == 2) return Blocks.IRON_TRAPDOOR.defaultBlockState();
        return null;
    }

    /** A 10-wide, 9-deep house with a gabled roof. hz counts from the front wall inward. */
    private static BlockState house(int cx, int cz, int lx, int hz, int up, boolean north) {
        if (lx < 3 || lx > 12 || hz < 0 || hz > 8) return null;
        boolean lit = chance(cx, cz, 62, 7);
        boolean wallX = lx == 3 || lx == 12;
        boolean wallZ = hz == 0 || hz == 8;
        boolean cornerPost = wallX && wallZ;
        Direction front = north ? Direction.NORTH : Direction.SOUTH;

        if (up == 0) return Blocks.OAK_PLANKS.defaultBlockState();
        if (up >= 1 && up <= 4) {
            if (cornerPost) return Blocks.STRIPPED_OAK_LOG.defaultBlockState();
            if (wallZ || wallX) {
                // Front door, dead centre, shut.
                if (hz == 0 && lx == 7 && up <= 2) {
                    return Blocks.OAK_DOOR.defaultBlockState()
                            .setValue(DoorBlock.FACING, front)
                            .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT)
                            .setValue(DoorBlock.HALF, up == 1 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
                }
                // Windows: two on the front, one on each side, at eye height.
                boolean window = (up == 2 || up == 3)
                        && ((hz == 0 && (lx == 5 || lx == 10)) || (wallX && (hz == 3 || hz == 4)));
                if (window) return Blocks.GLASS_PANE.defaultBlockState();
                return Blocks.WHITE_CONCRETE.defaultBlockState();
            }
            // Inside: dark unless this is the one house with a light on.
            if (up == 4 && lx == 7 && hz == 4) {
                return (lit ? Blocks.GLOWSTONE : Blocks.OAK_PLANKS).defaultBlockState();
            }
            if (up == 4) return Blocks.OAK_PLANKS.defaultBlockState();
            return null;
        }
        // The roof: a gable stepping in along x.
        int tier = up - 5;
        int inset = Math.min(lx - 3, 12 - lx);
        if (tier >= 0 && tier <= 4 && inset >= tier) {
            if (inset == tier && tier < 4) {
                return Blocks.DARK_OAK_STAIRS.defaultBlockState()
                        .setValue(StairBlock.FACING, lx < 8 ? Direction.EAST : Direction.WEST);
            }
            return Blocks.DARK_OAK_PLANKS.defaultBlockState();
        }
        // A chimney, cold.
        if (lx == 10 && hz == 6 && up >= 5 && up <= 10) return Blocks.BRICKS.defaultBlockState();
        return null;
    }

    // ----------------------------------------------------------------- static

    private static BlockState staticState(int x, int y, int z) {
        if (y == MIN_Y) return Blocks.BEDROCK.defaultBlockState();
        if (y < STATIC_GROUND) return Blocks.STONE.defaultBlockState();
        if (y == STATIC_GROUND) {
            int v = (int) (mix(x, z, 71) & 7);
            switch (v) {
                case 0: case 1: case 2: return Blocks.BLACK_CONCRETE.defaultBlockState();
                case 3: case 4: return Blocks.WHITE_CONCRETE.defaultBlockState();
                case 5: case 6: return Blocks.GRAY_CONCRETE.defaultBlockState();
                default: return Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
            }
        }
        int up = y - STATIC_GROUND;
        int lx = x & 15;
        int lz = z & 15;
        int cx = x >> 4;
        int cz = z >> 4;

        // Antenna masts, one chunk in twenty-four, with a single red light at the top.
        if (lx == 8 && lz == 8 && chance(cx, cz, 72, 24)) {
            int tall = 30 + (int) (mix(cx, cz, 73) & 47);
            if (up < tall) return Blocks.IRON_BARS.defaultBlockState();
            if (up == tall) return Blocks.REDSTONE_BLOCK.defaultBlockState();
            if (up == tall + 1) return Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true);
            return null;
        }
        // Guy wires: a ring of bars part-way up the mast.
        if (chance(cx, cz, 72, 24) && Math.abs(lx - 8) <= 2 && Math.abs(lz - 8) <= 2
                && (Math.abs(lx - 8) == 2 || Math.abs(lz - 8) == 2) && up == 10) {
            return Blocks.IRON_BARS.defaultBlockState();
        }

        // Dead pixels: lone columns of black standing out of the plain.
        if (chance(x, z, 74, 240)) {
            int tall = 2 + (int) (mix(x, z, 75) & 3);
            if (up <= tall) return Blocks.BLACK_CONCRETE.defaultBlockState();
        }
        // A little white noise lifted off the ground, flickering in the fog.
        if (up == 1 && chance(x, z, 76, 90)) return Blocks.WHITE_CARPET.defaultBlockState();
        return null;
    }
}
