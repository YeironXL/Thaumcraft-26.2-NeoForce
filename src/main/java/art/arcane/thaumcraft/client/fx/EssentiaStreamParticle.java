package art.arcane.thaumcraft.client.fx;

import art.arcane.thaumcraft.client.fx.beam.TcBeams;
import art.arcane.thaumcraft.fx.GenericParticleOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Port of TC6 FXEssentiaStream: a wobbling tube of essentia that grows from its source towards a target, drips droplets
 * and is extended (instead of duplicated) while the same source/target/color stream keeps being requested.
 */
public final class EssentiaStreamParticle extends TcBeams.BeamParticle {
    private static final Identifier TEX = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/essentia.png");
    private static final Map<String, EssentiaStreamParticle> STREAMS = new HashMap<>();
    private record Quat(double s, double x, double y, double z) {}

    private final double targetX, targetY, targetZ, startX, startY, startZ;
    private final int count;
    private int length;
    private String key = "";
    private float scale, gravity = 0.2f;
    private final float r, g, b;
    private int growing = -1;
    private final List<Quat> vecs = new ArrayList<>();
    private Vec3[] points;
    private float[] radii;

    private EssentiaStreamParticle(ClientLevel w, double x, double y, double z, double tx, double ty, double tz, int count, int color, float scale, int extend, double my) {
        super(w, x, y, z);
        this.hasPhysics = false;
        this.scale = (float) (scale * (1.0 + random.nextGaussian() * 0.15));
        this.length = Math.max(20, extend);
        this.count = count;
        this.targetX = tx; this.targetY = ty; this.targetZ = tz;
        double dx = tx - x, dy = ty - y, dz = tz - z;
        this.lifetime = Math.max(1, (int) (Mth.sqrt((float) (dx * dx + dy * dy + dz * dz)) * 21.0f));
        this.xd = Mth.sin(count / 4.0f) * 0.015f;
        this.yd = my + Mth.sin(count / 3.0f) * 0.015f;
        this.zd = Mth.sin(count / 2.0f) * 0.015f;
        this.r = ((color >> 16) & 255) / 255f; this.g = ((color >> 8) & 255) / 255f; this.b = (color & 255) / 255f;
        vecs.add(new Quat(0.001, 0, 0, 0));
        vecs.add(new Quat(0.001, 0, 0, 0));
        startX = x; startY = y; startZ = z;
    }

    /** FXDispatcher.waterTrailFx / essentiaTrailFx entry point: extends a live identical stream instead of making a new one. */
    public static void spawn(ClientLevel w, double x, double y, double z, double tx, double ty, double tz, int count, int color, float scale, int extend, double my) {
        var bp1 = BlockPos.containing(x, y, z);
        var bp2 = BlockPos.containing(tx, ty, tz);
        String k = bp1.asLong() + "" + bp2.asLong() + "" + color;
        var old = STREAMS.get(k);
        if (old != null && old.isAlive() && old.vecs.size() < old.length) {
            old.length += Math.max(extend, 5);
            old.lifetime += Math.max(extend, 5);
            return;
        }
        var p = new EssentiaStreamParticle(w, x, y, z, tx, ty, tz, count, color, scale, extend, my);
        p.key = k;
        STREAMS.put(k, p);
        net.minecraft.client.Minecraft.getInstance().particleEngine.add(p);
    }

    public void setGravity(float v) { gravity = v; }
    @Override public RenderType renderType() { return RenderTypes.beaconBeam(TEX, true); }

    @Override
    public void tick() {
        xo = x; yo = y; zo = z;
        if (age++ >= lifetime || length < 1) {
            remove();
            if (STREAMS.get(key) == this) STREAMS.remove(key);
            return;
        }
        yd += 0.01 * gravity;
        x += xd; y += yd; z += zd;
        xd = Mth.clamp(xd * 0.985, -0.05, 0.05);
        yd = Mth.clamp(yd * 0.985, -0.05, 0.05);
        zd = Mth.clamp(zd * 0.985, -0.05, 0.05);
        double dx = targetX - x, dy = targetY - y, dz = targetZ - z;
        double d14 = Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx /= d14; dy /= d14; dz /= d14;
        double pull = 0.01 / Math.min(1.0, d14);
        xd += dx * pull; yd += dy * pull; zd += dz * pull;
        float s = scale * (0.75f + Mth.sin((count + age) / 2.0f) * 0.25f);
        if (d14 < 1.0) { float f = Mth.sin((float) (d14 * Math.PI / 2)); s *= f; scale *= f; }
        if (scale > 0.001) vecs.add(new Quat(s, x - startX, y - startY, z - startZ));
        else {
            if (growing < 0) growing = age;
            --length;
            drop(targetX + random.nextGaussian() * 0.075, targetY + random.nextGaussian() * 0.075, targetZ + random.nextGaussian() * 0.075);
        }
        while (vecs.size() > Math.max(length, 0) && !vecs.isEmpty()) vecs.removeFirst();
        int n = vecs.size();
        points = new Vec3[n];
        radii = new float[n];
        for (int c = n - 1; c >= 0; --c) {
            Quat v = vecs.get(c);
            float variance = 1.0f + Mth.sin((c + age) / 3.0f) * 0.2f;
            points[c] = new Vec3(v.x + Mth.sin((c + age) / 6.0f) * 0.03f, v.y + Mth.sin((c + age) / 7.0f) * 0.03f, v.z + Mth.sin((c + age) / 8.0f) * 0.03f);
            float rad = (float) (v.s * variance);
            if (c > n - 10) rad *= Mth.cos((float) ((c - (n - 12)) / 10.0f * Math.PI / 2));
            if (c <= 1) rad = 0;
            else if (c == 2) rad = (scale * 0.5f + rad) / 2.0f;
            else if (c == 3) rad = (scale + rad) / 2.0f;
            else if (c == 4) rad = (scale + rad * 2.0f) / 3.0f;
            radii[c] = rad;
        }
        if (n > 2 && random.nextBoolean()) {
            int q = random.nextBoolean() ? n - 2 : random.nextInt(3);
            Quat v = vecs.get(q);
            drop(v.x + startX, v.y + startY, v.z + startZ);
        }
    }

    /** FXDispatcher.essentiaDropFx. */
    private void drop(double px, double py, double pz) {
        FXDispatcher.generic(px, py, pz, random.nextGaussian() * 0.005, random.nextGaussian() * 0.005, random.nextGaussian() * 0.005,
                GenericParticleOptions.builder().maxAge(20 + random.nextInt(10)).color(r, g, b).alpha(0.5f).loop(false).particles(25, 1, 1)
                        .scale(0.4f + random.nextFloat() * 0.2f, 0.2f).layer(1).gravity(0.01f).rotation(0.0f).build(), 0);
    }

    @Override
    public SubmitNodeCollector.CustomGeometryRenderer geometry(Vec3 cam, float f) {
        if (points == null || points.length <= 2) return (pose, vc) -> {};
        Vec3 origin = new Vec3(startX, startY, startZ).subtract(cam);
        Vec3[] pts = new Vec3[points.length];
        for (int i = 0; i < pts.length; i++) pts[i] = origin.add(points[i]);
        float[] rad = radii.clone();
        int argb = ARGB.colorFromFloat(1.0f, r, g, b);
        return (pose, vc) -> TcBeams.tube(pose, vc, pts, rad, argb, 8);
    }
}
