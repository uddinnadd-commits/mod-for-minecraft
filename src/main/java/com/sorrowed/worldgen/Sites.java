package com.sorrowed.worldgen;

import com.sorrowed.block.DoorwayBlock;
import com.sorrowed.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Alex's world. The places that stand in the Backup, each holding one doorway, laid out once as
 * a block list and looked up by the generator: a pure function of (x, y, z), so every chunk is
 * the same every time. y here is relative to the ground: 0 is the block you stand on top of.
 *
 * <pre>
 *   the house   (0, 0)      the hallway, in the back wall of his room
 *   the gate    (0, -14)    the way home
 *   the well    (36, -28)   the drowned hall, at the bottom
 *   the oak     (-44, 24)   the hush, inside the hollow trunk
 *   the shed    (34, 36)    the archive, behind the shelves
 *   the stop    (-12, -60)  the street, in the back of the shelter
 *   the mast    (62, 4)     the static, behind the television
 *   the orchard (-58, -18)  the orchard, through the fence
 *   the clinic  (22, 66)    the ward, past the bed
 * </pre>
 */
public final class Sites {
    private Sites() {}

    public static final int GROUND = 64;

    public record Site(String name, int x, int z, int half) {}

    public static final List<Site> SITES = List.of(
            new Site("house", 0, 0, 9),
            new Site("gate", 0, -14, 3),
            new Site("well", 36, -28, 4),
            new Site("oak", -44, 24, 6),
            new Site("shed", 34, 36, 5),
            new Site("stop", -12, -60, 9),
            new Site("mast", 62, 4, 6),
            new Site("orchard", -58, -18, 8),
            new Site("clinic", 22, 66, 6),
            new Site("server", -36, 60, 5),
            new Site("pond", -22, -34, 6),
            new Site("dirthouse", 18, -46, 4),
            new Site("treehouse", -20, 40, 5),
            new Site("campfire", 54, 38, 4));

    private static final Map<Long, BlockState> BLOCKS = new HashMap<>();
    private static boolean built;

    private static long key(int x, int y, int z) {
        return ((long) x & 0xFFFFFFL) << 40 | ((long) y & 0xFFFFL) << 24 | ((long) z & 0xFFFFFFL);
    }

    /** Ground height at (x, z): flat on a site, blending out into the natural meadow around it. */
    public static int ground(int x, int z, int natural) {
        int best = natural;
        for (Site s : SITES) {
            int d = Math.max(Math.abs(x - s.x()), Math.abs(z - s.z())) - s.half();
            if (d <= 0) return GROUND;
            if (d < 8) {
                int blended = (int) Math.round(GROUND + (natural - GROUND) * (d / 8.0D));
                if (Math.abs(blended - GROUND) < Math.abs(best - GROUND)) best = blended;
            }
        }
        return best;
    }

    public static boolean onSite(int x, int z) {
        for (Site s : SITES) {
            if (Math.abs(x - s.x()) <= s.half() && Math.abs(z - s.z()) <= s.half()) return true;
        }
        return false;
    }

    /** The structure block at absolute (x, y, z), or null. */
    public static BlockState at(int x, int y, int z) {
        if (!built) build();
        return BLOCKS.get(key(x, y - GROUND - 1, z));
    }

    // ------------------------------------------------------------------ building

    private static int ox;
    private static int oz;

    private static void set(int x, int y, int z, BlockState s) {
        BLOCKS.put(key(ox + x, y, oz + z), s);
    }

    private static void set(int x, int y, int z, Block b) {
        set(x, y, z, b.defaultBlockState());
    }

