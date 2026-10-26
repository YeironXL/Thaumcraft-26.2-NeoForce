package art.arcane.thaumcraft.client.render.entity;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.client.fx.beam.TcBeams;
import art.arcane.thaumcraft.entity.FluxRiftEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;

/**
 * TC6 RenderFluxRift: a GLE poly-cone along the rift spine drawn with the "ender" star shader, three widening additive
 * halo passes (the first one through walls when wearing goggles of revealing) and a final solid core. The core uses
 * vanilla's screen-space end gateway shader, the halos the end portal star texture with additive blending.
 */
public final class FluxRiftRenderer extends EntityRenderer<FluxRiftEntity, FluxRiftRenderer.State> {
    private static final Identifier STARS = Identifier.withDefaultNamespace("textures/entity/end_portal/end_portal.png");
    // TC6 drew the rift with its own "ender" GLSL program, which has no fog term: under dense (warp) fog the rift stays
    // dark and starry instead of washing out to the fog colour. Same pipelines as before, fragment shaders without fog.
    private static final Identifier HALO_FSH = Identifier.fromNamespaceAndPath("thaumcraft", "core/rift_halo");
    private static final com.mojang.blaze3d.pipeline.RenderPipeline HALO = TcBeams.ADDITIVE.toBuilder()
            .withLocation(Identifier.fromNamespaceAndPath("thaumcraft", "pipeline/rift_halo")).withFragmentShader(HALO_FSH).build();
    private static final com.mojang.blaze3d.pipeline.RenderPipeline HALO_OVERLAY = TcBeams.OVERLAY_ADDITIVE.toBuilder()
            .withLocation(Identifier.fromNamespaceAndPath("thaumcraft", "pipeline/rift_halo_overlay")).withFragmentShader(HALO_FSH).build();
    private static final com.mojang.blaze3d.pipeline.RenderPipeline CORE = net.minecraft.client.renderer.RenderPipelines.END_GATEWAY.toBuilder()
            .withLocation(Identifier.fromNamespaceAndPath("thaumcraft", "pipeline/rift_core")).withFragmentShader(Identifier.fromNamespaceAndPath("thaumcraft", "core/rift_core")).build();
    private static final RenderType HALO_TYPE = RenderType.create("thaumcraft_rift_halo", net.minecraft.client.renderer.rendertype.RenderSetup.builder(HALO).withTexture("Sampler0", STARS).sortOnUpload().createRenderSetup());
    private static final RenderType HALO_OVERLAY_TYPE = RenderType.create("thaumcraft_rift_halo_overlay", net.minecraft.client.renderer.rendertype.RenderSetup.builder(HALO_OVERLAY).withTexture("Sampler0", STARS).sortOnUpload().createRenderSetup());
    private static final RenderType CORE_TYPE = RenderType.create("thaumcraft_rift_core", net.minecraft.client.renderer.rendertype.RenderSetup.builder(CORE)
            .withTexture("Sampler0", net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer.END_SKY_LOCATION)
            .withTexture("Sampler1", net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer.END_PORTAL_LOCATION).createRenderSetup());

    public static final class State extends EntityRenderState {
        Vec3[] points = new Vec3[0];
        float[] widths = new float[0];
        float time, stability;
        boolean goggles;
    }

    public FluxRiftRenderer(EntityRendererProvider.Context ctx) { super(ctx); this.shadowRadius = 0.0f; }

    @Override public State createRenderState() { return new State(); }

    @Override public boolean shouldRender(FluxRiftEntity e, Frustum f, double x, double y, double z) {
        return e.points.size() > 2 && super.shouldRender(e, f, x, y, z);
    }

    @Override public void extractRenderState(FluxRiftEntity e, State s, float pt) {
        super.extractRenderState(e, s, pt);
        s.points = e.points.toArray(new Vec3[0]);
        s.widths = new float[e.pointsWidth.size()];
        for (int i = 0; i < s.widths.length; i++) s.widths[i] = e.pointsWidth.get(i);
        s.time = e.tickCount + pt;
        s.stability = e.getRiftStability();
        var p = Minecraft.getInstance().player;
        s.goggles = art.arcane.thaumcraft.util.Baubles.hasRevealer(p);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector c, CameraRenderState cam) {
        int n = Math.min(s.points.length, s.widths.length);
        if (n > 2) {
            float amp = 1.0f;
            float stab = Mth.clamp(1.0f - s.stability / 50.0f, 0.0f, 1.5f);
            for (int q = 0; q <= 3; ++q) {
                Vec3[] pp = new Vec3[n];
                float[] radii = new float[n];
                for (int a = 0; a < n; ++a) {
                    float var = s.time;
                    if (a > n / 2) var -= a * 10;
                    else if (a < n / 2) var += a * 10;
                    Vec3 p = s.points[a];
                    pp[a] = new Vec3(p.x + Math.sin(var / 50.0f * amp) * 0.1 * stab, p.y + Math.sin(var / 60.0f * amp) * 0.1 * stab,
                            p.z + Math.sin(var / 70.0f * amp) * 0.1 * stab);
                    double w = 1.0 - Math.sin(var / 8.0f * amp) * 0.1 * stab;
                    radii[a] = (float) (s.widths[a] * w * (q < 3 ? 1.25f + 0.5f * q : 1.0f));
                }
                RenderType type = q == 3 ? CORE_TYPE : (q == 0 && s.goggles ? HALO_OVERLAY_TYPE : HALO_TYPE);
                Vec3[] rp = new Vec3[n];
                float[] rr = new float[n];
                for (int a = 0; a < n; a++) { rp[a] = pp[n - 1 - a]; rr[a] = radii[n - 1 - a]; }
                c.submitCustomGeometry(ps, type, (pose, vc) -> {
                    // glePolyCone, POLYCYL_TESS 6; both windings so the culled core shows from every side.
                    TcBeams.polyCone(pose, vc, pp, radii, 0xFFFFFFFF, 6, 1.0f, 0.0f);
                    TcBeams.polyCone(pose, vc, rp, rr, 0xFFFFFFFF, 6, 1.0f, 0.0f);
                });
            }
        }
        super.submit(s, ps, c, cam);
    }
}
