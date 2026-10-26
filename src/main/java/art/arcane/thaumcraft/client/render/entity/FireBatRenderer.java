package art.arcane.thaumcraft.client.render.entity;

import art.arcane.thaumcraft.entity.FireBatEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** TC6 RenderFireBat: ModelFireBat at 0.35 scale, full bright, bobbing while flying. */
public final class FireBatRenderer extends MobRenderer<FireBatEntity, FireBatModel.State, FireBatModel> {
    private static final Identifier TEX = Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/firebat.png");
    public FireBatRenderer(EntityRendererProvider.Context ctx) { super(ctx, new FireBatModel(ctx.bakeLayer(TcModelLayers.FIRE_BAT)), 0.25f); }
    @Override public Identifier getTextureLocation(FireBatModel.State s) { return TEX; }
    @Override public FireBatModel.State createRenderState() { return new FireBatModel.State(); }
    @Override public void extractRenderState(FireBatEntity e, FireBatModel.State s, float pt) {
        super.extractRenderState(e, s, pt);
        s.hanging = e.getIsBatHanging();
        s.lightCoords = 0xF000F0;
    }
    @Override protected void scale(FireBatModel.State s, PoseStack ps) { ps.scale(0.35f, 0.35f, 0.35f); }
    @Override protected void setupRotations(FireBatModel.State s, PoseStack ps, float bodyRot, float scale) {
        if (!s.hanging) ps.translate(0.0f, Mth.cos(s.ageInTicks * 0.3f) * 0.1f, 0.0f);
        else ps.translate(0.0f, -0.1f, 0.0f);
        super.setupRotations(s, ps, bodyRot, scale);
    }
}
