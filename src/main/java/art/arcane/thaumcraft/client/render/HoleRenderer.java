package art.arcane.thaumcraft.client.render;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.blockentity.HoleBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.EnumSet;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** TC6 TileHoleRenderer: the end-sky shader on every solid face bordering the hole. */
public final class HoleRenderer implements BlockEntityRenderer<HoleBlockEntity, HoleRenderer.State> {
    public static final class State extends BlockEntityRenderState { final EnumSet<Direction> faces = EnumSet.noneOf(Direction.class); }
    public HoleRenderer(BlockEntityRendererProvider.Context c) {}
    @Override public State createRenderState() { return new State(); }
    @Override
    public void extractRenderState(HoleBlockEntity te, State s, float pt, Vec3 cam, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(te, s, pt, cam, crumbling);
        s.faces.clear();
        for (Direction d : Direction.values()) {
            var bs = te.getLevel().getBlockState(te.getBlockPos().relative(d));
            if (bs.isSolidRender() && !bs.is(ModBlocks.HOLE.get())) s.faces.add(d);
        }
    }
    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (s.faces.isEmpty()) return;
        var faces = EnumSet.copyOf(s.faces);
        collector.submitCustomGeometry(pose, RenderTypes.endPortal(), (p, vc) -> {
            float lo = 0.001f, hi = 0.999f;
            for (Direction d : faces) {
                // Quad on the inner side of the neighbouring block's face, wound to face into the hole.
                float[][] q = switch (d) {
                    case DOWN -> new float[][] {{0, lo, 0}, {1, lo, 0}, {1, lo, 1}, {0, lo, 1}};
                    case UP -> new float[][] {{0, hi, 1}, {1, hi, 1}, {1, hi, 0}, {0, hi, 0}};
                    case NORTH -> new float[][] {{0, 1, lo}, {1, 1, lo}, {1, 0, lo}, {0, 0, lo}};
                    case SOUTH -> new float[][] {{0, 0, hi}, {1, 0, hi}, {1, 1, hi}, {0, 1, hi}};
                    case WEST -> new float[][] {{lo, 0, 0}, {lo, 0, 1}, {lo, 1, 1}, {lo, 1, 0}};
                    case EAST -> new float[][] {{hi, 1, 0}, {hi, 1, 1}, {hi, 0, 1}, {hi, 0, 0}};
                };
                for (float[] v : q) vc.addVertex(p, v[0], v[1], v[2]).setColor(-1).setUv(0f, 0f).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(p, 0f, 1f, 0f);
            }
        });
    }
}
