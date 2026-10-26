package art.arcane.thaumcraft.client.render.entity;

import art.arcane.thaumcraft.entity.SpellBatEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** TC6 RenderSpellBat: fire bat model at 0.35 scale, tinted with the package colour at half alpha, full bright. */
public final class SpellBatRenderer extends MobRenderer<SpellBatEntity, FireBatModel.State, FireBatModel> {
    private static final Identifier TEX = Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/spellbat.png");
    public SpellBatRenderer(EntityRendererProvider.Context ctx) { super(ctx, new FireBatModel(ctx.bakeLayer(TcModelLayers.FIRE_BAT)), 0.25f); }
    @Override public Identifier getTextureLocation(FireBatModel.State s) { return TEX; }
    @Override public FireBatModel.State createRenderState() { return new FireBatModel.State(); }
    @Override public void extractRenderState(SpellBatEntity e, FireBatModel.State s, float pt) { super.extractRenderState(e, s, pt); s.color = e.color; s.lightCoords = 0xF000F0; }
    @Override protected void scale(FireBatModel.State s, PoseStack ps) { ps.scale(0.35f, 0.35f, 0.35f); }
    @Override protected void setupRotations(FireBatModel.State s, PoseStack ps, float bodyRot, float scale) { ps.translate(0.0f, -0.1f, 0.0f); super.setupRotations(s, ps, bodyRot, scale); }
    @Override protected int getModelTint(FireBatModel.State s) { return 0x80000000 | (s.color & 0xFFFFFF); }
    @Override protected RenderType getRenderType(FireBatModel.State s, boolean visible, boolean transparent, boolean glowing) { return RenderTypes.entityTranslucent(TEX); }
}
