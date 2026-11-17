package com.sorrowed.worldgen;

import com.sorrowed.registry.Dimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Small graves scattered through the overworld and the Hush. */
public class GraveFeature extends Feature<NoneFeatureConfiguration> {
    public GraveFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        if (level.getLevel().dimension().equals(Dimensions.UNFINISHED)) return false;
        if (!level.getBlockState(origin.below()).getFluidState().isEmpty()) return false;

        GraveBuilder.build(level, origin, ctx.random());
        return true;
    }
}
