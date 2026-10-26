package art.arcane.thaumcraft.client.render;

import art.arcane.thaumcraft.blockentity.PedestalBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

/** TC6 TilePedestalRenderer: the held item spins 1.25x size above the pedestal. */
public final class PedestalRenderer<T extends PedestalBlockEntity> implements BlockEntityRenderer<T, PedestalRenderer.State> {
    public static final class State extends BlockEntityRenderState { final ItemStackRenderState item = new ItemStackRenderState(); float ticks, scale; }
    private final ItemModelResolver resolver;
    public PedestalRenderer(BlockEntityRendererProvider.Context c) { resolver = c.itemModelResolver(); }
    @Override public State createRenderState() { return new State(); }
    @Override
    public void extractRenderState(T te, State s, float pt, Vec3 cam, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(te, s, pt, cam, crumbling);
        resolver.updateForTopItem(s.item, te.item().copyWithCount(1), ItemDisplayContext.GROUND, te.getLevel(), null, 0);
        s.scale = te.renderScale();
        var view = Minecraft.getInstance().getCameraEntity();
        s.ticks = (view == null ? 0 : view.tickCount) + pt;
    }
    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (s.item.isEmpty()) return;
        pose.pushPose();
        pose.translate(0.5f, 0.75f, 0.5f);
        pose.scale(s.scale, s.scale, s.scale);
        pose.mulPose(Axis.YP.rotationDegrees(s.ticks % 360.0f));
        pose.translate(0f, 0.125f, 0f); // ItemEntityRenderer ground offset
        s.item.submit(pose, collector, s.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }
}
