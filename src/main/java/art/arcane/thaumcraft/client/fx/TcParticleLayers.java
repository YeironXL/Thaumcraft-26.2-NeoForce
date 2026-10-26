package art.arcane.thaumcraft.client.fx;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;

/** TC6 ParticleEngine layers: 0 additive, 1 alpha blend, 2/3 the same without depth testing; depth is never written. */
public final class TcParticleLayers {
    private static final DepthStencilState TEST = new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false);
    private static final DepthStencilState NONE = new DepthStencilState(CompareOp.ALWAYS_PASS, false);
    public static final RenderPipeline ADDITIVE = pipeline("additive", BlendFunction.LIGHTNING, TEST);
    public static final RenderPipeline BLEND = pipeline("blend", BlendFunction.TRANSLUCENT, TEST);
    public static final RenderPipeline ADDITIVE_NO_DEPTH = pipeline("additive_no_depth", BlendFunction.LIGHTNING, NONE);
    public static final RenderPipeline BLEND_NO_DEPTH = pipeline("blend_no_depth", BlendFunction.TRANSLUCENT, NONE);
    private static RenderPipeline pipeline(String name, BlendFunction blend, DepthStencilState depth) {
        return RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET).withLocation(net.minecraft.resources.Identifier.fromNamespaceAndPath("thaumcraft", "pipeline/particle_" + name))
                .withColorTargetState(new ColorTargetState(blend)).withDepthStencilState(depth).build();
    }
    private static final SingleQuadParticle.Layer[] LAYERS = {
            new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES, ADDITIVE),
            new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES, BLEND),
            new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES, ADDITIVE_NO_DEPTH),
            new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES, BLEND_NO_DEPTH)};
    public static SingleQuadParticle.Layer layer(int n) { return LAYERS[Math.floorMod(n, 4)]; }
    public static void register(net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent event) {
        event.registerPipeline(ADDITIVE); event.registerPipeline(BLEND); event.registerPipeline(ADDITIVE_NO_DEPTH); event.registerPipeline(BLEND_NO_DEPTH);
    }
    private TcParticleLayers() {}
}
