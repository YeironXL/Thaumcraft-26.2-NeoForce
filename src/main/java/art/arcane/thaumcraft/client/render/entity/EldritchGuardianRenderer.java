package art.arcane.thaumcraft.client.render.entity;

import art.arcane.thaumcraft.entity.monster.EldritchGuardianEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.Difficulty;

/** TC6 RenderEldritchGuardian: translucent (60%), fading out with distance from the viewer (16 to 32/24 blocks). */
public final class EldritchGuardianRenderer extends MobRenderer<EldritchGuardianEntity, EldritchGuardianRenderer.State, EldritchGuardianModel> {
    public static final class State extends EldritchGuardianModel.State { public float alpha = 1.0f; }
    private static final Identifier TEX = Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/eldritch_guardian.png");

    public EldritchGuardianRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new EldritchGuardianModel(ctx.bakeLayer(EldritchGuardianModel.LAYER)), 0.5f);
    }

    /** The overworld alpha of RenderEldritchGuardian.doRenderLiving for a squared viewer distance. */
    public static float alpha(double distSq, boolean hard) {
        float d4 = hard ? 576.0f : 1024.0f, d5 = 256.0f;
        if (distSq < 256.0) return 0.6f;
        return (float) (1.0 - Math.min(d4 - d5, distSq - d5) / (d4 - d5)) * 0.6f;
    }

    @Override public Identifier getTextureLocation(State s) { return TEX; }
    @Override public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(EldritchGuardianEntity e, State s, float pt) {
        super.extractRenderState(e, s, pt);
        s.armLiftL = e.armLiftL;
        s.armLiftR = e.armLiftR;
        var cam = Minecraft.getInstance().getCameraEntity();
        s.alpha = cam == null ? 0.6f : alpha(e.distanceToSqr(cam.getX(), cam.getY(), cam.getZ()), cam.level().getDifficulty() == Difficulty.HARD);
    }

    @Override protected int getModelTint(State s) { return ARGB.colorFromFloat(s.alpha, 1.0f, 1.0f, 1.0f); }
    @Override protected RenderType getRenderType(State s, boolean visible, boolean transparent, boolean glowing) { return RenderTypes.entityTranslucent(TEX); }
}
