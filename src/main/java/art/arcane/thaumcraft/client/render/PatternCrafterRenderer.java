package art.arcane.thaumcraft.client.render;

import art.arcane.thaumcraft.block.PatternCrafterBlock;
import art.arcane.thaumcraft.blockentity.PatternCrafterBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
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

/** TC6 TilePatternCrafterRenderer: the selected mode icon on the dial face and two counter-rotating brass gears. */
public final class PatternCrafterRenderer implements BlockEntityRenderer<PatternCrafterBlockEntity, PatternCrafterRenderer.State> {
    private static final Identifier TEX = Identifier.fromNamespaceAndPath("thaumcraft", "textures/block/pattern_crafter_modes.png");
    private static final Identifier ICON = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/gear_brass.png");
    public static final class State extends BlockEntityRenderState { int type; float rot; Direction facing = Direction.SOUTH; }
    public PatternCrafterRenderer(BlockEntityRendererProvider.Context c) {}
    @Override public State createRenderState() { return new State(); }
    @Override
    public void extractRenderState(PatternCrafterBlockEntity te, State s, float pt, Vec3 cam, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(te, s, pt, cam, crumbling);
        s.type = te.type;
        s.rot = te.rot + te.rp * pt;
        s.facing = te.getBlockState().getValue(PatternCrafterBlock.FACING);
    }
    private static void quad(PoseStack.Pose p, VertexConsumer vc, float s, float u0, float u1, float v0, float v1, int light) {
        // UtilsFX.renderQuadCentered vertex order, both windings
        float h = 0.5f * s;
        float[][] q = {{-h, h, u1, v1}, {h, h, u1, v0}, {h, -h, u0, v0}, {-h, -h, u0, v1}};
        for (float[] v : q) vc.addVertex(p, v[0], v[1], 0).setColor(-1).setUv(v[2], v[3]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 0, 1);
        for (int i = 3; i >= 0; --i) vc.addVertex(p, q[i][0], q[i][1], 0).setColor(-1).setUv(q[i][2], q[i][3]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 0, -1);
    }
    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector c, CameraRenderState camera) {
        int light = s.lightCoords;
        pose.pushPose();
        pose.translate(0.5, 0.75, 0.5);
        switch (s.facing) {
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(90));
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(270));
            case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(180));
            default -> {}
        }
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(90));
        pose.translate(0, 0, -0.5);
        float u0 = (s.type % 10) / 10f, u1 = u0 + 0.1f;
        c.submitCustomGeometry(pose, RenderTypes.entityTranslucent(TEX), (p, vc) -> quad(p, vc, 0.5f, u0, u1, 0, 1, light));
        pose.popPose();
        for (int side = -1; side <= 1; side += 2) {
            pose.pushPose();
            pose.translate(0.2f * side, -0.40625f, 0.05f);
            pose.mulPose(Axis.ZP.rotationDegrees((side < 0 ? -s.rot : s.rot) % 360f));
            pose.scale(0.5f, 0.5f, 1f);
            pose.translate(-0.5f, -0.5f, 0f);
            c.submitCustomGeometry(pose, RenderTypes.entityCutout(ICON), (p, vc) -> TcRender.textureIn3D(p, vc, 1f, 0f, 0f, 1f, 16, 16, 0.1f, light));
            pose.popPose();
        }
        pose.popPose();
    }
}
