package com.sorrowed.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sorrowed.Sorrowed;
import com.sorrowed.entity.MemoryShattererEntity;
import com.sorrowed.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Him, wrong: his own shape at twice his size, gone dark, cracked through with light, arms out,
 * twitching. Ghosts of him slip off to either side in red and cyan, and pieces of him circle
 * him, more of them every stage. While it forms it grows out of his size; while it dies it shakes
 * itself apart.
 */
public class ShattererRenderer extends MobRenderer<MemoryShattererEntity, ShattererRenderer.Model> {
    private static final ResourceLocation SKIN = new ResourceLocation("textures/entity/player/slim/alex.png");
    private static final ResourceLocation CRACKS = new ResourceLocation(Sorrowed.MODID, "textures/entity/shatterer_cracks.png");
    private static final float FULL = 1.9F;
    private static final float START = 0.9375F;

    private final ItemStack shard = new ItemStack(ModItems.MEMORY_SHARD.get());

    public ShattererRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(ModelLayers.PLAYER_SLIM)), 0.9F);
        this.addLayer(new Ghosts(this));
        this.addLayer(new Cracks(this));
    }

    @Override
    public ResourceLocation getTextureLocation(MemoryShattererEntity entity) {
        return SKIN;
    }

    static float size(MemoryShattererEntity e, float partialTick) {
        float formed = 1.0F - Mth.clamp((e.forming() - partialTick) / MemoryShattererEntity.FORM_TICKS, 0.0F, 1.0F);
        float eased = formed * formed * (3.0F - 2.0F * formed);
        return START + (FULL - START) * eased;
    }

    @Override
    protected void scale(MemoryShattererEntity entity, PoseStack pose, float partialTick) {
        float s = size(entity, partialTick);
        pose.scale(s, s, s);
    }

    @Override
    protected float getFlipDegrees(MemoryShattererEntity entity) {
        return 0.0F;
    }

    @Override
    public void render(MemoryShattererEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        // While it dies it shakes harder and harder.
        if (e.deathTime > 0) {
            RandomSource r = RandomSource.create(e.tickCount * 31L + (long) (partialTick * 4.0F));
            float k = e.deathTime / (float) MemoryShattererEntity.DEATH_TICKS * 0.25F;
            pose.translate((r.nextFloat() - 0.5F) * k, (r.nextFloat() - 0.5F) * k, (r.nextFloat() - 0.5F) * k);
        }
        this.model.tint = e.shielded() && e.forming() <= 0 && e.deathTime <= 0 ? 0.6F : 0.42F;
        super.render(e, yaw, partialTick, pose, buffers, LightTexture.FULL_BRIGHT);
        pose.popPose();
        orbit(e, partialTick, pose, buffers);
    }

    /** Pieces of him, circling. */
    private void orbit(MemoryShattererEntity e, float partialTick, PoseStack pose, MultiBufferSource buffers) {
        if (e.deathTime > 0) return;
        float s = size(e, partialTick);
        int n = 4 + e.stage() * 2;
        float t = (e.tickCount + partialTick) * (2.0F + e.stage() * 0.8F);
        for (int i = 0; i < n; i++) {
            float a = t + i * 360.0F / n;
            double r = 0.9D * s + Math.sin((t + i * 40.0F) * 0.03D) * 0.15D;
            double y = (0.9D + Math.sin(Math.toRadians(a * 2.0F + i * 50.0F)) * 0.5D) * s;
            pose.pushPose();
            pose.translate(Math.cos(Math.toRadians(a)) * r, y, Math.sin(Math.toRadians(a)) * r);
            pose.mulPose(Axis.YP.rotationDegrees(-a * 3.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(a * 2.0F));
            pose.scale(0.6F, 0.6F, 0.6F);
            Minecraft.getInstance().getItemRenderer().renderStatic(shard, ItemDisplayContext.GROUND, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, pose, buffers, e.level(), e.getId() * 31 + i);
            pose.popPose();
        }
    }

    /** His model, tinted, posed like something hanging from strings. */
    public static final class Model extends PlayerModel<MemoryShattererEntity> {
        float tint = 0.42F;
        float ghostR = 1.0F;
        float ghostG = 1.0F;
        float ghostB = 1.0F;
        float ghostA = -1.0F;

        Model(ModelPart root) {
            super(root, true);
        }

        @Override
        public void setupAnim(MemoryShattererEntity e, float limbSwing, float limbAmount, float age, float headYaw, float headPitch) {
            super.setupAnim(e, 0.0F, 0.0F, age, headYaw, headPitch);
            RandomSource r = RandomSource.create(e.getId() * 977L + (long) (age / 3.0F));
            float twitch = 0.12F + e.stage() * 0.06F;
            float spread = 1.15F + 0.1F * Mth.sin(age * 0.08F);
            this.rightArm.zRot = spread + (r.nextFloat() - 0.5F) * twitch;
            this.leftArm.zRot = -spread + (r.nextFloat() - 0.5F) * twitch;
            this.rightArm.xRot = -0.3F + (r.nextFloat() - 0.5F) * twitch;
            this.leftArm.xRot = -0.3F + (r.nextFloat() - 0.5F) * twitch;
            if (this.attackTime > 0.0F) {
                float swing = Mth.sin(this.attackTime * Mth.PI);
                this.rightArm.xRot = -1.8F * swing;
                this.leftArm.xRot = -1.8F * swing;
                this.rightArm.zRot *= 1.0F - swing;
                this.leftArm.zRot *= 1.0F - swing;
            }
            this.rightLeg.xRot = 0.15F + 0.1F * Mth.sin(age * 0.11F);
            this.leftLeg.xRot = 0.1F - 0.1F * Mth.sin(age * 0.11F);
            this.rightLeg.zRot = 0.05F;
            this.leftLeg.zRot = -0.05F;
            this.head.zRot = 0.35F + (r.nextFloat() - 0.5F) * twitch * 2.0F;
            this.head.xRot += (r.nextFloat() - 0.5F) * twitch;
            this.body.xRot = 0.12F;
            this.hat.copyFrom(this.head);
            this.rightSleeve.copyFrom(this.rightArm);
            this.leftSleeve.copyFrom(this.leftArm);
            this.rightPants.copyFrom(this.rightLeg);
            this.leftPants.copyFrom(this.leftLeg);
            this.jacket.copyFrom(this.body);
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
            if (ghostA >= 0.0F) {
                super.renderToBuffer(pose, vc, light, overlay, ghostR, ghostG, ghostB, ghostA);
            } else {
                super.renderToBuffer(pose, vc, light, overlay, r * tint, g * tint * 0.8F, b * tint * 0.95F, a);
            }
        }
    }

    /** Copies of him slipping off to either side, red and cyan, more often each stage. */
    private static final class Ghosts extends RenderLayer<MemoryShattererEntity, Model> {
        Ghosts(RenderLayerParent<MemoryShattererEntity, Model> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, MemoryShattererEntity e, float limbSwing,
                           float limbAmount, float partialTick, float age, float headYaw, float headPitch) {
            RandomSource r = RandomSource.create(e.getId() * 131L + (long) (age / 2.0F));
            boolean glitching = e.forming() > 0 || e.deathTime > 0 || r.nextInt(6 - Math.min(4, e.stage() + 1)) == 0;
            if (!glitching) return;
            Model m = this.getParentModel();
            VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(SKIN));
            float[][] colours = {{1.0F, 0.15F, 0.2F}, {0.2F, 0.9F, 1.0F}};
            for (float[] c : colours) {
                pose.pushPose();
                pose.translate((r.nextFloat() - 0.5F) * 0.35F, (r.nextFloat() - 0.5F) * 0.1F, (r.nextFloat() - 0.5F) * 0.35F);
                m.ghostR = c[0];
                m.ghostG = c[1];
                m.ghostB = c[2];
                m.ghostA = 0.35F;
                m.renderToBuffer(pose, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
                m.ghostA = -1.0F;
                pose.popPose();
            }
        }
    }

    /** The light coming through the cracks, and the eyes. */
    private static final class Cracks extends RenderLayer<MemoryShattererEntity, Model> {
        Cracks(RenderLayerParent<MemoryShattererEntity, Model> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, MemoryShattererEntity e, float limbSwing,
                           float limbAmount, float partialTick, float age, float headYaw, float headPitch) {
            float pulse = 0.65F + 0.35F * Mth.sin(age * (0.15F + e.stage() * 0.08F));
            if (e.deathTime > 0) pulse = 1.0F;
            Model m = this.getParentModel();
            m.ghostR = pulse;
            m.ghostG = pulse * (0.75F - e.stage() * 0.12F);
            m.ghostB = pulse * (0.8F - e.stage() * 0.15F);
            m.ghostA = 1.0F;
            m.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(CRACKS)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    1.0F, 1.0F, 1.0F, 1.0F);
            m.ghostA = -1.0F;
        }
    }
}
