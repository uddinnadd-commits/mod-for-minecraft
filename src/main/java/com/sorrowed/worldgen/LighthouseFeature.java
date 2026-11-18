package com.sorrowed.worldgen;

import com.sorrowed.registry.Dimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Overworld only: the lighthouse that never lights. */
public class LighthouseFeature extends Feature<NoneFeatureConfiguration> {
    public LighthouseFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        if (!(level.getLevel().dimension().equals(Level.OVERWORLD))) return false;
        if (!level.getBlockState(origin.below()).getFluidState().isEmpty()) return false;
        LighthouseBuilder.build(level, origin, ctx.random());
        return true;
    }
}
