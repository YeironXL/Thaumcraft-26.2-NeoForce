package art.arcane.thaumcraft.golems.client;

import art.arcane.thaumcraft.entity.projectile.GolemDartEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

/** TC6 RenderDart: the vanilla arrow at 0.033/0.05625 scale. */
public final class GolemDartRenderer extends ArrowRenderer<GolemDartEntity, ArrowRenderState> {
    private static final Identifier TEX = Identifier.withDefaultNamespace("textures/entity/projectiles/arrow.png");
    public GolemDartRenderer(EntityRendererProvider.Context ctx) { super(ctx); }
    @Override protected Identifier getTextureLocation(ArrowRenderState s) { return TEX; }
    @Override public ArrowRenderState createRenderState() { return new ArrowRenderState(); }
    @Override public void submit(ArrowRenderState s, PoseStack ps, SubmitNodeCollector c, CameraRenderState cam) {
        ps.pushPose();
        float k = 0.033f / 0.05625f;
        ps.scale(k, k, k);
        super.submit(s, ps, c, cam);
        ps.popPose();
    }
}
