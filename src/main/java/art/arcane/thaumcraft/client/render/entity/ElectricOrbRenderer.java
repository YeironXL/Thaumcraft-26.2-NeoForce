package art.arcane.thaumcraft.client.render.entity;

import art.arcane.thaumcraft.client.fx.beam.TcBeams;
import art.arcane.thaumcraft.entity.projectile.GolemOrbEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** TC6 RenderElectricOrb: a bobbing additive spark from the particle sheet (red row for crimson cleric orbs). */
public final class ElectricOrbRenderer extends EntityRenderer<GolemOrbEntity, ElectricOrbRenderer.State> {
    public static final class State extends EntityRenderState { int ticks; boolean red; }
    private static final Identifier PARTICLES = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png");

    public ElectricOrbRenderer(EntityRendererProvider.Context ctx) { super(ctx); shadowRadius = 0.0f; }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(GolemOrbEntity e, State s, float pt) { super.extractRenderState(e, s, pt); s.ticks = e.tickCount; s.red = e.red; }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector c, CameraRenderState cam) {
        ps.pushPose();
        ps.mulPose(cam.orientation);
        float bob = Mth.sin(s.ticks / 5.0f) * 0.2f + 0.2f;
        ps.scale(1.0f + bob, 1.0f + bob, 1.0f + bob);
        float f2 = (1 + s.ticks % 6) / 32.0f, f3 = f2 + 0.03125f, f4 = s.red ? 0.1875f : 0.21875f, f5 = f4 + 0.03125f, h = 0.5f;
        int col = 0xFFFFFFFF;
        c.submitCustomGeometry(ps, TcBeams.additive(PARTICLES), (pose, vc) -> {
            TcBeams.v(pose, vc, new Vec3(-h, -h, 0), f2, f5, col); TcBeams.v(pose, vc, new Vec3(h, -h, 0), f3, f5, col);
            TcBeams.v(pose, vc, new Vec3(h, h, 0), f3, f4, col); TcBeams.v(pose, vc, new Vec3(-h, h, 0), f2, f4, col);
            TcBeams.v(pose, vc, new Vec3(-h, h, 0), f2, f4, col); TcBeams.v(pose, vc, new Vec3(h, h, 0), f3, f4, col);
            TcBeams.v(pose, vc, new Vec3(h, -h, 0), f3, f5, col); TcBeams.v(pose, vc, new Vec3(-h, -h, 0), f2, f5, col);
        });
        ps.popPose();
        super.submit(s, ps, c, cam);
    }
}
