package art.arcane.thaumcraft.golems.client;

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
 * Travelling Trunk: TC4 chest body (lid/base/lock laid out like the original trunk.png, which follows the vanilla chest
 * UVs) whose lid works as a mouth, plus four stubby legs (invented; TC4's trunk hopped).
 */
public final class TravellingTrunkModel extends EntityModel<TravellingTrunkModel.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("thaumcraft", "travelling_trunk"), "main");
    public static class State extends LivingEntityRenderState { public float lid; public boolean angry; public boolean staying; }
    private final ModelPart base, lid, fl, fr, bl, br;

    public TravellingTrunkModel(ModelPart root) {
        super(root);
        base = root.getChild("base"); lid = root.getChild("lid");
        fl = root.getChild("leg_fl"); fr = root.getChild("leg_fr"); bl = root.getChild("leg_bl"); br = root.getChild("leg_br");
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 19).addBox(-7, -10, -7, 14, 10, 14), PartPose.offset(0, 21, 0));
        PartDefinition lid = root.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 0).addBox(-7, -5, -14, 14, 5, 14), PartPose.offset(0, 11, 7));
        lid.addOrReplaceChild("lock", CubeListBuilder.create().texOffs(0, 0).addBox(-1, -2, -15, 2, 4, 1), PartPose.ZERO);
        String[] n = {"leg_fl", "leg_fr", "leg_bl", "leg_br"};
        float[][] p = {{4.5f, -4.5f}, {-4.5f, -4.5f}, {4.5f, 4.5f}, {-4.5f, 4.5f}};
        for (int i = 0; i < 4; i++)
            root.addOrReplaceChild(n[i], CubeListBuilder.create().texOffs(0, 44).addBox(-1.5f, 0, -1.5f, 3, 3, 3), PartPose.offset(p[i][0], 21, p[i][1]));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override public void setupAnim(State s) {
        super.setupAnim(s);
        lid.xRot = -s.lid * 1.1f;
        float sw = Mth.cos(s.walkAnimationPos * 1.6f) * 0.9f * s.walkAnimationSpeed;
        fl.xRot = sw; br.xRot = sw; fr.xRot = -sw; bl.xRot = -sw;
        float bob = s.staying ? 0 : Math.abs(Mth.cos(s.walkAnimationPos * 1.6f)) * s.walkAnimationSpeed * 1.2f;
        base.y = 21 - bob; lid.y = 11 - bob;
    }
}
