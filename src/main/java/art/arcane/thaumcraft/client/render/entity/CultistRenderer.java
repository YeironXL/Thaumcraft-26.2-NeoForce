package art.arcane.thaumcraft.client.render.entity;

import art.arcane.thaumcraft.client.fx.beam.TcBeams;
import art.arcane.thaumcraft.entity.monster.CultistClericEntity;
import art.arcane.thaumcraft.entity.monster.CultistEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * TC6 RenderCultist: the 64x32 biped with the cultist skin and its held item. Worn armour (the crimson robe/plate pieces)
 * is drawn by the armour layer with their custom models. Ritualist clerics bob in the air and are tied to their altar
 * focus by the purple wispy floaty line.
 */
public final class CultistRenderer extends MobRenderer<CultistEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
    /** Ritualist cleric data: bob offset and the floaty line from the altar focus (home + 0.5, 1.5, 0.5) to the cleric. */
    public static final class State extends HumanoidRenderState {
        public boolean ritualist;
        public float bob, distance, time;
        public double ox, oy, oz, dcx, dcy, dcz, ex, ey, ez;
    }
    private static final Identifier WISPY = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/wispy.png");
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("thaumcraft", "cultist"), "main");
    private static final Identifier SKIN = Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/cultist.png");

    /** ModelBiped(): 64x32 texture. */
    public static LayerDefinition create() { return LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f), 64, 32); }

    public CultistRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(LAYER)), 0.5f);
        addLayer(new net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer<>(this,
                net.minecraft.client.renderer.entity.ArmorModelSet.bake(net.minecraft.client.model.geom.ModelLayers.ZOMBIE_ARMOR, ctx.getModelSet(), HumanoidModel::new), ctx.getEquipmentRenderer()));
        addLayer(new ItemInHandLayer<>(this));
    }

    @Override public Identifier getTextureLocation(HumanoidRenderState s) { return SKIN; }
    @Override public HumanoidRenderState createRenderState() { return new State(); }

    @Override
    public void extractRenderState(CultistEntity e, HumanoidRenderState s, float pt) {
        super.extractRenderState(e, s, pt);
        HumanoidMobRenderer.extractHumanoidRenderState(e, s, pt, itemModelResolver);
        if (!(s instanceof State st)) return;
        st.ritualist = e instanceof CultistClericEntity c && c.getIsRitualist();
        if (!st.ritualist) return;
        // RenderCultist.doRender: bob = sin((ticks + pt + Random(id).nextInt(1000)) / 9) * 0.1 + 0.21
        int val = new java.util.Random(e.getId()).nextInt(1000);
        float cc = e.tickCount + pt + val;
        st.bob = Mth.sin(cc / 9.0f) * 0.1f + 0.21f;
        BlockPos h = e.getHomePosition();
        double hx = h.getX() + 0.5, hy = h.getY() + 1.5 - st.bob, hz = h.getZ() + 0.5;
        // the line is anchored at the altar point (relative to the interpolated render origin, inside the bob translate)
        st.ox = hx - Mth.lerp(pt, e.xo, e.getX()); st.oy = hy - Mth.lerp(pt, e.yo, e.getY()); st.oz = hz - Mth.lerp(pt, e.zo, e.getZ());
        st.ex = e.getX(); st.ey = e.getY() + e.getEyeHeight() * 1.2f; st.ez = e.getZ();
        st.dcx = (float) (st.ex - hx); st.dcy = (float) (st.ey - hy); st.dcz = (float) (st.ez - hz);
        st.distance = Math.min(e.tickCount, 10) / 10.0f;
        st.time = (float) (System.nanoTime() / 30000000L);
    }

    @Override
    public void submit(HumanoidRenderState s, PoseStack ps, SubmitNodeCollector c, CameraRenderState cam) {
        State st = (State) s;
        ps.pushPose();
        if (st.ritualist) ps.translate(0.0, st.bob, 0.0);
        super.submit(s, ps, c, cam);
        if (st.ritualist) {
            ps.pushPose();
            ps.translate(st.ox, st.oy, st.oz);
            c.submitCustomGeometry(ps, TcBeams.translucent(WISPY), (pose, vc) -> drawFloatyLine(pose, vc, st, 0x110011, -0.03f, 0.25f));
            ps.popPose();
        }
        ps.popPose();
    }

    /** RenderCultist.drawFloatyLine: two crossed GL_TRIANGLE_STRIPs (vertical and horizontal width), emitted as quads. */
    private static void drawFloatyLine(PoseStack.Pose pose, VertexConsumer vc, State st, int color, float speed, float width) {
        float r = (color >> 16 & 255) / 255.0f, g = (color >> 8 & 255) / 255.0f, b = (color & 255) / 255.0f;
        int argb = ARGB.colorFromFloat(0.8f, r, g, b);
        double d3 = st.dcx, d4 = st.dcy, d5 = st.dcz;
        float dist = Mth.sqrt((float) (d3 * d3 + d4 * d4 + d5 * d5));
        float length = Math.round(dist) * 6.0f;
        int n = (int) (length * st.distance);
        if (length <= 0 || n < 1) return;
        float time = st.time;
        for (int pass = 0; pass < 2; ++pass) {
            float[] px = new float[n + 1], py = new float[n + 1], pz = new float[n + 1], u = new float[n + 1];
            for (int i = 0; i <= n; ++i) {
                float f11 = i / length;
                float f12 = 1.0f - Math.abs(i - length / 2.0f) / (length / 2.0f);
                double dx3 = st.dcx + Mth.sin((float) ((st.ez % 16.0 + dist * (1.0f - f11) * 6.0f - time % 32767.0f / 5.0f) / 4.0)) * 0.5f * f12;
                double dy3 = st.dcy + Mth.sin((float) ((st.ex % 16.0 + dist * (1.0f - f11) * 6.0f - time % 32767.0f / 5.0f) / 3.0)) * 0.5f * f12;
                double dz3 = st.dcz + Mth.sin((float) ((st.ey % 16.0 + dist * (1.0f - f11) * 6.0f - time % 32767.0f / 5.0f) / 2.0)) * 0.5f * f12;
                px[i] = (float) (dx3 * f11); py[i] = (float) (dy3 * f11); pz[i] = (float) (dz3 * f11);
                u[i] = (1.0f - f11) * dist - time * speed;
            }
            float wx = pass == 1 ? width : 0.0f, wy = pass == 0 ? width : 0.0f;
            for (int i = 0; i < n; ++i) {
                // strip pairs (lo,hi)_i , (lo,hi)_i+1 -> quad lo_i, hi_i, hi_i+1, lo_i+1
                vert(pose, vc, px[i] - wx, py[i] - wy, pz[i], u[i], 1.0f, argb);
                vert(pose, vc, px[i] + wx, py[i] + wy, pz[i], u[i], 0.0f, argb);
                vert(pose, vc, px[i + 1] + wx, py[i + 1] + wy, pz[i + 1], u[i + 1], 0.0f, argb);
                vert(pose, vc, px[i + 1] - wx, py[i + 1] - wy, pz[i + 1], u[i + 1], 1.0f, argb);
            }
        }
    }

    private static void vert(PoseStack.Pose pose, VertexConsumer vc, float x, float y, float z, float u, float v, int argb) {
        vc.addVertex(pose, x, y, z).setColor(argb).setUv(u, v).setLight(0xF000F0).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(pose, 0f, 1f, 0f);
    }
}
