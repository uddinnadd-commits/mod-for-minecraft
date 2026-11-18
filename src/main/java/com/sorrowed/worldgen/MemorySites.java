package com.sorrowed.worldgen;

import com.sorrowed.SorrowedData;
import com.sorrowed.block.LighthouseLampBlock;
import com.sorrowed.block.ScreenBlock;
import com.sorrowed.registry.Dimensions;
import com.sorrowed.registry.ModBlocks;
import com.sorrowed.story.Finale;
import com.sorrowed.story.Objectives;
import com.sorrowed.story.Progress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/**
 * The one thing to find in each world, built once near where you arrive, and the places the
 * objectives point at. Each world's piece is built the first time anyone arrives (or logs in
 * there) and its key position is remembered in {@link SorrowedData}.
 *
 * <pre>
 *   hallway  a desk in a nearby room, the chat still open on the screen
 *   archive  a book open on a stand, down the aisle behind you
 *   street   his house: light on, door open, the switch by the door
 *   static   a television on a stand under a mast, ahead of you in the noise
 *   orchard  the one tree still alive, his laptop under it
 *   ward     room four, off the corridor: the bed by the window, the lamp that never worked
 *   server   a console along the cross aisle
 *   drowned  the lamp at the top of the lighthouse (the lighthouse itself is HubBuilder's)
 *   hush     the standing stones (also HubBuilder's); there are nine, eventually
 * </pre>
 */
public final class MemorySites {
    private MemorySites() {}

    /** The four places in the Backup, in the order of the objective's parts. y is where you stand. */
    public static final BlockPos[] PLACES = {
            new BlockPos(-22, Sites.GROUND + 1, -34),   // the pond
            new BlockPos(18, Sites.GROUND + 1, -46),    // the dirt house
            new BlockPos(-20, Sites.GROUND + 7, 40),    // the treehouse platform
            new BlockPos(54, Sites.GROUND + 1, 38),     // the campfire
    };
    public static final String[] PLACE_NAMES = {"the pond", "the dirt house", "the treehouse", "the campfire"};

    /** The stones in the Hush, relative to the soul fire in the middle. The ninth is not there at first. */
    public static final int[][] STONES = {{6, 0}, {4, 4}, {0, 6}, {-4, 4}, {-6, 0}, {-4, -4}, {0, -6}, {4, -4}};
    public static final int[] NINTH = {-6, -2};

    private static final String STONES_KEY = "stones:the_hush:-22:14";
    private static final String LIGHTHOUSE_KEY = "lighthouse:the_drowned:-26:-18";

    /** The lamp in the ceiling of room four, in the ward. */
    public static final BlockPos WARD_LAMP = new BlockPos(24, LiminalGenerator.WARD_CEIL, 25);

    private static String targetKey(String world) {
        return "target:" + world;
    }

    // ------------------------------------------------------------------ lookups

    public static BlockPos target(ServerLevel level, String world) {
        return SorrowedData.get(level).getPos(targetKey(world));
    }

    /** The soul fire in the middle of the Hush's standing stones, or null if they have not been built. */
    public static BlockPos stonesCenter(ServerLevel level) {
        Integer y = SorrowedData.get(level).getY(STONES_KEY);
        return y == null ? null : new BlockPos(-22, y, 14);
    }

    public static BlockPos lamp(ServerLevel level) {
        Integer y = SorrowedData.get(level).getY(LIGHTHOUSE_KEY);
        return y == null ? null : new BlockPos(-26, y + 21, -18);
    }

