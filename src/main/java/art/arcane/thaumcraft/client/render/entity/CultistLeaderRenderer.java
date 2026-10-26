package art.arcane.thaumcraft.client.render.entity;

import art.arcane.thaumcraft.entity.monster.boss.CultistLeaderEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/** TC6 RenderCultistLeader: the cultist biped (cultist.png) with armour and held item, scaled x1.15. */
public final class CultistLeaderRenderer extends MobRenderer<CultistLeaderEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
    private static final Identifier SKIN = Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/cultist.png");

    public CultistLeaderRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(CultistRenderer.LAYER)), 0.5f);
        addLayer(new net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer<>(this,
                net.minecraft.client.renderer.entity.ArmorModelSet.bake(net.minecraft.client.model.geom.ModelLayers.ZOMBIE_ARMOR, ctx.getModelSet(), HumanoidModel::new), ctx.getEquipmentRenderer()));
        addLayer(new ItemInHandLayer<>(this));
    }

    @Override public Identifier getTextureLocation(HumanoidRenderState s) { return SKIN; }
    @Override public HumanoidRenderState createRenderState() { return new HumanoidRenderState(); }

    @Override
    public void extractRenderState(CultistLeaderEntity e, HumanoidRenderState s, float pt) {
        super.extractRenderState(e, s, pt);
        HumanoidMobRenderer.extractHumanoidRenderState(e, s, pt, itemModelResolver);
    }

    @Override protected void scale(HumanoidRenderState s, PoseStack ps) { ps.scale(1.15f, 1.15f, 1.15f); }
}
