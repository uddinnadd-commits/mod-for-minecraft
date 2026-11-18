package com.sorrowed.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

public final class BuildUtil {
    private BuildUtil() {}

    /** Flag 2 = send to clients, no neighbour updates (safe during world generation). */
    public static void set(LevelAccessor level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 2);
    }

    /** Air, liquid, or something soft like grass and leaves. */
    public static boolean soft(BlockState state) {
        return state.isAir() || !state.getFluidState().isEmpty() || !state.canOcclude();
    }

    /** Fill downward from 'from' until we hit something solid (at most 'max' blocks). */
    public static void fillDown(LevelAccessor level, BlockPos from, BlockState fill, int max) {
        for (int i = 0; i < max; i++) {
            BlockPos p = from.below(i);
            if (!soft(level.getBlockState(p))) break;
            set(level, p, fill);
        }
    }
}
