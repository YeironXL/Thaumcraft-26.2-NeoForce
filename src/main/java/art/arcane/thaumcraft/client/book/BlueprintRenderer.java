package art.arcane.thaumcraft.client.book;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.Vec3;

/**
 * GuiResearchPage.renderBluePrint in 26.2: renders the blueprint's real block models (BlockRendererDispatcher.renderBlock),
 * fluids as still-fluid cubes (BlockFluidRenderer) and, for blocks without a model, their block entity renderer.
 */
public final class BlueprintRenderer extends PictureInPictureRenderer<BlueprintRenderState> {
    private static final BlockDisplayContext CONTEXT = BlockDisplayContext.create();
    private static final int LIGHT = 0xF000F0;
    private BlockModelResolver resolver;

    @Override public Class<BlueprintRenderState> getRenderStateClass() { return BlueprintRenderState.class; }
    @Override protected String getTextureLabel() { return "thaumcraft blueprint"; }
    @Override protected float getTranslateY(int height, int guiScale) { return height / 2.0f; }

    @Override
    protected void renderToTexture(BlueprintRenderState s, PoseStack pose, SubmitNodeCollector collector) {
        var mc = Minecraft.getInstance();
        mc.gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_FLAT);
        if (resolver == null) resolver = new BlockModelResolver(mc.getModelManager());
        float cx = (s.x0() + s.x1()) / 2.0f, cy = (s.y0() + s.y1()) / 2.0f;
        // GL: translate(transX, transY, depth); scale(scale, -scale, 1); rotate rotX (X); rotate rotY (Y); translate(-z/2, -y/2, -x/2)
        pose.translate(s.transX() - cx, s.transY() - cy, 0.0f);
        pose.scale(s.blockScale(), -s.blockScale(), -s.blockScale());
        pose.mulPose(Axis.XP.rotationDegrees(s.rotX()));
        pose.mulPose(Axis.YP.rotationDegrees(s.rotY()));
        pose.translate(s.zSize() / -2.0f, s.ySize() / -2.0f, s.xSize() / -2.0f);
        var bed = mc.getBlockEntityRenderDispatcher();
        var camera = new CameraRenderState();
        for (var b : s.blocks()) {
            var state = b.state();
            pose.pushPose();
            pose.translate(b.pos().getX(), b.pos().getY(), b.pos().getZ());
            if (!state.getFluidState().isEmpty() && state.getRenderShape() != RenderShape.MODEL) {
                fluid(pose, collector, state.getFluidState().is(FluidTags.LAVA) ? "lava_still" : "water_still", state.getFluidState().is(FluidTags.WATER) ? 0xFF3F76E4 : -1);
            } else if (state.getRenderShape() == RenderShape.MODEL) {
                var rs = new BlockModelRenderState();
                resolver.update(rs, state, CONTEXT);
                rs.submit(pose, collector, LIGHT, OverlayTexture.NO_OVERLAY, 0);
            }
            if (state.getRenderShape() != RenderShape.MODEL && state.getBlock() instanceof EntityBlock eb) {
                try {
                    var be = eb.newBlockEntity(b.pos(), state);
                    if (be != null) {
                        be.setLevel(mc.level);
                        var renderer = bed.getRenderer(be);
                        if (renderer != null) {
                            @SuppressWarnings({"unchecked", "rawtypes"})
                            var r = (net.minecraft.client.renderer.blockentity.BlockEntityRenderer) renderer;
                            var st = r.createRenderState();
                            r.extractRenderState(be, st, mc.getDeltaTracker().getGameTimeDeltaPartialTick(false), Vec3.ZERO, null);
                            ((net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState) st).lightCoords = LIGHT;
                            r.submit(st, pose, collector, camera);
                        }
                    }
                } catch (Exception ignored) {}
            }
            pose.popPose();
        }
    }

    /** BlockFluidRenderer for a still source block: full sides, surface at 8/9. */
    private static void fluid(PoseStack pose, SubmitNodeCollector collector, String sprite, int color) {
        TextureAtlasSprite sp = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.BLOCKS).getSprite(Identifier.withDefaultNamespace("block/" + sprite));
        float h = 8.0f / 9.0f;
        collector.submitCustomGeometry(pose, RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS), (p, vc) -> {
            float u0 = sp.getU0(), u1 = sp.getU1(), v0 = sp.getV0(), v1 = sp.getV1(), vh = sp.getV(1.0f - h);
            quad(p, vc, color, 0, h, 0, 0, h, 1, 1, h, 1, 1, h, 0, u0, v0, u1, v1, 0, 1, 0);
            quad(p, vc, color, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 1, u0, v0, u1, v1, 0, -1, 0);
            quad(p, vc, color, 0, h, 1, 0, 0, 1, 1, 0, 1, 1, h, 1, u0, vh, u1, v1, 0, 0, 1);
            quad(p, vc, color, 1, h, 0, 1, 0, 0, 0, 0, 0, 0, h, 0, u0, vh, u1, v1, 0, 0, -1);
            quad(p, vc, color, 1, h, 1, 1, 0, 1, 1, 0, 0, 1, h, 0, u0, vh, u1, v1, 1, 0, 0);
            quad(p, vc, color, 0, h, 0, 0, 0, 0, 0, 0, 1, 0, h, 1, u0, vh, u1, v1, -1, 0, 0);
        });
    }
    private static void quad(PoseStack.Pose p, VertexConsumer vc, int c, float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3, float u0, float v0, float u1, float v1, float nx, float ny, float nz) {
        vc.addVertex(p, x0, y0, z0).setColor(c).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LIGHT).setNormal(p, nx, ny, nz);
        vc.addVertex(p, x1, y1, z1).setColor(c).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LIGHT).setNormal(p, nx, ny, nz);
        vc.addVertex(p, x2, y2, z2).setColor(c).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LIGHT).setNormal(p, nx, ny, nz);
        vc.addVertex(p, x3, y3, z3).setColor(c).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LIGHT).setNormal(p, nx, ny, nz);
    }
}
