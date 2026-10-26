package art.arcane.thaumcraft.client.render.entity;

import art.arcane.thaumcraft.entity.TaintSeedEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/** TC6 RenderTaintSeed: ModelTaintSeed (blended, no culling) with textures/entity/taintseed.png. */
public final class TaintSeedRenderer extends MobRenderer<TaintSeedEntity, TaintSeedModel.State, TaintSeedModel> {
    private static final Identifier TEX = Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/taintseed.png");

    public TaintSeedRenderer(EntityRendererProvider.Context ctx) { super(ctx, new TaintSeedModel(ctx.bakeLayer(TaintSeedModel.LAYER)), 0.4f); }
    @Override public Identifier getTextureLocation(TaintSeedModel.State s) { return TEX; }
    @Override public TaintSeedModel.State createRenderState() { return new TaintSeedModel.State(); }
    @Override public void extractRenderState(TaintSeedEntity e, TaintSeedModel.State s, float pt) {
        super.extractRenderState(e, s, pt);
        s.hurt = e.hurtTime; s.attackAnim = e.attackAnim; s.height = e.getBbHeight(); s.ticks = e.tickCount;
    }
}
