package art.arcane.thaumcraft.client.render.entity;

import art.arcane.thaumcraft.entity.monster.EldritchAutocasterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Ancient Autocaster: the arcane bore's swivel head (a focus caster) in eldritch purple-black. */
public final class AutocasterRenderer extends MobRenderer<EldritchAutocasterEntity, TurretModels.State, TurretModels.Bore> {
    private static final Identifier TEX = Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/autocaster_eldritch.png");
    public AutocasterRenderer(EntityRendererProvider.Context ctx) { super(ctx, new TurretModels.Bore(ctx.bakeLayer(TurretModels.BORE)), 0.4f); }
    @Override public Identifier getTextureLocation(TurretModels.State s) { return TEX; }
    @Override public TurretModels.State createRenderState() { return new TurretModels.State(); }
    @Override public void extractRenderState(EldritchAutocasterEntity e, TurretModels.State s, float pt) {
        super.extractRenderState(e, s, pt);
        s.headYaw = Mth.rotLerp(pt, e.yHeadRotO, e.yHeadRot);
        s.pitch = Mth.lerp(pt, e.xRotO, e.getXRot());
        s.bodyRot = 0; s.yRot = 0;
        s.attackTime = e.getAttackAnim(pt);
    }
}
