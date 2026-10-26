package art.arcane.thaumcraft.client.render.entity;

import art.arcane.thaumcraft.entity.monster.boss.EldritchGolemEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.Identifier;

/** TC6 RenderEldritchGolem: eldritch_golem.png, alpha-blended, scaled x1.7, shadow 0.5. */
public final class EldritchGolemRenderer extends MobRenderer<EldritchGolemEntity, EldritchGolemModel.State, EldritchGolemModel> {
    private static final Identifier TEX = Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/eldritch_golem.png");

    public EldritchGolemRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new EldritchGolemModel(ctx.bakeLayer(EldritchGolemModel.LAYER), false), 0.5f);
        EldritchGolemModel cloth = new EldritchGolemModel(ctx.bakeLayer(EldritchGolemModel.CLOTH), true);
        addLayer(new RenderLayer<>(this) {
            @Override
            public void submit(PoseStack ps, SubmitNodeCollector c, int light, EldritchGolemModel.State s, float yRot, float xRot) {
                if (s.isInvisible) return;
                c.submitModel(cloth, s, ps, RenderTypes.entityTranslucent(TEX), light, LivingEntityRenderer.getOverlayCoords(s, 0.0f), -1, null, s.outlineColor, null);
            }
        });
    }

    @Override public Identifier getTextureLocation(EldritchGolemModel.State s) { return TEX; }
    @Override public EldritchGolemModel.State createRenderState() { return new EldritchGolemModel.State(); }

    @Override
    public void extractRenderState(EldritchGolemEntity e, EldritchGolemModel.State s, float pt) {
        super.extractRenderState(e, s, pt);
        s.headless = e.isHeadless();
        s.spawnTimer = e.getSpawnTimer();
        s.attackTimer = e.getAttackTimer();
        s.pt = pt;
    }

    @Override protected void scale(EldritchGolemModel.State s, PoseStack ps) { ps.scale(1.7f, 1.7f, 1.7f); }
    @Override protected RenderType getRenderType(EldritchGolemModel.State s, boolean visible, boolean transparent, boolean glowing) { return RenderTypes.entityTranslucent(TEX); }
}
