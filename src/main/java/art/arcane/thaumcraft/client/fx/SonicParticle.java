package art.arcane.thaumcraft.client.fx;

import art.arcane.thaumcraft.client.fx.beam.TcBeams;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * TC6 FXSonic: the hemisphere OBJ (models/obj/hemis.obj) textured with the animated ripple frames, placed in front of
 * the screeching entity's face (yaw/pitch frozen at spawn), alpha blended without depth writes, colour 0.5.
 */
public final class SonicParticle extends TcBeams.BeamParticle {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath("thaumcraft", "models/obj/hemis.obj");
    /** Triangles: x,y,z,u,v per vertex, 3 vertices per face. */
    private static float[] mesh;
    private final Entity target;
    private final float yaw, pitch;

    public SonicParticle(ClientLevel level, Entity target, int age) {
        super(level, target.getX(), target.getY() + target.getEyeHeight(), target.getZ());
        this.target = target;
        this.lifetime = age + random.nextInt(age / 2);
        this.hasPhysics = false;
        this.gravity = 0.0f;
        this.xd = this.yd = this.zd = 0.0;
        this.yaw = target.getYHeadRot();
        this.pitch = target.getXRot();
        this.xo = x; this.yo = y; this.zo = z;
    }

    @Override
    public void tick() {
        xo = x; yo = y; zo = z;
        if (age++ >= lifetime) remove();
        x = target.getX(); y = target.getY() + target.getEyeHeight(); z = target.getZ();
    }

    private int frame(float pt) {
        float fade = (age + pt) / lifetime;
        return Math.min(15, (int) (14.0f * fade) + 1);
    }

    private float lastPt;
    @Override public RenderType renderType() {
        return TcBeams.translucent(Identifier.fromNamespaceAndPath("thaumcraft", "textures/models/ripple" + frame(lastPt) + ".png"));
    }

    @Override
    public SubmitNodeCollector.CustomGeometryRenderer geometry(Vec3 cam, float f) {
        lastPt = f;
        float[] m = mesh();
        if (m.length == 0) return (pose, vc) -> {};
        PoseStack ps = new PoseStack();
        ps.translate(xo + (x - xo) * f - cam.x, yo + (y - yo) * f - cam.y, zo + (z - zo) * f - cam.z);
        ps.mulPose(Axis.YP.rotationDegrees(-yaw));
        ps.mulPose(Axis.XP.rotationDegrees(pitch));
        float h = target.getBbHeight(), w = target.getBbWidth();
        ps.translate(0.0, 0.0, 2.0f * h + w / 2.0f);
        ps.scale(0.25f * h, 0.25f * h, -1.0f * h);
        PoseStack.Pose local = ps.last().copy();
        int argb = ARGB.colorFromFloat(1.0f, 0.5f, 0.5f, 0.5f);
        return (pose, vc) -> {
            PoseStack.Pose p = pose.copy();
            p.mulPose(local.pose());
            p.normal().mul(local.normal());
            for (int i = 0; i < m.length; i += 15) {
                // GL_TRIANGLES emitted as quads with the last vertex doubled
                for (int k = 0; k < 4; ++k) {
                    int o = i + Math.min(k, 2) * 5;
                    vc.addVertex(p, m[o], m[o + 1], m[o + 2]).setColor(argb).setUv(m[o + 3], m[o + 4]).setLight(0xF000F0).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(p, 0f, 1f, 0f);
                }
            }
        };
    }

    /** Minimal Wavefront reader as TC's AdvancedModelLoader: v, vt (v flipped), triangular f v/vt[/vn]. */
    static float[] mesh() {
        if (mesh != null) return mesh;
        List<float[]> vs = new ArrayList<>(), vts = new ArrayList<>();
        List<Float> out = new ArrayList<>();
        try (var in = Minecraft.getInstance().getResourceManager().open(MODEL);
             var r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] t = line.trim().split("\\s+");
                if (t.length == 0) continue;
                switch (t[0]) {
                    case "v" -> vs.add(new float[]{Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3])});
                    case "vt" -> vts.add(new float[]{Float.parseFloat(t[1]), 1.0f - Float.parseFloat(t[2])});
                    case "f" -> {
                        for (int k = 1; k <= 3; ++k) {
                            String[] idx = t[k].split("/");
                            float[] v = vs.get(Integer.parseInt(idx[0]) - 1);
                            float[] uv = idx.length > 1 && !idx[1].isEmpty() ? vts.get(Integer.parseInt(idx[1]) - 1) : new float[2];
                            out.add(v[0]); out.add(v[1]); out.add(v[2]); out.add(uv[0]); out.add(uv[1]);
                        }
                    }
                    default -> {}
                }
            }
        } catch (Exception e) {
            mesh = new float[0];
            return mesh;
        }
        float[] a = new float[out.size()];
        for (int i = 0; i < a.length; ++i) a[i] = out.get(i);
        mesh = a;
        return mesh;
    }
}
