package com.sorrowed.worldgen;

import com.sorrowed.story.Letters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A ring of lit candles around a lectern holding a book of names. The last name is yours. */
public final class VigilRingBuilder {
    private VigilRingBuilder() {}

    public static void build(LevelAccessor level, BlockPos o, RandomSource rand) {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                for (int y = 0; y <= 3; y++) {
                    BlockPos p = o.offset(x, y, z);
                    if (!level.getBlockState(p).isAir()) BuildUtil.set(level, p, air);
                }
            }
        }
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BuildUtil.set(level, o.offset(x, -1, z), Blocks.PODZOL.defaultBlockState());
            }
        }

        for (int i = 0; i < 18; i++) {
            double a = Math.toRadians(i * 20.0D);
            BlockPos pos = o.offset((int) Math.round(5.0D * Math.cos(a)), 0, (int) Math.round(5.0D * Math.sin(a)));
            BuildUtil.set(level, pos.below(), Blocks.PODZOL.defaultBlockState());
            BuildUtil.fillDown(level, pos.below(2), Blocks.DIRT.defaultBlockState(), 6);
            BuildUtil.set(level, pos, Blocks.CANDLE.defaultBlockState()
                    .setValue(CandleBlock.LIT, true)
                    .setValue(CandleBlock.CANDLES, 1 + rand.nextInt(4)));
        }

        BuildUtil.set(level, o, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH));
        BlockEntity be = level.getBlockEntity(o);
        if (be instanceof ChestBlockEntity chest) {
            chest.setItem(0, Letters.names());
        }
    }
}
