package com.sorrowed.client;

import com.sorrowed.entity.ReflectionEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/** Draws the Reflection in its owner's own skin, see-through. */
public class ReflectionRenderer extends HumanoidMobRenderer<ReflectionEntity, HumanoidModel<ReflectionEntity>> {
    private static final ResourceLocation FALLBACK = new ResourceLocation("textures/entity/player/wide/steve.png");

    public ReflectionRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(ReflectionEntity entity) {
        Optional<UUID> id = entity.getOwnerId();
        if (id.isPresent() && Minecraft.getInstance().level != null) {
            Player owner = Minecraft.getInstance().level.getPlayerByUUID(id.get());
            if (owner instanceof AbstractClientPlayer acp) {
                return acp.getSkinTextureLocation();
            }
        }
        return FALLBACK;
    }

    @Nullable
    @Override
    protected RenderType getRenderType(ReflectionEntity entity, boolean visible, boolean invisibleToPlayer, boolean glowing) {
        return RenderType.entityTranslucent(getTextureLocation(entity));
    }
}
