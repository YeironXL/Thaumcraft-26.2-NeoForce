package art.arcane.thaumcraft.client.fx;

import art.arcane.thaumcraft.client.fx.beam.TcBeams;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Port of TC6 FXBolt: a jittering lightning tube living three ticks. */
public final class BoltParticle extends TcBeams.BeamParticle {
    private static final Identifier BEAM = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/essentia.png");
    private final float width, dr, r, g, b;
    private final double tX, tY, tZ;
    private final float length;
    private final long seed;

    public BoltParticle(ClientLevel level, double x, double y, double z, double tx, double ty, double tz, float red, float green, float blue, float width) {
        super(level, x, y, z);
        this.r = red; this.g = green; this.b = blue;
        this.tX = tx - x; this.tY = ty - y; this.tZ = tz - z;
        this.width = width;
        this.lifetime = 3;
        this.length = (float) (new Vec3(tX, tY, tZ).length() * Math.PI);
        this.dr = (float) (random.nextInt(50) * Math.PI);
        this.seed = random.nextInt(1000);
        this.hasPhysics = false;
    }

    @Override public void tick() { xo = x; yo = y; zo = z; if (age++ >= lifetime) remove(); }
    @Override public RenderType renderType() { return TcBeams.additive(BEAM); }

    @Override
    public SubmitNodeCollector.CustomGeometryRenderer geometry(Vec3 cam, float f) {
        Random rr = new Random(seed);
        List<Vec3> points = new ArrayList<>();
        List<Float> widths = new ArrayList<>();
        int steps = (int) length;
        points.add(Vec3.ZERO); widths.add(width);
        float ampl = (age + f) / 10.0f;
        for (int a = 1; a < steps - 1; ++a) {
            float dist = a * (length / steps) + dr;
            double dx = tX / steps * a + Mth.sin(dist / 4.0f) * ampl + (rr.nextFloat() - rr.nextFloat()) * 0.1f;
            double dy = tY / steps * a + Mth.sin(dist / 3.0f) * ampl + (rr.nextFloat() - rr.nextFloat()) * 0.1f;
            double dz = tZ / steps * a + Mth.sin(dist / 2.0f) * ampl + (rr.nextFloat() - rr.nextFloat()) * 0.1f;
            points.add(new Vec3(dx, dy, dz));
            widths.add((rr.nextInt(4) == 0 ? 1.0f - age * 0.25f : 1.0f) * width);
        }
        widths.add(width); points.add(new Vec3(tX, tY, tZ));
        float alpha = Mth.clamp(1.0f - age / (float) lifetime, 0.1f, 1.0f);
        Vec3 origin = new Vec3(xo + (x - xo) * f, yo + (y - yo) * f, zo + (z - zo) * f).subtract(cam);
        if (points.size() <= 2) return (pose, vc) -> {};
        Vec3[] pts = points.stream().map(origin::add).toArray(Vec3[]::new);
        float[] radii = new float[pts.length], thin = new float[pts.length];
        for (int i = 0; i < pts.length; i++) { radii[i] = widths.get(i) / 10.0f; thin[i] = radii[i] / 3.0f; }
        int argb = ARGB.colorFromFloat(alpha, r, g, b);
        return (pose, vc) -> { TcBeams.tube(pose, vc, pts, radii, argb, 5); TcBeams.tube(pose, vc, pts, thin, argb, 5); };
    }
}
