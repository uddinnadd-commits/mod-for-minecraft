package com.sorrowed.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sorrowed.entity.ScarecrowEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * One animated model class for all the custom bodies. The geometry comes from ModModels
 * (generated); this only knows the joint names and how each creature carries itself.
 * Limbs are jointed: elbows bend as arms swing, knees bend as legs lift, feet stay flat,
 * the chest breathes and the head drifts a little when the body is still.
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
    private final ModelPart rightForearm;
    private final ModelPart leftForearm;
    private final ModelPart rightHand;
    private final ModelPart leftHand;
    private final ModelPart rightShin;
    private final ModelPart leftShin;
    private final ModelPart rightFoot;
    private final ModelPart leftFoot;
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
        this.rightForearm = optional(rightArm, "right_forearm");
        this.leftForearm = optional(leftArm, "left_forearm");
        this.rightHand = rightForearm == null ? null : optional(rightForearm, "right_hand");
        this.leftHand = leftForearm == null ? null : optional(leftForearm, "left_hand");
        this.rightShin = optional(rightLeg, "right_shin");
        this.leftShin = optional(leftLeg, "left_shin");
        this.rightFoot = rightShin == null ? null : optional(rightShin, "right_foot");
        this.leftFoot = leftShin == null ? null : optional(leftShin, "left_foot");
        this.style = style;
    }

    private static ModelPart optional(ModelPart parent, String name) {
        return parent.hasChild(name) ? parent.getChild(name) : null;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);

        float still = 1.0F - Mth.clamp(limbSwingAmount * 4.0F, 0.0F, 1.0F);
        float breath = Mth.sin(age * 0.11F);

        // Head: look where it is looking, bowed by temperament, drifting a little when still.
        head.yRot += netHeadYaw * Mth.DEG_TO_RAD + Mth.sin(age * 0.023F) * 0.06F * still;
        head.xRot += headPitch * Mth.DEG_TO_RAD + style.headBow() + Mth.sin(age * 0.05F) * 0.015F;
        head.zRot += Mth.sin(age * 0.017F) * 0.02F * still;

        // Chest: hunch, and a slow breath that lifts the shoulders.
        body.xRot = style.hunch() + breath * 0.012F;
        body.y += breath * 0.25F;
        head.y += breath * 0.25F;
        rightArm.y += breath * 0.25F;
        leftArm.y += breath * 0.25F;

        float swing = limbSwing * 0.6662F;
        float amount = 1.2F * limbSwingAmount * style.walk();
        float legR = Mth.cos(swing) * amount;
        float legL = Mth.cos(swing + Mth.PI) * amount;
        rightLeg.xRot = legR;
        leftLeg.xRot = legL;

        // Knees bend on the back-swing and the foot stays flat on the ground.
        if (rightShin != null) {
            float kneeR = Mth.clamp(Mth.sin(swing) * amount, 0.0F, 2.0F) * 0.9F + 0.04F;
            float kneeL = Mth.clamp(Mth.sin(swing + Mth.PI) * amount, 0.0F, 2.0F) * 0.9F + 0.04F;
            rightShin.xRot = kneeR;
            leftShin.xRot = kneeL;
            if (rightFoot != null) {
                rightFoot.xRot = -(legR + kneeR) * 0.6F;
                leftFoot.xRot = -(legL + kneeL) * 0.6F;
            }
        }

        float armR = Mth.cos(swing + Mth.PI) * amount * 0.7F + style.rightArmRest();
        float armL = Mth.cos(swing) * amount * 0.7F + style.leftArmRest();
        rightArm.xRot = armR;
        leftArm.xRot = armL;

        float sway = Mth.cos(age * 0.09F) * 0.04F * style.armSway();
        rightArm.zRot = 0.06F + sway;
        leftArm.zRot = -0.06F - sway;

        // Elbows: a resting bend, more when the arm comes forward, held tight when the arm is raised.
        if (rightForearm != null) {
            rightForearm.xRot = elbow(armR, style.rightArmRest());
            leftForearm.xRot = elbow(armL, style.leftArmRest());
            if (rightHand != null) {
                rightHand.xRot = 0.12F;
                leftHand.xRot = 0.12F;
            }
        }

        // Ears (Hark) twitch and the head tilts toward whatever it is hearing.
        if (head.hasChild("right_ear")) {
            head.zRot += Mth.sin(age * 0.05F) * 0.15F;
            head.getChild("right_ear").yRot = Mth.sin(age * 0.3F) * 0.12F;
            head.getChild("left_ear").yRot = -Mth.sin(age * 0.27F) * 0.12F;
        }
        // Antennae (the Carrier) quiver.
        if (head.hasChild("right_antenna")) {
            head.getChild("right_antenna").zRot += Mth.sin(age * 0.7F) * 0.05F;
            head.getChild("left_antenna").zRot -= Mth.sin(age * 0.63F) * 0.05F;
        }
        // The Scarecrow on its pole: arms out along the crossbar, legs straight, only the head moves.
        if (entity instanceof ScarecrowEntity sc && !sc.isDown()) {
            rightArm.xRot = 0.0F;
            leftArm.xRot = 0.0F;
            rightArm.zRot = 1.45F;
            leftArm.zRot = -1.45F;
            if (rightForearm != null) {
                rightForearm.xRot = 0.0F;
                leftForearm.xRot = 0.0F;
            }
            rightLeg.xRot = 0.0F;
            leftLeg.xRot = 0.0F;
            if (rightShin != null) {
                rightShin.xRot = 0.0F;
                leftShin.xRot = 0.0F;
                if (rightFoot != null) {
                    rightFoot.xRot = 0.0F;
                    leftFoot.xRot = 0.0F;
                }
            }
            body.xRot = 0.0F;
            body.y -= 10.0F;   // hung up on the pole, feet off the ground
            head.y -= 10.0F;
            rightArm.y -= 10.0F;
            leftArm.y -= 10.0F;
            rightLeg.y -= 10.0F;
            leftLeg.y -= 10.0F;
        }
    }

    private static float elbow(float upper, float rest) {
        float forward = Math.max(0.0F, -upper);
        float base = rest < -0.5F ? -1.1F : -0.18F;   // arms held up (a book, a hammer) fold the elbow hard
        return base - 0.55F * forward;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay,
                               float red, float green, float blue, float alpha) {
        root.render(pose, buffer, light, overlay, red, green, blue, alpha);
    }
}
