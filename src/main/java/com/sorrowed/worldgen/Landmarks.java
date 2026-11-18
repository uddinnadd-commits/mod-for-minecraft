package com.sorrowed.worldgen;

import com.sorrowed.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * The places worth finding in each world, one chunk (or one lot, or one room) each, rare enough
 * that coming across one feels like a find. Most of them have a page lying somewhere in them.
 * Also the loose pages scattered on the floors in between.
 *
 * Same contract as {@link Exits}: null means "not mine, generate as usual"; AIR means "keep clear".
 */
public final class Landmarks {
    private Landmarks() {}

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final int G = 64;

    private static long mix(long a, long b, long c) {
        long h = a * 0x9E3779B97F4A7C15L ^ b * 0xC2B2AE3D27D4EB4FL ^ c * 0x165667B19E3779F9L;
        h ^= h >>> 32;
        h *= 0xD6E8FEB86659FD93L;
        h ^= h >>> 32;
        return h;
    }

    private static boolean chance(long a, long b, long salt, int n) {
        return Math.floorMod(mix(a, b, salt), (long) n) == 0L;
    }

    private static BlockState d(Block b) {
        return b.defaultBlockState();
    }

    private static BlockState papers() {
        return ModBlocks.LOOSE_PAPERS.get().defaultBlockState();
    }

    private static BlockState stairs(Block b, Direction facing) {
        return b.defaultBlockState().setValue(StairBlock.FACING, facing);
    }

    private static BlockState topSlab(Block b) {
        return b.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
    }

