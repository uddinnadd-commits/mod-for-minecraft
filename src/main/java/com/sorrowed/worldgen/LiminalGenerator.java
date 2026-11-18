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
import com.sorrowed.registry.ModBlocks;
import net.minecraft.world.level.block.Block;
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
 * "backup": Alex's world, the day before. A perfect meadow with the same tree in every chunk,
 * flowers on a grid, lamp posts nobody lit, and the places he built (Sites): his house, the
 * well, the hollow oak, the shed, the bus stop, the mast, the orchard fence, the clinic. Each
 * holds one doorway. This is the hub; everything else is reached from here.
 *
 * "hallway": a sealed, carpeted, yellow-walled maze of rooms. Every 16x16 chunk is a room with
 * walls on its west/north edges that may or may not have a doorway. Some rooms are furnished:
 * a living room with a television showing static, a bedroom, a kitchen, and the odd room
 * where the lights are off and the carpet is black.
 *
 * "drowned": a sea 100 blocks deep with a rolling seabed, kelp forests, sea pickles, and the
 * occasional rock spire breaking the surface. The streetlights from his road are down here too.
 *
 * "archive": an endless library. Aisles of bookshelves under a low beamed ceiling, candles that
 * never burn down, ladders to nowhere, and reading rooms where the chairs are pulled out.
 *
 * "street": the road he grew up on, at night, in the rain. Identical houses on identical lots,
 * sodium streetlights, and a light on in one window in seven.
 *
 * "static": a flat plain of black, white and grey that goes on forever, like a television tuned
 * to nothing. Antenna masts with one red light each. Dead pixels standing up out of the ground.
 *
 * "orchard": his grandfather's orchard, dead. Rows of bare trees in grey fog, apples gone to
 * stone, a scarecrow every few rows, and the rows never end.
 *
 * "ward": the hospital. One corridor, white, lit by lamps that are half burnt out, rooms off it
 * with the beds made and the curtains drawn. A monitor is beeping somewhere. It never stops.
 */
