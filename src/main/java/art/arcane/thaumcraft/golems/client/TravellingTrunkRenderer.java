package art.arcane.thaumcraft.golems.client;

import art.arcane.thaumcraft.golems.trunk.TravellingTrunkEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Travelling Trunk renderer: TC4 trunk textures (angry eyes while fighting), lid driven by the entity's mouth animation. */
public final class TravellingTrunkRenderer extends MobRenderer<TravellingTrunkEntity, TravellingTrunkModel.State, TravellingTrunkModel> {
    private static final Identifier TEX = Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/travelling_trunk.png");
    private static final Identifier ANGRY = Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/travelling_trunk_angry.png");
    public TravellingTrunkRenderer(EntityRendererProvider.Context ctx) { super(ctx, new TravellingTrunkModel(ctx.bakeLayer(TravellingTrunkModel.LAYER)), 0.45f); }
    @Override public Identifier getTextureLocation(TravellingTrunkModel.State s) { return s.angry ? ANGRY : TEX; }
    @Override public TravellingTrunkModel.State createRenderState() { return new TravellingTrunkModel.State(); }
    @Override public void extractRenderState(TravellingTrunkEntity e, TravellingTrunkModel.State s, float pt) {
        super.extractRenderState(e, s, pt);
        s.lid = Mth.lerp(pt, e.prevLidAngle, e.lidAngle);
        s.angry = e.isAngry();
        s.staying = e.isStaying();
    }
    @Override protected void scale(TravellingTrunkModel.State s, PoseStack ps) { ps.scale(0.75f, 0.75f, 0.75f); }
}
