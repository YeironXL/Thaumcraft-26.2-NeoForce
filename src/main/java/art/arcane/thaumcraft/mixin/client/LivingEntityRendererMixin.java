package art.arcane.thaumcraft.mixin.client;

import art.arcane.thaumcraft.client.fx.ChampionFx;
import art.arcane.thaumcraft.client.render.entity.TaintedLayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * TC6 RenderEventHandler.renderLivingPre -> ChampionModifier.mods[t].effect.preRender: GL11.glColor4f before the
 * living model is drawn (fiery, grim, venomous, vampiric). 26.2 has no global colour state, so the same multiplier
 * goes into the model tint LivingEntityRenderer applies to the body.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "getModelTint", at = @At("RETURN"), cancellable = true)
    private void thaumcraft$championTint(LivingEntityRenderState state, CallbackInfoReturnable<Integer> cir) {
        Integer t = state.getRenderData(TaintedLayer.CHAMPION);
        if (t == null || t < 0) return;
        int tint = ChampionFx.tint(t);
        if (tint != -1) cir.setReturnValue(ARGB.multiply(cir.getReturnValueI(), tint));
    }
}
