package com.sorrowed.worldgen;

import com.sorrowed.story.Letters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

/** The Set Table: a perfect little cottage, a table laid for two, and a letter in the chest. */
public final class CottageBuilder {
    private CottageBuilder() {}

    public static void build(LevelAccessor level, BlockPos o, RandomSource rand) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState planks = Blocks.BIRCH_PLANKS.defaultBlockState();
        BlockState log = Blocks.STRIPPED_BIRCH_LOG.defaultBlockState();
        for (int x = -5; x <= 5; x++) {
            for (int z = -4; z <= 4; z++) {
                for (int y = 0; y <= 6; y++) {
                    BlockPos p = o.offset(x, y, z);
                    if (!level.getBlockState(p).isAir()) BuildUtil.set(level, p, air);
                }
            }
        }
        for (int x = -4; x <= 4; x++) {
            for (int z = -3; z <= 3; z++) {
                BlockPos f = o.offset(x, -1, z);
                BuildUtil.set(level, f, Blocks.SMOOTH_STONE.defaultBlockState());
                BuildUtil.fillDown(level, f.below(), Blocks.DIRT.defaultBlockState(), 6);
            }
        }

        for (int y = 0; y <= 3; y++) {
            for (int x = -4; x <= 4; x++) {
                for (int z = -3; z <= 3; z++) {
                    if (Math.abs(x) != 4 && Math.abs(z) != 3) continue;
                    boolean corner = Math.abs(x) == 4 && Math.abs(z) == 3;
                    if (x == 0 && z == 3 && y <= 1) continue;
                    BlockState s = corner ? log : planks;
                    if (y >= 1 && y <= 2 && !corner && ((Math.abs(z) == 3 && Math.abs(x) % 2 == 0 && x != 0)
                            || (Math.abs(x) == 4 && z == 0))) {
                        s = Blocks.GLASS_PANE.defaultBlockState();
                    }
                    BuildUtil.set(level, o.offset(x, y, z), s);
                }
            }
        }
        for (int x = -5; x <= 5; x++) {
            for (int z = -4; z <= 4; z++) {
                BuildUtil.set(level, o.offset(x, 4, z), Blocks.BIRCH_SLAB.defaultBlockState());
            }
        }
        for (int x = -3; x <= 3; x++) {
            for (int z = -2; z <= 2; z++) {
                BuildUtil.set(level, o.offset(x, 4, z), planks);
            }
        }

        // The table: three planks, two cakes, one candle, two chairs facing each other.
        for (int x = -1; x <= 1; x++) {
            BuildUtil.set(level, o.offset(x, 0, 0), Blocks.SPRUCE_PLANKS.defaultBlockState());
        }
        BuildUtil.set(level, o.offset(-1, 1, 0), Blocks.CAKE.defaultBlockState());
        BuildUtil.set(level, o.offset(1, 1, 0), Blocks.CAKE.defaultBlockState());
        BuildUtil.set(level, o.offset(0, 1, 0), Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true));
        BuildUtil.set(level, o.offset(-2, 0, 0), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
        BuildUtil.set(level, o.offset(2, 0, 0), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));

        // A made bed and a wall of books.
        BlockPos foot = o.offset(-3, 0, -2);
        BuildUtil.set(level, foot, Blocks.WHITE_BED.defaultBlockState()
                .setValue(BedBlock.FACING, Direction.EAST).setValue(BedBlock.PART, BedPart.FOOT));
        BuildUtil.set(level, foot.east(), Blocks.WHITE_BED.defaultBlockState()
                .setValue(BedBlock.FACING, Direction.EAST).setValue(BedBlock.PART, BedPart.HEAD));
        for (int x = 1; x <= 3; x++) {
            BuildUtil.set(level, o.offset(x, 0, -2), Blocks.BOOKSHELF.defaultBlockState());
            BuildUtil.set(level, o.offset(x, 1, -2), Blocks.BOOKSHELF.defaultBlockState());
        }

        BlockPos chest = o.offset(3, 0, 2);
        BuildUtil.set(level, chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST));
        BlockEntity be = level.getBlockEntity(chest);
        if (be instanceof ChestBlockEntity c) {
            c.setItem(0, Letters.reply());
        }
    }
}