public class LiminalGenerator extends ChunkGenerator {
    public static final Codec<LiminalGenerator> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.getBiomeSource()),
            Codec.STRING.fieldOf("mode").forGetter(g -> g.mode)
    ).apply(inst, inst.stable(LiminalGenerator::new)));

    public static final int MIN_Y = -64;
    public static final int HEIGHT = 384;
    public static final int HALL_FLOOR = 64;
    public static final int HALL_CEIL = 73;
    public static final int ARCHIVE_FLOOR = 64;
    public static final int ARCHIVE_CEIL = 76;
    public static final int STREET_GROUND = 64;
    public static final int STATIC_GROUND = 64;
    public static final int ORCHARD_GROUND = 64;
    public static final int WARD_FLOOR = 64;
    public static final int WARD_CEIL = 70;
    public static final int SERVER_FLOOR = 64;
    public static final int SERVER_CEIL = 72;
    public static final int DROWN_SEA = 40;

    private enum Mode {
        BACKUP, HALLWAY, DROWNED, ARCHIVE, STREET, STATIC, ORCHARD, WARD, SERVER;

        static Mode of(String name) {
            for (Mode m : values()) {
                if (m.name().equalsIgnoreCase(name)) return m;
            }
            return BACKUP;
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
            case ORCHARD: return ORCHARD_GROUND + 12;
            case WARD: return WARD_CEIL;
            case SERVER: return SERVER_CEIL;
            default: return backupHeight(x, z) + 26;
        }
    }

    private int columnTopWithExits(int x, int z) {
        return Math.max(columnTop(x, z), Exits.top(mode, x, z));
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
                int hi = columnTopWithExits(x, z);
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
            case ORCHARD: return ORCHARD_GROUND;
            case WARD: return WARD_CEIL;
            case SERVER: return SERVER_CEIL;
            default: return backupHeight(x, z);
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
        if (kind != Mode.BACKUP) {
            BlockState exit = Exits.state(mode, x, y, z);
            if (exit != null) return exit.isAir() ? null : exit;
        }
        switch (kind) {
            case HALLWAY: return hallwayState(x, y, z);
            case DROWNED: return drownedState(x, y, z);
            case BACKUP: return backupState(x, y, z);
            case ARCHIVE: return archiveState(x, y, z);
            case STREET: return streetState(x, y, z);
            case STATIC: return staticState(x, y, z);
            case ORCHARD: return orchardState(x, y, z);
            case WARD: return wardState(x, y, z);
            case SERVER: return serverState(x, y, z);
            default: return backupState(x, y, z);
        }
    }

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

    // ---------------------------------------------------------------- hallway

    private static boolean hasWall(int cx, int cz, int salt) {
        return Math.floorMod(mix(cx, cz, salt), 3L) != 0L;
    }

    private enum Room { PLAIN, LIVING, BEDROOM, KITCHEN, WRONG, POOL }

    private static Room roomOf(int cx, int cz) {
        if (chance(cx, cz, 31, 17)) return Room.LIVING;
        if (chance(cx, cz, 32, 13)) return Room.BEDROOM;
        if (chance(cx, cz, 33, 11)) return Room.KITCHEN;
        if (chance(cx, cz, 34, 41)) return Room.WRONG;
        if (chance(cx, cz, 35, 23)) return Room.POOL;
        return Room.PLAIN;
    }

    private static BlockState hallwayState(int x, int y, int z) {
        if (y == MIN_Y) return Blocks.BEDROCK.defaultBlockState();
        if (y < HALL_FLOOR - 1) return Blocks.STONE.defaultBlockState();
        int cx = x >> 4;
        int cz = z >> 4;
        Room room = roomOf(cx, cz);
        int lx = x & 15;
        int lz = z & 15;
        if (y <= HALL_FLOOR) {
            if (room == Room.POOL) {
                // A pool room: white tile, a foot of still water, and the lights reflected in it.
                boolean rim = lx <= 2 || lx >= 14 || lz <= 2 || lz >= 14;
                if (y == HALL_FLOOR && !rim) return Blocks.WATER.defaultBlockState();
                return ((lx + lz) % 2 == 0 ? Blocks.WHITE_CONCRETE : Blocks.SMOOTH_QUARTZ).defaultBlockState();
            }
            return (room == Room.WRONG ? Blocks.BLACK_CONCRETE : Blocks.BROWN_TERRACOTTA).defaultBlockState();
        }

        if (y == HALL_CEIL) {
            boolean light = (lx % 8 == 3 || lx % 8 == 4) && (lz % 8 == 3 || lz % 8 == 4);
            if (room == Room.WRONG) return (light ? Blocks.BLACK_CONCRETE : Blocks.SMOOTH_SANDSTONE).defaultBlockState();
            // Some of the panels have gone out, in patches, so the dark comes in pools.
            boolean dead = light && chance(x >> 3, z >> 3, 36, 4);
            if (dead) return Blocks.REDSTONE_LAMP.defaultBlockState();
            if (room == Room.POOL) return (light ? Blocks.SEA_LANTERN : Blocks.WHITE_CONCRETE).defaultBlockState();
            return (light ? Blocks.GLOWSTONE : Blocks.SMOOTH_SANDSTONE).defaultBlockState();
        }
        if (y > HALL_CEIL) return null;

        boolean pillar = lx < 2 && lz < 2;
        boolean wallW = lx == 0 && hasWall(cx, cz, 11) && !(lz >= 5 && lz <= 10);
        boolean wallN = lz == 0 && hasWall(cx, cz, 12) && !(lx >= 5 && lx <= 10);
        if (pillar) return Blocks.CUT_SANDSTONE.defaultBlockState();
        if (wallW || wallN) {
            if (room == Room.WRONG) return Blocks.BLACK_CONCRETE.defaultBlockState();
            if (room == Room.POOL) return Blocks.WHITE_CONCRETE.defaultBlockState();
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
        int ccx = x >> 4;
        int ccz = z >> 4;
        // Ribs: something enormous died here, and its bones are still arched over the seabed.
        if (chance(ccx, ccz, 29, 40) && floor < DROWN_SEA - 14) {
            int llx = x & 15;
            int dz = (z & 15) - 8;
            if (llx >= 1 && llx <= 14 && llx % 3 == 1 && up >= 0) {
                double r = Math.sqrt(dz * dz + up * up);
                if (Math.abs(r - 7.0D) < 0.7D) return Blocks.BONE_BLOCK.defaultBlockState();
            }
            if (dz == 0 && up == 0) return Blocks.BONE_BLOCK.defaultBlockState();   // the spine
        }
        // A room, sunk: four walls with the roof gone and the door still shut.
        if (chance(ccx, ccz, 30, 30) && floor < DROWN_SEA - 6 && up >= 1 && up <= 3) {
            int llx = x & 15;
            int llz = z & 15;
            boolean wall = (llx == 4 || llx == 11) && llz >= 4 && llz <= 11 || (llz == 4 || llz == 11) && llx >= 4 && llx <= 11;
            if (wall && !(up == 3 && chance(x, z, 31, 3))) {
                if (llz == 4 && llx == 7 && up <= 2) return Blocks.DARK_OAK_PLANKS.defaultBlockState();
                return (chance(x, z * 3L + up, 32, 3) ? Blocks.MOSSY_COBBLESTONE : Blocks.OAK_PLANKS).defaultBlockState();
            }
        }
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

    public static int backupHeight(int x, int z) {
        int natural = 62 + (int) Math.round(3.0D * Math.sin(x * 0.02D) + 3.0D * Math.cos(z * 0.02D)
                + 1.5D * Math.sin(x * 0.006D + z * 0.004D));
        return Sites.ground(x, z, natural);
    }

    private static boolean backupPath(int x, int z) {
        int px = Math.floorMod(x, 64);
        int pz = Math.floorMod(z, 64);
        return px <= 1 || pz <= 1;
    }

    private static BlockState backupState(int x, int y, int z) {
        if (y == MIN_Y) return Blocks.BEDROCK.defaultBlockState();
        int h = backupHeight(x, z);
        // The places he built own their columns, above the grass and below it (the well goes down).
        boolean site = Sites.onSite(x, z);
        if (site) {
            BlockState built = Sites.at(x, y, z);
            if (built != null) return built.isAir() ? null : built;
        }
        if (y < h - 3) return Blocks.STONE.defaultBlockState();
        if (y < h) return Blocks.DIRT.defaultBlockState();
        if (site) return y == h ? Blocks.GRASS_BLOCK.defaultBlockState() : null;
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
        boolean burnt = !reading && chance(cx, cz, 59, 9);
        // The pit: a square hole in the floor with a ladder going down into the dark. It goes a long way.
        if (!reading && chance(cx, cz, 60, 37) && lx >= 6 && lx <= 9 && lz >= 6 && lz <= 9 && y <= ARCHIVE_FLOOR && y > MIN_Y + 1) {
            if (lz == 6 && lx == 7) return Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
            if (lz == 6) return Blocks.STONE.defaultBlockState();
            return null;
        }

        if (y == ARCHIVE_FLOOR) {
            // Parquet with a runner down each aisle.
            if (!reading && (lx == 6 || lx == 7 || lx == 14 || lx == 15)) return (burnt ? Blocks.BLACK_CARPET : Blocks.RED_CARPET).defaultBlockState();
            return ((lx + lz) % 2 == 0 ? Blocks.DARK_OAK_PLANKS : Blocks.SPRUCE_PLANKS).defaultBlockState();
        }
        if (y == ARCHIVE_CEIL) {
            return (lz % 4 == 0 ? Blocks.DARK_OAK_LOG : Blocks.DARK_OAK_PLANKS).defaultBlockState();
        }
        if (y > ARCHIVE_CEIL) return null;

        int up = y - ARCHIVE_FLOOR;
        // Hanging lanterns, few and far between. The light is the colour of old paper.
        if (y == ARCHIVE_CEIL - 1 && lx % 8 == 6 && lz % 8 == 2) {
            return (burnt ? Blocks.SOUL_LANTERN : Blocks.LANTERN).defaultBlockState().setValue(LanternBlock.HANGING, true);
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
                if (up <= 6) {
                    // In the burnt aisles half the shelves are char.
                    if (burnt && chance(x, z * 7L + up, 61, 2)) return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
                    return Blocks.BOOKSHELF.defaultBlockState();
                }
                if (up == 7) {
                    if (lz % 8 == 2 && chance(x, z, 55, 2)) {
                        return Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true);
                    }
                    return Blocks.DARK_OAK_SLAB.defaultBlockState();
                }
            }
            return null;
        }
        // Books knocked off the shelves, lying open in the aisles.
        if (up == 1 && (lx == 4 || lx == 10) && chance(x, z, 58, 31)) return Blocks.BOOKSHELF.defaultBlockState();
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
            // A car left in the road, one lot in thirteen, lights on, nobody in it.
            if (chance(x >> 4, Math.floorDiv(z, BLOCK), 66, 13) && pz >= 2 && pz <= 4 && (x & 15) >= 4 && (x & 15) <= 9) {
                int clx = x & 15;
                if (up == 1) {
                    if (clx == 9) return Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState();
                    if (clx == 4) return Blocks.REDSTONE_LAMP.defaultBlockState();
                    return Blocks.BLACK_CONCRETE.defaultBlockState();
                }
                if (up == 2 && clx >= 5 && clx <= 8) return (clx == 5 || clx == 8 ? Blocks.BLACK_CONCRETE : Blocks.BLACK_STAINED_GLASS).defaultBlockState();
            }
            if (up > 0) return null;
            // Puddles: a still source block sunk into the road, holding the streetlight.
            if (pz >= 1 && pz <= 5 && chance(x >> 2, z >> 1, 63, 9) && chance(x, z, 64, 2)) return Blocks.WATER.defaultBlockState();
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

    /** A swing frame, two chains, a seat, and a roundabout that does not turn. */
    private static BlockState playground(int lx, int hz, int up) {
        if (hz < 0 || hz > 8 || lx < 3 || lx > 12) return null;
        if (hz == 2) {
            if ((lx == 4 || lx == 11) && up >= 1 && up <= 3) return Blocks.DARK_OAK_FENCE.defaultBlockState();
            if (lx >= 4 && lx <= 11 && up == 4) return Blocks.DARK_OAK_SLAB.defaultBlockState();
            if ((lx == 6 || lx == 9) && (up == 2 || up == 3)) return Blocks.CHAIN.defaultBlockState();
            if ((lx == 6 || lx == 9) && up == 1) return Blocks.DARK_OAK_PRESSURE_PLATE.defaultBlockState();
        }
        if (hz >= 5 && hz <= 7 && lx >= 6 && lx <= 8 && up == 1) {
            return (lx == 7 && hz == 6 ? Blocks.IRON_BARS : Blocks.SMOOTH_STONE_SLAB).defaultBlockState();
        }
        return null;
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
        // One lot in nine has no house: a playground nobody plays in.
        if (chance(cx, cz, 65, 9)) return playground(lx, hz, up);
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
                            .setValue(DoorBlock.HALF, up == 1 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER)
                            .setValue(DoorBlock.OPEN, lit);   // the house with the light on has its door open
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

        // The test card: bars of colour standing in the noise, one chunk in fifty.
        if (chance(cx, cz, 79, 50) && lz == 8 && lx >= 1 && lx <= 14 && up >= 1 && up <= 9) {
            Block[] bars = {Blocks.WHITE_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.CYAN_CONCRETE, Blocks.LIME_CONCRETE,
                    Blocks.MAGENTA_CONCRETE, Blocks.RED_CONCRETE, Blocks.BLUE_CONCRETE};
            if (up <= 2) return Blocks.BLACK_CONCRETE.defaultBlockState();
            return bars[(lx - 1) / 2].defaultBlockState();
        }
        // A living room, with no house around it: carpet, a sofa, the set on, facing nobody.
        if (chance(cx, cz, 80, 35) && lx >= 4 && lx <= 11 && lz >= 4 && lz <= 11) {
            if (up == 1 && lz == 10 && lx >= 6 && lx <= 9) return Blocks.RED_NETHER_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
            if (lz == 5 && lx == 7) {
                if (up == 1) return Blocks.DARK_OAK_SLAB.defaultBlockState();
                if (up == 2) return Blocks.SEA_LANTERN.defaultBlockState();
            }
            if (up == 1) return Blocks.RED_CARPET.defaultBlockState();
            return null;
        }
        // Ghost houses: the outline of a house that is not loaded in, one chunk in forty.
        if (chance(cx, cz, 77, 40) && lx >= 4 && lx <= 11 && lz >= 4 && lz <= 11 && up >= 1 && up <= 5) {
            boolean edge = lx == 4 || lx == 11 || lz == 4 || lz == 11;
            boolean roof = up == 5;
            if ((edge || roof) && chance(x, z * 7L + up, 78, 3)) {
                return (roof ? Blocks.LIGHT_GRAY_STAINED_GLASS : Blocks.WHITE_STAINED_GLASS).defaultBlockState();
            }
            return null;
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

    // ---------------------------------------------------------------- orchard

    private static BlockState orchardState(int x, int y, int z) {
        if (y == MIN_Y) return Blocks.BEDROCK.defaultBlockState();
        if (y < ORCHARD_GROUND - 2) return Blocks.STONE.defaultBlockState();
        if (y < ORCHARD_GROUND) return Blocks.COARSE_DIRT.defaultBlockState();
        if (y == ORCHARD_GROUND) {
            // Grass gone grey; bare dirt under the trees; a cart track between every other row.
            int rx = Math.floorMod(x, 8);
            if (Math.floorMod(x, 16) == 12) return Blocks.DIRT_PATH.defaultBlockState();
            if (rx <= 1 || rx >= 7) return Blocks.PODZOL.defaultBlockState();
            return (chance(x, z, 81, 5) ? Blocks.COARSE_DIRT : Blocks.GRASS_BLOCK).defaultBlockState();
        }
        int up = y - ORCHARD_GROUND;
        int rx = Math.floorMod(x, 8);
        int rz = Math.floorMod(z, 6);
        int row = Math.floorDiv(x, 8);
        int col = Math.floorDiv(z, 6);

        // The barn: a long dark ruin, one in forty-five, the trees cut away around it.
        int bcx = x >> 4;
        int bcz = z >> 4;
        if (chance(bcx, bcz, 86, 45)) {
            int lx = x & 15;
            int lz = z & 15;
            if (lx >= 2 && lx <= 13 && lz >= 3 && lz <= 12) {
                boolean wall = lx == 2 || lx == 13 || lz == 3 || lz == 12;
                boolean door = lz == 3 && lx >= 7 && lx <= 8 && up <= 3;
                if (wall && !door && up <= 5) {
                    if (up >= 3 && chance(x, z * 5L + up, 87, 4)) return null;   // boards missing
                    return ((lx + up) % 4 == 0 ? Blocks.DARK_OAK_LOG : Blocks.DARK_OAK_PLANKS).defaultBlockState();
                }
                if (up == 6 && lx >= 3 && lx <= 12 && !chance(x, z, 88, 3)) return Blocks.DARK_OAK_SLAB.defaultBlockState();
                if (up == 1 && lx >= 10 && lz >= 9 && lz <= 11) return Blocks.HAY_BLOCK.defaultBlockState();
                if (lx == 5 && lz == 10 && up >= 1 && up <= 3) return Blocks.OAK_FENCE.defaultBlockState();
                if (lx == 5 && lz == 10 && up == 4) return Blocks.CARVED_PUMPKIN.defaultBlockState();
                return null;
            }
        }
        // The trees: the same dead tree every 8 by 6, trunk four high, four bare branches, no leaves.
        if (rx == 0 && rz == 0) {
            if (up <= 4) return Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
            if (up == 5 && chance(row, col, 82, 3)) return Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
        }
        if (up == 4 && rz == 0 && (rx == 1 || rx == 7)) return Blocks.SPRUCE_FENCE.defaultBlockState();
        if (up == 4 && rx == 0 && (rz == 1 || rz == 5)) return Blocks.SPRUCE_FENCE.defaultBlockState();
        if (up == 5 && ((rz == 0 && (rx == 2 || rx == 6)) || (rx == 0 && (rz == 2 || rz == 4)))) return Blocks.SPRUCE_FENCE.defaultBlockState();
        // Apples, gone to stone, lying where they fell.
        if (up == 1 && (rx == 1 || rx == 7 || rz == 1 || rz == 5) && chance(x, z, 83, 7)) return Blocks.COBBLESTONE.defaultBlockState();
        // The scarecrow, one in every eleven trees' worth of row, standing in the row with its arms out.
        if (chance(row, col, 84, 11) && rz == 3) {
            if (rx == 4) {
                if (up <= 2) return Blocks.OAK_FENCE.defaultBlockState();
                if (up == 3) return Blocks.HAY_BLOCK.defaultBlockState();
                if (up == 4) return Blocks.CARVED_PUMPKIN.defaultBlockState();
            }
            if (up == 3 && (rx == 3 || rx == 5)) return Blocks.OAK_FENCE.defaultBlockState();
        }
        if (up == 1 && chance(x, z, 85, 17)) return Blocks.DEAD_BUSH.defaultBlockState();
        return null;
    }

    // ------------------------------------------------------------------- ward

    private static BlockState wardState(int x, int y, int z) {
        if (y == MIN_Y) return Blocks.BEDROCK.defaultBlockState();
        if (y < WARD_FLOOR - 1) return Blocks.STONE.defaultBlockState();
        int pz = Math.floorMod(z, 32);
        boolean corridor = pz >= 13 && pz <= 18;
        int lx = x & 15;
        if (y == WARD_FLOOR - 1) return Blocks.SMOOTH_STONE.defaultBlockState();
        if (y == WARD_FLOOR) {
            if (corridor && (pz == 15 || pz == 16)) return Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
            if (corridor) return Blocks.WHITE_CONCRETE.defaultBlockState();
            return ((lx + pz) % 2 == 0 ? Blocks.WHITE_CONCRETE : Blocks.LIGHT_GRAY_CONCRETE).defaultBlockState();
        }
        if (y == WARD_CEIL) {
            if (corridor && (pz == 15 || pz == 16) && lx % 6 == 2) {
                boolean dead = chance(x >> 1, z >> 5, 91, 3);
                return (dead ? Blocks.REDSTONE_LAMP.defaultBlockState()
                        : Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true));
            }
            boolean darkRoom = chance(x >> 4, z >> 5, 97, 4);
            if (!corridor && lx == 8 && (pz == 6 || pz == 25) && !darkRoom) return Blocks.SEA_LANTERN.defaultBlockState();
            return Blocks.WHITE_CONCRETE.defaultBlockState();
        }
        if (y > WARD_CEIL) return null;
        int up = y - WARD_FLOOR;

        // The corridor walls, with a door into a room every 16 blocks on each side.
        if (pz == 12 || pz == 19) {
            boolean door = lx == 8 && up >= 1 && up <= 2;
            if (door) {
                // Some rooms are barred from the corridor side.
                return chance(x >> 4, z >> 5, 98, 6) ? Blocks.IRON_BARS.defaultBlockState() : null;
            }
            if (up == 1 && lx == 4 && chance(x >> 4, z >> 5, 92, 2)) return Blocks.IRON_BARS.defaultBlockState();   // a window into the room
            return (up == 2 ? Blocks.QUARTZ_BLOCK : Blocks.WHITE_CONCRETE).defaultBlockState();
        }
        if (corridor) {
            // A gurney left in the corridor, now and then. A trail of something on the floor.
            if (up == 1 && pz == 14 && lx >= 6 && lx <= 7 && chance(x >> 4, z >> 5, 93, 5)) return Blocks.WHITE_WOOL.defaultBlockState();
            // The nurses' station: a counter, a lamp, nobody behind it.
            if (chance(x >> 4, z >> 5, 99, 7) && pz == 13 && lx >= 2 && lx <= 5) {
                if (up == 1) return Blocks.QUARTZ_BLOCK.defaultBlockState();
                if (up == 2 && lx == 3) return Blocks.LANTERN.defaultBlockState();
            }
            return null;
        }
        // The rooms: walls between every 16 blocks, beds inside, curtains, a chair.
        if (lx == 0) return Blocks.WHITE_CONCRETE.defaultBlockState();
        boolean north = pz < 13;
        int depth = north ? 11 - pz : pz - 20;   // 0 at the corridor wall, 11 at the back
        if (depth >= 11) return (up == 1 && lx == 8 ? Blocks.IRON_BARS : Blocks.WHITE_CONCRETE).defaultBlockState();   // back wall with a barred window
        if (depth < 0) return null;
        if (up == 1) {
            if (depth == 8 && (lx == 3 || lx == 4)) return Blocks.WHITE_WOOL.defaultBlockState();            // the bed
            if (depth == 8 && lx == 11 && chance(x >> 4, z >> 5, 94, 2)) return Blocks.WHITE_WOOL.defaultBlockState();
            if (depth == 5 && lx == 9) return Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
            if (depth == 9 && lx == 2) return Blocks.FLOWER_POT.defaultBlockState();
            if (depth == 9 && lx == 5 && chance(x >> 4, z >> 5, 96, 3)) return Blocks.REDSTONE_TORCH.defaultBlockState();   // the monitor light
        }
        if (up == 2 && depth == 8 && lx == 3) return Blocks.QUARTZ_SLAB.defaultBlockState();                // the pillow
        if (up >= 1 && up <= 3 && depth >= 5 && depth <= 9 && lx == 6 && chance(x >> 4, z >> 5, 95, 2)) return Blocks.LIGHT_GRAY_WOOL.defaultBlockState();   // the curtain
        return null;
    }

    // ----------------------------------------------------------------- server

    /**
     * Where his world was kept. Rows of racks under a low ceiling, cold light every few metres,
     * cables along the floor, and the noise of the fans. Every rack has a few lights on it.
     */
    private static BlockState serverState(int x, int y, int z) {
        if (y == MIN_Y) return Blocks.BEDROCK.defaultBlockState();
        if (y < SERVER_FLOOR - 1) return Blocks.STONE.defaultBlockState();
        int px = Math.floorMod(x, 6);
        int pz = Math.floorMod(z, 48);
        boolean crossAisle = pz <= 3;
        boolean rack = (px == 1 || px == 2) && !crossAisle;
        if (y == SERVER_FLOOR - 1) return Blocks.DEEPSLATE_TILES.defaultBlockState();
        if (y == SERVER_FLOOR) {
            // Raised floor tiles; perforated (grates) in front of the racks, where the cold air comes up.
            if (!crossAisle && (px == 0 || px == 3)) return Blocks.IRON_TRAPDOOR.defaultBlockState();
            return ((x + z) % 2 == 0 ? Blocks.POLISHED_DEEPSLATE : Blocks.DEEPSLATE_TILES).defaultBlockState();
        }
        if (y == SERVER_CEIL) {
            if (px == 4 && Math.floorMod(z, 8) == 4 && !chance(x >> 3, z >> 3, 111, 3)) return Blocks.SEA_LANTERN.defaultBlockState();
            return Blocks.GRAY_CONCRETE.defaultBlockState();
        }
        if (y > SERVER_CEIL) return null;
        int up = y - SERVER_FLOOR;

        if (rack) {
            if (Math.floorMod(z, 48) == 25) return null;   // a gap in the middle of each row, to slip through
            if (up <= 5) {
                // Blinking lights: a few lit faces on each rack. Lime and red, mostly dark.
                long h = mix(x, z * 13L + up, 112);
                if ((h & 15) == 0) return Blocks.VERDANT_FROGLIGHT.defaultBlockState();
                if ((h & 31) == 1) return Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true);
                if ((h & 63) == 2) return Blocks.SEA_LANTERN.defaultBlockState();
                return (up == 0 || up == 5 ? Blocks.POLISHED_BLACKSTONE : Blocks.POLISHED_BLACKSTONE_BRICKS).defaultBlockState();
            }
            if (up == 6) return Blocks.IRON_BARS.defaultBlockState();   // the cable tray
            return null;
        }
        // Cables along the floor in the hot aisle.
        if (up == 1 && px == 5 && Math.floorMod(z, 3) != 0 && !crossAisle) return Blocks.BLACK_CARPET.defaultBlockState();
        // A terminal at the end of a row now and then, the screen still on.
        if (crossAisle && pz == 3 && px == 4 && chance(x >> 3, z >> 5, 113, 5)) {
            if (up == 1) return Blocks.IRON_BLOCK.defaultBlockState();
            if (up == 2) return Blocks.BLACK_STAINED_GLASS.defaultBlockState();
        }
        return null;
    }
}
