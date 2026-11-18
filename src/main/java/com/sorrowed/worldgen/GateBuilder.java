package com.sorrowed.worldgen;

import com.sorrowed.block.DoorwayBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A weathered stone doorframe with a Doorway block inside.
 * 'base' is the first air block above the ground, centre of the doorway.
 * axis = the direction the frame runs along (the doorway's flat side).
 */
public final class GateBuilder {
    private GateBuilder() {}

    public static BlockPos pos(BlockPos base, Direction.Axis axis, int along, int up, int across) {
        return axis == Direction.Axis.X ? base.offset(along, up, across) : base.offset(across, up, along);
    }

    private static BlockState brick(RandomSource rand) {
        int r = rand.nextInt(10);
        if (r < 6) return Blocks.STONE_BRICKS.defaultBlockState();
        if (r < 9) return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
    }

    public static void build(LevelAccessor level, BlockPos base, Direction.Axis axis, Block door, RandomSource rand) {
        BlockState air = Blocks.AIR.defaultBlockState();

        // Clear the space and lay a stone apron (7x7) with a foundation.
        for (int a = -3; a <= 3; a++) {
            for (int c = -3; c <= 3; c++) {
                for (int y = 0; y <= 6; y++) {
                    BlockPos p = pos(base, axis, a, y, c);
                    if (!level.getBlockState(p).isAir()) BuildUtil.set(level, p, air);
                }
                BlockPos floor = pos(base, axis, a, -1, c);
                BuildUtil.set(level, floor, brick(rand));
                BuildUtil.fillDown(level, floor.below(), Blocks.COBBLESTONE.defaultBlockState(), 8);
            }
        }

        // Pillars and lintel.
        for (int y = 0; y <= 4; y++) {
            BuildUtil.set(level, pos(base, axis, -2, y, 0), brick(rand));
            BuildUtil.set(level, pos(base, axis, 2, y, 0), brick(rand));
        }
        for (int a = -1; a <= 1; a++) {
            BuildUtil.set(level, pos(base, axis, a, 4, 0), brick(rand));
        }

        // The doorway itself: 3 wide, 4 tall.
        BlockState doorState = door.defaultBlockState().setValue(DoorwayBlock.AXIS, axis);
        for (int a = -1; a <= 1; a++) {
            for (int y = 0; y <= 3; y++) {
                BuildUtil.set(level, pos(base, axis, a, y, 0), doorState);
            }
        }

        // A lantern on each pillar so the gate is lit in the rain.
        BuildUtil.set(level, pos(base, axis, -2, 5, 0), Blocks.LANTERN.defaultBlockState());
        BuildUtil.set(level, pos(base, axis, 2, 5, 0), Blocks.LANTERN.defaultBlockState());
    }
}
