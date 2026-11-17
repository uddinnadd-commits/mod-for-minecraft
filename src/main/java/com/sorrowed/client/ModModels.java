package com.sorrowed.client;

import com.sorrowed.Sorrowed;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * The bodies. Coordinates are in 1/16 blocks, y down, feet at y=24. Every layer is a 64x64 texture;
 * tools/gen_textures.py paints the matching UV zones, so keep the two in step.
 */
public final class ModModels {
    private ModModels() {}

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(new ResourceLocation(Sorrowed.MODID, name), "main");
    }

    public static final ModelLayerLocation ECHO = layer("echo");
    public static final ModelLayerLocation HOLLOW = layer("hollow");
    public static final ModelLayerLocation LISTENER = layer("listener");
    public static final ModelLayerLocation MASON = layer("mason");
    public static final ModelLayerLocation ARCHIVIST = layer("archivist");
    public static final ModelLayerLocation NEIGHBOUR = layer("neighbour");
    public static final ModelLayerLocation CARRIER = layer("carrier");

    // headBow, hunch, rightArmRest, leftArmRest, armSway, walk
    public static final SorrowModel.Style ECHO_STYLE = new SorrowModel.Style(0.25F, 0.0F, 0.0F, 0.0F, 0.6F, 0.6F);
    public static final SorrowModel.Style HOLLOW_STYLE = new SorrowModel.Style(0.2F, 0.3F, 0.15F, 0.15F, 1.0F, 0.7F);
    public static final SorrowModel.Style LISTENER_STYLE = new SorrowModel.Style(0.1F, 0.15F, 0.05F, 0.05F, 0.3F, 0.9F);
    public static final SorrowModel.Style MASON_STYLE = new SorrowModel.Style(0.0F, 0.0F, -0.8F, 0.0F, 0.2F, 1.0F);
    public static final SorrowModel.Style ARCHIVIST_STYLE = new SorrowModel.Style(0.45F, 0.08F, 0.0F, -1.3F, 0.15F, 0.35F);
    public static final SorrowModel.Style NEIGHBOUR_STYLE = new SorrowModel.Style(0.0F, 0.0F, 0.0F, 0.0F, 0.1F, 1.0F);
    public static final SorrowModel.Style CARRIER_STYLE = new SorrowModel.Style(0.0F, 0.05F, 0.0F, 0.0F, 0.5F, 0.8F);

    /** The Vigil: thin, 2.3 blocks tall, head bowed, arms that hang to the knees. */
    public static LayerDefinition echoLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3.0F, -7.0F, -3.0F, 6.0F, 7.0F, 6.0F), PartPose.offset(0.0F, -6.0F, 0.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-3.0F, 0.0F, -2.0F, 6.0F, 15.0F, 4.0F), PartPose.offset(0.0F, -6.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(24, 16)
                .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 18.0F, 3.0F), PartPose.offset(-4.5F, -5.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(24, 16).mirror()
                .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 18.0F, 3.0F), PartPose.offset(4.5F, -5.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(40, 16)
                .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 15.0F, 3.0F), PartPose.offset(-1.5F, 9.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(40, 16).mirror()
                .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 15.0F, 3.0F), PartPose.offset(1.5F, 9.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** The Unsaid: hunched, faceless, 2.6 blocks tall, arms long enough to drag. */
    public static LayerDefinition hollowLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3.5F, -9.0F, -3.5F, 7.0F, 9.0F, 7.0F), PartPose.offset(0.0F, -8.0F, 1.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 18)
                .addBox(-4.0F, 0.0F, -3.0F, 8.0F, 15.0F, 6.0F), PartPose.offset(0.0F, -8.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(36, 0)
                .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 24.0F, 3.0F), PartPose.offset(-6.0F, -7.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(36, 0).mirror()
                .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 24.0F, 3.0F), PartPose.offset(6.0F, -7.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 40)
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 17.0F, 4.0F), PartPose.offset(-2.5F, 7.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 40).mirror()
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 17.0F, 4.0F), PartPose.offset(2.5F, 7.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Hark: a huge round head with flat ears on a thin stooped body, 2.7 blocks tall. */
    public static LayerDefinition listenerLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-5.0F, -10.0F, -5.0F, 10.0F, 10.0F, 10.0F), PartPose.offset(0.0F, -9.0F, 0.0F));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(44, 0)
                .addBox(-1.0F, -4.0F, -2.5F, 1.0F, 8.0F, 5.0F), PartPose.offset(-5.0F, -5.0F, 0.0F));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(44, 0).mirror()
                .addBox(0.0F, -4.0F, -2.5F, 1.0F, 8.0F, 5.0F), PartPose.offset(5.0F, -5.0F, 0.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 22)
                .addBox(-3.0F, 0.0F, -2.0F, 6.0F, 16.0F, 4.0F), PartPose.offset(0.0F, -9.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(24, 22)
                .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 22.0F, 3.0F), PartPose.offset(-4.5F, -8.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(24, 22).mirror()
                .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 22.0F, 3.0F), PartPose.offset(4.5F, -8.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(40, 22)
                .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 17.0F, 3.0F), PartPose.offset(-2.0F, 7.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(40, 22).mirror()
                .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 17.0F, 3.0F), PartPose.offset(2.0F, 7.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** The Finisher: broad, stocky, hard hat, tool belt, and a hammer held out in front. */
    public static LayerDefinition masonLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
        head.addOrReplaceChild("hat_brim", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-5.0F, -9.0F, -5.0F, 10.0F, 2.0F, 10.0F), PartPose.ZERO);
        head.addOrReplaceChild("hat_crown", CubeListBuilder.create().texOffs(0, 28)
                .addBox(-4.0F, -12.0F, -4.0F, 8.0F, 3.0F, 8.0F), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 40)
                .addBox(-5.0F, 0.0F, -3.0F, 10.0F, 12.0F, 6.0F), PartPose.ZERO);
        body.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(0, 40)
                .addBox(-5.5F, 9.0F, -3.5F, 11.0F, 2.0F, 7.0F), PartPose.ZERO);
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 0)
                .addBox(-2.0F, -2.0F, -2.0F, 4.0F, 13.0F, 4.0F), PartPose.offset(-7.0F, 2.0F, 0.0F));
        rightArm.addOrReplaceChild("hammer_handle", CubeListBuilder.create().texOffs(40, 40)
                .addBox(-0.5F, 8.0F, -7.0F, 1.0F, 1.0F, 8.0F), PartPose.ZERO);
        rightArm.addOrReplaceChild("hammer_head", CubeListBuilder.create().texOffs(40, 50)
                .addBox(-1.5F, 6.5F, -9.0F, 3.0F, 4.0F, 3.0F), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 0).mirror()
                .addBox(-2.0F, -2.0F, -2.0F, 4.0F, 13.0F, 4.0F), PartPose.offset(7.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(40, 18)
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-2.5F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(40, 18).mirror()
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(2.5F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** The Archivist: 2.7 blocks of robe, a long neck, a small bowed head, and a book held up to read. */
    public static LayerDefinition archivistLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3.0F, -6.0F, -3.0F, 6.0F, 6.0F, 6.0F), PartPose.offset(0.0F, -14.0F, 0.0F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 14)
                .addBox(-3.0F, 0.0F, -2.0F, 6.0F, 14.0F, 4.0F), PartPose.offset(0.0F, -8.0F, 0.0F));
        body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(24, 0)
                .addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(24, 14)
                .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 18.0F, 2.0F), PartPose.offset(-4.0F, -7.0F, 0.0F));
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(24, 14).mirror()
                .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 18.0F, 2.0F), PartPose.offset(4.0F, -7.0F, 0.0F));
        leftArm.addOrReplaceChild("book", CubeListBuilder.create().texOffs(44, 0)
                .addBox(-3.0F, 14.0F, -2.5F, 4.0F, 5.0F, 1.0F), PartPose.ZERO);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(36, 14)
                .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 18.0F, 2.0F), PartPose.offset(-1.5F, 6.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(36, 14).mirror()
                .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 18.0F, 2.0F), PartPose.offset(1.5F, 6.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** The Neighbour: an ordinary person in a long coat and a wide hat, two blocks tall, face in shadow. */
    public static LayerDefinition neighbourLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(0.0F, -2.0F, 0.0F));
        head.addOrReplaceChild("hat_brim", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-5.0F, -8.0F, -5.0F, 10.0F, 1.0F, 10.0F), PartPose.ZERO);
        head.addOrReplaceChild("hat_crown", CubeListBuilder.create().texOffs(0, 28)
                .addBox(-3.0F, -11.0F, -3.0F, 6.0F, 3.0F, 6.0F), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 38)
                .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F), PartPose.offset(0.0F, -2.0F, 0.0F));
        body.addOrReplaceChild("coat", CubeListBuilder.create().texOffs(24, 38)
                .addBox(-5.0F, 11.0F, -3.0F, 10.0F, 6.0F, 6.0F), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 0)
                .addBox(-2.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-6.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 0).mirror()
                .addBox(-2.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(6.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(40, 16)
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 14.0F, 4.0F), PartPose.offset(-2.0F, 10.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(40, 16).mirror()
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 14.0F, 4.0F), PartPose.offset(2.0F, 10.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** The Carrier: a television set for a head, two bent antennae, and a thin suit underneath. 2.4 blocks. */
    public static LayerDefinition carrierLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-5.0F, -8.0F, -5.0F, 10.0F, 8.0F, 10.0F), PartPose.offset(0.0F, -8.0F, 0.0F));
        head.addOrReplaceChild("right_antenna", CubeListBuilder.create().texOffs(40, 0)
                .addBox(-0.5F, -6.0F, -0.5F, 1.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(-2.0F, -8.0F, 0.0F, 0.0F, 0.0F, 0.5F));
        head.addOrReplaceChild("left_antenna", CubeListBuilder.create().texOffs(40, 0)
                .addBox(-0.5F, -6.0F, -0.5F, 1.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(2.0F, -8.0F, 0.0F, 0.0F, 0.0F, -0.5F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 18)
                .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F), PartPose.offset(0.0F, -8.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(24, 18)
                .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 14.0F, 3.0F), PartPose.offset(-5.5F, -7.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(24, 18).mirror()
                .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 14.0F, 3.0F), PartPose.offset(5.5F, -7.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(36, 18)
                .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 20.0F, 3.0F), PartPose.offset(-2.0F, 4.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(36, 18).mirror()
                .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 20.0F, 3.0F), PartPose.offset(2.0F, 4.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
