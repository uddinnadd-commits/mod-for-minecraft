package com.sorrowed.worldgen;

import com.sorrowed.SorrowedData;
import com.sorrowed.registry.Dimensions;
import com.sorrowed.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Builds (once) the gates and buildings you arrive at inside the dimensions. */
public final class HubBuilder {
    private HubBuilder() {}

    /** Where a player arrives in 'key'. Builds the hub the first time. */
    public static Vec3 arrival(ServerLevel dest, ResourceKey<Level> key) {
        if (key.equals(Dimensions.UNFINISHED)) {
            // Only the way home stands at the hub. The other gates are out along the walkways.
            BlockPos gate = ensureGate(dest, 0, 0, ModBlocks.UNFINISHED_DOOR.get());
            ensureHouse(dest, -18, 10, true, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);
            return front(gate);
        }
        Block door = doorFor(key);
        if (door != null) {
            BlockPos gate = ensureGate(dest, 0, 0, door);
            if (key.equals(Dimensions.DROWNED)) {
                // His house, on the sea floor, still holding a pocket of air, and a lighthouse nobody lit.
                ensureHouse(dest, 20, 14, false, Heightmap.Types.OCEAN_FLOOR);
                ensureBuilt(dest, "lighthouse", -26, -18, Heightmap.Types.OCEAN_FLOOR, LighthouseBuilder::build);
            } else if (key.equals(Dimensions.HUSH)) {
                ensureBuilt(dest, "chapel", 24, -16, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ChapelBuilder::build);
                ensureBuilt(dest, "stones", -22, 14, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, StandingStonesBuilder::build);
            } else if (key.equals(Dimensions.BACKUP)) {
                ensureBuilt(dest, "cottage", 16, 16, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, CottageBuilder::build);
            }
            return front(gate);
        }
        return Vec3.atBottomCenterOf(dest.getSharedSpawnPos());
    }

    private static Vec3 front(BlockPos gate) {
        return new Vec3(gate.getX() + 0.5D, gate.getY(), gate.getZ() + 3.5D);
    }

    private static Block doorFor(ResourceKey<Level> key) {
        if (key.equals(Dimensions.HUSH)) return ModBlocks.HUSH_DOOR.get();
        if (key.equals(Dimensions.DROWNED)) return ModBlocks.DROWNED_DOOR.get();
        if (key.equals(Dimensions.HALLWAY)) return ModBlocks.HALLWAY_DOOR.get();
        if (key.equals(Dimensions.BACKUP)) return ModBlocks.BACKUP_DOOR.get();
        if (key.equals(Dimensions.ARCHIVE)) return ModBlocks.ARCHIVE_DOOR.get();
        if (key.equals(Dimensions.STREET)) return ModBlocks.STREET_DOOR.get();
        if (key.equals(Dimensions.STATIC)) return ModBlocks.STATIC_DOOR.get();
        return null;
    }

    /** Ground height at x,z, decided once and remembered so rebuild checks stay stable. */
    private static int stableY(ServerLevel level, String key, int x, int z, Heightmap.Types type) {
        level.getChunk(x >> 4, z >> 4);
        if (level.dimension().equals(Dimensions.HALLWAY)) {
            return LiminalGenerator.HALL_FLOOR + 1;
        }
        if (level.dimension().equals(Dimensions.ARCHIVE)) {
            return LiminalGenerator.ARCHIVE_FLOOR + 1;
        }
        SorrowedData data = SorrowedData.get(level);
        Integer y = data.getY(key);
        if (y == null) {
            y = level.getHeight(type, x, z);
            data.setY(key, y);
        }
        return y;
    }

    private static BlockPos ensureGate(ServerLevel level, int x, int z, Block door) {
        String key = "gate:" + level.dimension().location().getPath() + ":" + x + ":" + z;
        BlockPos base = new BlockPos(x, stableY(level, key, x, z, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES), z);
        if (!level.getBlockState(base).is(door)) {
            GateBuilder.build(level, base, Direction.Axis.X, door, level.random);
        }
        return base;
    }

    private interface Placer {
        void place(ServerLevel level, BlockPos pos, RandomSource random);
    }

    private static void ensureBuilt(ServerLevel level, String name, int x, int z, Heightmap.Types type, Placer placer) {
        String key = name + ":" + level.dimension().location().getPath() + ":" + x + ":" + z;
        int y = stableY(level, key, x, z, type);
        SorrowedData data = SorrowedData.get(level);
        if (!data.isBuilt(key)) {
            placer.place(level, new BlockPos(x, y, z), level.random);
            data.setBuilt(key);
        }
    }

    private static void ensureHouse(ServerLevel level, int x, int z, boolean finalPage, Heightmap.Types type) {
        String key = "house:" + level.dimension().location().getPath() + ":" + x + ":" + z;
        int y = stableY(level, key, x, z, type);
        SorrowedData data = SorrowedData.get(level);
        if (!data.isBuilt(key)) {
            HouseBuilder.build(level, new BlockPos(x, y, z), level.random, finalPage);
            data.setBuilt(key);
        }
    }
}
