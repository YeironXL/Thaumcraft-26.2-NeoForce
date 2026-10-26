package art.arcane.thaumcraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** TC6 RenderThaumicSlime: the vanilla slime model and outer gel, both with the purple tslime texture. */
public final class ThaumicSlimeRenderer extends SlimeRenderer {
    private static final Identifier TEX = Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/tslime.png");
    public ThaumicSlimeRenderer(EntityRendererProvider.Context c) {
        super(c);
        layers.clear();
        SlimeModel outer = new SlimeModel(c.getModelSet().bakeLayer(ModelLayers.SLIME_OUTER));
        addLayer(new RenderLayer<>(this) {
            @Override public void submit(PoseStack pose, SubmitNodeCollector col, int light, SlimeRenderState s, float yRot, float xRot) {
                if (s.isInvisible) return;
                col.order(1).submitModel(outer, s, pose, RenderTypes.entityTranslucent(TEX), light, LivingEntityRenderer.getOverlayCoords(s, 0.0f), s.outlineColor, null);
            }
        });
    }
    @Override public Identifier getTextureLocation(SlimeRenderState s) { return TEX; }
}
