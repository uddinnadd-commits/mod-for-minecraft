package com.sorrowed.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sorrowed.entity.FriendEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;

/** Him, sitting at the end of the dock with his hands in his lap. He looks like he always did. */
public class FriendRenderer extends LivingEntityRenderer<FriendEntity, PlayerModel<FriendEntity>> {
    private static final ResourceLocation SKIN = new ResourceLocation("textures/entity/player/slim/alex.png");

    public FriendRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Sitting(ctx.bakeLayer(ModelLayers.PLAYER_SLIM)), 0.35F);
    }

    @Override
    public ResourceLocation getTextureLocation(FriendEntity entity) {
        return SKIN;
    }

    @Override
    protected void scale(FriendEntity entity, PoseStack pose, float partialTick) {
        pose.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    public void render(FriendEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0.0D, -0.62D, 0.0D);   // down onto the planks, legs over the edge
        super.render(entity, yaw, partialTick, pose, buffers, light);
        pose.popPose();
    }

    @Override
    protected boolean shouldShowName(FriendEntity entity) {
        return false;
    }

    private static final class Sitting extends PlayerModel<FriendEntity> {
        Sitting(ModelPart root) {
            super(root, true);
        }

        @Override
        public void setupAnim(FriendEntity entity, float limbSwing, float limbAmount, float age, float headYaw, float headPitch) {
            this.riding = true;
            super.setupAnim(entity, 0.0F, 0.0F, age, headYaw, headPitch);
            this.rightArm.xRot = -0.62F;
            this.leftArm.xRot = -0.55F;
            this.rightArm.zRot = 0.10F;
            this.leftArm.zRot = -0.10F;
            this.rightLeg.xRot = -1.35F;
            this.leftLeg.xRot = -1.45F;
            this.rightLeg.yRot = 0.08F;
            this.leftLeg.yRot = -0.06F;
            this.rightSleeve.copyFrom(this.rightArm);
            this.leftSleeve.copyFrom(this.leftArm);
            this.rightPants.copyFrom(this.rightLeg);
            this.leftPants.copyFrom(this.leftLeg);
        }
    }
}
