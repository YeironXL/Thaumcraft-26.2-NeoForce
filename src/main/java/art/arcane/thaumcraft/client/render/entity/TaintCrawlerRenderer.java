package art.arcane.thaumcraft.client.render.entity;

import art.arcane.thaumcraft.entity.monster.TaintCrawlerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.silverfish.SilverfishModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** TC6 RenderTaintCrawler: the silverfish model with textures/entity/crawler.png at 0.7 scale, flipping 180 degrees on death. */
public final class TaintCrawlerRenderer extends MobRenderer<TaintCrawlerEntity, LivingEntityRenderState, SilverfishModel> {
    private static final Identifier TEX = Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/crawler.png");
    public TaintCrawlerRenderer(EntityRendererProvider.Context ctx) { super(ctx, new SilverfishModel(ctx.bakeLayer(ModelLayers.SILVERFISH)), 0.2f); }
    @Override protected float getFlipDegrees() { return 180.0f; }
    @Override public Identifier getTextureLocation(LivingEntityRenderState s) { return TEX; }
    @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
    @Override protected void scale(LivingEntityRenderState s, PoseStack ps) { ps.scale(0.7f, 0.7f, 0.7f); }
}
