package art.arcane.thaumcraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/** TC6 ModelPech (128x64): a hunched goblin with a huge backpack and wobbling jowls that flap while it mumbles. */
public final class PechModel extends EntityModel<PechModel.State> implements ArmedModel<PechModel.State> {
    public static class State extends ArmedEntityRenderState { public float mumble; public int type; public boolean riding, crouching; }
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("thaumcraft", "pech"), "main");
    private final ModelPart body, rightLeg, leftLeg, head, jowls, lowerPack, upperPack, rightArm, leftArm;

    public PechModel(ModelPart root) {
        super(root);
        body = root.getChild("body"); rightLeg = root.getChild("right_leg"); leftLeg = root.getChild("left_leg");
        head = root.getChild("head"); jowls = root.getChild("jowls"); lowerPack = root.getChild("lower_pack"); upperPack = root.getChild("upper_pack");
        rightArm = root.getChild("right_arm"); leftArm = root.getChild("left_arm");
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(34, 12).mirror().addBox(-3, 0, 0, 6, 10, 6), PartPose.offsetAndRotation(0, 9, -3, 0.3129957f, 0, 0));
        r.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(35, 1).mirror().addBox(-2.9f, 0, 0, 3, 6, 3), PartPose.offset(0, 18, 0));
        r.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(35, 1).mirror().addBox(-0.1f, 0, 0, 3, 6, 3), PartPose.offset(0, 18, 0));
        r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(2, 11).mirror().addBox(-3.5f, -5, -5, 7, 5, 5), PartPose.offset(0, 8, 0));
        r.addOrReplaceChild("jowls", CubeListBuilder.create().texOffs(1, 21).mirror().addBox(-4, -1, -6, 8, 3, 5), PartPose.offset(0, 8, 0));
        r.addOrReplaceChild("lower_pack", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-5, 0, 0, 10, 5, 5), PartPose.offsetAndRotation(0, 10, 3.5f, 0.3013602f, 0, 0));
        r.addOrReplaceChild("upper_pack", CubeListBuilder.create().texOffs(64, 1).mirror().addBox(-7.5f, -14, 0, 15, 14, 11), PartPose.offsetAndRotation(0, 10, 3, 0.4537856f, 0, 0));
        r.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(52, 2).addBox(-2, 0, -1, 2, 6, 2), PartPose.offset(-3, 10, -1));
        r.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(52, 2).mirror().addBox(0, 0, -1, 2, 6, 2), PartPose.offset(3, 10, -1));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float par1 = s.walkAnimationPos, par2 = s.walkAnimationSpeed, par3 = s.ageInTicks;
        head.yRot = s.yRot / 57.295776f;
        head.xRot = s.xRot / 57.295776f;
        jowls.yRot = head.yRot;
        jowls.xRot = head.xRot + (0.2617994f + Mth.cos(par1 * 0.6662f) * par2 * 0.25f) + 0.34906587f * Math.abs(Mth.sin(s.mumble / 8.0f));
        rightArm.xRot = Mth.cos(par1 * 0.6662f + (float) Math.PI) * 2.0f * par2 * 0.5f;
        leftArm.xRot = Mth.cos(par1 * 0.6662f) * 2.0f * par2 * 0.5f;
        rightArm.zRot = 0.0f;
        leftArm.zRot = 0.0f;
        rightLeg.xRot = Mth.cos(par1 * 0.6662f) * 1.4f * par2;
        leftLeg.xRot = Mth.cos(par1 * 0.6662f + (float) Math.PI) * 1.4f * par2;
        rightLeg.yRot = 0.0f;
        leftLeg.yRot = 0.0f;
        lowerPack.yRot = Mth.cos(par1 * 0.6662f) * 2.0f * par2 * 0.125f;
        lowerPack.zRot = Mth.cos(par1 * 0.6662f) * 2.0f * par2 * 0.125f;
        if (s.riding) {
            rightArm.xRot -= 0.62831855f; leftArm.xRot -= 0.62831855f;
            rightLeg.xRot = -1.2566371f; leftLeg.xRot = -1.2566371f;
            rightLeg.yRot = 0.31415927f; leftLeg.yRot = -0.31415927f;
        }
        rightArm.yRot = 0.0f;
        leftArm.yRot = 0.0f;
        float swing = s.attackTime;
        if (swing > 0.0f) {
            float f6 = 1.0f - swing;
            f6 *= f6; f6 *= f6;
            f6 = 1.0f - f6;
            float f7 = Mth.sin(f6 * (float) Math.PI);
            float f8 = Mth.sin(swing * (float) Math.PI) * -(head.xRot - 0.7f) * 0.75f;
            rightArm.xRot -= (float) (f7 * 1.2 + f8);
            rightArm.zRot = Mth.sin(swing * (float) Math.PI) * -0.4f;
        }
        if (s.crouching) { rightArm.xRot += 0.4f; leftArm.xRot += 0.4f; }
        rightArm.zRot += Mth.cos(par3 * 0.09f) * 0.05f + 0.05f;
        leftArm.zRot -= Mth.cos(par3 * 0.09f) * 0.05f + 0.05f;
        rightArm.xRot += Mth.sin(par3 * 0.067f) * 0.05f;
        leftArm.xRot -= Mth.sin(par3 * 0.067f) * 0.05f;
    }

    /** LayerHeldItemPech: postRenderArm then (0, -0.1, 0.0625), bows a little further in. */
    @Override
    public void translateToHand(State s, HumanoidArm arm, PoseStack ps) {
        (arm == HumanoidArm.LEFT ? leftArm : rightArm).translateAndRotate(ps);
        ps.translate(0.0f, -0.1f, 0.0625f);
        if ((arm == HumanoidArm.LEFT ? s.leftHandItemStack : s.rightHandItemStack).getItem() instanceof net.minecraft.world.item.BowItem) ps.translate(-0.07500000298023224, -0.1, 0.0);
    }
}
