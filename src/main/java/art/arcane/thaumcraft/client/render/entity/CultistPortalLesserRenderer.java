package art.arcane.thaumcraft.client.render.entity;

import art.arcane.thaumcraft.client.fx.beam.TcBeams;
import art.arcane.thaumcraft.entity.monster.CultistPortalLesserEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * TC6 RenderCultistPortalLesser: once active, a full-bright camera-facing swirl (16-frame strip) that grows in over
 * 50 ticks, wobbles when hurt or summoning, and flickers as it loses health.
 */
public final class CultistPortalLesserRenderer extends EntityRenderer<CultistPortalLesserEntity, CultistPortalLesserRenderer.State> {
    public static final class State extends EntityRenderState { boolean active; float scale, scaley, alpha; int frame; float height; }
    private static final Identifier TEX = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/cultist_portal.png");

    public CultistPortalLesserRenderer(EntityRendererProvider.Context ctx) { super(ctx); shadowRadius = 0.0f; }
    @Override public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(CultistPortalLesserEntity portal, State s, float f) {
        super.extractRenderState(portal, s, f);
        s.active = portal.isActive();
        s.height = portal.getBbHeight();
        long time = System.nanoTime() / 50000000L;
        float scaley = 1.4f;
        int e = (int) Math.min(50.0f, portal.activeCounter + f);
        if (portal.hurtTime > 0) {
            double d = Math.sin(portal.hurtTime * 72 * Math.PI / 180.0);
            scaley -= (float) (d / 4.0);
            e += (int) (6.0 * d);
        }
        if (portal.pulse > 0) {
            double d = Math.sin(portal.pulse * 36 * Math.PI / 180.0);
            scaley += (float) (d / 4.0);
            e += (int) (12.0 * d);
        }
        float scale = e / 50.0f * 1.25f;
        float m = (1.0f - portal.getHealth() / portal.getMaxHealth()) / 3.0f;
        float bob = Mth.sin(portal.activeCounter / (5.0f - 12.0f * m)) * m + m;
        float bob2 = Mth.sin(portal.activeCounter / (6.0f - 15.0f * m)) * m + m;
        s.alpha = Mth.clamp(1.0f - bob, 0.0f, 1.0f);
        s.scaley = scaley - bob / 4.0f;
        s.scale = scale - bob2 / 3.0f;
        s.frame = 15 - (int) time % 16;
    }

    @Override protected int getBlockLightLevel(CultistPortalLesserEntity e, net.minecraft.core.BlockPos pos) { return 15; }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector c, CameraRenderState cam) {
        if (s.active) {
            ps.pushPose();
            ps.translate(0.0f, s.height / 2.0f, 0.0f);
            ps.scale(s.scale, s.scaley, s.scale);
            ps.mulPose(cam.orientation);
            float f2 = s.frame / 16.0f, f3 = f2 + 0.0625f;
            int col = ARGB.colorFromFloat(s.alpha, 1.0f, 1.0f, 1.0f);
            // TC6 billboard UVs: u runs right to left (f3 at the right edge) and v is flipped (0 at the bottom).
            c.submitCustomGeometry(ps, RenderTypes.beaconBeam(TEX, true), (pose, vc) -> {
                TcBeams.v(pose, vc, new Vec3(-1, -1, 0), f2, 0.0f, col); TcBeams.v(pose, vc, new Vec3(1, -1, 0), f3, 0.0f, col);
                TcBeams.v(pose, vc, new Vec3(1, 1, 0), f3, 1.0f, col); TcBeams.v(pose, vc, new Vec3(-1, 1, 0), f2, 1.0f, col);
                TcBeams.v(pose, vc, new Vec3(-1, 1, 0), f2, 1.0f, col); TcBeams.v(pose, vc, new Vec3(1, 1, 0), f3, 1.0f, col);
                TcBeams.v(pose, vc, new Vec3(1, -1, 0), f3, 0.0f, col); TcBeams.v(pose, vc, new Vec3(-1, -1, 0), f2, 0.0f, col);
            });
            ps.popPose();
        }
        super.submit(s, ps, c, cam);
    }
}
