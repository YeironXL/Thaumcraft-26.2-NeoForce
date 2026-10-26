package art.arcane.thaumcraft.client.render;

import art.arcane.thaumcraft.blockentity.BrainJarBlockEntity;
import art.arcane.thaumcraft.client.render.entity.TcModelLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** TC6 TileJarRenderer.renderBrain: the bobbing brain (ModelBrain, brain2.png) turned towards its target, in brine. */
public final class BrainJarRenderer implements BlockEntityRenderer<BrainJarBlockEntity, BrainJarRenderer.State> {
    public static final class State extends BlockEntityRenderState { float rot, bob; }
    private static final Identifier BRAIN = Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/brain2.png");
    private static final Identifier BRINE = Identifier.fromNamespaceAndPath("thaumcraft", "textures/models/jarbrine.png");
    private final ModelPart brain, brine;
    public BrainJarRenderer(BlockEntityRendererProvider.Context c) { brain = c.bakeLayer(TcModelLayers.BRAIN); brine = c.bakeLayer(TcModelLayers.JAR_BRINE); }
    @Override public State createRenderState() { return new State(); }
    @Override
    public void extractRenderState(BrainJarBlockEntity te, State s, float pt, Vec3 cam, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(te, s, pt, cam, crumbling);
        float d = te.rota - te.rotb;
        while (d >= Mth.PI) d -= Mth.TWO_PI;
        while (d < -Mth.PI) d += Mth.TWO_PI;
        s.rot = te.rotb + d * pt;
        var p = Minecraft.getInstance().player;
        s.bob = Mth.sin(((p == null ? 0 : p.tickCount) + pt) / 14.0f) * 0.03f + 0.03f;
    }
    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector c, CameraRenderState cam) {
        pose.pushPose();
        pose.translate(0.5f, 0.01f, 0.5f);
        pose.mulPose(Axis.XP.rotationDegrees(180));
        pose.pushPose();
        pose.translate(0.0f, -0.8f + s.bob, 0.0f);
        pose.mulPose(Axis.YP.rotation(s.rot));
        pose.mulPose(Axis.YP.rotationDegrees(-90));
        pose.scale(0.4f, 0.4f, 0.4f);
        c.submitModelPart(brain, pose, RenderTypes.entityCutout(BRAIN), s.lightCoords, OverlayTexture.NO_OVERLAY, null);
        pose.popPose();
        c.submitModelPart(brine, pose, RenderTypes.entityTranslucent(BRINE), s.lightCoords, OverlayTexture.NO_OVERLAY, null);
        pose.popPose();
    }
}
