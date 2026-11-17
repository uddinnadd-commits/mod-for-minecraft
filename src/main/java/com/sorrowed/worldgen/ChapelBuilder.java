package com.sorrowed.worldgen;

import com.sorrowed.story.Letters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The Chapel of Quiet: pews that face a lectern, a black carpet, and a book of apologies. */
public final class ChapelBuilder {
    private ChapelBuilder() {}

    public static void build(LevelAccessor level, BlockPos o, RandomSource rand) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState brick = Blocks.STONE_BRICKS.defaultBlockState();
        for (int x = -5; x <= 5; x++) {
            for (int z = -7; z <= 7; z++) {
                for (int y = 0; y <= 8; y++) {
                    BlockPos p = o.offset(x, y, z);
                    if (!level.getBlockState(p).isAir()) BuildUtil.set(level, p, air);
                }
            }
        }
        for (int x = -3; x <= 3; x++) {
            for (int z = -5; z <= 5; z++) {
                BlockPos f = o.offset(x, -1, z);
                BuildUtil.set(level, f, Blocks.DEEPSLATE_TILES.defaultBlockState());
                BuildUtil.fillDown(level, f.below(), Blocks.COBBLESTONE.defaultBlockState(), 8);
            }
        }

        // Walls and a slab roof. Door at the south end.
        for (int y = 0; y <= 4; y++) {
            for (int x = -3; x <= 3; x++) {
                for (int z = -5; z <= 5; z++) {
                    if (Math.abs(x) != 3 && Math.abs(z) != 5) continue;
                    if (x == 0 && z == 5 && y <= 2) continue;
                    BuildUtil.set(level, o.offset(x, y, z), brick);
                }
            }
        }
        for (int x = -3; x <= 3; x++) {
            for (int z = -5; z <= 5; z++) {
                BuildUtil.set(level, o.offset(x, 5, z), Blocks.STONE_BRICK_SLAB.defaultBlockState());
            }
        }
        for (int z : new int[]{-2, 2}) {
            for (int y = 1; y <= 3; y++) {
                BuildUtil.set(level, o.offset(-3, y, z), Blocks.GLASS_PANE.defaultBlockState());
                BuildUtil.set(level, o.offset(3, y, z), Blocks.GLASS_PANE.defaultBlockState());
            }
        }

        // Aisle, pews, altar.
        for (int z = -3; z <= 4; z++) {
            BuildUtil.set(level, o.offset(0, 0, z), Blocks.BLACK_CARPET.defaultBlockState());
        }
        for (int z : new int[]{-1, 1, 3}) {
            for (int x : new int[]{-2, -1, 1, 2}) {
                BuildUtil.set(level, o.offset(x, 0, z), Blocks.SPRUCE_SLAB.defaultBlockState());
            }
        }
        BlockPos altar = o.offset(0, 0, -4);
        BuildUtil.set(level, altar, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
        BuildUtil.set(level, o.offset(-1, 0, -4), Blocks.SOUL_LANTERN.defaultBlockState());
        BuildUtil.set(level, o.offset(1, 0, -4), Blocks.SOUL_LANTERN.defaultBlockState());
        BlockEntity be = level.getBlockEntity(altar);
        if (be instanceof ChestBlockEntity chest) {
            chest.setItem(0, Letters.unsaid());
        }
    }
}
