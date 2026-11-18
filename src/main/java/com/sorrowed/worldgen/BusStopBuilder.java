package com.sorrowed.worldgen;

import com.sorrowed.story.Letters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A bus shelter in the middle of nowhere. The timetable has one line on it. */
public final class BusStopBuilder {
    private BusStopBuilder() {}

    public static void build(LevelAccessor level, BlockPos o, RandomSource rand) {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = -4; x <= 4; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = 0; y <= 5; y++) {
                    BlockPos p = o.offset(x, y, z);
                    if (!level.getBlockState(p).isAir()) BuildUtil.set(level, p, air);
                }
            }
        }
        for (int x = -3; x <= 3; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos f = o.offset(x, -1, z);
                BuildUtil.set(level, f, Blocks.STONE_BRICKS.defaultBlockState());
                BuildUtil.fillDown(level, f.below(), Blocks.COBBLESTONE.defaultBlockState(), 6);
            }
        }

        // Four posts, a slab roof, a glass back wall.
        for (int y = 0; y <= 2; y++) {
            for (int x : new int[]{-2, 2}) {
                for (int z : new int[]{-1, 1}) {
                    BuildUtil.set(level, o.offset(x, y, z), Blocks.OAK_FENCE.defaultBlockState());
                }
            }
        }
        for (int x = -2; x <= 2; x++) {
            for (int z = -1; z <= 1; z++) {
                BuildUtil.set(level, o.offset(x, 3, z), Blocks.OAK_SLAB.defaultBlockState());
            }
        }
        for (int x = -1; x <= 1; x++) {
            BuildUtil.set(level, o.offset(x, 1, -1), Blocks.GLASS_PANE.defaultBlockState());
            BuildUtil.set(level, o.offset(x, 2, -1), Blocks.GLASS_PANE.defaultBlockState());
        }

        // A bench and a lantern that never swings.
        for (int x = -1; x <= 1; x++) {
            BuildUtil.set(level, o.offset(x, 0, 0), Blocks.SPRUCE_SLAB.defaultBlockState());
        }
        BuildUtil.set(level, o.offset(0, 2, 0), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));

        // The lost-property box.
        BlockPos chest = o.offset(2, 0, 0);
        BuildUtil.set(level, chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST));
        BlockEntity be = level.getBlockEntity(chest);
        if (be instanceof ChestBlockEntity c) {
            c.setItem(0, Letters.ticket());
        }
    }
}
