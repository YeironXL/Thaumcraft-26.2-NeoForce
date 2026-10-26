package art.arcane.thaumcraft.client.render.entity;

import art.arcane.thaumcraft.entity.champion.ChampionModifier;
import com.google.common.reflect.TypeToken;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import org.joml.Matrix4f;

/**
 * TC6 LayerTainted (ChampionModTainted.preRender): tainted creatures are redrawn with the taint fibre texture, tiled
 * 8x4 and offset by entity id, at 66% alpha.
 */
public final class TaintedLayer<S extends LivingEntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
    public static final ContextKey<Integer> CHAMPION = new ContextKey<>(Identifier.fromNamespaceAndPath("thaumcraft", "champion"));
    public static final ContextKey<Integer> ENTITY_ID = new ContextKey<>(Identifier.fromNamespaceAndPath("thaumcraft", "entity_id"));
    private static final Identifier TAINT_TEXTURE = Identifier.fromNamespaceAndPath("thaumcraft", "textures/models/taint_fibres.png");
    private static final Map<Integer, RenderType> TYPES = new HashMap<>();

    public TaintedLayer(RenderLayerParent<S, M> parent) { super(parent); }

    private static RenderType type(int id) {
        return TYPES.computeIfAbsent(id, i -> {
            float f2 = (float) Math.cos(i * 2.5E-4f), f3 = i * 0.001f;
            return RenderType.create("thaumcraft_tainted", RenderSetup.builder(RenderPipelines.BREEZE_WIND).withTexture("Sampler0", TAINT_TEXTURE)
                    .setTextureTransform(new TextureTransform("thaumcraft_taint", () -> new Matrix4f().scale(8.0f, 4.0f, 4.0f).translate(f2, f3, 0.0f)))
                    .useLightmap().sortOnUpload().createRenderSetup());
        });
    }

    @Override
    public void submit(PoseStack ps, SubmitNodeCollector c, int light, S s, float yRot, float xRot) {
        Integer t = s.getRenderData(CHAMPION);
        if (t == null || t != ChampionModifier.TAINTED || s.isInvisible) return;
        Integer id = s.getRenderData(ENTITY_ID);
        c.order(1).submitModel(getParentModel(), s, ps, type(id == null ? 0 : id), light, OverlayTexture.NO_OVERLAY, ARGB.colorFromFloat(0.66f, 1.0f, 1.0f, 1.0f), null, s.outlineColor, null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void register(IEventBus bus) {
        bus.addListener((RegisterRenderStateModifiersEvent e) -> e.registerEntityModifier(new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>() {},
                (LivingEntity en, LivingEntityRenderState s) -> {
                    s.setRenderData(CHAMPION, ChampionModifier.championValue(en));
                    s.setRenderData(ENTITY_ID, en.getId());
                }));
        bus.addListener((EntityRenderersEvent.AddLayers e) -> {
            for (var type : e.getEntityTypes()) {
                if (e.getRenderer(type) instanceof LivingEntityRenderer r) r.addLayer(new TaintedLayer(r));
            }
        });
    }
}
