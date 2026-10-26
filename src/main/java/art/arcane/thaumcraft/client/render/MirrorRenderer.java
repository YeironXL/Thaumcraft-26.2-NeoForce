package art.arcane.thaumcraft.client.render;

import art.arcane.thaumcraft.block.MirrorBlock;
import art.arcane.thaumcraft.blockentity.EssentiaMirrorBlockEntity;
import art.arcane.thaumcraft.blockentity.MirrorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * TC6 TileMirrorRenderer: the extruded frame (mirrorframe / mirrorframe2) against the supporting face, and the glass:
 * the plain pane when unlinked; when linked (and the player within 32 blocks) TC6's 16-layer parallax plane
 * (layer 0 textures/misc/tunnel.png, layers 1-15 textures/misc/particlefield.png - the 1.12 end-portal algorithm, which
 * 26.2 runs as the 16-layer end_gateway shader) under the translucent pane.
 */
public final class MirrorRenderer implements BlockEntityRenderer<MirrorBlockEntity, MirrorRenderer.State> {
    public static final class State extends BlockEntityRenderState { Direction dir = Direction.UP; boolean linked, essentia, near; }
    private static final Identifier FRAME = Identifier.fromNamespaceAndPath("thaumcraft", "textures/block/mirrorframe.png");
    private static final Identifier FRAME2 = Identifier.fromNamespaceAndPath("thaumcraft", "textures/block/mirrorframe2.png");
    private static final Identifier MP = Identifier.fromNamespaceAndPath("thaumcraft", "textures/block/mirrorpane.png");
    private static final Identifier MPT = Identifier.fromNamespaceAndPath("thaumcraft", "textures/block/mirrorpanetrans.png");

    private static final Identifier TUNNEL = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/tunnel.png");
    private static final Identifier FIELD = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/particlefield.png");
    /** drawPlane*: for i in 0..15, i==0 bindTexture(t1=tunnel), else t2=particlefield; 16 layers = END_GATEWAY pipeline. */
    private static final RenderType TUNNEL_PLANE = RenderType.create("thaumcraft_mirror_tunnel",
            RenderSetup.builder(RenderPipelines.END_GATEWAY).withTexture("Sampler0", TUNNEL).withTexture("Sampler1", FIELD).createRenderSetup());

    public MirrorRenderer(BlockEntityRendererProvider.Context c) {}
    @Override public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(MirrorBlockEntity te, State s, float pt, Vec3 cam, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(te, s, pt, cam, crumbling);
        s.dir = te.getBlockState().getValue(MirrorBlock.FACING);
        s.linked = te.linked;
        s.essentia = te instanceof EssentiaMirrorBlockEntity;
        var p = Minecraft.getInstance().player;
        s.near = p != null && p.distanceToSqr(Vec3.atCenterOf(te.getBlockPos())) < 1024.0;
    }

    /** translateFromOrientation(x, y, z, orientation, off). */
    private static void orient(PoseStack ps, Direction dir, float off) {
        switch (dir) {
            case DOWN -> { ps.translate(0, 1, 1); ps.mulPose(Axis.XP.rotationDegrees(-90)); }
            case UP -> ps.mulPose(Axis.XP.rotationDegrees(90));
            case NORTH -> ps.translate(0, 0, 1);
            case SOUTH -> { ps.translate(1, 0, 0); ps.mulPose(Axis.YP.rotationDegrees(180)); }
            case WEST -> { ps.translate(1, 0, 1); ps.mulPose(Axis.YP.rotationDegrees(90)); }
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(-90));
        }
        ps.translate(0, 0, -off);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector c, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        orient(ps, s.dir, 0.01f);
        c.submitCustomGeometry(ps, RenderTypes.entityCutout(s.essentia ? FRAME2 : FRAME), (p, vc) -> TcRender.textureIn3D(p, vc, 1f, 0f, 0f, 1f, 16, 16, 0.0625f, light));
        ps.popPose();
        boolean open = s.linked && s.near;
        if (open) {
            final Direction dir = s.dir;
            c.submitCustomGeometry(ps, TUNNEL_PLANE, (p, vc) -> plane(p, vc, dir));
        }
        ps.pushPose();
        orient(ps, s.dir, 0.02f);
        ps.mulPose(Axis.ZP.rotationDegrees(90));
        ps.translate(0.5, -0.5, 0.0);
        c.submitCustomGeometry(ps, RenderTypes.entityTranslucent(open ? MPT : MP), (p, vc) -> quadCentered(p, vc, light));
        ps.popPose();
    }

    /** drawPlane*: the void plane inset by 3px, 0.01 in front of the supporting face. */
    private static void plane(PoseStack.Pose p, VertexConsumer vc, Direction dir) {
        float o = 0.1875f, lo = 0.01f, hi = 0.99f;
        float[][] q = switch (dir) {
            case DOWN -> new float[][] {{o, hi, 1 - o}, {o, hi, o}, {1 - o, hi, o}, {1 - o, hi, 1 - o}};
            case UP -> new float[][] {{o, lo, o}, {o, lo, 1 - o}, {1 - o, lo, 1 - o}, {1 - o, lo, o}};
            case NORTH -> new float[][] {{o, o, hi}, {o, 1 - o, hi}, {1 - o, 1 - o, hi}, {1 - o, o, hi}};
            case SOUTH -> new float[][] {{o, 1 - o, lo}, {o, o, lo}, {1 - o, o, lo}, {1 - o, 1 - o, lo}};
            case WEST -> new float[][] {{hi, o, o}, {hi, o, 1 - o}, {hi, 1 - o, 1 - o}, {hi, 1 - o, o}};
            case EAST -> new float[][] {{lo, 1 - o, o}, {lo, 1 - o, 1 - o}, {lo, o, 1 - o}, {lo, o, o}};
        };
        for (float[] v : q) vc.addVertex(p, v[0], v[1], v[2]).setColor(-1).setUv(0f, 0f).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(p, 0f, 1f, 0f);
        for (int i = 3; i >= 0; --i) vc.addVertex(p, q[i][0], q[i][1], q[i][2]).setColor(-1).setUv(0f, 0f).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(p, 0f, 1f, 0f);
    }

    /** UtilsFX.renderQuadCentered: a unit quad around the origin (drawn from both sides). */
    private static void quadCentered(PoseStack.Pose p, VertexConsumer vc, int light) {
        float[][] v = {{-0.5f, 0.5f, 1, 1}, {0.5f, 0.5f, 1, 0}, {0.5f, -0.5f, 0, 0}, {-0.5f, -0.5f, 0, 1}};
        for (float[] a : v) vc.addVertex(p, a[0], a[1], 0).setColor(-1).setUv(a[2], a[3]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 0, 1);
        for (int i = 3; i >= 0; --i) vc.addVertex(p, v[i][0], v[i][1], 0).setColor(-1).setUv(v[i][2], v[i][3]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 0, -1);
    }
}
