package com.sorrowed.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sorrowed.Sorrowed;
import com.sorrowed.entity.FriendEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Him. Sitting at the end of the dock with his hands in his lap, looking like he always did. At
 * the very end he is standing instead, coming down out of the sky, lit from inside, with a halo.
 */
public class FriendRenderer extends LivingEntityRenderer<FriendEntity, PlayerModel<FriendEntity>> {
    private static final ResourceLocation SKIN = new ResourceLocation("textures/entity/player/slim/alex.png");
    private static final ResourceLocation HALO = new ResourceLocation(Sorrowed.MODID, "textures/entity/halo.png");

    public FriendRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Posed(ctx.bakeLayer(ModelLayers.PLAYER_SLIM)), 0.35F);
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
    public boolean shouldRender(FriendEntity entity, Frustum frustum, double x, double y, double z) {
        // Coming down from high above his real position: never culled for being "off screen".
        return entity.isHalo() || super.shouldRender(entity, frustum, x, y, z);
    }

    @Override
    public void render(FriendEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        if (entity.isHalo()) {
            float age = entity.tickCount + partialTick;
            double up = (1.0D - entity.descent(partialTick)) * FriendEntity.DESCENT_HEIGHT + 0.06D * Math.sin(age * 0.08D);
            pose.translate(0.0D, up, 0.0D);
            super.render(entity, yaw, partialTick, pose, buffers, LightTexture.FULL_BRIGHT);
            halo(pose, buffers, age);
        } else {
            pose.translate(0.0D, -0.62D, 0.0D);   // down onto the planks, legs over the edge
            super.render(entity, yaw, partialTick, pose, buffers, light);
        }
        pose.popPose();
    }

    /** A ring of light a little above his head, turning slowly, with a softer glow around it. */
    private static void halo(PoseStack pose, MultiBufferSource buffers, float age) {
        pose.pushPose();
        pose.translate(0.0D, 2.08D + 0.03D * Math.sin(age * 0.11D), 0.0D);
        pose.mulPose(Axis.XP.rotationDegrees(12.0F));
        pose.mulPose(Axis.YP.rotationDegrees(age * 1.5F));
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(HALO));
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        ring(vc, m, n, 0.20F, 0.30F, 1.0F, 0.92F, 0.55F, 0.95F);
        ring(vc, m, n, 0.14F, 0.40F, 1.0F, 0.95F, 0.75F, 0.25F);
        pose.popPose();
    }

    private static void ring(VertexConsumer vc, Matrix4f m, Matrix3f n, float inner, float outer, float r, float g, float b, float a) {
        int seg = 28;
        for (int i = 0; i < seg; i++) {
            float a0 = i * Mth.TWO_PI / seg;
            float a1 = (i + 1) * Mth.TWO_PI / seg;
            float c0 = Mth.cos(a0);
            float s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1);
            float s1 = Mth.sin(a1);
            vertex(vc, m, n, c0 * inner, s0 * inner, 0.0F, r, g, b, a);
            vertex(vc, m, n, c0 * outer, s0 * outer, 1.0F, r, g, b, a);
            vertex(vc, m, n, c1 * outer, s1 * outer, 1.0F, r, g, b, a);
            vertex(vc, m, n, c1 * inner, s1 * inner, 0.0F, r, g, b, a);
        }
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float z, float v,
                               float r, float g, float b, float a) {
        vc.vertex(m, x, 0.0F, z).color(r, g, b, a).uv(0.5F, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(n, 0.0F, 1.0F, 0.0F).endVertex();
    }

    @Override
    protected boolean shouldShowName(FriendEntity entity) {
        return false;
    }

    private static final class Posed extends PlayerModel<FriendEntity> {
        Posed(ModelPart root) {
            super(root, true);
        }

        @Override
        public void setupAnim(FriendEntity entity, float limbSwing, float limbAmount, float age, float headYaw, float headPitch) {
            this.riding = !entity.isHalo();
            super.setupAnim(entity, 0.0F, 0.0F, age, headYaw, headPitch);
            if (entity.isHalo()) {
                // Standing, arms a little open, at peace.
                this.rightArm.xRot = 0.0F;
                this.leftArm.xRot = 0.0F;
                this.rightArm.zRot = 0.18F + 0.03F * Mth.sin(age * 0.07F);
                this.leftArm.zRot = -0.18F - 0.03F * Mth.sin(age * 0.07F);
                this.rightLeg.xRot = 0.0F;
                this.leftLeg.xRot = 0.0F;
            } else {
                this.rightArm.xRot = -0.62F;
                this.leftArm.xRot = -0.55F;
                this.rightArm.zRot = 0.10F;
                this.leftArm.zRot = -0.10F;
                this.rightLeg.xRot = -1.35F;
                this.leftLeg.xRot = -1.45F;
                this.rightLeg.yRot = 0.08F;
                this.leftLeg.yRot = -0.06F;
            }
            this.rightSleeve.copyFrom(this.rightArm);
            this.leftSleeve.copyFrom(this.leftArm);
            this.rightPants.copyFrom(this.rightLeg);
            this.leftPants.copyFrom(this.leftLeg);
        }
    }
}
