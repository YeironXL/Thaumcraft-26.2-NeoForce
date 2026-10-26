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
 * TC6 ModelTaintacle (non-seed): a base cube and length-1 stacked segments ending in a 12px bulb with a small orb, each
 * child drawn at 0.88 of its parent (ModelRendererTaintacle scale), swaying with the flail intensity. The model rises out
 * of the ground over its first height*10 ticks.
 */
public final class TaintacleModel extends EntityModel<TaintacleModel.State> {
    public static class State extends LivingEntityRenderState { public float flail = 1.0f, hurt, height = 3.0f; public int ticks; }
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("thaumcraft", "taintacle"), "main");
    public static final ModelLayerLocation LAYER_SMALL = new ModelLayerLocation(Identifier.fromNamespaceAndPath("thaumcraft", "taintacle"), "small");
    public static final ModelLayerLocation LAYER_GIANT = new ModelLayerLocation(Identifier.fromNamespaceAndPath("thaumcraft", "taintacle"), "giant");
    private static final float SCALE = 0.88f;
    private final int length;
    private final ModelPart tentacle;
    private final ModelPart[] tents;

    public TaintacleModel(ModelPart root, int length) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityTranslucent);
        this.length = length;
        tentacle = root.getChild("tentacle");
        tents = new ModelPart[length - 1];
        ModelPart parent = tentacle;
        for (int k = 0; k < length - 1; ++k) { tents[k] = parent.getChild("s" + k).getChild("tent" + k); parent = tents[k]; }
    }

    public static LayerDefinition create(int length) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parent = mesh.getRoot().addOrReplaceChild("tentacle", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -4, -4, 8, 8, 8), PartPose.offset(0, 12, 0));
        for (int k = 0; k < length - 1; ++k) {
            PartDefinition s = parent.addOrReplaceChild("s" + k, CubeListBuilder.create(), PartPose.ZERO.withScale(SCALE));
            parent = s.addOrReplaceChild("tent" + k, CubeListBuilder.create().texOffs(0, 16).addBox(-4, -4, -4, 8, 8, 8), PartPose.offset(0, -8, 0));
        }
        // tents[length-2] gets the orb and the bulb (tents[length-1]) as further children
        PartDefinition so = parent.addOrReplaceChild("so", CubeListBuilder.create(), PartPose.ZERO.withScale(SCALE));
        so.addOrReplaceChild("orb", CubeListBuilder.create().texOffs(0, 56).addBox(-2, -2, -2, 4, 4, 4), PartPose.offset(0, -8, 0));
        PartDefinition sb = parent.addOrReplaceChild("sb", CubeListBuilder.create(), PartPose.ZERO.withScale(SCALE));
        sb.addOrReplaceChild("bulb", CubeListBuilder.create().texOffs(0, 32).addBox(-6, -6, -6, 12, 12, 12), PartPose.offset(0, -8, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float par3 = s.ageInTicks;
        float mod = 0.0625f * 0.2f;
        float fs = s.flail > 1.0f ? 3.0f : 1.0f - mod;
        float fi = s.flail + (s.hurt > 0.0f ? mod : -mod);
        tentacle.xRot = 0.0f;
        for (int k = 0; k < length - 1; ++k) {
            tents[k].xRot = 0.15f * fi * Mth.sin(par3 * 0.1f * fs - k / 2.0f);
            tents[k].zRot = 0.1f / fi * Mth.sin(par3 * 0.15f - k / 2.0f);
        }
        // ModelTaintacle.render: translate (0, 0.6 or 1.2 + rising offset, 0) and scale height/3
        float h = s.height, hc = h * 10.0f, grow = s.ticks < hc ? (hc - s.ticks) / hc * h : 0.0f;
        root().y = ((h == 3.0f ? 0.6f : 1.2f) + grow) * 16.0f;
        root().xScale = root().yScale = root().zScale = h / 3.0f;
    }

}
