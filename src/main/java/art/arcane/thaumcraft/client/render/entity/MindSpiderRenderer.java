package art.arcane.thaumcraft.client.render.entity;

import art.arcane.thaumcraft.entity.monster.MindSpiderEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.ARGB;

/**
 * TC6 RenderMindSpider: a 0.6-scale ghostly spider (body at most 10% opaque, eyes glowing) that only its viewer
 * (the warped player) can see; spiders without a viewer are visible to everyone.
 */
public final class MindSpiderRenderer extends SpiderRenderer<MindSpiderEntity> {
    public MindSpiderRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.5f;
    }

    public static boolean visibleTo(MindSpiderEntity spider, String viewerName) {
        return spider.getViewer().isEmpty() || spider.getViewer().equals(viewerName);
    }

    @Override
    public boolean shouldRender(MindSpiderEntity e, Frustum culler, double camX, double camY, double camZ) {
        var cam = Minecraft.getInstance().getCameraEntity();
        if (cam == null || !visibleTo(e, cam.getName().getString())) return false;
        return super.shouldRender(e, culler, camX, camY, camZ);
    }

    @Override protected void scale(LivingEntityRenderState s, PoseStack ps) { ps.scale(0.6f, 0.6f, 0.6f); }
    @Override protected int getModelTint(LivingEntityRenderState s) { return ARGB.colorFromFloat(Math.min(0.1f, s.ageInTicks / 100.0f), 1.0f, 1.0f, 1.0f); }
    @Override protected RenderType getRenderType(LivingEntityRenderState s, boolean visible, boolean transparent, boolean glowing) {
        return RenderTypes.entityTranslucent(getTextureLocation(s));
    }
}
