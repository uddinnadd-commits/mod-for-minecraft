package com.sorrowed.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sorrowed.block.LighthouseLampBlock;
import com.sorrowed.block.LighthouseLampBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * The lighthouse lamp, once it is lit: two long soft beams, back to back, turning slowly through
 * the water, wide and faint at the far end. Drawn even when the lamp itself is off screen.
 */
public class LighthouseBeamRenderer implements BlockEntityRenderer<LighthouseLampBlockEntity> {
    private static final ResourceLocation BEAM = new ResourceLocation("textures/entity/beacon_beam.png");
    private static final float LENGTH = 48.0F;
    private static final float NEAR = 0.3F;
    private static final float FAR = 6.0F;

    public LighthouseBeamRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(LighthouseLampBlockEntity lamp, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        if (lamp.getLevel() == null || !lamp.getBlockState().getValue(LighthouseLampBlock.LIT)) return;
        long time = lamp.getLevel().getGameTime();
        float angle = ((time % 480L) + partialTick) / 480.0F * 360.0F;
        VertexConsumer vc = buffers.getBuffer(RenderType.beaconBeam(BEAM, true));
        pose.pushPose();
        pose.translate(0.5D, 0.5D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        for (int dir : new int[] {1, -1}) {
            Matrix4f m = pose.last().pose();
            Matrix3f n = pose.last().normal();
            float end = dir * LENGTH;
            // A flat fan and an upright fan, crossed, so the beam has body from any angle.
            quad(vc, m, n, 0, 0, -NEAR, end, 0, -FAR, end, 0, FAR, 0, 0, NEAR);
            quad(vc, m, n, 0, -NEAR, 0, end, -FAR * 0.6F, 0, end, FAR * 0.6F, 0, 0, NEAR, 0);
        }
        pose.popPose();
    }

    /** Four corners: two at the lamp (bright), two at the far end (gone). Both sides. */
    private static void quad(VertexConsumer vc, Matrix4f m, Matrix3f n,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3) {
        float near = 0.42F;
        vertex(vc, m, n, x0, y0, z0, near, 0.0F, 0.0F);
        vertex(vc, m, n, x1, y1, z1, 0.0F, 0.0F, 1.0F);
        vertex(vc, m, n, x2, y2, z2, 0.0F, 0.1F, 1.0F);
        vertex(vc, m, n, x3, y3, z3, near, 0.1F, 0.0F);
        vertex(vc, m, n, x3, y3, z3, near, 0.1F, 0.0F);
        vertex(vc, m, n, x2, y2, z2, 0.0F, 0.1F, 1.0F);
        vertex(vc, m, n, x1, y1, z1, 0.0F, 0.0F, 1.0F);
        vertex(vc, m, n, x0, y0, z0, near, 0.0F, 0.0F);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float a, float u, float v) {
        vc.vertex(m, x, y, z).color(1.0F, 0.93F, 0.72F, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(n, 0.0F, 1.0F, 0.0F).endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(LighthouseLampBlockEntity lamp) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
