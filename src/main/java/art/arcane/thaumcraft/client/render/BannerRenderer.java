package art.arcane.thaumcraft.client.render;

import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.block.TcBannerBlock;
import art.arcane.thaumcraft.blockentity.TcBannerBlockEntity;
import art.arcane.thaumcraft.client.render.entity.TcModelLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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

/** TC6 TileBannerRenderer with ModelBanner: pole (standing only), beam, dyed tabs and a gently swaying cloth with its aspect. */
public final class BannerRenderer implements BlockEntityRenderer<TcBannerBlockEntity, BannerRenderer.State> {
    public static final class State extends BlockEntityRenderState { int rotation, color = -1; boolean wall; String aspect = ""; float sway; }
    private static final Identifier CULT = Identifier.fromNamespaceAndPath("thaumcraft", "textures/models/banner_cultist.png");
    private static final Identifier BLANK = Identifier.fromNamespaceAndPath("thaumcraft", "textures/models/banner_blank.png");
    private final ModelPart pole, beam, tabs, cloth;

    public BannerRenderer(BlockEntityRendererProvider.Context c) {
        ModelPart root = c.bakeLayer(TcModelLayers.BANNER);
        pole = root.getChild("pole"); beam = root.getChild("beam"); tabs = root.getChild("tabs"); cloth = root.getChild("banner");
    }
    @Override public State createRenderState() { return new State(); }
    @Override
    public void extractRenderState(TcBannerBlockEntity te, State s, float pt, Vec3 cam, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(te, s, pt, cam, crumbling);
        var st = te.getBlockState();
        s.rotation = st.hasProperty(TcBannerBlock.ROTATION) ? st.getValue(TcBannerBlock.ROTATION) : 0;
        s.wall = st.hasProperty(TcBannerBlock.WALL) && st.getValue(TcBannerBlock.WALL);
        s.color = st.getBlock() instanceof TcBannerBlock b && b.dye != null ? b.dye.getTextureDiffuseColor() | 0xFF000000 : -1;
        s.aspect = te.aspect();
        var p = Minecraft.getInstance().player;
        var bp = te.getBlockPos();
        float f3 = bp.getX() * 7 + bp.getY() * 9 + bp.getZ() * 13 + (p == null ? 0 : p.tickCount) + pt;
        s.sway = 0.02f - Mth.sin(f3 / 11.0f) * 0.02f;
    }
    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector c, CameraRenderState cam) {
        Identifier tex = s.aspect.isEmpty() && s.color == -1 ? CULT : BLANK;
        var rt = RenderTypes.entityCutout(tex);
        int light = s.lightCoords;
        pose.pushPose();
        pose.translate(0.5f, 1.5f, 0.5f);
        pose.mulPose(Axis.XP.rotationDegrees(180));
        pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.mulPose(Axis.YP.rotationDegrees(s.rotation * 360 / 16.0f));
        if (!s.wall) c.submitModelPart(pole, pose, rt, light, OverlayTexture.NO_OVERLAY, null);
        else pose.translate(0.0, 1.0, -0.4125);
        c.submitModelPart(beam, pose, rt, light, OverlayTexture.NO_OVERLAY, null);
        c.submitModelPart(tabs, pose, rt, light, OverlayTexture.NO_OVERLAY, null, s.color, null);
        // Banner.rotateAngleX = sway, about its pivot (0, -5, 0).
        pose.pushPose();
        pose.translate(0, -5 / 16f, 0);
        pose.mulPose(Axis.XP.rotation(s.sway));
        pose.translate(0, 5 / 16f, 0);
        c.submitModelPart(cloth, pose, rt, light, OverlayTexture.NO_OVERLAY, null, s.color, null);
        pose.popPose();
        if (!s.aspect.isEmpty()) {
            pose.pushPose();
            pose.translate(0.0f, 0.0f, 0.05001f);
            pose.scale(0.0375f, 0.0375f, 0.0375f);
            pose.mulPose(Axis.YP.rotationDegrees(180));
            pose.mulPose(Axis.XP.rotation(-s.sway * 2.0f));
            int col = 0xBF000000 | AspectRegistry.color(s.aspect);
            // UtilsFX.drawTag(-8, 0, aspect): a 16x16 icon whose top-left sits at (-8, 0).
            pose.translate(0, 8, 0);
            c.submitCustomGeometry(pose, RenderTypes.entityTranslucent(Identifier.fromNamespaceAndPath("thaumcraft", "textures/aspects/" + s.aspect + ".png")), (p, vc) -> quad(p, vc, 8f, col, light));
            pose.popPose();
        }
        pose.popPose();
    }
    private static void quad(PoseStack.Pose p, VertexConsumer vc, float h, int argb, int light) {
        v(p, vc, -h, h, 0, 1, argb, light); v(p, vc, h, h, 1, 1, argb, light); v(p, vc, h, -h, 1, 0, argb, light); v(p, vc, -h, -h, 0, 0, argb, light);
    }
    private static void v(PoseStack.Pose p, VertexConsumer vc, float x, float y, float u, float w, int argb, int light) {
        vc.addVertex(p, x, y, 0).setColor(argb).setUv(u, w).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 0, -1);
    }
}
