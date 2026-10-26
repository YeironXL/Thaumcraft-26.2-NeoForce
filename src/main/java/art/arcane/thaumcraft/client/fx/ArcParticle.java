package art.arcane.thaumcraft.client.fx;

import art.arcane.thaumcraft.client.fx.beam.TcBeams;
import art.arcane.thaumcraft.fx.GenericParticleOptions;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Port of TC6 FXArc (FXDispatcher.arcLightning): a noisy ballistic arc drawn as two crossed additive ribbons for three
 * ticks, leaving sparkles along its path.
 */
public final class ArcParticle extends TcBeams.BeamParticle {
    private static final Identifier BEAM = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/beamh.png");
    private final List<Vec3> points = new ArrayList<>();
    private final float r, g, b, length;

    public ArcParticle(ClientLevel level, double x, double y, double z, double tx, double ty, double tz, float red, float green, float blue, double hg) {
        super(level, x, y, z);
        r = red; g = green; b = blue;
        lifetime = Integer.getInteger("thaumcraft.debugArcLife", 3); // dev screenshots: hold the 3-tick arc longer
        hasPhysics = false;
        double gravity = 0.115, noise = 0.25;
        Vec3 vs = Vec3.ZERO, ve = new Vec3(tx - x, ty - y, tz - z), vc = Vec3.ZERO;
        length = (float) ve.length();
        Vec3 vv = calculateVelocity(vs, ve, hg, gravity);
        double l = vv.lengthSqr();
        points.add(vs);
        for (int c = 0; ve.distanceToSqr(vc) > l && c < 50; ++c) {
            Vec3 vt = vc.add(vv);
            vc = vt;
            vt = vt.add((random.nextDouble() - random.nextDouble()) * noise, (random.nextDouble() - random.nextDouble()) * noise, (random.nextDouble() - random.nextDouble()) * noise);
            points.add(vt);
            int age = 30 + random.nextInt(20);
            float[] alphas = new float[6 + random.nextInt(age / 3)];
            for (int a = 1; a < alphas.length - 1; ++a) alphas[a] = random.nextFloat();
            alphas[0] = 1.0f;
            boolean sp = random.nextFloat() < 0.2;
            var o = GenericParticleOptions.builder().maxAge(age)
                    .color(Mth.clamp(red * 3.0f, 0, 1), Mth.clamp(green * 3.0f, 0, 1), Mth.clamp(blue * 3.0f, 0, 1), random.nextFloat(), random.nextFloat(), random.nextFloat())
                    .alpha(alphas).particles(sp ? 320 : 512, 16, 1).loop(true).gravity(sp ? 0.0f : 0.125f).scale(0.5f, 0.125f).layer(0).slowDown(0.995)
                    .randomMovement(0.0025f, 0.001f, 0.0025f).build();
            FXDispatcher.generic(x + vt.x, y + vt.y, z + vt.z, 0, 0, 0, o, 2 + random.nextInt(3));
            vv = vv.subtract(0.0, gravity / 1.9, 0.0);
        }
        points.add(ve);
    }

    /** Utils.calculateVelocity. */
    static Vec3 calculateVelocity(Vec3 from, Vec3 to, double heightGain, double gravity) {
        double endGain = to.y - from.y;
        double horizDist = Math.sqrt((to.x - from.x) * (to.x - from.x) + (to.z - from.z) * (to.z - from.z));
        double maxGain = heightGain > endGain + heightGain ? heightGain : endGain + heightGain;
        double a = -horizDist * horizDist / (4.0 * maxGain), bb = horizDist, c = -endGain;
        double slope = -bb / (2.0 * a) - Math.sqrt(bb * bb - 4.0 * a * c) / (2.0 * a);
        double vy = Math.sqrt(maxGain * gravity);
        double vh = vy / slope;
        double dx = to.x - from.x, dz = to.z - from.z, mag = Math.sqrt(dx * dx + dz * dz);
        if (mag == 0 || Double.isNaN(vh)) return new Vec3(0, vy, 0);
        return new Vec3(vh * dx / mag, vy, vh * dz / mag);
    }

    private static void quad(com.mojang.blaze3d.vertex.PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer vc, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float u0, float u1, int argb) {
        TcBeams.v(pose, vc, a, u0, 1, argb); TcBeams.v(pose, vc, b, u0, 0, argb); TcBeams.v(pose, vc, c, u1, 0, argb); TcBeams.v(pose, vc, d, u1, 1, argb);
        TcBeams.v(pose, vc, d, u1, 1, argb); TcBeams.v(pose, vc, c, u1, 0, argb); TcBeams.v(pose, vc, b, u0, 0, argb); TcBeams.v(pose, vc, a, u0, 1, argb);
    }

    @Override public void tick() { xo = x; yo = y; zo = z; if (age++ >= lifetime) remove(); }
    @Override public RenderType renderType() { return TcBeams.additive(BEAM); }

    @Override
    public SubmitNodeCollector.CustomGeometryRenderer geometry(Vec3 cam, float f) {
        if (points.size() < 2) return (pose, vc) -> {};
        Vec3 origin = new Vec3(x, y, z).subtract(cam);
        Vec3[] pts = points.stream().map(origin::add).toArray(Vec3[]::new);
        float alpha = Mth.clamp(1.0f - (age + f) / lifetime, 0, 1);
        int argb = ARGB.colorFromFloat(alpha, r, g, b);
        float size = 0.125f, len = Math.max(length, 0.001f);
        return (pose, vc) -> {
            for (int c = 0; c + 1 < pts.length; ++c) {
                float u0 = c / len, u1 = (c + 1) / len;
                Vec3 p0 = pts[c], p1 = pts[c + 1];
                // Both windings: TC6 drew these strips with culling disabled.
                quad(pose, vc, p0.add(0, -size, 0), p0.add(0, size, 0), p1.add(0, size, 0), p1.add(0, -size, 0), u0, u1, argb);
                quad(pose, vc, p0.add(-size, 0, -size), p0.add(size, 0, size), p1.add(size, 0, size), p1.add(-size, 0, -size), u0, u1, argb);
            }
        };
    }
}
