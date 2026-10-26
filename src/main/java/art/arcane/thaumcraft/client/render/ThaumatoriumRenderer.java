package art.arcane.thaumcraft.client.render;

import art.arcane.thaumcraft.block.ThaumatoriumBlock;
import art.arcane.thaumcraft.blockentity.ThaumatoriumBlockEntity;
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
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

/** TC6 TileThaumatoriumRenderer: the selected outputs cycle on the front face, one per second. */
public final class ThaumatoriumRenderer implements BlockEntityRenderer<ThaumatoriumBlockEntity, ThaumatoriumRenderer.State> {
    public static final class State extends BlockEntityRenderState { final ItemStackRenderState item = new ItemStackRenderState(); Direction facing = Direction.NORTH; }
    private final ItemModelResolver resolver;
    public ThaumatoriumRenderer(BlockEntityRendererProvider.Context c) { resolver = c.itemModelResolver(); }
    @Override public State createRenderState() { return new State(); }
    @Override
    public void extractRenderState(ThaumatoriumBlockEntity te, State s, float pt, Vec3 cam, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(te, s, pt, cam, crumbling);
        s.facing = te.getBlockState().getValue(ThaumatoriumBlock.FACING);
        s.item.clear();
        if (te.selected.isEmpty()) return;
        var view = Minecraft.getInstance().getCameraEntity();
        int i = (view == null ? 0 : view.tickCount) / 20 % te.selected.size();
        resolver.updateForTopItem(s.item, te.selected.get(i).output().copyWithCount(1), ItemDisplayContext.GROUND, te.getLevel(), null, 0);
    }
    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector c, CameraRenderState camera) {
        if (s.item.isEmpty()) return;
        pose.pushPose();
        pose.translate(0.5f + s.facing.getStepX() / 1.99f, 1.125f, 0.5f + s.facing.getStepZ() / 1.99f);
        switch (s.facing) {
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(90));
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(270));
            case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(180));
            default -> {}
        }
        pose.scale(0.75f, 0.75f, 0.75f);
        s.item.submit(pose, c, s.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }
}
