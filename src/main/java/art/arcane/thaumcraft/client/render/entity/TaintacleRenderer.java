package art.arcane.thaumcraft.client.render.entity;

import art.arcane.thaumcraft.entity.monster.TaintacleEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/** TC6 RenderTaintacle(shadow, length): 0.6/10 for the taintacle, 0.2/6 for the small tendril; textures/entity/taintacle.png. */
public final class TaintacleRenderer extends MobRenderer<TaintacleEntity, TaintacleModel.State, TaintacleModel> {
    private static final Identifier TEX = Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/taintacle.png");

    public TaintacleRenderer(EntityRendererProvider.Context ctx, boolean small) {
        super(ctx, new TaintacleModel(ctx.bakeLayer(small ? TaintacleModel.LAYER_SMALL : TaintacleModel.LAYER), small ? 6 : 10), small ? 0.2f : 0.6f);
    }
    public static TaintacleRenderer big(EntityRendererProvider.Context ctx) { return new TaintacleRenderer(ctx, false); }
    private TaintacleRenderer(EntityRendererProvider.Context ctx, net.minecraft.client.model.geom.ModelLayerLocation layer, int length, float shadow) {
        super(ctx, new TaintacleModel(ctx.bakeLayer(layer), length), shadow);
    }
    /** EntityTaintacleGiant: RenderTaintacle(1.0, 14). */
    public static TaintacleRenderer giant(EntityRendererProvider.Context ctx) { return new TaintacleRenderer(ctx, TaintacleModel.LAYER_GIANT, 14, 1.0f); }
    public static TaintacleRenderer small(EntityRendererProvider.Context ctx) { return new TaintacleRenderer(ctx, true); }

    @Override public Identifier getTextureLocation(TaintacleModel.State s) { return TEX; }
    @Override public TaintacleModel.State createRenderState() { return new TaintacleModel.State(); }
    @Override
    public void extractRenderState(TaintacleEntity e, TaintacleModel.State s, float pt) {
        super.extractRenderState(e, s, pt);
        s.flail = e.flailIntensity; s.hurt = e.hurtTime; s.height = e.getBbHeight(); s.ticks = e.tickCount;
    }
}
