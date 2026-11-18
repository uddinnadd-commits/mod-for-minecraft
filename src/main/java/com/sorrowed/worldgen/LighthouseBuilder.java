package com.sorrowed.worldgen;

import com.sorrowed.story.Letters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The lighthouse that never lights: a 5x5 tower, a ladder, and a dark lamp at the top. */
public final class LighthouseBuilder {
    private LighthouseBuilder() {}

    public static void build(LevelAccessor level, BlockPos o, RandomSource rand) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState brick = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState white = Blocks.WHITE_CONCRETE.defaultBlockState();

        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                BlockPos f = o.offset(x, -1, z);
                BuildUtil.set(level, f, brick);
                BuildUtil.fillDown(level, f.below(), Blocks.COBBLESTONE.defaultBlockState(), 12);
            }
        }
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 0; y <= 25; y++) {
                    BlockPos p = o.offset(x, y, z);
                    if (!level.getBlockState(p).isAir()) BuildUtil.set(level, p, air);
                }
            }
        }

        // Shaft: banded stone and white, a doorway on the south side.
        for (int y = 0; y <= 19; y++) {
            BlockState wall = (y % 6 >= 4) ? white : brick;
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (Math.abs(x) != 2 && Math.abs(z) != 2) continue;
                    if (x == 0 && z == 2 && y <= 1) continue;
                    BuildUtil.set(level, o.offset(x, y, z), wall);
                }
            }
        }

        // Lamp-room floor, then the ladder through it.
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BuildUtil.set(level, o.offset(x, 20, z), brick);
            }
        }
        for (int y = 0; y <= 21; y++) {
            BuildUtil.set(level, o.offset(0, y, -1),
                    Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        }

        // Glass lamp room with an unlit lamp, and a roof.
        for (int y = 21; y <= 22; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                        BuildUtil.set(level, o.offset(x, y, z), Blocks.GLASS.defaultBlockState());
                    }
                }
            }
        }
        BuildUtil.set(level, o.offset(0, 21, 0), com.sorrowed.registry.ModBlocks.LIGHTHOUSE_LAMP.get().defaultBlockState());
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BuildUtil.set(level, o.offset(x, 23, z), brick);
            }
        }

        BlockPos chest = o.offset(1, 21, 1);
        BuildUtil.set(level, chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH));
        BlockEntity be = level.getBlockEntity(chest);
        if (be instanceof ChestBlockEntity c) {
            c.setItem(0, Letters.keeperLog());
        }
    }
}
