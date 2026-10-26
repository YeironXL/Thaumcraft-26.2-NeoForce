package art.arcane.thaumcraft.client.render.entity;

import art.arcane.thaumcraft.entity.monster.PechEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.resources.Identifier;

/** TC6 RenderPech: ModelPech (shadow 0.25) skinned per pech type, with its held item. */
public final class PechRenderer extends MobRenderer<PechEntity, PechModel.State, PechModel> {
    private static final Identifier[] SKIN = {
            Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/pech_forage.png"),
            Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/pech_thaum.png"),
            Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/pech_stalker.png")};

    public PechRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PechModel(ctx.bakeLayer(PechModel.LAYER)), 0.25f);
        addLayer(new ItemInHandLayer<>(this));
    }
    @Override public Identifier getTextureLocation(PechModel.State s) { return SKIN[Math.max(0, Math.min(2, s.type))]; }
    @Override public PechModel.State createRenderState() { return new PechModel.State(); }
    @Override
    public void extractRenderState(PechEntity e, PechModel.State s, float pt) {
        super.extractRenderState(e, s, pt);
        ArmedEntityRenderState.extractArmedEntityRenderState(e, s, itemModelResolver, pt);
        s.mumble = e.mumble; s.type = e.getPechType(); s.riding = e.isPassenger(); s.crouching = e.isCrouching();
    }
}
