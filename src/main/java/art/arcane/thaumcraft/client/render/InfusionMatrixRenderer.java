package art.arcane.thaumcraft.client.render;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.blockentity.InfusionMatrixBlockEntity;
import art.arcane.thaumcraft.client.render.entity.TcModelLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** TC6 TileInfusionMatrixRenderer: eight jittering cubes, rotating once started, with an additive purple glow when active. */
public final class InfusionMatrixRenderer implements BlockEntityRenderer<InfusionMatrixBlockEntity, InfusionMatrixRenderer.State> {
    private static final Identifier TEX1 = Identifier.fromNamespaceAndPath("thaumcraft", "textures/block/infuser_normal.png");
    private static final Identifier TEX2 = Identifier.fromNamespaceAndPath("thaumcraft", "textures/block/infuser_ancient.png");
    private static final Identifier TEX3 = Identifier.fromNamespaceAndPath("thaumcraft", "textures/block/infuser_eldritch.png");
    public static final class State extends BlockEntityRenderState { float ticks, inst, startup; int craftcount; boolean active, crafting; Identifier tex = TEX1; }
    private final ModelPart cube, over;
    public InfusionMatrixRenderer(BlockEntityRendererProvider.Context c) { cube = c.bakeLayer(TcModelLayers.CUBE); over = c.bakeLayer(TcModelLayers.CUBE_OVER); }
    @Override public State createRenderState() { return new State(); }
    @Override
    public void extractRenderState(InfusionMatrixBlockEntity te, State s, float pt, Vec3 cam, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(te, s, pt, cam, crumbling);
        var view = Minecraft.getInstance().getCameraEntity();
        s.ticks = (view == null ? 0 : view.tickCount) + pt;
        var bs = te.getLevel().getBlockState(te.getBlockPos().offset(-1, -2, -1));
        s.tex = bs.is(ModBlocks.PILLAR_ANCIENT.get()) ? TEX2 : bs.is(ModBlocks.PILLAR_ELDRITCH.get()) ? TEX3 : TEX1;
        s.inst = te.stability; s.craftcount = te.craftCount; s.startup = te.startUp; s.active = te.active; s.crafting = te.crafting;
    }
    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5f, 0.5f, 0.5f);
        pose.mulPose(Axis.YP.rotationDegrees(s.ticks % 360.0f * s.startup));
        pose.mulPose(Axis.XP.rotationDegrees(35.0f * s.startup));
        pose.mulPose(Axis.ZP.rotationDegrees(45.0f * s.startup));
        float instability = Math.min(6.0f, 1.0f + (s.inst < 0 ? -s.inst * 0.66f : 1.0f) * (Math.min(s.craftcount, 50) / 50.0f));
        for (int pass = 0; pass < (s.active ? 2 : 1); pass++)
            for (int a = 0; a < 2; ++a) for (int b = 0; b < 2; ++b) for (int c = 0; c < 2; ++c) {
                float b1 = 0, b2 = 0, b3 = 0;
                if (s.active) {
                    b1 = Mth.sin((s.ticks + a * 10) / 15.0f) * 0.01f * s.startup * instability;
                    b2 = Mth.sin((s.ticks + b * 10) / 14.0f) * 0.01f * s.startup * instability;
                    b3 = Mth.sin((s.ticks + c * 10) / 13.0f) * 0.01f * s.startup * instability;
                }
                pose.pushPose();
                pose.translate(b1 + (a == 0 ? -1 : 1) * 0.25f, b2 + (b == 0 ? -1 : 1) * 0.25f, b3 + (c == 0 ? -1 : 1) * 0.25f);
                if (a > 0) pose.mulPose(Axis.XP.rotationDegrees(90));
                if (b > 0) pose.mulPose(Axis.YP.rotationDegrees(90));
                if (c > 0) pose.mulPose(Axis.ZP.rotationDegrees(90));
                pose.scale(0.45f, 0.45f, 0.45f);
                if (pass == 0) collector.submitModelPart(cube, pose, RenderTypes.entityCutout(s.tex), s.lightCoords, OverlayTexture.NO_OVERLAY, null);
                else {
                    float alpha = (Mth.sin((s.ticks + a * 2 + b * 3 + c * 4) / 4.0f) * 0.1f + 0.2f) * s.startup;
                    int col = ((int) (Mth.clamp(alpha, 0, 1) * 255) << 24) | ((int) (0.8f * 255) << 16) | ((int) (0.1f * 255) << 8) | 255;
                    collector.submitModelPart(over, pose, RenderTypes.eyes(s.tex), 0xF000F0, OverlayTexture.NO_OVERLAY, null, col, null);
                }
                pose.popPose();
            }
        pose.popPose();
        if (s.crafting) drawHalo(s, pose, collector);
    }

    /** drawHalo: additive white-to-magenta ray fans around the matrix, growing over the first 50 craft ticks. */
    private static void drawHalo(State s, PoseStack pose, SubmitNodeCollector collector) {
        int count = s.craftcount;
        if (count <= 0) return;
        int q = 20; // fancy graphics
        float f1 = count / 500.0f;
        float grow = Math.min(count, 50) / 50.0f;
        pose.pushPose();
        pose.translate(0.5f, 0.5f, 0.5f);
        collector.submitCustomGeometry(pose, RenderTypes.dragonRays(), (p, vc) -> {
            java.util.Random random = new java.util.Random(245L);
            org.joml.Quaternionf rot = new org.joml.Quaternionf();
            int centre = net.minecraft.util.ARGB.color((int) (255.0f * Mth.clamp(1.0f - f1, 0, 1)), 255, 255, 255);
            int rimC = net.minecraft.util.ARGB.color(0, 255, 0, 255);
            for (int i = 0; i < q; ++i) {
                rot.rotationX(random.nextFloat() * 6.2831855f).rotateY(random.nextFloat() * 6.2831855f).rotateZ(random.nextFloat() * 6.2831855f)
                        .rotateX(random.nextFloat() * 6.2831855f).rotateY(random.nextFloat() * 6.2831855f).rotateZ(random.nextFloat() * 6.2831855f + f1 * 6.2831855f);
                p.rotate(rot);
                float fa = (random.nextFloat() * 20.0f + 5.0f) / (20.0f / grow);
                float f4 = (random.nextFloat() * 2.0f + 1.0f) / (20.0f / grow);
                float[][] rim = { { -0.866f * f4, fa, -0.5f * f4 }, { 0.866f * f4, fa, -0.5f * f4 }, { 0.0f, fa, f4 }, { -0.866f * f4, fa, -0.5f * f4 } };
                for (int t = 0; t < 3; t++) {
                    vc.addVertex(p, 0, 0, 0).setColor(centre).setUv(0f, 0f).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(p, 0f, 1f, 0f);
                    vc.addVertex(p, rim[t][0], rim[t][1], rim[t][2]).setColor(rimC).setUv(0f, 0f).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(p, 0f, 1f, 0f);
                    vc.addVertex(p, rim[t + 1][0], rim[t + 1][1], rim[t + 1][2]).setColor(rimC).setUv(0f, 0f).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(p, 0f, 1f, 0f);
                }
            }
        });
        pose.popPose();
    }
}
