package com.sorrowed.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/** Custom sky/fog behaviour for dimensions that have no sky of their own (the Hush, the Hallway). */
public class SorrowEffects extends DimensionSpecialEffects {
    private final boolean foggy;

    public SorrowEffects(boolean foggy) {
        super(Float.NaN, true, DimensionSpecialEffects.SkyType.NONE, false, false);
        this.foggy = foggy;
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        return color.multiply(brightness * 0.94F + 0.06F, brightness * 0.94F + 0.06F, brightness * 0.91F + 0.09F);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return foggy;
    }
}
