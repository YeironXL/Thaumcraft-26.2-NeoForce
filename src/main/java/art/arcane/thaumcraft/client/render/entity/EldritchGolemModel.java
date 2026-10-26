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

/**
 * TC6 ModelEldritchGolem (128x64): cloaked stone colossus with shoulder-plated arms and a hooded head that is swapped
 * for a small core (Head2) once headless. The three front cloths use a 114-wide texture in TC6 and live in {@link #CLOTH}.
 */
public final class EldritchGolemModel extends EntityModel<EldritchGolemModel.State> {
    public static class State extends LivingEntityRenderState {
        public boolean headless;
        public int spawnTimer, attackTimer;
        public float pt;
    }
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("thaumcraft", "eldritch_golem"), "main");
    public static final ModelLayerLocation CLOTH = new ModelLayerLocation(Identifier.fromNamespaceAndPath("thaumcraft", "eldritch_golem"), "cloth");

    private final boolean cloth;
    private ModelPart head, head2, armL, armR, legL, legR, cloak1, cloak2, cloak3, front1, front2;

    public EldritchGolemModel(ModelPart root, boolean cloth) {
        super(root);
        this.cloth = cloth;
        if (cloth) { front1 = root.getChild("frontcloth1"); front2 = root.getChild("frontcloth2"); return; }
        head = root.getChild("head"); head2 = root.getChild("head2");
        armL = root.getChild("arml"); armR = root.getChild("armr");
        legL = root.getChild("legl"); legR = root.getChild("legr");
        cloak1 = root.getChild("cloak1"); cloak2 = root.getChild("cloak2"); cloak3 = root.getChild("cloak3");
    }

    private static PartPose pr(float px, float py, float pz, float rx, float ry, float rz) { return PartPose.offsetAndRotation(px, py, pz, rx, ry, rz); }
    private static void box(PartDefinition p, String n, int u, int v, boolean mirror, float x, float y, float z, int w, int h, int d, PartPose pose) {
        CubeListBuilder b = CubeListBuilder.create().texOffs(u, v);
        if (mirror) b.mirror();
        p.addOrReplaceChild(n, b.addBox(x, y, z, w, h, d), pose);
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        box(r, "cloak1", 0, 47, false, -5.0f, 1.5f, 4.0f, 10, 12, 1, pr(0, 0, -2.5f, 0.1396263f, 0, 0));
        box(r, "cloak3", 0, 37, false, -5.0f, 17.5f, -0.8f, 10, 4, 1, pr(0, 0, -2.5f, 0.4465716f, 0, 0));
        box(r, "cloak2", 0, 59, false, -5.0f, 13.5f, 1.7f, 10, 4, 1, pr(0, 0, -2.5f, 0.3069452f, 0, 0));
        box(r, "cloakcl", 0, 43, false, 3.0f, 0.5f, 2.0f, 2, 1, 3, pr(0, 0, -2.5f, 0.1396263f, 0, 0));
        box(r, "cloakcr", 0, 43, false, -5.0f, 0.5f, 2.0f, 2, 1, 3, pr(0, 0, -2.5f, 0.1396263f, 0, 0));
        box(r, "head", 47, 12, false, -3.5f, -6.0f, -2.5f, 7, 7, 5, pr(0, 4.5f, -3.8f, -0.1047198f, 0, 0));
        box(r, "head2", 26, 16, false, -2.0f, -2.0f, -2.0f, 4, 4, 4, pr(0, 0, -5.0f, -0.1047198f, 0, 0));
        box(r, "collarl", 75, 50, false, 3.5f, -0.5f, -7.0f, 1, 4, 10, pr(0, 0, -2.5f, 0.837758f, 0, 0));
        box(r, "collarr", 67, 50, false, -4.5f, -0.5f, -7.0f, 1, 4, 10, pr(0, 0, -2.5f, 0.837758f, 0, 0));
        box(r, "collarb", 77, 59, false, -3.5f, -0.5f, 2.0f, 7, 4, 1, pr(0, 0, -2.5f, 0.837758f, 0, 0));
        box(r, "collarf", 77, 59, false, -3.5f, -0.5f, -7.0f, 7, 4, 1, pr(0, 0, -2.5f, 0.837758f, 0, 0));
        box(r, "collarblack", 22, 0, false, -3.5f, 0.0f, -6.0f, 7, 1, 8, pr(0, 0, -2.5f, 0.837758f, 0, 0));
        box(r, "torso", 34, 45, true, -5.0f, 2.5f, -3.0f, 10, 10, 6, pr(0, 0, -2.5f, 0.1745329f, 0, 0));
        PartDefinition armR = r.addOrReplaceChild("armr", CubeListBuilder.create().texOffs(78, 32).addBox(-3.5f, 1.5f, -2.0f, 4, 13, 5), pr(-5.0f, 3.0f, -2.0f, 0, 0, 0.1047198f));
        box(armR, "shoulderr1", 0, 23, false, -3.3f, 4.0f, -2.5f, 1, 2, 6, pr(0, 0, 0, 0, 0, 1.186824f));
        box(armR, "shoulderr", 0, 0, false, -4.3f, -1.0f, -3.0f, 4, 5, 7, pr(0, 0, 0, 0, 0, 1.186824f));
        box(armR, "shoulderr2", 0, 12, false, -2.3f, 4.0f, -3.0f, 2, 3, 7, pr(0, 0, 0, 0, 0, 1.186824f));
        box(armR, "shoulderr0", 56, 31, false, -4.5f, -1.5f, -2.5f, 5, 6, 6, PartPose.ZERO);
        PartDefinition armL = r.addOrReplaceChild("arml", CubeListBuilder.create().texOffs(78, 32).mirror().addBox(-0.5f, 1.5f, -2.0f, 4, 13, 5), pr(5.0f, 3.0f, -2.0f, 0, 0, -0.1047198f));
        box(armL, "shoulderl1", 0, 23, true, 2.3f, 4.0f, -2.5f, 1, 2, 6, pr(0, 0, 0, 0, 0, -1.186824f));
        box(armL, "shoulderl0", 56, 31, true, -0.5f, -1.5f, -2.5f, 5, 6, 6, PartPose.ZERO);
        box(armL, "shoulderl", 0, 0, true, 0.3f, -1.0f, -3.0f, 4, 5, 7, pr(0, 0, 0, 0, 0, -1.186824f));
        box(armL, "shoulderl2", 0, 12, true, 0.3f, 4.0f, -3.0f, 2, 3, 7, pr(0, 0, 0, 0, 0, -1.186824f));
        box(r, "backpanelr1", 96, 7, false, 0.0f, 2.5f, -2.5f, 2, 2, 5, pr(-2.0f, 12.0f, 0, 0, 0, 0.1396263f));
        box(r, "waistr1", 96, 14, false, -3.0f, -0.5f, -2.5f, 5, 3, 5, pr(-2.0f, 12.0f, 0, 0, 0, 0.1396263f));
        box(r, "waistr2", 116, 13, false, -3.0f, 2.5f, -2.5f, 1, 4, 5, pr(-2.0f, 12.0f, 0, 0, 0, 0.1396263f));
        box(r, "waistr3", 114, 5, true, -2.0f, 2.5f, -2.5f, 2, 3, 5, pr(-2.0f, 12.0f, 0, 0, 0, 0.1396263f));
        box(r, "legr", 79, 19, false, -2.5f, 2.5f, -2.0f, 4, 9, 4, pr(-2.0f, 12.5f, 0, 0, 0, 0));
        box(r, "waistl1", 96, 14, true, -2.0f, -0.5f, -2.5f, 5, 3, 5, pr(2.0f, 12.0f, 0, 0, 0, -0.1396263f));
        box(r, "waistl2", 116, 13, true, 2.0f, 2.5f, -2.5f, 1, 4, 5, pr(2.0f, 12.0f, 0, 0, 0, -0.1396263f));
        box(r, "waistl3", 114, 5, true, 0.0f, 2.5f, -2.5f, 2, 3, 5, pr(2.0f, 12.0f, 0, 0, 0, -0.1396263f));
        box(r, "backpanell1", 96, 7, true, -2.0f, 2.5f, -2.5f, 2, 2, 5, pr(2.0f, 12.0f, 0, 0, 0, -0.1396263f));
        box(r, "legl", 79, 19, true, -1.5f, 2.5f, -2.0f, 4, 9, 4, pr(2.0f, 12.5f, 0, 0, 0, 0));
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** Frontcloth0/1/2 (setTextureSize(114, 64) in TC6). */
    public static LayerDefinition createCloth() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        box(r, "frontcloth0", 114, 52, false, -3.0f, 3.2f, -3.5f, 6, 10, 1, pr(0, 0, -2.5f, 0.1745329f, 0, 0));
        box(r, "frontcloth1", 114, 39, false, -1.0f, 1.5f, -3.5f, 6, 6, 1, pr(-2.0f, 12.0f, 0, -0.1047198f, 0, 0));
        box(r, "frontcloth2", 114, 47, false, -1.0f, 8.5f, -1.5f, 6, 3, 1, pr(-2.0f, 11.0f, 0, -0.3316126f, 0, 0));
        return LayerDefinition.create(mesh, 114, 64);
    }

    private static float doAbs(float a, float b) { return (Math.abs(a % b - b * 0.5f) - b * 0.25f) / (b * 0.25f); }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float f = s.walkAnimationPos, f1 = s.walkAnimationSpeed;
        float a = Mth.cos(f * 0.44f) * 1.4f * f1, b = Mth.cos(f * 0.44f + 3.1415927f) * 1.4f * f1, c = Math.min(a, b);
        if (cloth) {
            front1.xRot = c - 0.1047198f;
            front2.xRot = c - 0.3316126f;
            return;
        }
        cloak1.xRot = -c / 3.0f + 0.1396263f;
        cloak2.xRot = -c / 3.0f + 0.3069452f;
        cloak3.xRot = -c / 3.0f + 0.4465716f;
        head.visible = !s.headless;
        head2.visible = s.headless;
        if (s.spawnTimer > 0) {
            head.yRot = 0.0f;
            head.xRot = s.spawnTimer / 2 / 57.295776f;
        } else {
            head.yRot = s.yRot / 4.0f / 57.295776f;
            head.xRot = s.xRot / 2.0f / 57.295776f;
            head2.yRot = s.yRot / 57.295776f;
            head2.xRot = s.xRot / 57.295776f;
        }
        legR.xRot = Mth.cos(f * 0.4662f) * 1.4f * f1;
        legL.xRot = Mth.cos(f * 0.4662f + 3.1415927f) * 1.4f * f1;
        int i = s.attackTimer;
        if (i > 0) {
            armR.xRot = -2.0f + 1.5f * doAbs(i - s.pt, 10.0f);
            armL.xRot = -2.0f + 1.5f * doAbs(i - s.pt, 10.0f);
        } else {
            armR.xRot = Mth.cos(f * 0.4f + 3.1415927f) * 2.0f * f1 * 0.5f;
            armL.xRot = Mth.cos(f * 0.4f) * 2.0f * f1 * 0.5f;
        }
    }
}
