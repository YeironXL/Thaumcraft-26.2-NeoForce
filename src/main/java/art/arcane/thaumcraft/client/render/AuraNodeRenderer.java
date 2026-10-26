package art.arcane.thaumcraft.client.render;

import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.aura.node.NodeModifier;
import art.arcane.thaumcraft.aura.node.NodeType;
import art.arcane.thaumcraft.blockentity.AuraNodeBlockEntity;
import art.arcane.thaumcraft.client.fx.beam.TcBeams;
import art.arcane.thaumcraft.util.Baubles;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * TC4 TileNodeRenderer.renderNode on TC6's leftover auranodes.png (32x32 grid of 64px animation frames): one
 * camera-facing, additively blended layer per aspect from strip 0 tinted with the aspect colour, sized by the square
 * root of its amount and pulsing at a slightly different rate each, then a core from the node type's strip. Bright
 * nodes are larger and brighter, pale ones smaller and dimmer, fading ones flicker. Nodes are always visible (faint,
 * depth tested); goggles of revealing draw them at full strength through walls.
 */
public final class AuraNodeRenderer implements BlockEntityRenderer<AuraNodeBlockEntity, AuraNodeRenderer.State> {
    private static final Identifier TEX = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/auranodes.png");
    private static final int GRID = 32;

    public static final class State extends BlockEntityRenderState {
        int[] colors = new int[0]; float[] amounts = new float[0];
        NodeType type = NodeType.NORMAL; NodeModifier mod = NodeModifier.NONE;
        boolean revealed; float time; int seed; double distance;
    }

    public AuraNodeRenderer(BlockEntityRendererProvider.Context c) {}
    @Override public State createRenderState() { return new State(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public int getViewDistance() { return 64; }

    @Override
    public void extractRenderState(AuraNodeBlockEntity te, State s, float pt, Vec3 cam, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(te, s, pt, cam, crumbling);
        int n = te.aspects.size(), i = 0;
        s.colors = new int[n]; s.amounts = new float[n];
        for (var e : te.aspects.entrySet()) { s.colors[i] = AspectRegistry.color(e.getKey()); s.amounts[i] = e.getValue(); i++; }
        s.type = te.type; s.mod = te.modifier;
        var mc = Minecraft.getInstance();
        s.revealed = mc.player != null && Baubles.hasGoggles(mc.player);
        s.time = (te.getLevel() == null ? 0 : te.getLevel().getGameTime()) + pt;
        s.seed = (int) (te.getBlockPos().asLong() & 0x7fffffff);
        s.distance = cam.distanceTo(Vec3.atCenterOf(te.getBlockPos()));
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector c, CameraRenderState camera) {
        double view = 64.0;
        if (s.distance > view) return;
        float alpha = (float) ((view - s.distance) / view);
        float bscale = 0.5f;
        switch (s.mod) {
            case BRIGHT -> { bscale *= 1.2f; alpha *= 1.5f; }
            case PALE -> { bscale *= 0.8f; alpha *= 0.66f; }
            case FADING -> alpha *= Mth.sin(s.time / 3.0f) * 0.25f + 0.33f;
            default -> {}
        }
        if (!s.revealed) alpha *= 0.45f;
        alpha = Mth.clamp(alpha, 0, 1);
        int frame = (int) ((s.time * 1.25f + s.seed) % GRID);
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(camera.orientation);
        RenderType additive = s.revealed ? TcBeams.overlayAdditive(TEX) : TcBeams.additive(TEX);
        RenderType blended = s.revealed ? TcBeams.overlay(TEX) : TcBeams.translucent(TEX);
        int n = s.colors.length;
        float layerAlpha = alpha / Math.max(1.0f, n / 2.0f);
        for (int i = 0; i < n; i++) {
            float size = 0.15f + 0.07f * Mth.sqrt(s.amounts[i]);
            float scale = Mth.sin(s.time / (14.0f - i)) * bscale * 0.25f + size * bscale * 2.0f;
            quad(ps, c, additive, (frame + i * 5) % GRID, scale, s.colors[i], layerAlpha);
        }
        // core
        RenderType core = s.type == NodeType.DARK || s.type == NodeType.HUNGRY ? blended : additive;
        float coreScale = bscale * (s.type == NodeType.HUNGRY ? 1.5f : 1.0f);
        int coreColor = switch (s.type) { case PURE -> 0xFFFFFF; case TAINTED -> 0xB060FF; default -> 0xFFFFFF; };
        quad(ps, c, core, s.type.row * GRID + frame, coreScale, coreColor, Mth.clamp(alpha * (s.type == NodeType.DARK ? 1.6f : 1.0f), 0, 1));
        if (s.type == NodeType.UNSTABLE && ((int) s.time / 4 + s.seed) % 3 == 0) quad(ps, c, additive, 7 * GRID + frame, bscale * 1.5f, 0xFFFFFF, alpha);
        ps.popPose();
    }

    /** UtilsFX.renderFacingStrip: one frame of a strip as a camera-facing quad. */
    private static void quad(PoseStack ps, SubmitNodeCollector c, RenderType type, int frame, float scale, int color, float alpha) {
        float u0 = frame % GRID / (float) GRID, u1 = u0 + 1.0f / GRID, v0 = frame / GRID / (float) GRID, v1 = v0 + 1.0f / GRID;
        int col = ARGB.color((int) (Mth.clamp(alpha, 0, 1) * 255.0f), color);
        c.submitCustomGeometry(ps, type, (pose, vc) -> {
            TcBeams.v(pose, vc, new Vec3(-scale, -scale, 0), u1, v1, col);
            TcBeams.v(pose, vc, new Vec3(-scale, scale, 0), u1, v0, col);
            TcBeams.v(pose, vc, new Vec3(scale, scale, 0), u0, v0, col);
            TcBeams.v(pose, vc, new Vec3(scale, -scale, 0), u0, v1, col);
        });
    }
}
