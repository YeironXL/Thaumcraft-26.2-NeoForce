package art.arcane.thaumcraft.client.render.entity;

import art.arcane.thaumcraft.client.fx.beam.TcBeams;
import art.arcane.thaumcraft.entity.WispEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/**
 * TC6 RenderWisp: no model, three additive camera-facing sprites at the wisp's position: a white animated core
 * (particles.png frames 512+), a faint white halo (frames 320+) and an aspect-coloured node swirl (auranodes.png frames 800+).
 */
public final class WispRenderer extends EntityRenderer<WispEntity, WispRenderer.State> {
    public static final class State extends EntityRenderState { int ticks; int color; boolean dead; }
    private static final Identifier PARTICLES = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png");
    private static final Identifier NODES = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/auranodes.png");

    public WispRenderer(EntityRendererProvider.Context ctx) { super(ctx); shadowRadius = 0.0f; }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(WispEntity e, State s, float pt) {
        super.extractRenderState(e, s, pt);
        s.ticks = e.tickCount; s.color = e.getAspectColor(); s.dead = e.getHealth() <= 0.0f;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector c, CameraRenderState cam) {
        if (s.dead) return;
        ps.pushPose();
        ps.mulPose(cam.orientation);
        quad(ps, c, PARTICLES, 64, 512 + s.ticks % 16, 0.4f, 0xFFFFFF, 1.0f);
        quad(ps, c, PARTICLES, 64, 320 + s.ticks % 16, 0.75f, 0xFFFFFF, 0.25f);
        quad(ps, c, NODES, 32, 800 + s.ticks % 16, 0.75f, s.color, 0.5f);
        ps.popPose();
        super.submit(s, ps, c, cam);
    }

    /** UtilsFX.renderFacingQuad (blend 770/1, full bright). */
    private static void quad(PoseStack ps, SubmitNodeCollector c, Identifier tex, int grid, int frame, float scale, int color, float alpha) {
        float u0 = frame % grid / (float) grid, u1 = u0 + 1.0f / grid, v0 = frame / grid / (float) grid, v1 = v0 + 1.0f / grid;
        int col = ARGB.color((int) (alpha * 255.0f), color);
        c.submitCustomGeometry(ps, TcBeams.additive(tex), (pose, vc) -> {
            TcBeams.v(pose, vc, new net.minecraft.world.phys.Vec3(-scale, -scale, 0), u1, v1, col);
            TcBeams.v(pose, vc, new net.minecraft.world.phys.Vec3(-scale, scale, 0), u1, v0, col);
            TcBeams.v(pose, vc, new net.minecraft.world.phys.Vec3(scale, scale, 0), u0, v0, col);
            TcBeams.v(pose, vc, new net.minecraft.world.phys.Vec3(scale, -scale, 0), u0, v1, col);
        });
    }
}
