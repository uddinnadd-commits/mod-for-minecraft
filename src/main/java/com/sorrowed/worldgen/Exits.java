package com.sorrowed.worldgen;

import com.sorrowed.block.DoorwayBlock;
import com.sorrowed.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The ways out. Every world except Alex's has one exit in each 96 by 96 region, so wherever you
 * wander there is a way back within a minute's walk. An exit is that world's own doorway in a
 * frame built from that world's materials, with a light on top; stepping through it from inside
 * the world takes you back to where you came in. The client plays a low hum from the nearest one.
 *
 * Pure functions of (mode, x, z): the generator places them and the client finds them the same way.
 */
public final class Exits {
    private Exits() {}

    public static final int REGION = 96;
    private static final int RADIUS = 3;

    public record Spot(int x, int z, int ground, boolean alongZ) {}

    private static long mix(long a, long b, long c) {
        long h = a * 0x9E3779B97F4A7C15L ^ b * 0xC2B2AE3D27D4EB4FL ^ c * 0x165667B19E3779F9L;
        h ^= h >>> 32;
        h *= 0xD6E8FEB86659FD93L;
        h ^= h >>> 32;
        return h;
    }

    private static int ground(String mode) {
        return "drowned".equals(mode) ? LiminalGenerator.DROWN_SEA + 1 : 64;
    }

    /** The exit in region (rx, rz) for this mode, or null if the region holds none. */
    public static Spot spot(String mode, int rx, int rz) {
        if (mode == null || "backup".equals(mode)) return null;
        long h = mix(rx, rz, 101L + mode.hashCode());
        int x = rx * REGION + 20 + (int) (h & 55);
        int z = rz * REGION + 20 + (int) ((h >>> 8) & 55);
        boolean alongZ = false;
        if ("ward".equals(mode)) {
            z = Math.floorDiv(z, 32) * 32 + 15;   // in a corridor; the door faces down it
            alongZ = true;
        }
        // Leave the arrival area alone: the gate you came in by, and what is built around it.
        if (Math.abs(x) < 44 && Math.abs(z) < 44) return null;
        return new Spot(x, z, ground(mode), alongZ);
    }

    /** The exit whose footprint covers (x, z), or null. */
    public static Spot at(String mode, int x, int z) {
        Spot s = spot(mode, Math.floorDiv(x, REGION), Math.floorDiv(z, REGION));
        if (s == null) return null;
        if (Math.abs(x - s.x()) > RADIUS || Math.abs(z - s.z()) > RADIUS) return null;
        return s;
    }

