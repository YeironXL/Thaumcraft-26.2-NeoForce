package art.arcane.thaumcraft.client.render;

import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity;
import art.arcane.thaumcraft.blockentity.EssentiaTubeBlockEntity;
import art.arcane.thaumcraft.client.render.entity.TcModelLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** TC6 TileJarRenderer (fillable jars): brass brace, lid extension under a connected tube and the aspect label. */
public final class JarRenderer implements BlockEntityRenderer<EssentiaJarBlockEntity, JarRenderer.State> {
    public static final class State extends BlockEntityRenderState { boolean blocked, extension; String filter = ""; Direction facing = Direction.NORTH; int hash; }
    private static final Identifier BRINE = Identifier.fromNamespaceAndPath("thaumcraft", "textures/models/jarbrine.png");
    private static final Identifier LABEL = Identifier.fromNamespaceAndPath("thaumcraft", "textures/models/label.png");
    private final ModelPart brace, extension;
    public JarRenderer(BlockEntityRendererProvider.Context c) { brace = c.bakeLayer(TcModelLayers.JAR_BRACE); extension = c.bakeLayer(TcModelLayers.JAR_LID_EXTENSION); }
    @Override public State createRenderState() { return new State(); }
    @Override
    public void extractRenderState(EssentiaJarBlockEntity te, State s, float pt, Vec3 cam, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(te, s, pt, cam, crumbling);
        s.blocked = !te.alembic() && te.blocked();
        s.extension = !te.alembic() && EssentiaTubeBlockEntity.neighbor(te.getLevel(), te.getBlockPos(), Direction.UP) != null;
        s.filter = te.alembic() ? "" : te.filter();
        s.facing = te.facing();
        s.hash = te.getBlockPos().getX();
    }
    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector c, CameraRenderState cam) {
        pose.pushPose();
        pose.translate(0.5f, 0.01f, 0.5f);
        pose.mulPose(Axis.XP.rotationDegrees(180));
        if (s.blocked) {
            pose.pushPose(); pose.scale(1.001f, 1.001f, 1.001f);
            c.submitModelPart(brace, pose, RenderTypes.entityCutout(BRINE), s.lightCoords, OverlayTexture.NO_OVERLAY, null);
            pose.popPose();
        }
        if (s.extension) {
            pose.pushPose(); pose.scale(0.9f, 1.0f, 0.9f);
            c.submitModelPart(extension, pose, RenderTypes.entityCutout(BRINE), s.lightCoords, OverlayTexture.NO_OVERLAY, null);
            pose.popPose();
        }
        if (!s.filter.isEmpty()) {
            switch (s.facing) {
                case SOUTH -> pose.mulPose(Axis.YP.rotationDegrees(180));
                case EAST -> pose.mulPose(Axis.YP.rotationDegrees(90));
                case WEST -> pose.mulPose(Axis.YP.rotationDegrees(270));
                default -> {}
            }
            float rot = (s.filter.hashCode() + s.hash + s.facing.get3DDataValue()) % 4 - 2;
            int light = s.lightCoords;
            pose.pushPose();
            pose.translate(0.0f, -0.4f, 0.315f);
            pose.mulPose(Axis.ZP.rotationDegrees(rot));
            c.submitCustomGeometry(pose, RenderTypes.entityTranslucent(LABEL), (p, vc) -> quad(p, vc, 0.25f, 0xFFFFFFFF, light));
            pose.popPose();
            pose.pushPose();
            pose.translate(0.0f, -0.4f, 0.316f);
            pose.mulPose(Axis.ZP.rotationDegrees(rot));
            pose.scale(0.021f, 0.021f, 0.021f);
            pose.mulPose(Axis.YP.rotationDegrees(180));
            int col = 0xFF000000 | AspectRegistry.color(s.filter);
            c.submitCustomGeometry(pose, RenderTypes.entityTranslucent(Identifier.fromNamespaceAndPath("thaumcraft", "textures/aspects/" + s.filter + ".png")), (p, vc) -> quad(p, vc, 8f, col, light));
            pose.popPose();
        }
        pose.popPose();
    }
    /** UtilsFX.renderQuadCentered: a square of half-size h facing +Z. */
    private static void quad(PoseStack.Pose p, VertexConsumer vc, float h, int argb, int light) {
        v(p, vc, -h, h, 0, 0, argb, light); v(p, vc, h, h, 1, 0, argb, light); v(p, vc, h, -h, 1, 1, argb, light); v(p, vc, -h, -h, 0, 1, argb, light);
        v(p, vc, -h, -h, 0, 1, argb, light); v(p, vc, h, -h, 1, 1, argb, light); v(p, vc, h, h, 1, 0, argb, light); v(p, vc, -h, h, 0, 0, argb, light);
    }
    private static void v(PoseStack.Pose p, VertexConsumer vc, float x, float y, float u, float w, int argb, int light) {
        vc.addVertex(p, x, y, 0).setColor(argb).setUv(u, w).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 0, 1);
    }
}
