package com.sorrowed.worldgen;

import com.mojang.serialization.Codec;
import com.sorrowed.registry.Dimensions;
import com.sorrowed.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Overworld only: a lit stone gate standing alone in the rain. */
public class GateFeature extends Feature<NoneFeatureConfiguration> {
    public GateFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        boolean hush = level.getLevel().dimension().equals(Dimensions.HUSH);
        if (!level.getLevel().dimension().equals(Level.OVERWORLD) && !hush) return false;
        if (!level.getBlockState(origin.below()).getFluidState().isEmpty()) return false;

        Direction.Axis axis = ctx.random().nextBoolean() ? Direction.Axis.X : Direction.Axis.Z;
        GateBuilder.build(level, origin, axis, hush ? ModBlocks.HUSH_DOOR.get() : ModBlocks.BACKUP_DOOR.get(), ctx.random());
        return true;
    }
}
