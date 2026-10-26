package art.arcane.thaumcraft.client.fx.beam;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Beam-like effects drawn with GL in TC6 (FXBolt, FXArc, FXBeamWand, essentia streams...). They render as custom
 * geometry through a dedicated particle group, with additive blending and no depth writes as the originals.
 */
public final class TcBeams {
    public static final ParticleRenderType GROUP = new ParticleRenderType("THAUMCRAFT_BEAMS", "TCB");
    public static final RenderPipeline ADDITIVE = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("thaumcraft", "pipeline/beam_additive")).withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false)).withCull(false).build();
    /** Alpha-blended, no depth test: architect overlays and similar see-through markers. */
    public static final RenderPipeline OVERLAY = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("thaumcraft", "pipeline/overlay")).withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false)).withCull(false).build();
    /** Additive, no depth test. */
    public static final RenderPipeline OVERLAY_ADDITIVE = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("thaumcraft", "pipeline/overlay_additive")).withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false)).withCull(false).build();
    /** Alpha-blended (GL 770/771), depth-tested, no depth writes, no culling. */
    public static final RenderPipeline TRANSLUCENT = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("thaumcraft", "pipeline/beam_translucent")).withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false)).withCull(false).build();
    private static final Map<Identifier, RenderType> TRANSLUCENTS = new ConcurrentHashMap<>();
    public static RenderType translucent(Identifier t) {
        return TRANSLUCENTS.computeIfAbsent(t, x -> RenderType.create("thaumcraft_beam_translucent", RenderSetup.builder(TRANSLUCENT).withTexture("Sampler0", x).sortOnUpload().createRenderSetup()));
    }
    private static final Map<Identifier, RenderType> TYPES = new ConcurrentHashMap<>();
    private static final Map<Identifier, RenderType> OVERLAYS = new ConcurrentHashMap<>();
    private static final Map<Identifier, RenderType> OVERLAYS_ADD = new ConcurrentHashMap<>();
    public static RenderType overlay(Identifier t) {
        return OVERLAYS.computeIfAbsent(t, x -> RenderType.create("thaumcraft_overlay", RenderSetup.builder(OVERLAY).withTexture("Sampler0", x).sortOnUpload().createRenderSetup()));
    }
    public static RenderType overlayAdditive(Identifier t) {
        return OVERLAYS_ADD.computeIfAbsent(t, x -> RenderType.create("thaumcraft_overlay_add", RenderSetup.builder(OVERLAY_ADDITIVE).withTexture("Sampler0", x).sortOnUpload().createRenderSetup()));
    }

    public static RenderType additive(Identifier texture) {
        return TYPES.computeIfAbsent(texture, t -> RenderType.create("thaumcraft_beam", RenderSetup.builder(ADDITIVE).withTexture("Sampler0", t).sortOnUpload().createRenderSetup()));
    }

    /** A particle drawing its own geometry; vertex positions are relative to the camera. */
    public abstract static class BeamParticle extends Particle {
        protected BeamParticle(net.minecraft.client.multiplayer.ClientLevel level, double x, double y, double z) { super(level, x, y, z); }
        @Override public ParticleRenderType getGroup() { return GROUP; }
        public abstract RenderType renderType();
        /** Prepare geometry for this frame (called on the render thread during extraction). */
        public abstract SubmitNodeCollector.CustomGeometryRenderer geometry(Vec3 camera, float partialTick);
    }

    public static final class Group extends ParticleGroup<BeamParticle> {
        public Group(ParticleEngine engine) { super(engine); }
        @Override
        public ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float pt) {
            List<Entry> entries = new ArrayList<>();
            Vec3 cam = camera.position();
            for (BeamParticle p : particles) entries.add(new Entry(p.renderType(), p.geometry(cam, pt)));
            return new State(entries);
        }
    }

    private record Entry(RenderType type, SubmitNodeCollector.CustomGeometryRenderer geometry) {}
    private record State(List<Entry> entries) implements ParticleGroupRenderState {
        @Override
        public void submit(SubmitNodeCollector collector, CameraRenderState camera) {
            PoseStack ps = new PoseStack();
            for (Entry e : entries) collector.submitCustomGeometry(ps, e.type, e.geometry);
        }
    }

    /** Port of the GLE poly-cylinder used by FXBolt/FXArc: a tube of {@code sides} faces through the points. */
    public static void tube(PoseStack.Pose pose, VertexConsumer vc, Vec3[] pts, float[] radii, int argb, int sides) {
        if (pts.length < 2) return;
        double len = 0;
        double[] dist = new double[pts.length];
        for (int i = 1; i < pts.length; i++) { len += pts[i].distanceTo(pts[i - 1]); dist[i] = len; }
        Vec3[][] rings = new Vec3[pts.length][sides + 1];
        Vec3 prevN = null;
        for (int i = 0; i < pts.length; i++) {
            Vec3 dir = (i < pts.length - 1 ? pts[i + 1].subtract(pts[i]) : pts[i].subtract(pts[i - 1])).normalize();
            Vec3 n = prevN != null ? prevN.subtract(dir.scale(prevN.dot(dir))) : (Math.abs(dir.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0)).cross(dir);
            if (n.lengthSqr() < 1e-8) n = (Math.abs(dir.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0)).cross(dir);
            n = n.normalize(); prevN = n;
            Vec3 b = dir.cross(n).normalize();
            for (int s = 0; s <= sides; s++) {
                double a = s * Math.PI * 2 / sides;
                rings[i][s] = pts[i].add(n.scale(Math.cos(a) * radii[i])).add(b.scale(Math.sin(a) * radii[i]));
            }
        }
        for (int i = 0; i < pts.length - 1; i++) {
            float v0 = (float) (dist[i] / Math.max(len, 1e-6)), v1 = (float) (dist[i + 1] / Math.max(len, 1e-6));
            for (int s = 0; s < sides; s++) {
                float u0 = s / (float) sides, u1 = (s + 1) / (float) sides;
                v(pose, vc, rings[i][s], u0, v0, argb); v(pose, vc, rings[i + 1][s], u0, v1, argb);
                v(pose, vc, rings[i + 1][s + 1], u1, v1, argb); v(pose, vc, rings[i][s + 1], u1, v0, argb);
            }
        }
    }

    /**
     * CoreGLE.glePolyCone with texture slices: like GLE, the first and last points only orient the ends, segments run
     * from point 1 to point n-2, and each segment maps v from SLICE_PROGRESS to SLICE_PROGRESS + texSlice (repeating).
     */
    public static void polyCone(PoseStack.Pose pose, VertexConsumer vc, Vec3[] pts, float[] radii, int argb, int sides, float texSlice, float start) {
        int n = pts.length;
        if (n < 4) return;
        Vec3[][] rings = new Vec3[n][sides + 1];
        Vec3 prevN = null;
        for (int i = 1; i < n - 1; i++) {
            Vec3 dir = pts[i + 1].subtract(pts[i]);
            if (dir.lengthSqr() < 1e-12) dir = pts[i].subtract(pts[i - 1]);
            dir = dir.normalize();
            Vec3 nn = prevN != null ? prevN.subtract(dir.scale(prevN.dot(dir))) : (Math.abs(dir.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0)).cross(dir);
            if (nn.lengthSqr() < 1e-8) nn = (Math.abs(dir.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0)).cross(dir);
            nn = nn.normalize(); prevN = nn;
            Vec3 b = dir.cross(nn).normalize();
            for (int s = 0; s <= sides; s++) {
                double a = s * Math.PI * 2 / sides;
                rings[i][s] = pts[i].add(nn.scale(Math.cos(a) * radii[i])).add(b.scale(Math.sin(a) * radii[i]));
            }
        }
        float progress = start;
        for (int i = 1; i < n - 2; i++) {
            float v0 = progress, v1 = progress + texSlice;
            for (int s = 0; s < sides; s++) {
                float u0 = s / (float) sides, u1 = (s + 1) / (float) sides;
                v(pose, vc, rings[i][s], u0, v0, argb); v(pose, vc, rings[i + 1][s], u0, v1, argb);
                v(pose, vc, rings[i + 1][s + 1], u1, v1, argb); v(pose, vc, rings[i][s + 1], u1, v0, argb);
            }
            progress += texSlice;
        }
    }

    public static void v(PoseStack.Pose pose, VertexConsumer vc, Vec3 p, float u, float v, int argb) {
        vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(argb).setUv(u, v).setLight(0xF000F0).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(pose, 0f, 1f, 0f);
    }

    public static void register(net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent e) { e.registerPipeline(ADDITIVE); e.registerPipeline(OVERLAY); e.registerPipeline(OVERLAY_ADDITIVE); e.registerPipeline(TRANSLUCENT); }
    public static void registerGroup(net.neoforged.neoforge.client.event.RegisterParticleGroupsEvent e) { e.register(GROUP, Group::new); }

    private TcBeams() {}
}
