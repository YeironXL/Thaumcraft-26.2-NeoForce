package art.arcane.thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * TC6 AdvancedModelLoader / WavefrontObject: a grouped OBJ mesh (v, vt with v flipped, vn, n-gon faces fanned into
 * triangles), drawn group by group like IModelCustom.renderPart. Triangles are emitted as quads with the last vertex
 * doubled so they work in QUADS render types.
 */
public final class ObjModel {
    private static final Map<Identifier, ObjModel> CACHE = new HashMap<>();
    /** group -> x,y,z,u,v,nx,ny,nz per vertex, 3 vertices per triangle. */
    private final LinkedHashMap<String, float[]> groups = new LinkedHashMap<>();

    public static ObjModel get(Identifier id) { return CACHE.computeIfAbsent(id, ObjModel::load); }
    public static void clearCache() { CACHE.clear(); }

    public List<String> partNames() { return new ArrayList<>(groups.keySet()); }
    public boolean has(String part) { return groups.containsKey(part); }

    private static ObjModel load(Identifier id) {
        ObjModel m = new ObjModel();
        List<float[]> vs = new ArrayList<>(), vts = new ArrayList<>(), vns = new ArrayList<>();
        Map<String, List<Float>> out = new LinkedHashMap<>();
        String group = "default";
        try (var in = Minecraft.getInstance().getResourceManager().open(id); var r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] t = line.trim().split("\\s+");
                if (t.length == 0 || t[0].isEmpty()) continue;
                switch (t[0]) {
                    case "v" -> vs.add(new float[]{Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3])});
                    case "vt" -> vts.add(new float[]{Float.parseFloat(t[1]), 1.0f - Float.parseFloat(t[2])});
                    case "vn" -> vns.add(new float[]{Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3])});
                    case "g", "o" -> group = t.length > 1 ? t[1] : "default";
                    case "f" -> {
                        int n = t.length - 1;
                        float[][] verts = new float[n][];
                        for (int k = 0; k < n; k++) {
                            String[] idx = t[k + 1].split("/");
                            float[] v = vs.get(Integer.parseInt(idx[0]) - 1);
                            float[] uv = idx.length > 1 && !idx[1].isEmpty() ? vts.get(Integer.parseInt(idx[1]) - 1) : new float[2];
                            float[] nn = idx.length > 2 && !idx[2].isEmpty() ? vns.get(Integer.parseInt(idx[2]) - 1) : null;
                            verts[k] = new float[]{v[0], v[1], v[2], uv[0], uv[1], nn == null ? Float.NaN : nn[0], nn == null ? 0 : nn[1], nn == null ? 0 : nn[2]};
                        }
                        List<Float> l = out.computeIfAbsent(group, k -> new ArrayList<>());
                        for (int k = 1; k + 1 < n; k++) emit(l, verts[0], verts[k], verts[k + 1]);
                    }
                    default -> {}
                }
            }
        } catch (Exception e) {
            return m;
        }
        for (var e : out.entrySet()) {
            float[] a = new float[e.getValue().size()];
            for (int i = 0; i < a.length; i++) a[i] = e.getValue().get(i);
            m.groups.put(e.getKey(), a);
        }
        return m;
    }
    private static void emit(List<Float> l, float[] a, float[] b, float[] c) {
        float[] fn = null;
        if (Float.isNaN(a[5]) || Float.isNaN(b[5]) || Float.isNaN(c[5])) {
            float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2], vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
            float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            fn = len > 0 ? new float[]{nx / len, ny / len, nz / len} : new float[]{0, 1, 0};
        }
        for (float[] v : new float[][]{a, b, c}) {
            l.add(v[0]); l.add(v[1]); l.add(v[2]); l.add(v[3]); l.add(v[4]);
            if (fn != null) { l.add(fn[0]); l.add(fn[1]); l.add(fn[2]); } else { l.add(v[5]); l.add(v[6]); l.add(v[7]); }
        }
    }

    /** IModelCustom.renderPart. */
    public void renderPart(String part, PoseStack.Pose p, VertexConsumer vc, int argb, int light, int overlay) { renderPart(part, p, vc, argb, light, overlay, false); }
    /** flipV: the TESRs that scaled the texture matrix by (1,-1) see the raw OBJ v. */
    public void renderPart(String part, PoseStack.Pose p, VertexConsumer vc, int argb, int light, int overlay, boolean flipV) {
        float[] m = groups.get(part);
        if (m == null) return;
        for (int i = 0; i < m.length; i += 24) {
            for (int k = 0; k < 4; ++k) {
                int o = i + Math.min(k, 2) * 8;
                vc.addVertex(p, m[o], m[o + 1], m[o + 2]).setColor(argb).setUv(m[o + 3], flipV ? 1.0f - m[o + 4] : m[o + 4]).setOverlay(overlay).setLight(light).setNormal(p, m[o + 5], m[o + 6], m[o + 7]);
            }
        }
    }
    public void renderAll(PoseStack.Pose p, VertexConsumer vc, int argb, int light, int overlay) { for (String g : groups.keySet()) renderPart(g, p, vc, argb, light, overlay); }
    public void renderAllExcept(String except, PoseStack.Pose p, VertexConsumer vc, int argb, int light, int overlay) { for (String g : groups.keySet()) if (!g.equals(except)) renderPart(g, p, vc, argb, light, overlay); }
}
