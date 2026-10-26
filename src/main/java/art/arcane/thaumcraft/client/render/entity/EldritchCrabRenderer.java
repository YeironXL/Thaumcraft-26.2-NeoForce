package art.arcane.thaumcraft.client.render.entity;

import art.arcane.thaumcraft.entity.monster.EldritchCrabEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/** TC6 RenderEldritchCrab: ModelEldritchCrab at 0.8 scale with textures/entity/crab.png. */
public final class EldritchCrabRenderer extends MobRenderer<EldritchCrabEntity, EldritchCrabModel.State, EldritchCrabModel> {
    private static final Identifier TEX = Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/crab.png");
    public EldritchCrabRenderer(EntityRendererProvider.Context ctx) { super(ctx, new EldritchCrabModel(ctx.bakeLayer(EldritchCrabModel.LAYER)), 0.5f); }
    @Override public Identifier getTextureLocation(EldritchCrabModel.State s) { return TEX; }
    @Override public EldritchCrabModel.State createRenderState() { return new EldritchCrabModel.State(); }
    @Override public void extractRenderState(EldritchCrabEntity e, EldritchCrabModel.State s, float pt) { super.extractRenderState(e, s, pt); s.helm = e.hasHelm(); }
    @Override protected void scale(EldritchCrabModel.State s, PoseStack ps) { ps.scale(0.8f, 0.8f, 0.8f); }
}
