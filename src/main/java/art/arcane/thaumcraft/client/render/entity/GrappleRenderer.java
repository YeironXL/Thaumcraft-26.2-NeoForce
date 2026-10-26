package art.arcane.thaumcraft.client.render.entity;

import art.arcane.thaumcraft.client.fx.beam.TcBeams;
import art.arcane.thaumcraft.entity.GrappleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

/** TC6 RenderGrapple: grappler head plus a sagging rope back to the thrower's hand (GLE poly-cone rebuilt as a tube). */
public final class GrappleRenderer extends EntityRenderer<GrappleEntity, GrappleRenderer.State> {
    public static final class State extends EntityRenderState { float yRot, xRot, length = 1.0f; int ticks; final List<Vec3> points = new ArrayList<>(); }
    private static final Identifier BEAM = Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/grappler.png");
    private static final Identifier PARTICLES = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png");
    private static final Identifier ROPE = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/rope.png");
    private final Model<Unit> model;

    public GrappleRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        model = new Model.Simple(ctx.bakeLayer(TcModelLayers.GRAPPLER), RenderTypes::entityCutout);
        shadowRadius = 0.0f;
    }
    @Override public State createRenderState() { return new State(); }
    @Override public boolean shouldRender(GrappleEntity e, net.minecraft.client.renderer.culling.Frustum f, double x, double y, double z) { return true; }

    @Override
    public void extractRenderState(GrappleEntity g, State s, float pt) {
        super.extractRenderState(g, s, pt);
        s.yRot = g.getYRot(pt); s.xRot = g.getXRot(pt); s.ticks = g.tickCount;
        s.points.clear();
        var thrower = g.getThrower();
        if (thrower == null) return;
        double tx = Mth.lerp(pt, thrower.xo, thrower.getX()), ty = Mth.lerp(pt, thrower.yo, thrower.getY()), tz = Mth.lerp(pt, thrower.zo, thrower.getZ());
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson() && thrower == Minecraft.getInstance().getCameraEntity()) {
            double yy = Mth.lerp(pt, thrower.yRotO, thrower.getYRot());
            int side = g.hand == InteractionHand.MAIN_HAND ? 1 : -1;
            double px = -Mth.cos((float) ((yy - 0.5) / 180.0 * Math.PI)) * 0.1f * side;
            double pz = -Mth.sin((float) ((yy - 0.5) / 180.0 * Math.PI)) * 0.1f * side;
            Vec3 vl = thrower.getViewVector(pt);
            tx += px + vl.x / 5.0; ty += thrower.getEyeHeight() / 2.6 + vl.y / 5.0; tz += pz + vl.z / 5.0;
        }
        double gx = Mth.lerp(pt, g.xo, g.getX()), gy = Mth.lerp(pt, g.yo, g.getY()), gz = Mth.lerp(pt, g.zo, g.getZ());
        double h = thrower.getBbHeight() / 2.0f;
        Vec3 ve = new Vec3(tx - gx, ty - gy + h, tz - gz);
        float length = (float) (ve.length() * 5.0);
        s.length = length;
        int steps = (int) length;
        s.points.add(Vec3.ZERO);
        for (int a = 1; a < steps - 1; ++a) {
            float dist = a * (length / steps);
            float ss = 1.0f - a / (steps * 1.25f);
            s.points.add(new Vec3((tx - gx) / steps * a + Mth.sin(dist / 10.0f) * g.ampl * ss,
                    (ty - gy + h) / steps * a + Mth.sin(dist / 8.0f) * g.ampl * ss,
                    (tz - gz) / steps * a + Mth.sin(dist / 6.0f) * g.ampl * ss));
        }
        s.points.add(ve);
        s.points.add(ve);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector c, CameraRenderState cam) {
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(s.yRot - 90.0f));
        ps.mulPose(Axis.ZP.rotationDegrees(s.xRot));
        c.submitModel(model, Unit.INSTANCE, ps, RenderTypes.entityCutout(BEAM), s.lightCoords, OverlayTexture.NO_OVERLAY, -1, null, s.outlineColor, null);
        ps.popPose();
        // glowing spark at the grappler head: particle sheet frame (1 + ticks % 6) / 32, row 7, additive, alpha 0.21, bobbing
        ps.pushPose();
        ps.mulPose(cam.orientation);
        float bob = Mth.sin(s.ticks / 5.0f) * 0.2f + 0.2f;
        ps.scale(1.0f + bob, 1.0f + bob, 1.0f + bob);
        float f2 = (1 + s.ticks % 6) / 32.0f, f3 = f2 + 0.03125f, f4 = 0.21875f, f5 = f4 + 0.03125f, f6 = 0.5f;
        int col = net.minecraft.util.ARGB.colorFromFloat(0.21f, 1.0f, 1.0f, 1.0f);
        c.submitCustomGeometry(ps, TcBeams.additive(PARTICLES), (pose, vc) -> {
            TcBeams.v(pose, vc, new Vec3(-f6, -f6, 0), f2, f5, col); TcBeams.v(pose, vc, new Vec3(f6, -f6, 0), f3, f5, col);
            TcBeams.v(pose, vc, new Vec3(f6, f6, 0), f3, f4, col); TcBeams.v(pose, vc, new Vec3(-f6, f6, 0), f2, f4, col);
            TcBeams.v(pose, vc, new Vec3(-f6, f6, 0), f2, f4, col); TcBeams.v(pose, vc, new Vec3(f6, f6, 0), f3, f4, col);
            TcBeams.v(pose, vc, new Vec3(f6, -f6, 0), f3, f5, col); TcBeams.v(pose, vc, new Vec3(-f6, -f6, 0), f2, f5, col);
        });
        ps.popPose();
        if (s.points.size() > 2) {
            Vec3[] pts = s.points.toArray(new Vec3[0]);
            float[] radii = new float[pts.length];
            java.util.Arrays.fill(radii, 0.025f);
            float start = s.length - pts.length;
            // glePolyCone(..., texSlice 2.0, start length - points.size()): the rope texture repeats per segment
            c.submitCustomGeometry(ps, RenderTypes.beaconBeam(ROPE, true), (pose, vc) -> TcBeams.polyCone(pose, vc, pts, radii, 0xFFFFFFFF, 4, 2.0f, start));
        }
        super.submit(s, ps, c, cam);
    }
}
