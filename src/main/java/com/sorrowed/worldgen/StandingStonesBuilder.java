package com.sorrowed.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Eight tall stones around a soul fire. In the Hush they all lean toward the middle. */
public final class StandingStonesBuilder {
    private StandingStonesBuilder() {}

    public static void build(LevelAccessor level, BlockPos o, RandomSource rand) {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                for (int y = 0; y <= 8; y++) {
                    BlockPos p = o.offset(x, y, z);
                    if (!level.getBlockState(p).isAir()) BuildUtil.set(level, p, air);
                }
            }
        }
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BuildUtil.set(level, o.offset(x, -1, z), Blocks.COARSE_DIRT.defaultBlockState());
            }
        }
        BuildUtil.set(level, o, Blocks.SOUL_CAMPFIRE.defaultBlockState());

        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45.0D);
            BlockPos base = o.offset((int) Math.round(6.0D * Math.cos(a)), 0, (int) Math.round(6.0D * Math.sin(a)));
            BuildUtil.fillDown(level, base.below(), Blocks.COBBLESTONE.defaultBlockState(), 8);
            int height = 4 + rand.nextInt(3);
            for (int y = 0; y < height; y++) {
                BlockState s = rand.nextInt(3) == 0 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
                        : Blocks.STONE_BRICKS.defaultBlockState();
                BuildUtil.set(level, base.above(y), s);
            }
            if (i % 3 == 0) {
                BuildUtil.set(level, base.above(height), Blocks.SOUL_LANTERN.defaultBlockState());
            }
        }
    }
}