    private static void box(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, s);
    }

    private static void box(int x0, int y0, int z0, int x1, int y1, int z1, Block b) {
        box(x0, y0, z0, x1, y1, z1, b.defaultBlockState());
    }

    private static void air(int x0, int y0, int z0, int x1, int y1, int z1) {
        box(x0, y0, z0, x1, y1, z1, Blocks.AIR);
    }

    private static BlockState stairs(Block b, Direction facing) {
        return b.defaultBlockState().setValue(StairBlock.FACING, facing);
    }

    private static BlockState lantern(boolean hanging) {
        return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, hanging);
    }

    /** A two-high doorway in a wall. axisX: the wall runs along x. */
    private static void doorway(int x, int z, Block door, boolean axisX) {
        BlockState s = door.defaultBlockState().setValue(DoorwayBlock.AXIS, axisX ? Direction.Axis.X : Direction.Axis.Z);
        set(x, 0, z, s);
        set(x, 1, z, s);
    }

    private static void frame(int x, int z, boolean axisX, Block post) {
        int dx = axisX ? 1 : 0;
        int dz = axisX ? 0 : 1;
        box(x - dx, 0, z - dz, x - dx, 2, z - dz, post);
        box(x + dx, 0, z + dz, x + dx, 2, z + dz, post);
        set(x, 2, z, post);
    }

    private static synchronized void build() {
        if (built) return;
        for (Site s : SITES) {
            ox = s.x();
            oz = s.z();
            switch (s.name()) {
                case "house": house(); break;
                case "gate": gate(); break;
                case "well": well(); break;
                case "oak": oak(); break;
                case "shed": shed(); break;
                case "stop": stop(); break;
                case "mast": mast(); break;
                case "orchard": orchard(); break;
                case "clinic": clinic(); break;
                case "server": server(); break;
                case "pond": pond(); break;
                case "dirthouse": dirthouse(); break;
                case "treehouse": treehouse(); break;
                case "campfire": campfire(); break;
                default: break;
            }
        }
        built = true;
    }

    /** His house. Oak and spruce, two windows on the front, a bed still made. The hallway starts in the back wall. */
    private static void house() {
        box(-4, -1, -3, 4, -1, 3, Blocks.OAK_PLANKS);                       // floor
        box(-4, 0, -3, 4, 3, 3, Blocks.OAK_PLANKS);                         // walls
        air(-3, 0, -2, 3, 3, 2);
        for (int x : new int[] {-4, 4}) for (int z : new int[] {-3, 3}) box(x, 0, z, x, 3, z, Blocks.SPRUCE_LOG);
        // Roof: a gable along x.
        for (int t = 0; t <= 4; t++) {
            int y = 4 + t;
            for (int z = -4; z <= 4; z++) {
                if (t < 4) {
                    set(-4 + t, y, z, stairs(Blocks.DARK_OAK_STAIRS, Direction.EAST));
                    set(4 - t, y, z, stairs(Blocks.DARK_OAK_STAIRS, Direction.WEST));
                } else {
                    set(0, y, z, Blocks.DARK_OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
                }
            }
            if (t > 0 && t < 4) box(-4 + t, 4, -3, 4 - t, 3 + t, 3, Blocks.OAK_PLANKS);   // gable fill
        }
        air(-3 + 1, 4, -2, 3 - 1, 5, 2);
        // Front wall: door and two windows. Back wall: the hallway.
        set(0, 0, -3, Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(0, 1, -3, Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        set(-2, 1, -3, Blocks.GLASS_PANE); set(2, 1, -3, Blocks.GLASS_PANE);
        set(-4, 1, 0, Blocks.GLASS_PANE); set(4, 1, 0, Blocks.GLASS_PANE);
        frame(0, 3, true, Blocks.DARK_OAK_PLANKS);
        doorway(0, 3, ModBlocks.HALLWAY_DOOR.get(), true);
        // Inside: the bed, the desk with the computer still on, the chest of papers.
        set(-3, 0, 1, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.FOOT));
        set(-3, 0, 2, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.HEAD));
        set(3, 0, 2, Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        set(3, 1, 2, Blocks.BLACK_CONCRETE);                                // the monitor
        set(3, 0, 1, stairs(Blocks.OAK_STAIRS, Direction.EAST));            // the chair
        set(3, 0, -2, Blocks.CHEST);
        set(-3, 0, -2, Blocks.CRAFTING_TABLE);
        set(1, 0, 1, papers());
        set(0, 3, 0, lantern(true));
        box(-2, -1, -2, 2, -1, 2, Blocks.SPRUCE_PLANKS);
        // Outside: the path to the gate, a fence, and the grave he dug himself.
        box(0, -1, -4, 0, -1, -11, Blocks.DIRT_PATH);
        box(-6, 0, -5, 6, 0, -5, Blocks.OAK_FENCE); set(0, 0, -5, Blocks.OAK_FENCE_GATE);
        box(-6, 0, -5, -6, 0, 5, Blocks.OAK_FENCE); box(6, 0, -5, 6, 0, 5, Blocks.OAK_FENCE);
        box(-6, 0, 5, 6, 0, 5, Blocks.OAK_FENCE);
        set(7, 0, 1, ModBlocks.GRAVE_MARKER.get());
        set(7, -1, 1, Blocks.COARSE_DIRT); set(7, -1, 2, Blocks.COARSE_DIRT);
        set(8, 0, 2, Blocks.POPPY);
    }

    /** The way home: a stone arch at the end of the garden path. */
    private static void gate() {
        box(-2, -1, -1, 2, -1, 1, Blocks.MOSSY_COBBLESTONE);
        frame(0, 0, true, Blocks.STONE_BRICKS);
        set(-1, 3, 0, Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
        set(1, 3, 0, Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
        set(0, 3, 0, Blocks.CHISELED_STONE_BRICKS);
        set(0, 4, 0, lantern(false));
        doorway(0, 0, ModBlocks.BACKUP_DOOR.get(), true);
    }

    /** The well. The bucket is gone. The ladder goes down further than the water should allow. */
    private static void well() {
        box(-1, -1, -1, 1, 0, 1, Blocks.COBBLESTONE);
        air(0, -1, 0, 0, 0, 0);
        for (int x : new int[] {-1, 1}) for (int z : new int[] {-1, 1}) box(x, 1, z, x, 2, z, Blocks.SPRUCE_FENCE);
        box(-2, 3, -2, 2, 3, 2, Blocks.SPRUCE_SLAB);
        set(0, 2, 0, lantern(true));
        // The shaft, the ladder, and the chamber at the bottom.
        box(-1, -11, -1, 1, -2, 1, Blocks.STONE_BRICKS);
        air(0, -11, 0, 0, -1, 0);
        for (int y = -11; y <= -1; y++) set(0, y, 0, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        box(-2, -12, 1, 2, -8, 5, Blocks.STONE_BRICKS);
        air(-1, -11, 1, 1, -9, 4);
        box(-1, -11, 1, 1, -11, 4, Blocks.WATER);
        set(0, -1, 0, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        set(0, -9, 3, lantern(true));
        box(-1, -10, 4, 1, -9, 4, Blocks.DARK_PRISMARINE);
        // The doorway sits in the far wall of the chamber; y is -10 and -9 there, so place it by hand.
        BlockState d = ModBlocks.DROWNED_DOOR.get().defaultBlockState().setValue(DoorwayBlock.AXIS, Direction.Axis.X);
        set(0, -10, 4, d); set(0, -9, 4, d);
    }

    /** A dead oak, bigger than any oak should be. Hollow. Something is kept inside it. */
    private static void oak() {
        box(-1, 0, -1, 1, 13, 1, Blocks.DARK_OAK_LOG);
        box(-2, -1, -2, 2, -1, 2, Blocks.PODZOL);
        air(0, 0, 0, 0, 2, 0);
        air(0, 0, 1, 0, 1, 1);                                              // the way in, from the south
        BlockState branchX = Blocks.DARK_OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.X);
        BlockState branchZ = Blocks.DARK_OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.Z);
        box(2, 10, 0, 5, 10, 0, branchX); box(-5, 12, 0, -2, 12, 0, branchX);
        box(0, 11, 2, 0, 11, 5, branchZ); box(0, 13, -5, 0, 13, -2, branchZ);
        set(5, 11, 0, Blocks.DARK_OAK_LOG); set(-5, 13, 0, Blocks.DARK_OAK_LOG);
        set(0, 12, 5, Blocks.DARK_OAK_LOG); set(0, 14, -5, Blocks.DARK_OAK_LOG);
        for (int i = -3; i <= 3; i += 2) { set(i, 0, -3, Blocks.DEAD_BUSH); set(3, 0, i, Blocks.DEAD_BUSH); }
        set(0, 2, 0, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        doorway(0, -1, ModBlocks.HUSH_DOOR.get(), true);
    }

    /** The shed where he kept everything he wrote down. The archive starts behind the shelves. */
    private static void shed() {
        box(-3, -1, -3, 3, -1, 3, Blocks.SPRUCE_PLANKS);
        box(-3, 0, -3, 3, 2, 3, Blocks.SPRUCE_PLANKS);
        air(-2, 0, -2, 2, 2, 2);
        air(0, 0, 3, 0, 1, 3);                                              // open doorway, south
        box(-4, 3, -4, 4, 3, 4, Blocks.SPRUCE_SLAB);
        box(-2, 0, -2, 2, 2, -2, Blocks.BOOKSHELF);
        box(-2, 0, -1, -2, 2, 2, Blocks.BOOKSHELF);
        box(2, 0, -1, 2, 2, 2, Blocks.BOOKSHELF);
        set(0, 2, 0, lantern(true));
        set(0, 0, 0, Blocks.LECTERN);
        set(1, 0, 1, papers());
        doorway(0, -2, ModBlocks.ARCHIVE_DOOR.get(), true);
    }

    /** The bus stop at the end of the lane. The bus has always been coming. */
    private static void stop() {
        box(-12, -1, -2, 12, -1, 2, Blocks.GRAY_CONCRETE);                   // a stub of road
        for (int x = -12; x <= 12; x += 4) set(x, -1, 0, Blocks.LIGHT_GRAY_CONCRETE);
        box(-12, -1, 3, 12, -1, 3, Blocks.SMOOTH_STONE);                     // the kerb
        box(-2, 0, 4, 2, 0, 4, stairs(Blocks.OAK_STAIRS, Direction.NORTH));  // the bench
        box(-3, 0, 5, 3, 2, 5, Blocks.GLASS_PANE);                           // the back
        box(-3, 0, 4, -3, 2, 5, Blocks.GLASS_PANE); box(3, 0, 4, 3, 2, 5, Blocks.GLASS_PANE);
        box(-3, 3, 3, 3, 3, 5, Blocks.SMOOTH_STONE_SLAB);
        set(0, 2, 4, lantern(true));
        set(4, 0, 3, Blocks.OAK_FENCE); set(4, 1, 3, Blocks.OAK_SIGN);
        set(-1, 1, 4, papers());
        doorway(0, 5, ModBlocks.STREET_DOOR.get(), true);
    }

    /** The mast on the hill and the shed with the television. The static starts behind the screen. */
    private static void mast() {
        box(-4, 0, 0, -4, 23, 0, Blocks.IRON_BARS);
        box(-5, -1, -1, -3, -1, 1, Blocks.STONE);
        set(-4, 24, 0, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true));
        set(-4, 23, 0, Blocks.REDSTONE_BLOCK);
        box(-1, -1, -3, 3, -1, 3, Blocks.GRAY_CONCRETE);
        box(-1, 0, -3, 3, 2, 3, Blocks.GRAY_CONCRETE);
        air(0, 0, -2, 2, 2, 2);
        air(1, 0, 3, 1, 1, 3);
        box(-2, 3, -4, 4, 3, 4, Blocks.SMOOTH_STONE_SLAB);
        set(3, 0, 0, Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        set(3, 1, 0, Blocks.BLACK_CONCRETE);                                 // the set, on, showing snow
        set(1, 2, 0, lantern(true));
        set(0, 0, 0, stairs(Blocks.SPRUCE_STAIRS, Direction.WEST));
        doorway(1, -3, ModBlocks.STATIC_DOOR.get(), true);
    }

    /** Two dead trees and a fence with a gap. The orchard was his grandfather's. It is not any more. */
    private static void orchard() {
        box(-7, -1, -3, 7, -1, 3, Blocks.PODZOL);
        box(-7, 0, 0, 7, 0, 0, Blocks.BIRCH_FENCE);
        air(0, 0, 0, 0, 0, 0);
        for (int x : new int[] {-5, 5}) {
            box(x, 0, -2, x, 5, -2, Blocks.STRIPPED_SPRUCE_LOG);
            set(x + 1, 4, -2, Blocks.STRIPPED_SPRUCE_LOG); set(x - 1, 5, -2, Blocks.STRIPPED_SPRUCE_LOG);
            set(x, 6, -2, Blocks.STRIPPED_SPRUCE_LOG);
            set(x + 1, 0, -1, Blocks.COBBLESTONE);                           // an apple, gone to stone
        }
        frame(0, 0, true, Blocks.STRIPPED_SPRUCE_LOG);
        set(0, 3, 0, lantern(false));
        doorway(0, 0, ModBlocks.ORCHARD_DOOR.get(), true);
    }

    /** A concrete box with a vent humming on the roof, half sunk in the grass. The server room is inside. */
    private static void server() {
        box(-3, -2, -3, 3, -1, 3, Blocks.POLISHED_DEEPSLATE);
        box(-3, 0, -3, 3, 2, 3, Blocks.GRAY_CONCRETE);
        air(-2, 0, -2, 2, 2, 2);
        air(0, 0, -3, 0, 1, -3);
        box(-4, 3, -4, 4, 3, 4, Blocks.SMOOTH_STONE_SLAB);
        set(2, 4, 2, Blocks.IRON_BARS); set(2, 5, 2, Blocks.IRON_TRAPDOOR);           // the vent
        set(-2, 0, 1, Blocks.POLISHED_BLACKSTONE_BRICKS); set(-2, 1, 1, Blocks.VERDANT_FROGLIGHT);
        set(2, 0, 1, Blocks.POLISHED_BLACKSTONE_BRICKS); set(2, 1, 1, Blocks.POLISHED_BLACKSTONE_BRICKS);
        set(0, 2, 0, Blocks.SEA_LANTERN);
        frame(0, 3, true, Blocks.IRON_BLOCK);
        doorway(0, 3, ModBlocks.SERVER_DOOR.get(), true);
    }

    private static BlockState papers() {
        return ModBlocks.LOOSE_PAPERS.get().defaultBlockState();
    }

    /** The pond where you used to fish. The float does not bob. */
    private static void pond() {
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                double r = Math.sqrt(x * x + z * z);
                if (r <= 4.6D) {
                    set(x, -1, z, Blocks.WATER);
                    set(x, -2, z, r <= 3.0D ? Blocks.WATER : Blocks.CLAY);
                    set(x, -3, z, Blocks.CLAY);
                    if (r <= 3.5D && (x * 7 + z * 3) % 9 == 0) set(x, 0, z, Blocks.LILY_PAD);
                } else if (r <= 5.6D) {
                    set(x, -1, z, (x + z) % 3 == 0 ? Blocks.SAND : Blocks.GRASS_BLOCK);
                    if ((x * 3 + z) % 4 == 0) set(x, 0, z, Blocks.TALL_GRASS);
                }
            }
        }
        // The dock: planks out over the water, two posts, and a rod left leaning.
        box(-6, -1, 0, -2, -1, 1, Blocks.OAK_PLANKS);
        set(-6, 0, -1, Blocks.OAK_FENCE); set(-6, 0, 2, Blocks.OAK_FENCE);
        set(-2, 0, 1, Blocks.OAK_FENCE);
        set(-3, 0, 0, papers());
        set(-4, 0, 1, Blocks.BARREL);
    }

    /** The first night. The dirt house you dug together, rebuilt exactly. There was never a door. */
    private static void dirthouse() {
        box(-2, 0, -2, 2, 2, 2, Blocks.DIRT);
        air(-1, 0, -1, 1, 1, 1);
        box(-2, 3, -2, 2, 3, 2, Blocks.DIRT);
        air(0, 0, -2, 0, 1, -2);
        set(1, 0, 1, Blocks.CRAFTING_TABLE);
        set(-1, 0, 1, Blocks.TORCH);
        set(-1, 0, -1, papers());
    }

    /** The treehouse: you can see every door in the backup from up here. */
    private static void treehouse() {
        box(0, 0, 0, 0, 8, 0, Blocks.OAK_LOG);
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = 8; y <= 10; y++) {
                    int r = Math.abs(x) + Math.abs(z) + (y - 8);
                    if (r <= 4 && !(x == 0 && z == 0 && y == 8)) {
                        set(x, y, z, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
                    }
                }
            }
        }
        box(-2, 5, -2, 2, 5, 2, Blocks.OAK_PLANKS);
        set(0, 5, 0, Blocks.OAK_LOG);
        for (int i = -2; i <= 2; i++) {
            set(i, 6, -2, Blocks.OAK_FENCE); set(i, 6, 2, Blocks.OAK_FENCE);
            set(-2, 6, i, Blocks.OAK_FENCE); set(2, 6, i, Blocks.OAK_FENCE);
        }
        air(0, 6, 2, 0, 6, 2);
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
        for (int y = 0; y <= 5; y++) set(0, y, 1, ladder);
        set(1, 6, -1, papers());
        set(-1, 6, -1, lantern(false));
    }

    /** The campfire where you sat when the server was new. It does not go out. */
    private static void campfire() {
        set(0, 0, 0, Blocks.CAMPFIRE);
        box(-1, -1, -1, 1, -1, 1, Blocks.COBBLESTONE);
        BlockState alongZ = Blocks.STRIPPED_OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.Z);
        BlockState alongX = Blocks.STRIPPED_OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.X);
        box(-3, 0, -1, -3, 0, 1, alongZ);
        box(3, 0, -1, 3, 0, 1, alongZ);
        box(-1, 0, 3, 1, 0, 3, alongX);
        set(0, 0, -3, papers());
    }

    /** A small white building with a red cross. He went in. He did not come out. */
    private static void clinic() {
        box(-4, -1, -3, 4, -1, 3, Blocks.WHITE_CONCRETE);
        box(-4, 0, -3, 4, 3, 3, Blocks.WHITE_CONCRETE);
        air(-3, 0, -2, 3, 3, 2);
        air(0, 0, -3, 0, 1, -3);
        box(-5, 4, -4, 5, 4, 4, Blocks.SMOOTH_STONE_SLAB);
        set(0, 2, -3, Blocks.RED_CONCRETE); set(-1, 2, -3, Blocks.RED_CONCRETE); set(1, 2, -3, Blocks.RED_CONCRETE);
        set(0, 3, -3, Blocks.RED_CONCRETE);
        set(-2, 1, -3, Blocks.IRON_BARS); set(2, 1, -3, Blocks.IRON_BARS);
        set(0, 3, 0, Blocks.SEA_LANTERN);
        set(-2, 0, 1, Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.FOOT));
        set(-2, 0, 2, Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.HEAD));
        box(-3, 0, 0, -3, 2, 2, Blocks.WHITE_WOOL);                          // the curtain
        set(2, 0, 2, Blocks.IRON_TRAPDOOR);                                  // a tray
        set(2, 0, 1, Blocks.FLOWER_POT);
        frame(0, 3, true, Blocks.QUARTZ_PILLAR);
        doorway(0, 3, ModBlocks.WARD_DOOR.get(), true);
    }
}
