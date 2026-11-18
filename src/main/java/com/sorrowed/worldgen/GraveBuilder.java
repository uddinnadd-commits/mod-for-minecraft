package com.sorrowed.worldgen;

import com.sorrowed.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** A small clearing: a nameless stone, a few flowers, a lantern on a fence post. */
public final class GraveBuilder {
    private GraveBuilder() {}

    public static void build(LevelAccessor level, BlockPos o, RandomSource rand) {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 0; y <= 3; y++) {
                    BlockPos p = o.offset(x, y, z);
                    if (!level.getBlockState(p).isAir()) BuildUtil.set(level, p, air);
                }
            }
        }
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockState ground = rand.nextBoolean() ? Blocks.PODZOL.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState();
                BuildUtil.set(level, o.offset(x, -1, z), ground);
            }
        }
        BuildUtil.set(level, o, ModBlocks.GRAVE_MARKER.get().defaultBlockState());

        BuildUtil.set(level, o.offset(-1, 0, -1), Blocks.AZURE_BLUET.defaultBlockState());
        BuildUtil.set(level, o.offset(1, 0, 1), Blocks.LILY_OF_THE_VALLEY.defaultBlockState());
        if (rand.nextBoolean()) BuildUtil.set(level, o.offset(1, 0, -1), Blocks.OXEYE_DAISY.defaultBlockState());

        BuildUtil.set(level, o.offset(2, -1, 0), Blocks.COARSE_DIRT.defaultBlockState());
        BuildUtil.set(level, o.offset(2, 0, 0), Blocks.OAK_FENCE.defaultBlockState());
        BuildUtil.set(level, o.offset(2, 1, 0), Blocks.LANTERN.defaultBlockState());
    }
}