    private static BlockState candle() {
        return Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true);
    }

    /** A page lying on the floor, now and then, between everything else. */
    public static boolean scatter(int x, int z) {
        return chance(x, z, 401, 700);
    }

    public static BlockState state(String mode, int x, int y, int z) {
        switch (mode) {
            case "hallway": return hallway(x, y, z);
            case "archive": return archive(x, y, z);
            case "drowned": return drowned(x, y, z);
            case "street": return street(x, y, z);
            case "static": return statik(x, y, z);
            case "orchard": return orchard(x, y, z);
            case "ward": return ward(x, y, z);
            case "server": return server(x, y, z);
            default: return null;
        }
    }

    // ---------------------------------------------------------------- hallway

    private static BlockState hallway(int x, int y, int z) {
        int up = y - G;
        if (up < 1 || up > 8) return null;
        int cx = x >> 4;
        int cz = z >> 4;
        int lx = x & 15;
        int lz = z & 15;
        if (lx == 0 || lz == 0) return null;   // keep the room's own walls
        if (chance(cx, cz, 301, 19)) return office(lx, up, lz);
        if (chance(cx, cz, 302, 29)) return childsRoom(x, z, lx, up, lz);
        if (chance(cx, cz, 303, 37)) return stairwell(lx, up, lz);
        return null;
    }

    /** Desks in rows, every screen on, every chair pushed in but one. */
    private static BlockState office(int lx, int up, int lz) {
        if (lx % 5 == 0 && lz >= 2 && lz <= 13 && lz % 4 != 1 && up <= 2) return d(Blocks.LIGHT_GRAY_WOOL);   // partitions
        if (lz % 4 == 3 && lx % 5 >= 1 && lx % 5 <= 3) {
            if (up == 1) return topSlab(Blocks.SPRUCE_SLAB);
            if (up == 2 && lx % 5 == 2) return d(Blocks.BLACK_CONCRETE);                                     // the monitor
            if (up == 2 && lx == 8 && lz == 7) return papers();
        }
        if (lz % 4 == 0 && lx % 5 == 2 && up == 1) return stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH);      // chairs
        if (lx == 13 && lz == 2 && up <= 2) return d(Blocks.WHITE_CONCRETE);                                  // the water cooler
        if (lx == 13 && lz == 2 && up == 3) return d(Blocks.LIGHT_BLUE_STAINED_GLASS);
        return AIR;
    }

    /** A child's room nobody remembers: blue walls, a small bed, crayon on the walls, a dollhouse. */
    private static BlockState childsRoom(int x, int z, int lx, int up, int lz) {
        boolean wall = lx == 1 || lx == 14 || lz == 1 || lz == 14;
        if (wall) {
            if (lz == 1 && lx >= 7 && lx <= 8 && up <= 2) return AIR;   // the door, open
            if (lz == 14 && lx >= 3 && lx <= 12 && up >= 2 && up <= 4 && chance(x, up, 304, 3)) {
                Block[] crayons = {Blocks.RED_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.GREEN_CONCRETE, Blocks.BLACK_CONCRETE};
                return d(crayons[(int) Math.floorMod(mix(x, up, 305), 4L)]);
            }
            return d(Blocks.LIGHT_BLUE_CONCRETE);
        }
        if (up == 8) return d(Blocks.LIGHT_BLUE_CONCRETE);
        if (up == 1) {
            if (lx == 3 && lz == 3) return Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.FOOT);
            if (lx == 3 && lz == 4) return Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.HEAD);
            if (lx == 12 && lz == 3) return d(Blocks.BARREL);                                                 // the toy box
            if (lx >= 10 && lx <= 12 && lz >= 10 && lz <= 12) return d(Blocks.OAK_PLANKS);                    // the dollhouse
            if (lx == 6 && lz == 8) return papers();
            return d(Blocks.BLUE_CARPET);
        }
        if (up == 2 && lx == 11 && lz == 11) return d(Blocks.SPRUCE_SLAB);
        if (up == 2 && lx == 11 && lz == 9) return d(Blocks.TORCH);                                           // a light left on in the dollhouse
        return AIR;
    }

    /** Stairs that climb into the ceiling and stop. */
    private static BlockState stairwell(int lx, int up, int lz) {
        if (lz >= 7 && lz <= 8) {
            if (lx >= 4 && lx <= 10) {
                int k = lx - 4;
                if (up == k + 1) return stairs(Blocks.SMOOTH_SANDSTONE_STAIRS, Direction.EAST);
                if (up <= k) return d(Blocks.SMOOTH_SANDSTONE);
            }
            if (lx >= 11 && lx <= 12 && up <= 7) return d(Blocks.SMOOTH_SANDSTONE);
            if (lx == 11 && lz == 7 && up == 8) return papers();
        }
        if ((lz == 6 || lz == 9) && lx >= 5 && lx <= 12 && up == Math.min(8, lx - 2)) return d(Blocks.DARK_OAK_FENCE);
        return AIR;
    }

    // ---------------------------------------------------------------- archive

    private static BlockState archive(int x, int y, int z) {
        int up = y - G;
        if (up < 1 || up > 11) return null;
        int cx = x >> 4;
        int cz = z >> 4;
        int lx = x & 15;
        int lz = z & 15;
        if (lx == 0) return null;
        if (chance(cx, cz, 311, 17)) return catalogue(lx, up, lz);
        if (chance(cx, cz, 312, 23)) return mapRoom(lx, up, lz);
        if (chance(cx, cz, 313, 21)) return collapse(x, z, lx, up, lz);
        return null;
    }

    /** Drawers and drawers, a card in each for every day. */
    private static BlockState catalogue(int lx, int up, int lz) {
        boolean row = (lz == 3 || lz == 4 || lz == 7 || lz == 8 || lz == 11 || lz == 12) && lx >= 2 && lx <= 13;
        if (row && up <= 3) return d(Blocks.BARREL);
        if (row && up == 4 && lz % 4 == 0 && lx % 4 == 2) return candle();
        if (lx == 7 && lz == 8 && up == 4) return papers();
        return AIR;
    }

    /** A table with a map of the archive on it, and a mark where you are standing. */
    private static BlockState mapRoom(int lx, int up, int lz) {
        if (lx >= 5 && lx <= 10 && lz >= 6 && lz <= 9 && up == 1) return d(Blocks.CARTOGRAPHY_TABLE);
        if (up == 2 && ((lx == 5 && lz == 6) || (lx == 10 && lz == 9))) return candle();
        if (up == 2 && lx == 7 && lz == 7) return papers();
        if (up == 1 && ((lx == 2 && lz == 2) || (lx == 13 && lz == 13))) return d(Blocks.LECTERN);
        if (up == 1 && (lx == 2 || lx == 13) && lz >= 4 && lz <= 11) return d(Blocks.BOOKSHELF);
        return AIR;
    }

    /** Where the stacks came down. */
    private static BlockState collapse(int x, int z, int lx, int up, int lz) {
        double r = Math.sqrt((lx - 8) * (lx - 8) + (lz - 8) * (lz - 8));
        int hgt = (int) Math.max(0, Math.round(5.0D - r * 0.75D));
        if (up <= hgt) {
            long h = mix(x, z * 31L + up, 314) & 7;
            if (h < 3) return d(Blocks.GRAVEL);
            if (h < 6) return d(Blocks.COBBLESTONE);
            return d(Blocks.BOOKSHELF);
        }
        if (lx == 8 && lz == 8 && up == hgt + 1) return papers();
        if (up == 1 && chance(x, z, 315, 9)) return d(Blocks.BOOKSHELF);
        return AIR;
    }

    // ---------------------------------------------------------------- drowned

    private static BlockState drowned(int x, int y, int z) {
        int cx = x >> 4;
        int cz = z >> 4;
        int lx = x & 15;
        int lz = z & 15;
        int sea = LiminalGenerator.DROWN_SEA;
        if (chance(cx, cz, 321, 25) && LiminalGenerator.drownedFloor(cx * 16 + 8, cz * 16 + 8) < sea - 3) {
            // A raft, with a lantern, drifting nowhere.
            if (y == sea && lx >= 6 && lx <= 10 && lz >= 6 && lz <= 9) return d(Blocks.OAK_PLANKS);
            if (lx == 7 && lz == 7 && y == sea + 1) return d(Blocks.OAK_FENCE);
            if (lx == 7 && lz == 7 && y == sea + 2) return d(Blocks.LANTERN);
            if (lx == 9 && lz == 8 && y == sea + 1) return papers();
            return null;
        }
        if (chance(cx, cz, 322, 45)) {
            // The school bus, on its side of the road, at the bottom of the sea.
            int fc = LiminalGenerator.drownedFloor(cx * 16 + 8, cz * 16 + 8);
            int rel = y - fc;
            if (fc < sea - 6 && lx >= 3 && lx <= 11 && lz >= 7 && lz <= 9 && rel >= 0 && rel <= 3) {
                if (rel == 0) return ((lx == 4 || lx == 10) && lz != 8 ? Blocks.BLACK_CONCRETE : Blocks.YELLOW_CONCRETE).defaultBlockState();
                boolean shell = lx == 3 || lx == 11 || lz == 7 || lz == 9 || rel == 3;
                if (!shell) return d(Blocks.WATER);
                if (rel == 2 && (lz == 7 || lz == 9) && lx >= 4 && lx <= 10) {
                    return (lx % 2 == 0 ? Blocks.GLASS : Blocks.WATER).defaultBlockState();
                }
                return d(Blocks.YELLOW_CONCRETE);
            }
            return null;
        }
        if (chance(cx, cz, 323, 35)) {
            // A graveyard on the seabed. The stones are in rows. Somebody kept them in rows.
            int f = LiminalGenerator.drownedFloor(x, z);
            if (f < sea - 4 && y == f + 1 && lx % 3 == 1 && lz % 3 == 1 && lx <= 13 && lz <= 13) {
                return (chance(x, z, 324, 2) ? Blocks.MOSSY_COBBLESTONE_WALL : Blocks.COBBLESTONE_WALL).defaultBlockState();
            }
        }
        return null;
    }

    // ----------------------------------------------------------------- street

    private static BlockState street(int x, int y, int z) {
        int up = y - G;
        if (up < 0 || up > 10) return null;
        int pz = Math.floorMod(z, 48);
        if (pz <= 7 || pz == 47) return null;   // the road and the kerbs stay as they are
        boolean north = pz < 24;
        int hz = north ? pz - 12 : 42 - pz;
        int lx = x & 15;
        int cx = x >> 4;
        int lot = Math.floorDiv(z, 48) * 2 + (north ? 0 : 1);
        Direction toRoad = north ? Direction.NORTH : Direction.SOUTH;
        if (chance(cx, lot, 331, 11)) return shop(lx, hz, up, toRoad);
        if (chance(cx, lot, 332, 23)) return school(lx, hz, up);
        if (chance(cx, lot, 333, 17)) return park(x, z, lx, hz, up);
        return null;
    }

    /** The corner shop. The sign is lit. The shelves are stocked. Nobody is at the till. */
    private static BlockState shop(int lx, int hz, int up, Direction toRoad) {
        if (lx < 3 || lx > 12 || hz < 0 || hz > 8) return null;
        if (up == 0) return d(Blocks.POLISHED_ANDESITE);
        if (up == 5) return d(Blocks.SMOOTH_STONE_SLAB);
        if (up > 5) return AIR;
        boolean side = lx == 3 || lx == 12 || hz == 8;
        if (hz == 0) {
            if (lx >= 7 && lx <= 8 && up <= 2) return AIR;                                                   // the door
            if (up == 4) return d(Blocks.YELLOW_CONCRETE);                                                   // the sign
            return d(Blocks.GLASS_PANE);
        }
        if (side) return d(Blocks.BRICKS);
        if (hz == 1 && up == 4 && lx >= 5 && lx <= 10) return d(Blocks.GLOWSTONE);
        if ((hz == 3 || hz == 5) && lx >= 5 && lx <= 10 && up <= 2) return (lx % 3 == 0 ? Blocks.BARREL : Blocks.SPRUCE_PLANKS).defaultBlockState();
        if (hz == 7 && lx >= 4 && lx <= 6 && up == 1) return d(Blocks.QUARTZ_BLOCK);                        // the counter
        if (hz == 7 && lx == 5 && up == 2) return papers();
        if (hz == 4 && lx == 8 && up == 4) return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        return AIR;
    }

    /** The school. The hall clock says ten to three. It always says ten to three. */
    private static BlockState school(int lx, int hz, int up) {
        if (lx < 1 || lx > 14 || hz < -3 || hz > 10) return null;
        if (hz < -1) {
            // The flagpole on the lawn, a white flag, no wind.
            if (lx == 2 && hz == -3 && up >= 1 && up <= 8) return d(Blocks.OAK_FENCE);
            if (lx == 3 && hz == -3 && (up == 7 || up == 8)) return d(Blocks.WHITE_WOOL);
            return up == 0 ? null : AIR;
        }
        if (up == 0) return d(Blocks.OAK_PLANKS);
        if (up == 8) return d(Blocks.SMOOTH_STONE);
        if (up > 8) return AIR;
        boolean wall = lx == 1 || lx == 14 || hz == -1 || hz == 10;
        if (wall) {
            if (hz == -1 && lx >= 7 && lx <= 8 && up <= 3) return AIR;                                       // the doors
            boolean window = (up == 2 || up == 3 || up == 5 || up == 6) && ((hz == -1 || hz == 10) ? lx % 3 != 1 : hz % 3 != 1);
            if (hz == 10 && lx >= 4 && lx <= 11 && up >= 2 && up <= 4) return d(Blocks.GREEN_TERRACOTTA);   // the board
            if (hz == 10 && lx == 7 && up == 6) return d(Blocks.QUARTZ_BLOCK);                               // the clock
            if (window) return d(Blocks.GLASS_PANE);
            return d(Blocks.BRICKS);
        }
        if ((hz == 3 || hz == 5 || hz == 7) && lx >= 3 && lx <= 12 && up == 1) {
            return lx % 2 == 1 ? topSlab(Blocks.OAK_SLAB) : stairs(Blocks.OAK_STAIRS, Direction.SOUTH);
        }
        if (hz == 9 && lx >= 6 && lx <= 9 && up == 1) return d(Blocks.SPRUCE_PLANKS);                       // the teacher's desk
        if (hz == 9 && lx == 7 && up == 2) return papers();
        if (up == 7 && lx % 4 == 2 && hz % 4 == 2) return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        return AIR;
    }

    /** The park: a pond with bread floating on it, two benches, a lamp, two trees. */
    private static BlockState park(int x, int z, int lx, int hz, int up) {
        if (hz < -3 || hz > 10) return null;
        double r = Math.sqrt((lx - 8) * (lx - 8) + (hz - 4) * (hz - 4));
        if (up == 0) return (r <= 3.2D ? Blocks.WATER : Blocks.GRASS_BLOCK).defaultBlockState();
        if ((lx == 4 || lx == 12) && hz == 9 && up == 1) return stairs(Blocks.OAK_STAIRS, Direction.SOUTH);
        if (lx == 5 && hz == 9 && up == 1) return papers();
        if (lx == 8 && hz == 9 && up <= 3) return d(Blocks.DARK_OAK_FENCE);
        if (lx == 8 && hz == 9 && up == 4) return d(Blocks.LANTERN);
        for (int tx : new int[] {2, 14}) {
            int tz = -1;
            if (lx == tx && hz == tz && up <= 4) return d(Blocks.OAK_LOG);
            int dx = Math.abs(lx - tx);
            int dz = Math.abs(hz - tz);
            BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
            if ((up == 4 || up == 5) && dx <= 2 && dz <= 2 && !(dx == 2 && dz == 2)) return leaves;
            if (up == 6 && dx <= 1 && dz <= 1) return leaves;
        }
        if (up == 1 && r > 3.2D && r < 4.2D && chance(x, z, 334, 3)) return d(Blocks.TALL_GRASS);
        return AIR;
    }

    // ----------------------------------------------------------------- static

    private static BlockState statik(int x, int y, int z) {
        int cx = x >> 4;
        int cz = z >> 4;
        int lx = x & 15;
        int lz = z & 15;
        int up = y - G;
        if (chance(cx, cz, 342, 60)) {
            // The hole where the picture did not load. It goes down a long way.
            double r = Math.sqrt((lx - 8) * (lx - 8) + (lz - 8) * (lz - 8));
            if (y >= 1 && y <= G) {
                if (r <= 4.5D) return y == 1 ? d(Blocks.BLACK_CONCRETE) : AIR;
                if (r <= 5.5D) return d(Blocks.BLACK_CONCRETE);
            }
            if (lx == 8 && lz == 15 && up == 1) return papers();
            return null;
        }
        if (up < 1 || up > 8) return null;
        if (chance(cx, cz, 341, 40) && lz == 8 && lx >= 2 && lx <= 13) {
            // A wall of screens. Each one is a room in the backup. One of them is this one.
            if (lx % 3 == 1 || up % 3 == 0) return d(Blocks.BLACK_CONCRETE);
            long h = mix(x, up, 343) & 3;
            return (h == 0 ? Blocks.GRAY_CONCRETE : h == 1 ? Blocks.LIGHT_GRAY_STAINED_GLASS : Blocks.SEA_LANTERN).defaultBlockState();
        }
        if (chance(cx, cz, 341, 40) && lx == 7 && lz == 6 && up == 1) return papers();
        if (chance(cx, cz, 344, 30) && lx >= 5 && lx <= 11 && lz >= 5 && lz <= 11 && up <= 4) {
            // The hut at the foot of a mast. A radio. A chair. Someone was listening.
            boolean edge = lx == 5 || lx == 11 || lz == 5 || lz == 11;
            if (up == 4) return d(Blocks.SMOOTH_STONE_SLAB);
            if (edge) return (lz == 5 && lx == 8 && up <= 2) ? AIR : d(Blocks.GRAY_CONCRETE);
            if (lx == 6 && lz == 10 && up == 1) return d(Blocks.JUKEBOX);
            if (lx == 8 && lz == 8 && up == 1) return stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH);
            if (lx == 10 && lz == 6 && up == 1) return papers();
            if (lx == 8 && lz == 8 && up == 3) return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
            return AIR;
        }
        return null;
    }

    // ---------------------------------------------------------------- orchard

    private static BlockState orchard(int x, int y, int z) {
        int cx = x >> 4;
        int cz = z >> 4;
        int lx = x & 15;
        int lz = z & 15;
        int up = y - G;
        if (chance(cx, cz, 353, 33) && lx >= 6 && lx <= 10 && lz >= 6 && lz <= 10) {
            // The dry well. Drop a stone in and you do not hear it land. You hear it land later, behind you.
            boolean ring = lx == 6 || lx == 10 || lz == 6 || lz == 10;
            if (!ring && y > -50 && y <= G) return AIR;
            if (ring && up == 1) return d(Blocks.COBBLESTONE);
            boolean corner = (lx == 6 || lx == 10) && (lz == 6 || lz == 10);
            if (corner && (up == 2 || up == 3)) return d(Blocks.OAK_FENCE);
            if (up == 4) return d(Blocks.SPRUCE_SLAB);
            if (up >= 2 && up <= 3) return AIR;
            return null;
        }
        if (chance(cx, cz, 353, 33) && lx == 11 && lz == 8 && up == 1) return papers();
        if (up < 0 || up > 10) return null;
        if (chance(cx, cz, 351, 50)) return farmhouse(lx, up, lz);
        if (chance(cx, cz, 352, 20)) {
            // The patch. Every pumpkin has the same face cut into it.
            if (up == 0) return (chance(x, z, 354, 3) ? Blocks.ROOTED_DIRT : Blocks.COARSE_DIRT).defaultBlockState();
            if (up == 1 && lx == 8 && lz == 8) return papers();
            if (up == 1 && (lx + lz) % 2 == 0 && chance(x, z, 355, 2)) {
                Direction[] dirs = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
                Block b = chance(x, z, 356, 9) ? Blocks.JACK_O_LANTERN : Blocks.CARVED_PUMPKIN;
                return b.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, dirs[(int) Math.floorMod(mix(x, z, 357), 4L)]);
            }
            return AIR;
        }
        return null;
    }

    /** Grandad's farmhouse: stone below, spruce above, the fire out, the table laid for one. */
    private static BlockState farmhouse(int lx, int up, int lz) {
        if (lx < 3 || lx > 12 || lz < 4 || lz > 11) return AIR;   // the trees are cut back around it
        if (up == 0) return d(Blocks.SPRUCE_PLANKS);
        int tier = up - 4;
        int inset = Math.min(lz - 4, 11 - lz);
        if (tier >= 0) {
            if (tier <= 3 && inset >= tier) {
                if (inset == tier) return stairs(Blocks.SPRUCE_STAIRS, lz < 8 ? Direction.SOUTH : Direction.NORTH);
                return d(Blocks.SPRUCE_PLANKS);
            }
            if (lx == 11 && lz == 9 && up <= 8) return d(Blocks.COBBLESTONE);   // the chimney
            return AIR;
        }
        boolean wall = lx == 3 || lx == 12 || lz == 4 || lz == 11;
        boolean corner = (lx == 3 || lx == 12) && (lz == 4 || lz == 11);
        if (corner) return d(Blocks.STRIPPED_SPRUCE_LOG);
        if (wall) {
            if (lz == 4 && lx == 7 && up <= 2) return AIR;
            if (up == 2 && ((lz == 4 && (lx == 5 || lx == 10)) || ((lx == 3 || lx == 12) && lz == 7))) return d(Blocks.GLASS_PANE);
            return (up == 1 ? Blocks.COBBLESTONE : Blocks.SPRUCE_PLANKS).defaultBlockState();
        }
        if (up == 1) {
            if (lx == 4 && (lz == 9 || lz == 10)) return d(Blocks.WHITE_WOOL);                                // the bed
            if (lx == 9 && lz == 8) return d(Blocks.SPRUCE_PLANKS);                                         // the table
            if (lx == 8 && lz == 8) return stairs(Blocks.SPRUCE_STAIRS, Direction.WEST);
            if (lx == 11 && lz == 10) return Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false);
        }
        if (up == 2 && lx == 9 && lz == 8) return papers();
        if (up == 3 && lx == 7 && lz == 7) return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        return AIR;
    }

    // ------------------------------------------------------------------- ward

    private static BlockState ward(int x, int y, int z) {
        int up = y - G;
        if (up < 1 || up > 5) return null;
        int pz = Math.floorMod(z, 32);
        if (pz >= 12 && pz <= 19) return null;   // the corridor and its walls
        int lx = x & 15;
        if (lx == 0) return null;
        boolean north = pz < 13;
        int depth = north ? 11 - pz : pz - 20;
        if (depth < 0 || depth >= 11) return null;
        int room = Math.floorDiv(z, 32) * 2 + (north ? 0 : 1);
        int rx = x >> 4;
        Direction sit = north ? Direction.SOUTH : Direction.NORTH;   // chairs face the back of the room
        if (chance(rx, room, 361, 7)) return waitingRoom(lx, up, depth, sit);
        if (chance(rx, room, 362, 11)) return chapel(lx, up, depth, sit);
        if (chance(rx, room, 363, 13)) return morgue(lx, up, depth);
        return null;
    }

    /** Rows of chairs facing a television showing a cooking show. A dead plant. Magazines. */
    private static BlockState waitingRoom(int lx, int up, int depth, Direction sit) {
        if (up == 1 && (depth == 4 || depth == 6) && lx >= 2 && lx <= 14 && lx % 2 == 0) return stairs(Blocks.SPRUCE_STAIRS, sit);
        if (depth == 9 && (lx == 7 || lx == 8)) {
            if (up == 1) return d(Blocks.QUARTZ_BLOCK);
            if (up == 2) return d(Blocks.SEA_LANTERN);
        }
        if (depth == 2 && lx == 12 && up == 1) return d(Blocks.SPRUCE_PLANKS);
        if (depth == 2 && lx == 12 && up == 2) return papers();
        if (depth == 9 && lx == 2 && up == 1) return d(Blocks.POTTED_DEAD_BUSH);
        return AIR;
    }

    /** The chapel. Pews, an altar, two candles. The monitor beeps in here too. */
    private static BlockState chapel(int lx, int up, int depth, Direction sit) {
        if (up == 1 && (depth == 3 || depth == 5 || depth == 7) && ((lx >= 2 && lx <= 6) || (lx >= 10 && lx <= 14))) {
            return stairs(Blocks.DARK_OAK_STAIRS, sit);
        }
        if (depth == 9 && lx >= 7 && lx <= 9) {
            if (up == 1) return d(Blocks.QUARTZ_BLOCK);
            if (up == 2 && lx == 8) return papers();
            if (up == 2) return candle();
        }
        if (up == 1 && lx >= 7 && lx <= 9 && depth <= 8) return d(Blocks.RED_CARPET);
        return AIR;
    }

    /** The cold room. Steel drawers in the wall, one table, something on it under a sheet. */
    private static BlockState morgue(int lx, int up, int depth) {
        if (depth >= 9 && lx >= 2 && lx <= 14 && up <= 3) {
            return ((lx + up) % 2 == 0 ? Blocks.IRON_BLOCK : Blocks.SMOOTH_STONE).defaultBlockState();
        }
        if (depth == 5 && (lx == 7 || lx == 8)) {
            if (up == 1) return d(Blocks.IRON_BLOCK);
            if (up == 2) return d(Blocks.WHITE_WOOL);
        }
        if (depth == 3 && lx == 12 && up == 1) return papers();
        if (up == 1 && depth <= 8) return d(Blocks.LIGHT_BLUE_CARPET);
        return AIR;
    }

    // ----------------------------------------------------------------- server

    private static BlockState server(int x, int y, int z) {
        int up = y - G;
        if (up < 1 || up > 7) return null;
        int cx = x >> 4;
        int cz = z >> 4;
        int lx = x & 15;
        int lz = z & 15;
        if (chance(cx, cz, 371, 25)) return noc(x, lx, up, lz);
        if (chance(cx, cz, 372, 19)) return tapes(lx, up, lz);
        if (chance(cx, cz, 373, 70)) return core(lx, up, lz);
        return null;
    }

    /** The operations room: a wall of screens, desks facing it, one screen showing the player list. */
    private static BlockState noc(int x, int lx, int up, int lz) {
        if (lz == 2 && lx >= 1 && lx <= 14 && up <= 6) {
            if (lx % 3 == 0 || up % 3 == 0) return d(Blocks.BLACK_CONCRETE);
            long h = mix(x, up, 374) & 3;
            return (h == 0 ? Blocks.BLACK_STAINED_GLASS : h == 1 ? Blocks.VERDANT_FROGLIGHT : Blocks.SEA_LANTERN).defaultBlockState();
        }
        if ((lz == 6 || lz == 9) && lx >= 2 && lx <= 13) {
            if (up == 1) return d(Blocks.POLISHED_BLACKSTONE);
            if (up == 2 && lx % 3 == 1) return d(Blocks.LIME_STAINED_GLASS);
            if (up == 2 && lx == 5 && lz == 9) return papers();
        }
        if ((lz == 7 || lz == 10) && lx % 3 == 1 && lx >= 2 && lx <= 13 && up == 1) return stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH);
        return AIR;
    }

    /** The tape library. Every tape is the same day. */
    private static BlockState tapes(int lx, int up, int lz) {
        boolean row = (lz == 3 || lz == 4 || lz == 7 || lz == 8 || lz == 11 || lz == 12) && lx >= 2 && lx <= 13;
        if (row && up <= 5) return d(Blocks.BARREL);
        if ((lz == 6 || lz == 10) && up == 7) return d(Blocks.IRON_BARS);   // the picker's rail
        if (lz == 6 && lx == 7 && up == 6) return d(Blocks.IRON_BLOCK);     // the picker, stopped mid-reach
        if (lz == 6 && lx == 9 && up == 1) return papers();
        return AIR;
    }

    /** The core. Warm, like something alive, inside a cage. */
    private static BlockState core(int lx, int up, int lz) {
        double r = Math.sqrt((lx - 7.5D) * (lx - 7.5D) + (lz - 7.5D) * (lz - 7.5D));
        if (r <= 1.5D && up <= 6) return d(Blocks.SHROOMLIGHT);
        if (r <= 2.6D) return d(Blocks.IRON_BARS);
        if (r > 3.2D && r <= 4.2D && up == 1) return d(Blocks.MAGMA_BLOCK);
        if (lx == 8 && lz == 14 && up == 1) return papers();
        return AIR;
    }
}
