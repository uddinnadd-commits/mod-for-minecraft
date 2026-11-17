package com.sorrowed.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** Renders any of the custom-bodied mobs from a model layer and a texture. */
public class SorrowMobRenderer<T extends Mob> extends MobRenderer<T, SorrowModel<T>> {
    private final ResourceLocation texture;

    public SorrowMobRenderer(EntityRendererProvider.Context ctx, ModelLayerLocation layer, ResourceLocation texture,
                             float shadow, SorrowModel.Style style, boolean translucent) {
        super(ctx, new SorrowModel<>(ctx.bakeLayer(layer), style, translucent), shadow);
        this.texture = texture;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
