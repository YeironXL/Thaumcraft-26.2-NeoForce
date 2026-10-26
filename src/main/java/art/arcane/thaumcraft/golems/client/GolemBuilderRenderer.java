package art.arcane.thaumcraft.golems.client;

import art.arcane.thaumcraft.client.render.ObjModel;
import art.arcane.thaumcraft.client.render.TcRender;
import art.arcane.thaumcraft.golems.builder.GolemBuilderBlock;
import art.arcane.thaumcraft.golems.builder.GolemBuilderBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** TC6 TileGolemBuilderRenderer: the press OBJ facing the multiblock, its ram lowered by the press animation, a lava surface in the cauldron. */
public final class GolemBuilderRenderer implements BlockEntityRenderer<GolemBuilderBlockEntity, GolemBuilderRenderer.State> {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath("thaumcraft", "models/block/golembuilder.obj");
    private static final Identifier TEX = Identifier.fromNamespaceAndPath("thaumcraft", "textures/block/golembuilder.png");
    public static final class State extends BlockEntityRenderState { Direction facing = Direction.NORTH; int press; }
    public GolemBuilderRenderer(BlockEntityRendererProvider.Context c) {}
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(GolemBuilderBlockEntity te, State s, float pt, Vec3 cam, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(te, s, pt, cam, crumbling);
        s.facing = te.getBlockState().getValue(GolemBuilderBlock.FACING);
        s.press = te.press;
    }
    @Override public AABB getRenderBoundingBox(GolemBuilderBlockEntity te) { return new AABB(te.getBlockPos()).inflate(1.0, 0, 1.0).expandTowards(0, 1, 0); }
    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector c, CameraRenderState cam) {
        ObjModel m = ObjModel.get(MODEL);
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        switch (s.facing) {
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(270));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            default -> {}
        }
        c.submitCustomGeometry(ps, RenderTypes.entityCutout(TEX), (p, vc) -> { for (String g : m.partNames()) if (!g.equals("press")) m.renderPart(g, p, vc, -1, light, OverlayTexture.NO_OVERLAY, true); });
        ps.pushPose();
        ps.translate(0.0, -Math.sin(Math.toRadians(s.press)) * 0.625, 0.0);
        c.submitCustomGeometry(ps, RenderTypes.entityCutout(TEX), (p, vc) -> m.renderPart("press", p, vc, -1, light, OverlayTexture.NO_OVERLAY, true));
        ps.popPose();
        // lava in the cauldron: UtilsFX.renderQuadFromIcon(lava, 0.625) after translate(-0.3125, 0.625, 1.3125), rotate -90 X
        var lava = TcRender.blockSprite(Identifier.withDefaultNamespace("block/lava_still"));
        ps.translate(-0.3125, 0.625, 0.6875);
        ps.scale(0.625f, 1.0f, 0.625f);
        c.submitCustomGeometry(ps, RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS), (p, vc) -> TcRender.flatQuad(p, vc, lava, 0.0f, -1, 0xF000F0));
        ps.popPose();
    }
}