    /** The nearest exit to (x, z), searching the surrounding regions. */
    public static BlockPos nearest(String mode, double x, double z) {
        int rx = Math.floorDiv((int) Math.floor(x), REGION);
        int rz = Math.floorDiv((int) Math.floor(z), REGION);
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Spot s = spot(mode, rx + dx, rz + dz);
                if (s == null) continue;
                double d = (s.x() - x) * (s.x() - x) + (s.z() - z) * (s.z() - z);
                if (d < bestD) {
                    bestD = d;
                    best = new BlockPos(s.x(), s.ground() + 1, s.z());
                }
            }
        }
        return best;
    }

    /** Highest y an exit at (x, z) can put a block at, or Integer.MIN_VALUE. */
    public static int top(String mode, int x, int z) {
        Spot s = at(mode, x, z);
        return s == null ? Integer.MIN_VALUE : s.ground() + 6;
    }

    private static Block door(String mode) {
        switch (mode) {
            case "hallway": return ModBlocks.HALLWAY_DOOR.get();
            case "drowned": return ModBlocks.DROWNED_DOOR.get();
            case "archive": return ModBlocks.ARCHIVE_DOOR.get();
            case "street": return ModBlocks.STREET_DOOR.get();
            case "static": return ModBlocks.STATIC_DOOR.get();
            case "orchard": return ModBlocks.ORCHARD_DOOR.get();
            case "ward": return ModBlocks.WARD_DOOR.get();
            case "server": return ModBlocks.SERVER_DOOR.get();
            default: return ModBlocks.BACKUP_DOOR.get();
        }
    }

    private record Theme(BlockState base, BlockState post, BlockState lintel, BlockState lintelMid, BlockState light) {}

    private static BlockState d(Block b) {
        return b.defaultBlockState();
    }

    private static Theme theme(String mode) {
        switch (mode) {
            case "hallway": // a door frame in the carpet, a red sign over it that says nothing
                return new Theme(null, d(Blocks.DARK_OAK_PLANKS), d(Blocks.RED_CONCRETE), d(Blocks.RED_CONCRETE), d(Blocks.GLOWSTONE));
            case "archive": // shelves standing apart from the others, a candle on top
                return new Theme(null, d(Blocks.BOOKSHELF), d(Blocks.DARK_OAK_LOG), d(Blocks.CHISELED_BOOKSHELF),
                        Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true));
            case "drowned": // a pillar out of the sea with a light on it
                return new Theme(d(Blocks.MOSSY_STONE_BRICKS), d(Blocks.PRISMARINE_BRICKS), d(Blocks.DARK_PRISMARINE),
                        d(Blocks.DARK_PRISMARINE), d(Blocks.SEA_LANTERN));
            case "street": // a red phone box with the light on
                return new Theme(d(Blocks.SMOOTH_STONE), d(Blocks.RED_CONCRETE), d(Blocks.RED_CONCRETE), d(Blocks.RED_CONCRETE), d(Blocks.SHROOMLIGHT));
            case "static": // a black frame on a white square, a red light on a powered lintel
                return new Theme(d(Blocks.WHITE_CONCRETE), d(Blocks.BLACK_CONCRETE), d(Blocks.WHITE_CONCRETE), d(Blocks.REDSTONE_BLOCK),
                        Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true));
            case "orchard": // a gate in a fence that is not there any more
                return new Theme(d(Blocks.COARSE_DIRT), d(Blocks.STRIPPED_SPRUCE_LOG), d(Blocks.SPRUCE_PLANKS), d(Blocks.SPRUCE_PLANKS),
                        Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
            case "ward": // the fire door, under the green light
                return new Theme(null, d(Blocks.QUARTZ_PILLAR), d(Blocks.LIME_CONCRETE), d(Blocks.LIME_CONCRETE), d(Blocks.GLOWSTONE));
            case "server": // a steel frame, a green light
                return new Theme(d(Blocks.POLISHED_DEEPSLATE), d(Blocks.IRON_BLOCK), d(Blocks.GREEN_CONCRETE), d(Blocks.GREEN_CONCRETE),
                        d(Blocks.VERDANT_FROGLIGHT));
            default:
                return new Theme(null, d(Blocks.STONE_BRICKS), d(Blocks.STONE_BRICKS), d(Blocks.CHISELED_STONE_BRICKS), d(Blocks.LANTERN));
        }
    }

    /**
     * The exit's block at (x, y, z). Null means "not part of an exit, generate as usual"; AIR means
     * "keep this clear".
     */
    public static BlockState state(String mode, int x, int y, int z) {
        Spot s = at(mode, x, z);
        if (s == null) return null;
        int g = s.ground();
        int up = y - g;
        int along = s.alongZ() ? z - s.z() : x - s.x();   // across the doorway
        int across = s.alongZ() ? x - s.x() : z - s.z();  // through it
        Theme t = theme(mode);

        if ("drowned".equals(mode)) {
            // The pillar: from the seabed up to the platform, radius two.
            if (up <= 0 && Math.abs(along) <= 2 && Math.abs(across) <= 2) {
                if (y <= LiminalGenerator.MIN_Y) return null;
                return up == 0 ? t.base() : d(Blocks.STONE_BRICKS);
            }
            if (up <= 0) return null;
        } else if (up < 0) {
            return null;
        } else if (up == 0) {
            if (t.base() == null || Math.abs(along) > 2 || Math.abs(across) > 2) return null;
            return t.base();
        }
        if (up > 5) return null;

        if (across == 0) {
            if (along == 0 && (up == 1 || up == 2)) {
                return door(mode).defaultBlockState().setValue(DoorwayBlock.AXIS, s.alongZ() ? Direction.Axis.Z : Direction.Axis.X);
            }
            if (Math.abs(along) == 1 && up <= 2) return t.post();
            if (Math.abs(along) <= 1 && up == 3) return along == 0 ? t.lintelMid() : t.lintel();
            if (along == 0 && up == 4) return t.light();
        }
        // Clear the space around the frame so it can be seen and walked through.
        return Blocks.AIR.defaultBlockState();
    }
}
