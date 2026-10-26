package art.arcane.thaumcraft.client.render.entity;

import art.arcane.thaumcraft.entity.FocusMineEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Unit;

/** TC6 RenderFocusMine: the grappler model, pulsing from white to red as the arming counter runs. */
public final class FocusMineRenderer extends EntityRenderer<FocusMineEntity, FocusMineRenderer.State> {
    public static final class State extends EntityRenderState { float pulse, yRot, xRot; }
    private static final Identifier TEX = Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/mine.png");
    private final Model<Unit> model;

    public FocusMineRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        ModelPart root = ctx.bakeLayer(TcModelLayers.GRAPPLER);
        this.model = new Model.Simple(root, RenderTypes::entityTranslucent);
        this.shadowRadius = 0.0f;
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(FocusMineEntity e, State s, float pt) {
        super.extractRenderState(e, s, pt);
        s.pulse = (e.counter + pt) % 8.0f / 8.0f;
        s.yRot = e.getYRot(pt); s.xRot = e.getXRot(pt);
    }
    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector c, CameraRenderState cam) {
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(s.yRot - 90.0f));
        ps.mulPose(Axis.ZP.rotationDegrees(s.xRot));
        int color = ARGB.colorFromFloat(1.0f, 1.0f, 1.0f - s.pulse, 1.0f - s.pulse);
        c.submitModel(model, Unit.INSTANCE, ps, RenderTypes.entityTranslucent(TEX), 0xF000F0, OverlayTexture.NO_OVERLAY, color, null, s.outlineColor, null);
        ps.popPose();
        super.submit(s, ps, c, cam);
    }
}
