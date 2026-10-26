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

/** TC6 ModelEldritchCrab (128x64): a crab with snapping claws; the helmeted tail is drawn instead of the bare one while it wears a helm. */
public final class EldritchCrabModel extends EntityModel<EldritchCrabModel.State> {
    public static class State extends LivingEntityRenderState { public boolean helm; }
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("thaumcraft", "eldritch_crab"), "main");
    private final ModelPart tailHelm, tailBare, rClaw1, rClaw2, lClaw1, lClaw2, rrLeg1, rfLeg1, lrLeg1, lfLeg1, rrLeg0, rfLeg0, lfLeg0, lrLeg0;

    public EldritchCrabModel(ModelPart root) {
        super(root);
        tailHelm = root.getChild("tailhelm"); tailBare = root.getChild("tailbare");
        rClaw1 = root.getChild("rclaw1"); rClaw2 = root.getChild("rclaw2"); lClaw1 = root.getChild("lclaw1"); lClaw2 = root.getChild("lclaw2");
        rrLeg1 = root.getChild("rrleg1"); rfLeg1 = root.getChild("rfleg1"); lrLeg1 = root.getChild("lrleg1"); lfLeg1 = root.getChild("lfleg1");
        rrLeg0 = root.getChild("rrleg0"); rfLeg0 = root.getChild("rfleg0"); lfLeg0 = root.getChild("lfleg0"); lrLeg0 = root.getChild("lrleg0");
    }

    private static PartPose pose(float x, float y, float z, float rx, float ry, float rz) { return PartPose.offsetAndRotation(x, y, z, rx, ry, rz); }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        r.addOrReplaceChild("tailhelm", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5f, -4.5f, -0.4f, 9, 9, 9), pose(0, 18, 0, 0.1047198f, 0, 0));
        r.addOrReplaceChild("tailbare", CubeListBuilder.create().texOffs(64, 0).addBox(-4.0f, -4.0f, -0.4f, 8, 8, 8), pose(0, 18, 0, 0.1047198f, 0, 0));
        r.addOrReplaceChild("rclaw1", CubeListBuilder.create().texOffs(0, 47).addBox(-2.0f, -1.0f, -5.066667f, 4, 3, 5), pose(-6, 15.5f, -10, 0, 0, 0));
        r.addOrReplaceChild("head1", CubeListBuilder.create().texOffs(0, 38).addBox(-2.0f, -1.5f, -9.066667f, 4, 4, 1), pose(0, 18, 0, 0, 0, 0));
        r.addOrReplaceChild("rclaw0", CubeListBuilder.create().texOffs(0, 55).addBox(-2.0f, -2.5f, -3.066667f, 4, 5, 3), pose(-6, 17, -7, 0, 0, 0));
        r.addOrReplaceChild("rclaw2", CubeListBuilder.create().texOffs(14, 54).addBox(-1.5f, -1.0f, -4.066667f, 3, 2, 5), pose(-6, 18.5f, -10, 0.3141593f, 0, 0));
        r.addOrReplaceChild("rarm", CubeListBuilder.create().texOffs(44, 4).addBox(-1.0f, -1.0f, -5.066667f, 2, 2, 6), pose(-3, 17, -4, 0, 0.7504916f, 0));
        r.addOrReplaceChild("lclaw2", CubeListBuilder.create().texOffs(14, 54).addBox(-1.5f, -1.0f, -4.066667f, 3, 2, 5), pose(6, 18.5f, -10, 0.3141593f, 0, 0));
        r.addOrReplaceChild("lclaw1", CubeListBuilder.create().texOffs(0, 47).mirror().addBox(-2.0f, -1.0f, -5.066667f, 4, 3, 5), pose(6, 15.5f, -10, 0, 0, 0));
        r.addOrReplaceChild("lclaw0", CubeListBuilder.create().texOffs(0, 55).mirror().addBox(-2.0f, -2.5f, -3.066667f, 4, 5, 3), pose(6, 17, -7, 0, 0, 0));
        r.addOrReplaceChild("larm", CubeListBuilder.create().texOffs(44, 4).addBox(-1.0f, -1.0f, -4.066667f, 2, 2, 6), pose(4, 17, -5, 0, -0.7504916f, 0));
        r.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(0, 18).addBox(-3.5f, -3.5f, -6.066667f, 7, 7, 6), pose(0, 18, 0, 0.0523599f, 0, 0));
        r.addOrReplaceChild("head0", CubeListBuilder.create().texOffs(0, 31).addBox(-2.5f, -2.0f, -8.066667f, 5, 5, 2), pose(0, 18, 0, 0, 0, 0));
        r.addOrReplaceChild("rrleg1", CubeListBuilder.create().texOffs(36, 4).addBox(-4.5f, 1.0f, -0.9f, 2, 5, 2), pose(-4, 20, -1.5f, 0, 0, 0));
        r.addOrReplaceChild("rfleg1", CubeListBuilder.create().texOffs(36, 4).addBox(-5.0f, 1.0f, -1.066667f, 2, 5, 2), pose(-4, 20, -3.5f, 0, 0, 0));
        r.addOrReplaceChild("lrleg1", CubeListBuilder.create().texOffs(36, 4).addBox(2.5f, 1.0f, -0.9f, 2, 5, 2), pose(4, 20, -1.5f, 0, 0, 0));
        r.addOrReplaceChild("lfleg1", CubeListBuilder.create().texOffs(36, 4).addBox(3.0f, 1.0f, -1.066667f, 2, 5, 2), pose(4, 20, -3.5f, 0, 0, 0));
        r.addOrReplaceChild("rrleg0", CubeListBuilder.create().texOffs(36, 0).addBox(-4.5f, -1.0f, -0.9f, 6, 2, 2), pose(-4, 20, -1.5f, 0, 0, 0));
        r.addOrReplaceChild("rfleg0", CubeListBuilder.create().texOffs(36, 0).addBox(-5.0f, -1.0f, -1.066667f, 6, 2, 2), pose(-4, 20, -3.5f, 0, 0, 0));
        r.addOrReplaceChild("lfleg0", CubeListBuilder.create().texOffs(36, 0).addBox(-1.0f, -1.0f, -1.066667f, 6, 2, 2), pose(4, 20, -3.5f, 0, 0, 0));
        r.addOrReplaceChild("lrleg0", CubeListBuilder.create().texOffs(36, 0).addBox(-1.5f, -1.0f, -0.9f, 6, 2, 2), pose(4, 20, -1.5f, 0, 0, 0));
        return LayerDefinition.create(mesh, 128, 64);
    }

    private static void set(ModelPart p, float x, float y, float z) { p.xRot = x; p.yRot = y; p.zRot = z; }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        tailHelm.visible = s.helm;
        tailBare.visible = !s.helm;
        float par1 = s.walkAnimationPos, par2 = s.walkAnimationSpeed, t = s.ageInTicks;
        set(rrLeg1, 0, 0.2094395f, 0.4363323f); set(rfLeg1, 0, -0.2094395f, 0.4363323f);
        set(lrLeg1, 0, -0.2094395f, -0.4363323f); set(lfLeg1, 0, 0.2094395f, -0.4363323f);
        set(rrLeg0, 0, 0.2094395f, 0.4363323f); set(rfLeg0, 0, -0.2094395f, 0.4363323f);
        set(lfLeg0, 0, 0.2094395f, -0.4363323f); set(lrLeg0, 0, -0.2094395f, -0.4363323f);
        float f9 = -(Mth.cos(par1 * 0.6662f * 2.0f) * 0.4f) * par2;
        float f10 = -(Mth.cos(par1 * 0.6662f * 2.0f + (float) Math.PI) * 0.4f) * par2;
        rrLeg1.yRot += f9; rrLeg0.yRot += f9; lrLeg1.yRot -= f9; lrLeg0.yRot -= f9;
        rfLeg1.yRot += f10; rfLeg0.yRot += f10; lfLeg1.yRot -= f10; lfLeg0.yRot -= f10;
        rrLeg1.zRot += f9; rrLeg0.zRot += f9; lrLeg1.zRot -= f9; lrLeg0.zRot -= f9;
        rfLeg1.zRot += f10; rfLeg0.zRot += f10; lfLeg1.zRot -= f10; lfLeg0.zRot -= f10;
        tailHelm.yRot = tailBare.yRot = Mth.cos(par1 * 0.6662f) * 2.0f * par2 * 0.125f;
        tailHelm.zRot = tailBare.zRot = Mth.cos(par1 * 0.6662f) * par2 * 0.125f;
        rClaw2.xRot = 0.3141593f - Mth.sin(t / 4.0f) * 0.25f;
        lClaw2.xRot = 0.3141593f + Mth.sin(t / 4.1f) * 0.25f;
        rClaw1.xRot = Mth.sin(t / 4.0f) * 0.125f;
        lClaw1.xRot = -Mth.sin(t / 4.1f) * 0.125f;
    }
}
