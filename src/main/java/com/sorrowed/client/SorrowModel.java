package com.sorrowed.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * One animated model class for all the custom bodies. The geometry lives in ModModels; this
 * only knows the part names (head, body, right_arm, left_arm, right_leg, left_leg, optional ears)
 * and how each creature carries itself.
 */
public class SorrowModel<T extends LivingEntity> extends EntityModel<T> {
    public record Style(float headBow, float hunch, float rightArmRest, float leftArmRest, float armSway, float walk) {}

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final Style style;

    public SorrowModel(ModelPart root, Style style, boolean translucent) {
        super(translucent ? rl -> RenderType.entityTranslucent(rl) : rl -> RenderType.entityCutoutNoCull(rl));
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.style = style;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD + style.headBow();
        head.zRot = 0.0F;
        body.xRot = style.hunch();

        float swing = limbSwing * 0.6662F;
        float amount = 1.2F * limbSwingAmount * style.walk();
        rightLeg.xRot = Mth.cos(swing) * amount;
        leftLeg.xRot = Mth.cos(swing + Mth.PI) * amount;
        rightArm.xRot = Mth.cos(swing + Mth.PI) * amount * 0.7F + style.rightArmRest();
        leftArm.xRot = Mth.cos(swing) * amount * 0.7F + style.leftArmRest();

        float sway = Mth.cos(age * 0.09F) * 0.04F * style.armSway();
        rightArm.zRot = 0.05F + sway;
        leftArm.zRot = -0.05F - sway;

        // Ears (the Listener) twitch and the head tilts toward whatever it is hearing.
        if (head.hasChild("right_ear")) {
            head.zRot = Mth.sin(age * 0.05F) * 0.15F;
            head.getChild("right_ear").yRot = Mth.sin(age * 0.3F) * 0.12F;
            head.getChild("left_ear").yRot = -Mth.sin(age * 0.27F) * 0.12F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay,
                               float red, float green, float blue, float alpha) {
        root.render(pose, buffer, light, overlay, red, green, blue, alpha);
    }
}
