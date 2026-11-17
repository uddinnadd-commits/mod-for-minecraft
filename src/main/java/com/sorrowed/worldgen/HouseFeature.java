package com.sorrowed.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Overworld only: the half-built house with a chest of his notes. */
public class HouseFeature extends Feature<NoneFeatureConfiguration> {
    public HouseFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        if (!level.getLevel().dimension().equals(Level.OVERWORLD)) return false;
        if (!level.getBlockState(origin.below()).getFluidState().isEmpty()) return false;

        HouseBuilder.build(level, origin, ctx.random(), false);
        return true;
    }
}
