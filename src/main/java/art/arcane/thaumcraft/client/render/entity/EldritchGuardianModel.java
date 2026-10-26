package art.arcane.thaumcraft.client.render.entity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** TC6 ModelEldritchGuardian (128x64): hooded, armoured torso with floating arms, swaying cloak and leg panels. */
public final class EldritchGuardianModel extends EntityModel<EldritchGuardianModel.State> {
    public static class State extends LivingEntityRenderState { public float armLiftL, armLiftR; }
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("thaumcraft", "eldritch_guardian"), "main");

    private final ModelPart hood1, armL1, armR1, legpanelC1, legpanelC2, legpanelC3, cloak1, cloak2, cloak3,
            sidepanelL2, sidepanelL3, sidepanelL4, sidepanelR2, sidepanelR3, sidepanelR4;

    public EldritchGuardianModel(ModelPart root) {
        super(root);
        hood1 = root.getChild("hood1");
        armL1 = root.getChild("arml1");
        armR1 = root.getChild("armr1");
        legpanelC1 = root.getChild("legpanelc1"); legpanelC2 = legpanelC1.getChild("legpanelc2"); legpanelC3 = legpanelC2.getChild("legpanelc3");
        cloak1 = root.getChild("cloak1"); cloak2 = cloak1.getChild("cloak2"); cloak3 = cloak2.getChild("cloak3");
        sidepanelL2 = root.getChild("sidepanell2"); sidepanelL3 = sidepanelL2.getChild("sidepanell3"); sidepanelL4 = sidepanelL3.getChild("sidepanell4");
        sidepanelR2 = root.getChild("sidepanelr2"); sidepanelR3 = sidepanelR2.getChild("sidepanelr3"); sidepanelR4 = sidepanelR3.getChild("sidepanelr4");
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_BeltR = root.addOrReplaceChild("beltr", CubeListBuilder.create().texOffs(76, 44).addBox(-5.0f, 4.0f, -3.0f, 1, 3, 6), PartPose.offsetAndRotation(0.0f, -6.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        PartDefinition p_Mbelt = root.addOrReplaceChild("mbelt", CubeListBuilder.create().texOffs(56, 55).addBox(-4.0f, 8.0f, -3.0f, 8, 4, 1), PartPose.offsetAndRotation(0.0f, -6.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        PartDefinition p_MbeltL = root.addOrReplaceChild("mbeltl", CubeListBuilder.create().texOffs(76, 44).addBox(4.0f, 8.0f, -3.0f, 1, 3, 6), PartPose.offsetAndRotation(0.0f, -6.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        PartDefinition p_MbeltR = root.addOrReplaceChild("mbeltr", CubeListBuilder.create().texOffs(76, 44).addBox(-5.0f, 8.0f, -3.0f, 1, 3, 6), PartPose.offsetAndRotation(0.0f, -6.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        PartDefinition p_BeltL = root.addOrReplaceChild("beltl", CubeListBuilder.create().texOffs(76, 44).addBox(4.0f, 4.0f, -3.0f, 1, 3, 6), PartPose.offsetAndRotation(0.0f, -6.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        PartDefinition p_Chestplate = root.addOrReplaceChild("chestplate", CubeListBuilder.create().texOffs(56, 45).addBox(-4.0f, 1.0f, -4.0f, 8, 7, 2), PartPose.offsetAndRotation(0.0f, -6.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        PartDefinition p_Hood1 = root.addOrReplaceChild("hood1", CubeListBuilder.create().texOffs(40, 12).addBox(-4.0f, -8.0f, -4.0f, 8, 8, 8), PartPose.offsetAndRotation(0.0f, -6.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        PartDefinition p_Hood2 = p_Hood1.addOrReplaceChild("hood2", CubeListBuilder.create().texOffs(36, 28).addBox(-3.5f, -8.7f, 2.0f, 7, 7, 3), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, -0.2268928f, 0.0f, 0.0f));
        PartDefinition p_Hood3 = p_Hood1.addOrReplaceChild("hood3", CubeListBuilder.create().texOffs(22, 19).addBox(-3.0f, -9.0f, 2.5f, 6, 6, 3), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, -0.3490659f, 0.0f, 0.0f));
        PartDefinition p_Hood4 = p_Hood1.addOrReplaceChild("hood4", CubeListBuilder.create().texOffs(40, 4).addBox(-2.5f, -9.7f, 3.5f, 5, 5, 3), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, -0.5759587f, 0.0f, 0.0f));
        PartDefinition p_Backplate = root.addOrReplaceChild("backplate", CubeListBuilder.create().texOffs(36, 45).addBox(-4.0f, 1.0f, 2.0f, 8, 11, 2), PartPose.offsetAndRotation(0.0f, -6.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        PartDefinition p_ShoulderplateTopR = root.addOrReplaceChild("shoulderplatetopr", CubeListBuilder.create().texOffs(110, 37).addBox(-5.5f, -2.5f, -3.5f, 2, 1, 7), PartPose.offsetAndRotation(-5.0f, -4.0f, 0.0f, -0.3665191f, 0.3141593f, 0.4363323f));
        PartDefinition p_ShoulderplateR1 = root.addOrReplaceChild("shoulderplater1", CubeListBuilder.create().texOffs(110, 45).addBox(3.5f, -1.5f, -3.5f, 1, 4, 7), PartPose.offsetAndRotation(5.0f, -4.0f, 0.0f, -0.3665191f, -0.3141593f, -0.4363323f));
        PartDefinition p_ShoulderplateR2 = root.addOrReplaceChild("shoulderplater2", CubeListBuilder.create().texOffs(94, 45).addBox(-3.5f, 1.5f, -3.5f, 1, 3, 7), PartPose.offsetAndRotation(-5.0f, -4.0f, 0.0f, -0.3665191f, 0.3141593f, 0.4363323f));
        PartDefinition p_ShoulderplateR3 = root.addOrReplaceChild("shoulderplater3", CubeListBuilder.create().texOffs(94, 45).addBox(-2.5f, 3.5f, -3.5f, 1, 3, 7), PartPose.offsetAndRotation(-5.0f, -4.0f, 0.0f, -0.3665191f, 0.3141593f, 0.4363323f));
        PartDefinition p_ShoulderR = root.addOrReplaceChild("shoulderr", CubeListBuilder.create().texOffs(56, 35).addBox(-3.5f, -2.5f, -2.5f, 5, 5, 5), PartPose.offsetAndRotation(-5.0f, -4.0f, 0.0f, -0.3665191f, 0.122173f, 0.0349066f));
        PartDefinition p_ArmL1 = root.addOrReplaceChild("arml1", CubeListBuilder.create().texOffs(72, 8).addBox(-1.0f, 2.5f, -1.5f, 4, 10, 5), PartPose.offsetAndRotation(5.0f, -4.0f, 0.0f, -0.9599311f, -0.1047198f, -0.1919862f));
        PartDefinition p_ArmL2 = p_ArmL1.addOrReplaceChild("arml2", CubeListBuilder.create().texOffs(76, 28).addBox(-1.0f, 9.5f, 3.5f, 4, 3, 3), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        PartDefinition p_ArmL3 = p_ArmL1.addOrReplaceChild("arml3", CubeListBuilder.create().texOffs(76, 23).addBox(-1.0f, 6.5f, 3.5f, 4, 3, 2), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        PartDefinition p_ArmR1 = root.addOrReplaceChild("armr1", CubeListBuilder.create().texOffs(72, 8).addBox(-3.0f, 2.5f, -1.5f, 4, 10, 5), PartPose.offsetAndRotation(-5.0f, -4.0f, 0.0f, -0.9599311f, 0.1047198f, 0.1919862f));
        PartDefinition p_ArmR2 = p_ArmR1.addOrReplaceChild("armr2", CubeListBuilder.create().texOffs(76, 28).addBox(-3.0f, 9.5f, 3.5f, 4, 3, 3), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        PartDefinition p_ArmR3 = p_ArmR1.addOrReplaceChild("armr3", CubeListBuilder.create().texOffs(76, 23).addBox(-3.0f, 6.5f, 3.5f, 4, 3, 2), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        PartDefinition p_ShoulderL = root.addOrReplaceChild("shoulderl", CubeListBuilder.create().texOffs(56, 35).mirror().addBox(-1.5f, -2.5f, -2.5f, 5, 5, 5), PartPose.offsetAndRotation(5.0f, -4.0f, 0.0f, -0.3665191f, -0.122173f, -0.0349066f));
        PartDefinition p_ShoulderplateLtop = root.addOrReplaceChild("shoulderplateltop", CubeListBuilder.create().texOffs(110, 37).addBox(3.5f, -2.5f, -3.5f, 2, 1, 7), PartPose.offsetAndRotation(5.0f, -4.0f, 0.0f, -0.3665191f, -0.3141593f, -0.4363323f));
        PartDefinition p_ShoulderplateL1 = root.addOrReplaceChild("shoulderplatel1", CubeListBuilder.create().texOffs(110, 45).addBox(-4.5f, -1.5f, -3.5f, 1, 4, 7), PartPose.offsetAndRotation(-5.0f, -4.0f, 0.0f, -0.3665191f, 0.3141593f, 0.4363323f));
        PartDefinition p_ShoulderplateL2 = root.addOrReplaceChild("shoulderplatel2", CubeListBuilder.create().texOffs(94, 45).addBox(2.5f, 1.5f, -3.5f, 1, 3, 7), PartPose.offsetAndRotation(5.0f, -4.0f, 0.0f, -0.3665191f, -0.3141593f, -0.4363323f));
        PartDefinition p_ShoulderplateL3 = root.addOrReplaceChild("shoulderplatel3", CubeListBuilder.create().texOffs(94, 45).addBox(1.5f, 3.5f, -3.5f, 1, 3, 7), PartPose.offsetAndRotation(5.0f, -4.0f, 0.0f, -0.3665191f, -0.3141593f, -0.4363323f));
        PartDefinition p_LegpanelR4 = root.addOrReplaceChild("legpanelr4", CubeListBuilder.create().texOffs(0, 43).addBox(-3.0f, 0.5f, -3.5f, 2, 3, 1), PartPose.offsetAndRotation(-2.0f, 6.0f, 0.0f, -0.4363323f, 0.0f, 0.0f));
        PartDefinition p_LegpanelR5 = root.addOrReplaceChild("legpanelr5", CubeListBuilder.create().texOffs(0, 47).addBox(-3.0f, 2.5f, -2.5f, 2, 3, 1), PartPose.offsetAndRotation(-2.0f, 6.0f, 0.0f, -0.4363323f, 0.0f, 0.0f));
        PartDefinition p_LegpanelR6 = root.addOrReplaceChild("legpanelr6", CubeListBuilder.create().texOffs(6, 43).addBox(-3.0f, 4.5f, -1.5f, 2, 3, 1), PartPose.offsetAndRotation(-2.0f, 6.0f, 0.0f, -0.4363323f, 0.0f, 0.0f));
        PartDefinition p_BackpanelR1 = root.addOrReplaceChild("backpanelr1", CubeListBuilder.create().texOffs(0, 18).addBox(-3.0f, 0.5f, 2.5f, 5, 3, 1), PartPose.offsetAndRotation(-2.0f, 6.0f, 0.0f, 0.4363323f, 0.0f, 0.0f));
        PartDefinition p_BackpanelR2 = root.addOrReplaceChild("backpanelr2", CubeListBuilder.create().texOffs(0, 18).addBox(-3.0f, 2.5f, 1.5f, 5, 3, 1), PartPose.offsetAndRotation(-2.0f, 6.0f, 0.0f, 0.4363323f, 0.0f, 0.0f));
        PartDefinition p_BackpanelR3 = root.addOrReplaceChild("backpanelr3", CubeListBuilder.create().texOffs(0, 18).addBox(-3.0f, 4.5f, 0.5f, 5, 3, 1), PartPose.offsetAndRotation(-2.0f, 6.0f, 0.0f, 0.4363323f, 0.0f, 0.0f));
        PartDefinition p_BackpanelL3 = root.addOrReplaceChild("backpanell3", CubeListBuilder.create().texOffs(0, 18).addBox(-2.0f, 4.5f, 0.5f, 5, 3, 1), PartPose.offsetAndRotation(2.0f, 6.0f, 0.0f, 0.4363323f, 0.0f, 0.0f));
        PartDefinition p_LegpanelL4 = root.addOrReplaceChild("legpanell4", CubeListBuilder.create().texOffs(0, 43).addBox(1.0f, 0.5f, -3.5f, 2, 3, 1), PartPose.offsetAndRotation(2.0f, 6.0f, 0.0f, -0.4363323f, 0.0f, 0.0f));
        PartDefinition p_LegpanelL5 = root.addOrReplaceChild("legpanell5", CubeListBuilder.create().texOffs(0, 47).addBox(1.0f, 2.5f, -2.5f, 2, 3, 1), PartPose.offsetAndRotation(2.0f, 6.0f, 0.0f, -0.4363323f, 0.0f, 0.0f));
        PartDefinition p_LegpanelL6 = root.addOrReplaceChild("legpanell6", CubeListBuilder.create().texOffs(6, 43).addBox(1.0f, 4.5f, -1.5f, 2, 3, 1), PartPose.offsetAndRotation(2.0f, 6.0f, 0.0f, -0.4363323f, 0.0f, 0.0f));
        PartDefinition p_BackpanelL1 = root.addOrReplaceChild("backpanell1", CubeListBuilder.create().texOffs(0, 18).addBox(-2.0f, 0.5f, 2.5f, 5, 3, 1), PartPose.offsetAndRotation(2.0f, 6.0f, 0.0f, 0.4363323f, 0.0f, 0.0f));
        PartDefinition p_BackpanelL2 = root.addOrReplaceChild("backpanell2", CubeListBuilder.create().texOffs(0, 18).addBox(-2.0f, 2.5f, 1.5f, 5, 3, 1), PartPose.offsetAndRotation(2.0f, 6.0f, 0.0f, 0.4363323f, 0.0f, 0.0f));
        PartDefinition p_SidepanelL1 = root.addOrReplaceChild("sidepanell1", CubeListBuilder.create().texOffs(0, 22).addBox(1.5f, 0.5f, -2.5f, 1, 4, 5), PartPose.offsetAndRotation(2.0f, 6.0f, 0.0f, 0.0f, 0.0f, -0.4363323f));
        PartDefinition p_SidepanelR1 = root.addOrReplaceChild("sidepanelr1", CubeListBuilder.create().texOffs(0, 22).addBox(-2.5f, 0.5f, -2.5f, 1, 4, 5), PartPose.offsetAndRotation(-2.0f, 6.0f, 0.0f, 0.0f, 0.0f, 0.4363323f));
        PartDefinition p_SidepanelR2 = root.addOrReplaceChild("sidepanelr2", CubeListBuilder.create().texOffs(0, 54).addBox(0.0f, 0.0f, -0.5f, 1, 5, 5), PartPose.offsetAndRotation(-4.5f, 9.5f, -2.0f, 0.0f, 0.0f, 0.122173f));
        PartDefinition p_SidepanelR3 = p_SidepanelR2.addOrReplaceChild("sidepanelr3", CubeListBuilder.create().texOffs(0, 35).addBox(0.0f, 0.0f, -0.5f, 1, 3, 5), PartPose.offsetAndRotation(0.0f, 5.0f, 0.0f, 0.0f, 0.0f, 0.296706f));
        PartDefinition p_SidepanelR4 = p_SidepanelR3.addOrReplaceChild("sidepanelr4", CubeListBuilder.create().texOffs(24, 35).addBox(0.0f, 0.0f, -0.5f, 1, 3, 5), PartPose.offsetAndRotation(0.0f, 3.0f, 0.0f, 0.0f, 0.0f, 0.5235988f));
        PartDefinition p_SidepanelL2 = root.addOrReplaceChild("sidepanell2", CubeListBuilder.create().texOffs(0, 54).addBox(0.0f, 0.0f, -0.5f, 1, 5, 5), PartPose.offsetAndRotation(4.5f, 9.5f, -2.0f, 0.0f, 0.0f, -0.122173f));
        PartDefinition p_SidepanelL3 = p_SidepanelL2.addOrReplaceChild("sidepanell3", CubeListBuilder.create().texOffs(0, 35).addBox(0.0f, 0.0f, -0.5f, 1, 3, 5), PartPose.offsetAndRotation(0.0f, 5.0f, 0.0f, 0.0f, 0.0f, -0.296706f));
        PartDefinition p_SidepanelL4 = p_SidepanelL3.addOrReplaceChild("sidepanell4", CubeListBuilder.create().texOffs(24, 35).addBox(0.0f, 0.0f, -0.5f, 1, 3, 5), PartPose.offsetAndRotation(0.0f, 3.0f, 0.0f, 0.0f, 0.0f, -0.5235988f));
        PartDefinition p_LegpanelC1 = root.addOrReplaceChild("legpanelc1", CubeListBuilder.create().texOffs(16, 45).addBox(-3.0f, 0.0f, -0.5f, 6, 8, 1), PartPose.offsetAndRotation(0.0f, 5.5f, -3.0f, 0.0f, 0.0f, 0.0f));
        PartDefinition p_LegpanelC2 = p_LegpanelC1.addOrReplaceChild("legpanelc2", CubeListBuilder.create().texOffs(16, 54).addBox(-3.0f, 0.0f, -0.5f, 6, 4, 1), PartPose.offsetAndRotation(0.0f, 8.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        PartDefinition p_LegpanelC3 = p_LegpanelC2.addOrReplaceChild("legpanelc3", CubeListBuilder.create().texOffs(32, 59).addBox(-3.0f, 0.0f, -0.5f, 6, 4, 1), PartPose.offsetAndRotation(0.0f, 4.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        PartDefinition p_Cloak1 = root.addOrReplaceChild("cloak1", CubeListBuilder.create().texOffs(106, 0).addBox(0.0f, 0.0f, -0.5f, 10, 18, 1), PartPose.offsetAndRotation(-5.0f, -6.0f, 4.0f, 0.0f, 0.0f, 0.0f));
        PartDefinition p_Cloak2 = p_Cloak1.addOrReplaceChild("cloak2", CubeListBuilder.create().texOffs(106, 19).addBox(0.0f, 0.0f, -0.5f, 10, 4, 1), PartPose.offsetAndRotation(0.0f, 18.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        PartDefinition p_Cloak3 = p_Cloak2.addOrReplaceChild("cloak3", CubeListBuilder.create().texOffs(106, 24).addBox(0.0f, 0.0f, -0.5f, 10, 4, 1), PartPose.offsetAndRotation(0.0f, 4.0f, 0.0f, 0.0f, 0.0f, 0.0f));

        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float t = s.ageInTicks; // ticksExisted + partialTicks
        hood1.yRot = s.yRot / 57.295776f;
        hood1.xRot = s.xRot / 57.295776f;
        armL1.xRot = -1.0f - s.armLiftL + Mth.sin((t + 20) / 10.0f) * 0.08f;
        armR1.xRot = -1.0f - s.armLiftR + Mth.sin(t / 10.0f) * 0.08f;
        legpanelC1.xRot = -0.15f + Mth.sin(t / 8.0f) * 0.12f;
        legpanelC2.xRot = Mth.sin((t - 5.0f) / 8.0f) * 0.13f;
        legpanelC3.xRot = Mth.sin((t - 10.0f) / 8.0f) * 0.14f;
        cloak1.xRot = 0.2f + Mth.sin(t / 7.0f) * 0.08f;
        cloak2.xRot = Mth.sin((t - 5.0f) / 7.0f) * 0.1f;
        cloak3.xRot = Mth.sin((t - 10.0f) / 7.0f) * 0.12f;
        sidepanelL2.zRot = -0.2f + Mth.sin((t + 10) / 8.0f) * 0.12f;
        sidepanelL3.zRot = Mth.sin((t + 5.0f) / 8.0f) * 0.13f;
        sidepanelL4.zRot = Mth.sin(t / 8.0f) * 0.14f;
        sidepanelR2.zRot = 0.2f + Mth.sin((t - 5) / 8.0f) * 0.12f;
        sidepanelR3.zRot = Mth.sin((t - 10.0f) / 8.0f) * 0.13f;
        sidepanelR4.zRot = Mth.sin((t - 15.0f) / 8.0f) * 0.14f;
    }
}