    /** His grave in the overworld: remembered when it was placed, or looked for once around spawn. */
    public static BlockPos grave(ServerLevel overworld) {
        SorrowedData data = SorrowedData.get(overworld);
        BlockPos known = data.getPos("grave");
        if (known != null || data.isBuilt("grave_search")) return known;
        BlockPos spawn = overworld.getSharedSpawnPos();
        Block marker = ModBlocks.GRAVE_MARKER.get();
        BlockPos found = null;
        search:
        for (int r = 0; r <= 40; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int x = spawn.getX() + dx;
                    int z = spawn.getZ() + dz;
                    int top = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    for (int y = top - 2; y <= top + 1; y++) {
                        BlockPos p = new BlockPos(x, y, z);
                        if (overworld.getBlockState(p).is(marker)) {
                            found = p;
                            break search;
                        }
                    }
                }
            }
        }
        data.setBuilt("grave_search");
        if (found != null) data.setPos("grave", found);
        return found;
    }

    /** What the player should be walking toward in the world they are in. Empty when there is nothing. */
    public static List<BlockPos> targets(ServerPlayer p) {
        List<BlockPos> out = new ArrayList<>();
        ServerLevel level = p.serverLevel();
        int stage = Progress.stage(p);
        if (stage == Progress.ENDED) return out;
        if (stage == Progress.READY) {
            if (level.dimension().equals(Dimensions.BACKUP)) out.add(BlockPos.containing(Finale.SEAT));
            return out;
        }
        Objectives.Objective o = Objectives.in(level.dimension());
        if (o == null || Progress.done(p, o.id())) return out;
        int mask = Progress.steps(p, o.id());
        switch (o.id()) {
            case "overworld" -> {
                BlockPos g = grave(level);
                if (g != null) out.add(g);
            }
            case "backup" -> {
                for (int i = 0; i < PLACES.length; i++) {
                    if ((mask & (1 << i)) == 0) out.add(PLACES[i]);
                }
            }
            case "hush" -> {
                BlockPos c = stonesCenter(level);
                if (c == null) break;
                boolean all = true;
                for (int i = 0; i < STONES.length; i++) {
                    if ((mask & (1 << i)) == 0) {
                        all = false;
                        out.add(c.offset(STONES[i][0], 6, STONES[i][1]));
                    }
                }
                if (all) out.add(c.offset(NINTH[0], 6, NINTH[1]));
            }
            case "drowned" -> {
                BlockPos l = lamp(level);
                if (l != null) out.add(l);
            }
            default -> {
                BlockPos t = target(level, o.id());
                if (t != null) out.add(t);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ building

    /** Builds this world's piece if it has one and it is not there yet. Cheap after the first time. */
    public static void ensure(ServerLevel level) {
        Objectives.Objective o = Objectives.in(level.dimension());
        if (o == null) return;
        String world = o.id();
        SorrowedData data = SorrowedData.get(level);
        String key = "memory:" + world;
        if (data.isBuilt(key)) return;
        BlockPos target = switch (world) {
            case "hallway" -> hallway(level);
            case "archive" -> archive(level);
            case "street" -> street(level);
            case "static" -> statik(level);
            case "orchard" -> orchard(level);
            case "ward" -> ward(level);
            case "server" -> server(level);
            case "drowned" -> drowned(level);
            default -> null;
        };
        if (target != null) {
            data.setPos(targetKey(world), target);
            data.setBuilt(key);
        }
    }

    private static void set(Level level, BlockPos pos, BlockState state) {
        BuildUtil.set(level, pos, state);
    }

    private static void set(Level level, BlockPos pos, Block block) {
        BuildUtil.set(level, pos, block.defaultBlockState());
    }

    private static void load(Level level, BlockPos pos) {
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    /** Everything in the box that is not air becomes air. */
    private static void clear(Level level, int x0, int y0, int z0, int x1, int y1, int z1) {
        for (int x = x0; x <= x1; x++)
            for (int y = y0; y <= y1; y++)
                for (int z = z0; z <= z1; z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (!level.getBlockState(p).isAir()) set(level, p, Blocks.AIR.defaultBlockState());
                }
    }

    private static void floor(Level level, int x0, int y, int z0, int x1, int z1, BlockState s) {
        for (int x = x0; x <= x1; x++)
            for (int z = z0; z <= z1; z++) set(level, new BlockPos(x, y, z), s);
    }

    private static BlockState screen(Block b, Direction facing) {
        return b.defaultBlockState().setValue(ScreenBlock.FACING, facing);
    }

    private static BlockState stairs(Block b, Direction facing) {
        return b.defaultBlockState().setValue(StairBlock.FACING, facing);
    }

    private static BlockState topSlab(Block b) {
        return b.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
    }

    private static BlockState candles(int n) {
        return Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, n).setValue(CandleBlock.LIT, true);
    }

    /** The hallway: the first ordinary room to the east or south, a desk in the middle of it, the chat still open. */
    private static BlockPos hallway(ServerLevel level) {
        int[][] rooms = {{2, 0}, {2, 1}, {1, 2}, {2, -1}, {-2, 1}, {0, 2}, {3, 0}};
        int[] room = rooms[0];
        for (int[] r : rooms) {
            if (LiminalGenerator.ordinaryHallwayRoom(r[0], r[1])) {
                room = r;
                break;
            }
        }
        int x = room[0] * 16 + 8;
        int z = room[1] * 16 + 8;
        int f = LiminalGenerator.HALL_FLOOR;
        load(level, new BlockPos(x, f, z));
        clear(level, x - 3, f + 1, z - 3, x + 3, LiminalGenerator.HALL_CEIL - 1, z + 3);
        floor(level, x - 3, f, z - 3, x + 3, z + 3, Blocks.BROWN_TERRACOTTA.defaultBlockState());
        floor(level, x - 1, f + 1, z - 1, x + 2, z + 2, Blocks.BROWN_CARPET.defaultBlockState());
        set(level, new BlockPos(x, f + 1, z), topSlab(Blocks.SPRUCE_SLAB));
        set(level, new BlockPos(x + 1, f + 1, z), topSlab(Blocks.SPRUCE_SLAB));
        set(level, new BlockPos(x - 1, f + 1, z), Blocks.SPRUCE_FENCE);
        set(level, new BlockPos(x + 2, f + 1, z), Blocks.SPRUCE_FENCE);
        BlockPos screen = new BlockPos(x, f + 2, z);
        set(level, screen, screen(ModBlocks.CHAT_SCREEN.get(), Direction.SOUTH));
        set(level, new BlockPos(x + 1, f + 2, z), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
        set(level, new BlockPos(x, f + 1, z + 1), stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH));   // the chair, pushed out
        set(level, new BlockPos(x - 2, f + 1, z + 2), ModBlocks.LOOSE_PAPERS.get());
        return screen;
    }

    /** The archive: down the aisle behind you, a lectern with a book open on the blank page. */
    private static BlockPos archive(ServerLevel level) {
        int f = LiminalGenerator.ARCHIVE_FLOOR;
        int x = 0;
        int z = 24;
        load(level, new BlockPos(x, f, z));
        clear(level, x - 2, f + 1, z - 3, x + 2, LiminalGenerator.ARCHIVE_CEIL - 1, z + 3);
        floor(level, x - 2, f, z - 3, x + 2, z + 3, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        floor(level, x - 1, f, z - 2, x + 1, z + 2, Blocks.SPRUCE_PLANKS.defaultBlockState());
        BlockPos lectern = new BlockPos(x, f + 1, z);
        set(level, lectern, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.NORTH)
                .setValue(LecternBlock.HAS_BOOK, true));
        if (level.getBlockEntity(lectern) instanceof LecternBlockEntity be) {
            be.setBook(new ItemStack(Items.WRITABLE_BOOK));
            be.setChanged();
        }
        set(level, new BlockPos(x, f + 1, z + 1), stairs(Blocks.DARK_OAK_STAIRS, Direction.SOUTH));   // the reader's chair, empty
        set(level, new BlockPos(x - 2, f + 1, z), candles(3));
        set(level, new BlockPos(x + 2, f + 1, z), candles(2));
        set(level, new BlockPos(x - 2, f + 1, z + 3), Blocks.BOOKSHELF);
        set(level, new BlockPos(x + 2, f + 1, z + 3), Blocks.BOOKSHELF);
        set(level, new BlockPos(x - 2, f + 2, z + 3), candles(1));
        set(level, new BlockPos(x, LiminalGenerator.ARCHIVE_CEIL - 1, z),
                Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        return lectern;
    }

    /**
     * The street: the nearest house across the road from where you arrive becomes his. The light is
     * on, the door is open, and the switch is on the wall just inside.
     */
    private static BlockPos street(ServerLevel level) {
        int[] lots = {1, -1, 2, -2, 3};
        int cx = lots[0];
        for (int c : lots) {
            if (LiminalGenerator.streetLotHasHouse(c, -1)) {
                cx = c;
                break;
            }
        }
        int g = LiminalGenerator.STREET_GROUND;
        int door = cx * 16 + 7;
        int front = -6;    // the front wall, facing the road you arrive on
        int back = -14;
        load(level, new BlockPos(door, g, front));
        boolean house = LiminalGenerator.streetLotHasHouse(cx, -1);
        if (!house) {
            // No house on any of the lots: put a plain one up.
            for (int x = cx * 16 + 3; x <= cx * 16 + 12; x++) {
                for (int z = back; z <= front; z++) {
                    set(level, new BlockPos(x, g, z), Blocks.OAK_PLANKS);
                    boolean wall = x == cx * 16 + 3 || x == cx * 16 + 12 || z == back || z == front;
                    for (int y = g + 1; y <= g + 3; y++) {
                        set(level, new BlockPos(x, y, z), wall ? Blocks.WHITE_CONCRETE.defaultBlockState() : Blocks.AIR.defaultBlockState());
                    }
                    set(level, new BlockPos(x, g + 4, z), Blocks.OAK_PLANKS);
                }
            }
        }
        // The door, open.
        BlockState d = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.OPEN, true);
        set(level, new BlockPos(door, g + 1, front), d.setValue(DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER));
        set(level, new BlockPos(door, g + 2, front), d.setValue(DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        // The light, on.
        set(level, new BlockPos(door, g + 4, -10), Blocks.GLOWSTONE);
        set(level, new BlockPos(door - 3, g + 4, -8), Blocks.GLOWSTONE);
        // His room, more or less as he left it.
        clear(level, door - 3, g + 1, back + 1, door + 4, g + 3, front - 1);
        set(level, new BlockPos(door - 3, g + 1, back + 2), Blocks.RED_BED.defaultBlockState()
                .setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.FOOT));
        set(level, new BlockPos(door - 3, g + 1, back + 1), Blocks.RED_BED.defaultBlockState()
                .setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.HEAD));
        set(level, new BlockPos(door + 3, g + 1, back + 1), topSlab(Blocks.SPRUCE_SLAB));
        set(level, new BlockPos(door + 2, g + 1, back + 1), topSlab(Blocks.SPRUCE_SLAB));
        set(level, new BlockPos(door + 3, g + 2, back + 1), screen(ModBlocks.CHAT_SCREEN.get(), Direction.SOUTH));
        set(level, new BlockPos(door + 3, g + 1, back + 2), stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH));
        set(level, new BlockPos(door + 2, g + 2, back + 1), Blocks.LANTERN.defaultBlockState());
        for (int x = door - 1; x <= door + 1; x++) {
            for (int z = back + 3; z <= back + 5; z++) set(level, new BlockPos(x, g + 1, z), Blocks.RED_CARPET);
        }
        set(level, new BlockPos(door - 2, g + 1, front - 1), Blocks.CHEST);
        set(level, new BlockPos(door + 4, g + 1, back + 4), ModBlocks.LOOSE_PAPERS.get());
        // The switch, on the front wall beside the door.
        BlockPos lever = new BlockPos(door + 1, g + 2, front - 1);
        set(level, lever, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL)
                .setValue(LeverBlock.FACING, Direction.NORTH).setValue(LeverBlock.POWERED, false));
        return lever;
    }

    /** The static: ahead of you, a television on a stand under a mast, and a chair in front of it. */
    private static BlockPos statik(ServerLevel level) {
        int g = LiminalGenerator.STATIC_GROUND;
        int x = 0;
        int z = -28;
        load(level, new BlockPos(x, g, z));
        clear(level, x - 4, g + 1, z - 4, x + 4, g + 32, z + 4);
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                boolean light = ((dx + dz) & 1) == 0;
                set(level, new BlockPos(x + dx, g, z + dz), (light ? Blocks.WHITE_CONCRETE : Blocks.LIGHT_GRAY_CONCRETE).defaultBlockState());
            }
        }
        // The mast.
        for (int y = g + 1; y <= g + 22; y++) set(level, new BlockPos(x + 3, y, z - 3), Blocks.IRON_BARS);
        set(level, new BlockPos(x + 3, g + 23, z - 3), Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true));
        // The set, on, showing snow.
        set(level, new BlockPos(x, g + 1, z), topSlab(Blocks.DARK_OAK_SLAB));
        BlockPos tv = new BlockPos(x, g + 2, z);
        set(level, tv, screen(ModBlocks.SNOW_SCREEN.get(), Direction.SOUTH));
        set(level, new BlockPos(x, g + 1, z + 2), stairs(Blocks.OAK_STAIRS, Direction.SOUTH));
        set(level, new BlockPos(x - 2, g + 1, z + 1), ModBlocks.LOOSE_PAPERS.get());
        return tv;
    }

    /** The orchard: one tree with its leaves still on, a bench against it, and his laptop on the grass. */
    private static BlockPos orchard(ServerLevel level) {
        int g = LiminalGenerator.ORCHARD_GROUND;
        int x = 4;
        int z = -30;
        load(level, new BlockPos(x, g, z));
        clear(level, x - 5, g + 1, z - 5, x + 5, g + 14, z + 5);
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                double r = Math.sqrt(dx * dx + dz * dz);
                if (r <= 4.5D) set(level, new BlockPos(x + dx, g, z + dz), Blocks.GRASS_BLOCK);
                else if (r <= 5.5D) set(level, new BlockPos(x + dx, g, z + dz), Blocks.COARSE_DIRT);
            }
        }
        for (int y = g + 1; y <= g + 5; y++) set(level, new BlockPos(x, y, z), Blocks.OAK_LOG);
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = 4; dy <= 8; dy++) {
                    double r = Math.sqrt(dx * dx + dz * dz + (dy - 6) * (dy - 6) * 1.6D);
                    BlockPos p = new BlockPos(x + dx, g + dy, z + dz);
                    if (r <= 3.3D && !(dx == 0 && dz == 0 && dy <= 5)) set(level, p, leaves);
                }
            }
        }
        // Flowers: the only place anything grows.
        Block[] flowers = {Blocks.POPPY, Blocks.DANDELION, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER};
        int[][] spots = {{-2, 3}, {3, 1}, {-3, -2}, {2, -3}, {1, 4}, {-4, 1}};
        for (int i = 0; i < spots.length; i++) {
            set(level, new BlockPos(x + spots[i][0], g + 1, z + spots[i][1]), flowers[i % flowers.length]);
        }
        set(level, new BlockPos(x, g + 1, z + 1), stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH));   // the bench, against the trunk
        BlockPos laptop = new BlockPos(x, g + 1, z + 2);
        set(level, laptop, screen(ModBlocks.LAPTOP.get(), Direction.NORTH));
        return laptop;
    }

    /**
     * The ward: the room off the corridor a little east of where you arrive becomes room four. The
     * door is open, the bed is by the window, and the lamp in the ceiling is dark.
     */
    private static BlockPos ward(ServerLevel level) {
        int f = LiminalGenerator.WARD_FLOOR;
        int ceil = LiminalGenerator.WARD_CEIL;
        load(level, new BlockPos(24, f, 25));
        clear(level, 17, f + 1, 20, 31, ceil - 1, 30);
        for (int x = 17; x <= 31; x++) {
            for (int z = 20; z <= 30; z++) {
                set(level, new BlockPos(x, f, z), (((x + z) & 1) == 0 ? Blocks.WHITE_CONCRETE : Blocks.LIGHT_GRAY_CONCRETE).defaultBlockState());
                set(level, new BlockPos(x, ceil, z), Blocks.WHITE_CONCRETE);
            }
        }
        // The doorway from the corridor, and a number on the wall beside it.
        set(level, new BlockPos(24, f + 1, 19), Blocks.AIR);
        set(level, new BlockPos(24, f + 2, 19), Blocks.AIR);
        BlockPos sign = new BlockPos(25, f + 2, 18);
        set(level, sign, Blocks.SPRUCE_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.NORTH));
        if (level.getBlockEntity(sign) instanceof SignBlockEntity be) {
            SignText text = new SignText().setMessage(1, Component.literal("4"));
            be.setText(text, true);
            be.setWaxed(true);
            be.setChanged();
            level.sendBlockUpdated(sign, be.getBlockState(), be.getBlockState(), 3);
        }
        // The bed by the window, made. The chair beside it. Flowers that nobody changed.
        BlockState bed = Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH);
        set(level, new BlockPos(23, f + 1, 29), bed.setValue(BedBlock.PART, BedPart.FOOT));
        BlockPos head = new BlockPos(23, f + 1, 30);
        set(level, head, bed.setValue(BedBlock.PART, BedPart.HEAD));
        set(level, new BlockPos(21, f + 1, 29), stairs(Blocks.SPRUCE_STAIRS, Direction.WEST));
        set(level, new BlockPos(24, f + 1, 30), Blocks.POTTED_DEAD_BUSH);
        set(level, new BlockPos(22, f + 1, 30), Blocks.QUARTZ_SLAB);
        for (int y = f + 1; y <= f + 3; y++) {
            for (int z = 25; z <= 27; z++) set(level, new BlockPos(19, y, z), Blocks.LIGHT_GRAY_WOOL);   // the curtain, half drawn
        }
        // The lamp that never worked.
        set(level, WARD_LAMP, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, false));
        return head;
    }

    /** The server room: a console on a desk along the cross aisle, its neighbours dark. */
    private static BlockPos server(ServerLevel level) {
        int f = LiminalGenerator.SERVER_FLOOR;
        int x = 24;
        load(level, new BlockPos(x, f, 1));
        clear(level, x - 2, f + 1, 0, x + 2, LiminalGenerator.SERVER_CEIL - 1, 3);
        floor(level, x - 2, f, 0, x + 2, 3, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        for (int dx = -1; dx <= 1; dx++) set(level, new BlockPos(x + dx, f + 1, 1), topSlab(Blocks.SMOOTH_STONE_SLAB));
        BlockPos console = new BlockPos(x, f + 2, 1);
        set(level, console, screen(ModBlocks.CONSOLE_SCREEN.get(), Direction.SOUTH));
        set(level, new BlockPos(x - 1, f + 2, 1), Blocks.BLACK_STAINED_GLASS);
        set(level, new BlockPos(x + 1, f + 2, 1), Blocks.BLACK_STAINED_GLASS);
        set(level, new BlockPos(x, f + 1, 2), stairs(Blocks.POLISHED_BLACKSTONE_STAIRS, Direction.SOUTH));
        set(level, new BlockPos(x + 2, f + 1, 2), ModBlocks.LOOSE_PAPERS.get());
        return console;
    }

    /** The lighthouse already stands (HubBuilder); older ones still have a plain lamp in them, so swap it. */
    private static BlockPos drowned(ServerLevel level) {
        BlockPos lamp = lamp(level);
        if (lamp == null || !SorrowedData.get(level).isBuilt(LIGHTHOUSE_KEY)) return null;
        load(level, lamp);
        BlockState here = level.getBlockState(lamp);
        if (!here.is(ModBlocks.LIGHTHOUSE_LAMP.get())) {
            set(level, lamp, ModBlocks.LIGHTHOUSE_LAMP.get().defaultBlockState().setValue(LighthouseLampBlock.LIT, false));
        }
        return lamp;
    }

    /** One more than you counted: the ninth stone, in the gap where there was not one before. */
    public static void raiseNinth(ServerLevel level) {
        BlockPos c = stonesCenter(level);
        if (c == null) return;
        BlockPos base = c.offset(NINTH[0], 0, NINTH[1]);
        BuildUtil.fillDown(level, base.below(), Blocks.COBBLESTONE.defaultBlockState(), 8);
        for (int y = 0; y < 5; y++) {
            set(level, base.above(y), (y % 2 == 0 ? Blocks.MOSSY_STONE_BRICKS : Blocks.CRACKED_STONE_BRICKS).defaultBlockState());
        }
        set(level, base.above(5), Blocks.SOUL_LANTERN);
    }
}
