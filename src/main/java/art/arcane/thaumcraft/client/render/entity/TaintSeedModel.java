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
 * TC6 ModelTaintSeed: a pulsing stack of a base cube and seven tentacle segments. ModelRendererTaintSeed scales
 * every part's children by (s + pulse, s * 0.9, s + pulse) with s = 1.6 - sin(PI * i / 7) for the i-th part drawn;
 * that per-parent scale lives on an empty "scale" part between each segment and the next.
 */
public final class TaintSeedModel extends EntityModel<TaintSeedModel.State> {
    public static class State extends LivingEntityRenderState { public float hurt; public float attackAnim; public float height; public int ticks; }
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("thaumcraft", "taint_seed"), "main");
    private static final int LENGTH = 8;
    private final ModelPart tentacle;
    private final ModelPart[] tents = new ModelPart[LENGTH - 1];
    private final ModelPart[] scales = new ModelPart[LENGTH - 1];

    public TaintSeedModel(ModelPart root) {
        super(root, net.minecraft.client.renderer.rendertype.RenderTypes::entityTranslucent);
        tentacle = root.getChild("tentacle");
        ModelPart parent = tentacle;
        for (int k = 0; k < LENGTH - 1; ++k) {
            scales[k] = parent.getChild("scale" + k);
            tents[k] = scales[k].getChild("tent" + k);
            parent = tents[k];
        }
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parent = mesh.getRoot().addOrReplaceChild("tentacle", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -4, -4, 8, 8, 8), PartPose.offset(0, 12, 0));
        for (int k = 0; k < LENGTH - 1; ++k) {
            PartDefinition scale = parent.addOrReplaceChild("scale" + k, CubeListBuilder.create(), PartPose.ZERO);
            int v = k < LENGTH - 4 ? 16 : k == LENGTH - 4 ? 48 : 56;
            CubeListBuilder cubes = CubeListBuilder.create().texOffs(0, v);
            if (k < LENGTH - 4) cubes.addBox(-4, -4, -4, 8, 8, 8); else cubes.addBox(-2, -2, -2, 4, 4, 4);
            float py = k < LENGTH - 4 ? -8 : k == LENGTH - 4 ? -8 : -4;
            parent = scale.addOrReplaceChild("tent" + k, cubes, PartPose.offset(0, py, 0));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        float par3 = s.ageInTicks;
        float ht = s.hurt / 200.0f;
        float flail = 0.1f;
        float mod = 0.0625f * 0.2f;
        float fi = (flail + (ht > 0.0f ? mod : -mod)) * 3.0f;
        tentacle.xRot = 0.0f;
        for (int k = 0; k < LENGTH - 1; ++k) {
            tents[k].xRot = 0.1f / fi * Mth.sin(par3 * 0.06f - k / 2.0f) / 5.0f + ht + s.attackAnim;
            tents[k].zRot = 0.1f / fi * Mth.sin(par3 * 0.05f - k / 2.0f) / 5.0f;
        }
        // ModelRendererTaintSeed.render(tt = ticks + partialTicks, height 1.6): part i scales its children
        for (int i = 0; i < LENGTH - 1; ++i) {
            float qq = (float) (Math.PI * (i / 7.0f));
            float scale = 1.6f - (float) Math.sin(qq);
            float pulse = (float) Math.sin(par3 / 12.0f - qq) * 0.33f;
            pulse *= pulse;
            scales[i].xScale = scale + pulse; scales[i].yScale = scale * 0.9f; scales[i].zScale = scale + pulse;
        }
        // ModelTaintSeed.render: rises out of the ground over height*10 ticks, then translate 1.2 and scale height/2
        float h = s.height, hc = h * 10.0f, grow = s.ticks < hc ? (hc - s.ticks) / hc * h : 0.0f;
        root().y = ((h == 3.0f ? 0.6f : 1.2f) + grow) * 16.0f;
        root().xScale = root().yScale = root().zScale = h / 2.0f;
    }
}
