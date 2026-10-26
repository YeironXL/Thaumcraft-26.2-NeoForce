package art.arcane.thaumcraft.client.render.entity;

import art.arcane.thaumcraft.entity.projectile.EldritchOrbEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Random;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.joml.Quaternionf;

/** TC6 RenderEldritchOrb: twelve white additive rays growing in over 10 ticks around a dark animated wisp sprite. */
public final class EldritchOrbRenderer extends EntityRenderer<EldritchOrbEntity, EldritchOrbRenderer.State> {
    public static final class State extends EntityRenderState { int ticks; }
    private static final Identifier PARTICLES = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png");

    public EldritchOrbRenderer(EntityRendererProvider.Context ctx) { super(ctx); shadowRadius = 0.0f; }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(EldritchOrbEntity e, State s, float pt) { super.extractRenderState(e, s, pt); s.ticks = e.tickCount; }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector c, CameraRenderState cam) {
        float f1 = s.ticks / 80.0f;
        float grow = Math.min(s.ticks, 10) / 10.0f;
        c.submitCustomGeometry(ps, RenderTypes.dragonRays(), (pose, vc) -> {
            Random random = new Random(187L);
            Quaternionf q = new Quaternionf();
            int white = 0xFFFFFFFF;
            for (int i = 0; i < 12; ++i) {
                q.rotationX(random.nextFloat() * 6.2831855f).rotateY(random.nextFloat() * 6.2831855f).rotateZ(random.nextFloat() * 6.2831855f)
                        .rotateX(random.nextFloat() * 6.2831855f).rotateY(random.nextFloat() * 6.2831855f).rotateZ(random.nextFloat() * 6.2831855f + f1 * 6.2831855f);
                pose.rotate(q);
                float fa = (random.nextFloat() * 20.0f + 5.0f) / (30.0f / grow);
                float f4 = (random.nextFloat() * 2.0f + 1.0f) / (30.0f / grow);
                // glBegin(TRIANGLE_FAN) centre + 4 rim vertices = 3 triangles; rim colour (64,64,64) clamps to white
                float[][] rim = { { -0.866f * f4, fa, -0.5f * f4 }, { 0.866f * f4, fa, -0.5f * f4 }, { 0.0f, fa, f4 }, { -0.866f * f4, fa, -0.5f * f4 } };
                for (int t = 0; t < 3; t++) {
                    vc.addVertex(pose, 0, 0, 0).setColor(white).setUv(0f, 0f).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0f, 1f, 0f);
                    vc.addVertex(pose, rim[t][0], rim[t][1], rim[t][2]).setColor(white).setUv(0f, 0f).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0f, 1f, 0f);
                    vc.addVertex(pose, rim[t + 1][0], rim[t + 1][1], rim[t + 1][2]).setColor(white).setUv(0f, 0f).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0f, 1f, 0f);
                }
            }
        });
        ps.pushPose();
        ps.mulPose(cam.orientation);
        ps.scale(0.75f, 0.75f, 0.75f);
        float u0 = s.ticks % 13 / 64.0f, u1 = u0 + 0.015625f, v0 = 0.046875f, v1 = v0 + 0.015625f, h = 0.5f;
        int col = ARGB.colorFromFloat(1.0f, 1.0f, 1.0f, 1.0f);
        c.submitCustomGeometry(ps, RenderTypes.beaconBeam(PARTICLES, true), (pose, vc) -> {
            art.arcane.thaumcraft.client.fx.beam.TcBeams.v(pose, vc, new net.minecraft.world.phys.Vec3(-h, -h, 0), u0, v1, col);
            art.arcane.thaumcraft.client.fx.beam.TcBeams.v(pose, vc, new net.minecraft.world.phys.Vec3(h, -h, 0), u1, v1, col);
            art.arcane.thaumcraft.client.fx.beam.TcBeams.v(pose, vc, new net.minecraft.world.phys.Vec3(h, h, 0), u1, v0, col);
            art.arcane.thaumcraft.client.fx.beam.TcBeams.v(pose, vc, new net.minecraft.world.phys.Vec3(-h, h, 0), u0, v0, col);
        });
        ps.popPose();
        super.submit(s, ps, c, cam);
    }
}
