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

/** TC6 ModelCrossbow and ModelArcaneBore. */
public final class TurretModels {
    public static class State extends LivingEntityRenderState { public float load, headYaw, pitch, attackTime; public boolean minecart; }
    public static final ModelLayerLocation CROSSBOW = new ModelLayerLocation(Identifier.fromNamespaceAndPath("thaumcraft", "turret_crossbow"), "main");
    public static final ModelLayerLocation BORE = new ModelLayerLocation(Identifier.fromNamespaceAndPath("thaumcraft", "arcane_bore"), "main");

    private static void legs(PartDefinition r) {
        r.addOrReplaceChild("tripod", CubeListBuilder.create().texOffs(13, 0).mirror().addBox(-1.5f, 0, -1.5f, 3, 2, 3), PartPose.offset(0, 12, 0));
        float[] ry = {0f, 1.570796f, 3.141593f, 4.712389f};
        for (int i = 0; i < 4; i++) r.addOrReplaceChild("leg" + (i + 1), CubeListBuilder.create().texOffs(20, 10).mirror().addBox(-1, 1, -1, 2, 13, 2), PartPose.offsetAndRotation(0, 12, 0, 0.5235988f, ry[i], 0));
    }
    public static LayerDefinition crossbow() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        PartDefinition cb = r.addOrReplaceChild("crossbow", CubeListBuilder.create().texOffs(28, 14).mirror().addBox(-2, 0, -7, 4, 2, 14), PartPose.offset(0, 10, 0));
        cb.addOrReplaceChild("basebarr", CubeListBuilder.create().texOffs(40, 23).mirror().addBox(-1, 0, 7, 1, 2, 5), PartPose.rotation(0, -0.1396263f, 0));
        cb.addOrReplaceChild("basebarl", CubeListBuilder.create().texOffs(40, 23).mirror().addBox(0, 0, 7, 1, 2, 5), PartPose.rotation(0, 0.1396263f, 0));
        cb.addOrReplaceChild("barrel", CubeListBuilder.create().texOffs(20, 28).mirror().addBox(-1, -1, -8, 2, 2, 2), PartPose.ZERO);
        cb.addOrReplaceChild("basebarcross", CubeListBuilder.create().texOffs(0, 13).mirror().addBox(-2, 0.5f, 10, 4, 1, 1), PartPose.ZERO);
        cb.addOrReplaceChild("ammobox", CubeListBuilder.create().texOffs(38, 0).mirror().addBox(-2, -5, -6, 4, 5, 9), PartPose.ZERO);
        cb.addOrReplaceChild("loadbarcross", CubeListBuilder.create().texOffs(0, 13).mirror().addBox(-2, -8.5f, -0.5f, 4, 1, 1), PartPose.rotation(-0.5585054f, 0, 0));
        cb.addOrReplaceChild("loadbarl", CubeListBuilder.create().texOffs(0, 15).mirror().addBox(2, -9, -1, 1, 11, 2), PartPose.rotation(-0.5585054f, 0, 0));
        cb.addOrReplaceChild("loadbarr", CubeListBuilder.create().texOffs(0, 15).mirror().addBox(-3, -9, -1, 1, 11, 2), PartPose.rotation(-0.5585054f, 0, 0));
        cb.addOrReplaceChild("crossl1", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0, 0, -6, 5, 2, 1), PartPose.rotation(0, -0.2443461f, 0));
        cb.addOrReplaceChild("crossl2", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(4, 0, -5, 3, 2, 1), PartPose.rotation(0, -0.2443461f, 0));
        cb.addOrReplaceChild("crossl3", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(6, 0, -4, 2, 2, 1), PartPose.rotation(0, -0.2443461f, 0));
        cb.addOrReplaceChild("crossr1", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-5, 0, -6, 5, 2, 1), PartPose.rotation(0, 0.2443461f, 0));
        cb.addOrReplaceChild("crossr2", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-7, 0, -5, 3, 2, 1), PartPose.rotation(0, 0.2443461f, 0));
        cb.addOrReplaceChild("crossr3", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-8, 0, -4, 2, 2, 1), PartPose.rotation(0, 0.2443461f, 0));
        legs(r);
        return LayerDefinition.create(mesh, 64, 32);
    }
    public static LayerDefinition bore() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        legs(r);
        PartDefinition base = r.addOrReplaceChild("base", CubeListBuilder.create().texOffs(32, 0).addBox(-3, -6, -3, 6, 6, 6), PartPose.offset(0, 13, 0));
        base.addOrReplaceChild("crystal", CubeListBuilder.create().texOffs(32, 25).addBox(-1, -4, 5, 2, 2, 2), PartPose.ZERO);
        base.addOrReplaceChild("domebase", CubeListBuilder.create().texOffs(32, 19).addBox(-2, -5, 3, 4, 4, 1), PartPose.ZERO);
        base.addOrReplaceChild("dome", CubeListBuilder.create().texOffs(44, 16).addBox(-2, -5, 4, 4, 4, 4), PartPose.ZERO);
        base.addOrReplaceChild("magbase", CubeListBuilder.create().texOffs(0, 18).mirror().addBox(-1, -4, -6, 2, 2, 3), PartPose.ZERO);
        base.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(0, 9).mirror().addBox(-1.5f, 0, -1.5f, 3, 3, 3), PartPose.offsetAndRotation(0, -3, -6, -1.570796f, 0, 0));
        return LayerDefinition.create(mesh, 64, 32);
    }

    public static final class Crossbow extends EntityModel<State> {
        private final ModelPart crossbow, l1, l2, l3, r1, r2, r3, lbc, lbl, lbr;
        private final ModelPart[] legs = new ModelPart[4];
        public Crossbow(ModelPart root) {
            super(root);
            crossbow = root.getChild("crossbow");
            l1 = crossbow.getChild("crossl1"); l2 = crossbow.getChild("crossl2"); l3 = crossbow.getChild("crossl3");
            r1 = crossbow.getChild("crossr1"); r2 = crossbow.getChild("crossr2"); r3 = crossbow.getChild("crossr3");
            lbc = crossbow.getChild("loadbarcross"); lbl = crossbow.getChild("loadbarl"); lbr = crossbow.getChild("loadbarr");
            for (int i = 0; i < 4; i++) legs[i] = root.getChild("leg" + (i + 1));
        }
        @Override public void setupAnim(State s) {
            super.setupAnim(s);
            crossbow.yRot = s.headYaw / 57.295776f;
            crossbow.xRot = s.pitch / 57.295776f;
            float f6 = s.attackTime;
            float ly = -0.2f + Mth.sin(Mth.sqrt(f6) * (float) Math.PI * 2.0f) * 0.2f;
            l1.yRot = l2.yRot = l3.yRot = ly;
            float ry = 0.2f - Mth.sin(Mth.sqrt(f6) * (float) Math.PI * 2.0f) * 0.2f;
            r1.yRot = r2.yRot = r3.yRot = ry;
            float lx = -0.5f + Mth.sin(Mth.sqrt(s.load) * (float) Math.PI * 2.0f) * 0.5f;
            lbc.xRot = lbl.xRot = lbr.xRot = lx;
            for (ModelPart l : legs) { l.y = s.minecart ? 12 - 8 : 12; l.xRot = s.minecart ? 0.1f : 0.5f; }
        }
    }
    public static final class Bore extends EntityModel<State> {
        private final ModelPart base;
        public Bore(ModelPart root) { super(root); base = root.getChild("base"); }
        @Override public void setupAnim(State s) { super.setupAnim(s); base.yRot = s.headYaw / 57.295776f; base.xRot = s.pitch / 57.295776f; }
    }
    private TurretModels() {}
}
