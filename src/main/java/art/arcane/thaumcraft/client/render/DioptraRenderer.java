package art.arcane.thaumcraft.client.render;

import art.arcane.thaumcraft.block.DioptraBlock;
import art.arcane.thaumcraft.blockentity.DioptraBlockEntity;
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
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** TC6 TileDioptraRenderer: glowing 12x12 height-map of the aura grid on top, plus four pulsing side panes. */
public final class DioptraRenderer implements BlockEntityRenderer<DioptraBlockEntity, DioptraRenderer.State> {
    private static final Identifier GRID = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/gridblock.png");
    private static final Identifier SIDE = Identifier.fromNamespaceAndPath("thaumcraft", "textures/models/dioptra_side.png");
    private static final int FULL = 0xF000F0;

    public static final class State extends BlockEntityRenderState { final int[] grid = new int[169]; float t; int counter; boolean vis; }
    public DioptraRenderer(BlockEntityRendererProvider.Context c) {}
    @Override public State createRenderState() { return new State(); }
    @Override public AABB getRenderBoundingBox(DioptraBlockEntity te) {
        var p = te.getBlockPos();
        return new AABB(p.getX() - 0.3, p.getY() - 0.3, p.getZ() - 0.3, p.getX() + 1.3, p.getY() + 2.3, p.getZ() + 1.3);
    }
    @Override
    public void extractRenderState(DioptraBlockEntity te, State s, float pt, Vec3 cam, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(te, s, pt, cam, crumbling);
        System.arraycopy(te.grid, 0, s.grid, 0, 169);
        s.t = te.getLevel().getGameTime() + pt;
        s.counter = te.counter;
        s.vis = te.getBlockState().getValue(DioptraBlock.ENABLED);
    }

    private static int col(float r, float g, float b, float a) {
        return ((int) (Mth.clamp(a, 0, 1) * 255) << 24) | ((int) (Mth.clamp(r * 0.8f, 0, 1) * 255) << 16) | ((int) (Mth.clamp(g, 0, 1) * 255) << 8) | (int) (Mth.clamp(b, 0, 1) * 255);
    }
    private static void v(PoseStack.Pose p, VertexConsumer vc, float x, float y, float z, float u, float w, int c) {
        vc.addVertex(p, x, y, z).setColor(c).setUv(u, w).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL).setNormal(p, 0, 1, 0);
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        float t = s.t;
        float rc = Mth.sin(t / 12f) * 0.05f + 0.85f;
        float gc = Mth.sin(t / 11f) * 0.05f + (s.vis ? 0.9f : 0.45f);
        float bc = Mth.sin(t / 10f) * 0.05f + 0.95f;
        int[] g = s.grid.clone();
        int counter = s.counter;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.pushPose();
        pose.translate(-0.495, 0.501, -0.495);
        pose.scale(0.99f, 1.0f, 0.99f);
        collector.submitCustomGeometry(pose, RenderTypes.entityTranslucentEmissive(GRID), (p, vc) -> {
            int c9 = col(rc, gc, bc, 0.9f), c0 = col(rc, gc, bc, 0f);
            for (int a = 0; a < 12; ++a) for (int b = 0; b < 12; ++b) {
                float h00 = g[a + b * 13] / 96f, h10 = g[a + 1 + b * 13] / 96f, h11 = g[a + 1 + (b + 1) * 13] / 96f, h01 = g[a + (b + 1) * 13] / 96f;
                float x0 = a / 12f, x1 = (a + 1) / 12f, z0 = b / 12f, z1 = (b + 1) / 12f;
                // top surface (double sided by emitting both windings)
                v(p, vc, x0, h01, z1, 0, 0, c9); v(p, vc, x1, h11, z1, 1, 0, c9); v(p, vc, x1, h10, z0, 1, 1, c9); v(p, vc, x0, h00, z0, 0, 1, c9);
                v(p, vc, x0, h00, z0, 0, 1, c9); v(p, vc, x1, h10, z0, 1, 1, c9); v(p, vc, x1, h11, z1, 1, 0, c9); v(p, vc, x0, h01, z1, 0, 0, c9);
                if (a == 0) skirt(p, vc, 0, z0, 0, z1, h00, h01, c0, c9);
                if (a == 11) skirt(p, vc, 1, z0, 1, z1, h10, h11, c0, c9);
                if (b == 0) skirt(p, vc, x0, 0, x1, 0, h00, h10, c0, c9);
                if (b == 11) skirt(p, vc, x0, 1, x1, 1, h01, h11, c0, c9);
            }
        });
        pose.popPose();
        pose.translate(0, 1, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(270));
        int side = col(rc, gc, bc, 0.8f);
        for (int q = 0; q < 4; ++q) {
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(90f * q));
            pose.translate(0, 0, -0.5);
            collector.submitCustomGeometry(pose, RenderTypes.entityTranslucentEmissive(SIDE), (p, vc) -> {
                v(p, vc, -0.5f, 0.5f, 0, 0, 0, side); v(p, vc, 0.5f, 0.5f, 0, 1, 0, side); v(p, vc, 0.5f, -0.5f, 0, 1, 1, side); v(p, vc, -0.5f, -0.5f, 0, 0, 1, side);
                v(p, vc, -0.5f, -0.5f, 0, 0, 1, side); v(p, vc, 0.5f, -0.5f, 0, 1, 1, side); v(p, vc, 0.5f, 0.5f, 0, 1, 0, side); v(p, vc, -0.5f, 0.5f, 0, 0, 0, side);
            });
            pose.popPose();
        }
        pose.popPose();
    }
    /** Edge skirt from the base (transparent) up to the grid heights (0.9 alpha), both windings. */
    private static void skirt(PoseStack.Pose p, VertexConsumer vc, float xa, float za, float xb, float zb, float ha, float hb, int c0, int c9) {
        v(p, vc, xa, 0, za, 0, 1, c0); v(p, vc, xb, 0, zb, 1, 1, c0); v(p, vc, xb, hb, zb, 1, 0, c9); v(p, vc, xa, ha, za, 0, 0, c9);
        v(p, vc, xa, ha, za, 0, 0, c9); v(p, vc, xb, hb, zb, 1, 0, c9); v(p, vc, xb, 0, zb, 1, 1, c0); v(p, vc, xa, 0, za, 0, 1, c0);
    }
}
