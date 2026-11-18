package com.sorrowed.worldgen;

import com.sorrowed.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** A few pages lying on the forest floor in the Hush. */
public class PapersFeature extends Feature<NoneFeatureConfiguration> {
    public PapersFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos pos = ctx.origin();
        if (!level.getBlockState(pos).isAir()) return false;
        if (!level.getBlockState(pos.below()).isSolidRender(level, pos.below())) return false;
        level.setBlock(pos, ModBlocks.LOOSE_PAPERS.get().defaultBlockState(), 2);
        return true;
    }
}
