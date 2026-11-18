package com.sorrowed.worldgen;

import com.sorrowed.registry.Dimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Overworld and Hush: a ring of candles around a book of names. */
public class VigilRingFeature extends Feature<NoneFeatureConfiguration> {
    public VigilRingFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        if (!(level.getLevel().dimension().equals(Level.OVERWORLD) || level.getLevel().dimension().equals(Dimensions.HUSH))) return false;
        if (!level.getBlockState(origin.below()).getFluidState().isEmpty()) return false;
        VigilRingBuilder.build(level, origin, ctx.random());
        return true;
    }
}
