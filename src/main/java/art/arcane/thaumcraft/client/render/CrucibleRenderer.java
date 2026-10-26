package art.arcane.thaumcraft.client.render;

import art.arcane.thaumcraft.blockentity.CrucibleBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** TC6 TileCrucibleRenderer: still-water plane at the fluid height, tinted by the dissolved essentia. */
public final class CrucibleRenderer implements BlockEntityRenderer<CrucibleBlockEntity, CrucibleRenderer.State> {
    public static final class State extends BlockEntityRenderState { float height; long vis; boolean water; }
    private static final Identifier WATER = Identifier.withDefaultNamespace("block/water_still");
    public CrucibleRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(CrucibleBlockEntity tile, State state, float partial, Vec3 camera, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partial, camera, crumbling);
        state.water = tile.mixture().water() > 0;
        state.height = tile.fluidHeight();
        state.vis = tile.mixture().total();
    }
    /** RGB multipliers of TileCrucibleRenderer.renderFluid for a dissolved essentia total. */
    public static int colorFor(long vis) {
        float recolor = Math.max(0, vis) / 500.0f;
        if (recolor > 0.0f) recolor = 0.5f + recolor / 2.0f;
        if (recolor > 1.0f) recolor = 1.0f;
        return 0xFF000000 | Math.round(255 * (1 - recolor / 3)) << 16 | Math.round(255 * (1 - recolor)) << 8 | Math.round(255 * (1 - recolor / 2));
    }
    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.water) return;
        int argb = colorFor(state.vis);
        var sprite = TcRender.blockSprite(WATER);
        float h = state.height; int light = state.lightCoords;
        collector.submitCustomGeometry(pose, RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS), (p, buffer) -> TcRender.flatQuad(p, buffer, sprite, h, argb, light));
    }
}
