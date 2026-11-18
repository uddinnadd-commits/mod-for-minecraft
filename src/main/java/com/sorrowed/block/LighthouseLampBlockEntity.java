package com.sorrowed.block;

import com.sorrowed.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Exists so the beam has something to be drawn from; the lamp's only state is lit or not. */
public class LighthouseLampBlockEntity extends BlockEntity {
    public LighthouseLampBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LIGHTHOUSE_LAMP.get(), pos, state);
    }

    @Override
    public AABB getRenderBoundingBox() {
        // The beam reaches far past the block; never cull it just because the lamp is off screen.
        return INFINITE_EXTENT_AABB;
    }
}
