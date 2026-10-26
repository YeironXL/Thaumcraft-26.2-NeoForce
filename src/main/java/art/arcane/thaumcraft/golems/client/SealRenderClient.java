package art.arcane.thaumcraft.golems.client;

import art.arcane.thaumcraft.client.fx.beam.TcBeams;
import art.arcane.thaumcraft.golems.GolemHelper;
import art.arcane.thaumcraft.golems.ISealDisplayer;
import art.arcane.thaumcraft.golems.seals.ISealConfigArea;
import art.arcane.thaumcraft.golems.seals.ISealEntity;
import art.arcane.thaumcraft.golems.seals.SealHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;

/** TC6 RenderEventHandler.drawSeals: while a seal displayer is held, seals within 16 blocks show their icon, a spinning disc and area corners (through walls when sneaking). */
public final class SealRenderClient {
    static final Identifier CFRAME = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/frame_corner.png");
    static final Identifier MIDDLE = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/seal_area.png");
    static final Direction[][] ROTFACES = {{Direction.DOWN, Direction.NORTH, Direction.WEST}, {Direction.UP, Direction.NORTH, Direction.WEST}, {Direction.DOWN, Direction.NORTH, Direction.EAST}, {Direction.UP, Direction.NORTH, Direction.EAST},
            {Direction.DOWN, Direction.SOUTH, Direction.EAST}, {Direction.UP, Direction.SOUTH, Direction.EAST}, {Direction.DOWN, Direction.SOUTH, Direction.WEST}, {Direction.UP, Direction.SOUTH, Direction.WEST}};
    static final int[][] ROTMAT = {{0, 270, 0}, {270, 180, 270}, {90, 0, 90}, {180, 90, 180}, {180, 180, 0}, {90, 270, 270}, {270, 90, 90}, {0, 0, 180}};

    public static void register() { NeoForge.EVENT_BUS.addListener(SealRenderClient::render); }

    private static void render(SubmitCustomGeometryEvent e) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.level == null) return;
        if (!(player.getMainHandItem().getItem() instanceof ISealDisplayer) && !(player.getOffhandItem().getItem() instanceof ISealDisplayer)) return;
        var seals = SealHandler.seals(mc.level);
        if (seals.isEmpty()) return;
        boolean see = player.isShiftKeyDown();
        Vec3 cam = e.getLevelRenderState().cameraRenderState.pos;
        PoseStack ps = e.getPoseStack();
        var col = e.getSubmitNodeCollector();
        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        var atlas = mc.getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS);
        for (ISealEntity seal : seals.values()) {
            var pos = seal.getSealPos().pos;
            double dis = pos.distToCenterSqr(player.position());
            if (dis > 256.0) continue;
            float alpha = 1.0f - (float) (dis / 256.0);
            boolean ia = false;
            if (seal.isStoppedByRedstone(mc.level)) {
                ia = true;
                if (mc.level.getRandom().nextFloat() < pt / 12.0f) {
                    Direction f = seal.getSealPos().face;
                    GolemClientFx.spark(pos.getX() + 0.5f + f.getStepX() * 0.66f, pos.getY() + 0.5f + f.getStepY() * 0.66f, pos.getZ() + 0.5f + f.getStepZ() * 0.66f, 2.0f, 0.8f - mc.level.getRandom().nextFloat() * 0.2f, 0, 0, 1);
                    ia = false;
                }
            }
            ps.pushPose();
            ps.translate(-cam.x, -cam.y, -cam.z);
            // icon
            var sprite = atlas.getSprite(seal.getSeal().getSealIcon());
            float c = ia ? 0.5f : 1.0f;
            int argb = ARGB.colorFromFloat(alpha, c, c, c);
            ps.pushPose();
            ps.translate(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            faceRotate(ps, seal.getSealPos().face);
            ps.translate(0, 0, 0.5 + (see ? 0.0 : 0.05));
            RenderType rt = see ? TcBeams.overlay(sprite.atlasLocation()) : TcBeams.translucent(sprite.atlasLocation());
            col.submitCustomGeometry(ps, rt, (p, vc) -> quad(p, vc, 0.25f, sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(), argb));
            ps.popPose();
            // spinning disc
            float r, g, b;
            if (seal.getColor() > 0) { int dc = DyeColor.byId(seal.getColor() - 1).getTextureDiffuseColor(); r = ARGB.red(dc) / 255f; g = ARGB.green(dc) / 255f; b = ARGB.blue(dc) / 255f; }
            else {
                r = 0.7f + Mth.sin((player.tickCount + pt + pos.getX()) / 4.0f) * 0.1f;
                g = 0.7f + Mth.sin((player.tickCount + pt + pos.getY()) / 5.0f) * 0.1f;
                b = 0.7f + Mth.sin((player.tickCount + pt + pos.getZ()) / 6.0f) * 0.1f;
            }
            int disc = ARGB.colorFromFloat(alpha * 0.8f, r, g, b);
            ps.pushPose();
            ps.translate(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            faceRotate(ps, seal.getSealPos().face);
            ps.translate(0, 0, 0.51);
            ps.mulPose(Axis.ZP.rotationDegrees(player.tickCount % 360 + pt));
            col.submitCustomGeometry(ps, see ? TcBeams.overlay(MIDDLE) : TcBeams.translucent(MIDDLE), (p, vc) -> quad(p, vc, 0.45f, 0, 1, 0, 1, disc));
            ps.popPose();
            if (seal.getSeal() instanceof ISealConfigArea) {
                AABB area = GolemHelper.getBoundsForArea(seal);
                double[][] locs = {{area.minX, area.minY, area.minZ}, {area.minX, area.maxY - 1, area.minZ}, {area.maxX - 1, area.minY, area.minZ}, {area.maxX - 1, area.maxY - 1, area.minZ},
                        {area.maxX - 1, area.minY, area.maxZ - 1}, {area.maxX - 1, area.maxY - 1, area.maxZ - 1}, {area.minX, area.minY, area.maxZ - 1}, {area.minX, area.maxY - 1, area.maxZ - 1}};
                int fc = ARGB.colorFromFloat(alpha * 0.7f, r, g, b);
                for (int q = 0; q < 8; q++) {
                    for (int w = 0; w < 3; w++) {
                        ps.pushPose();
                        ps.translate(locs[q][0] + 0.5, locs[q][1] + 0.5, locs[q][2] + 0.5);
                        faceRotate(ps, ROTFACES[q][w]);
                        ps.translate(0, 0, 0.49);
                        ps.mulPose(Axis.ZN.rotationDegrees(90));
                        ps.mulPose(Axis.ZP.rotationDegrees(ROTMAT[q][w]));
                        col.submitCustomGeometry(ps, see ? TcBeams.overlay(CFRAME) : TcBeams.translucent(CFRAME), (p, vc) -> quad(p, vc, 0.5f, 0, 1, 0, 1, fc));
                        ps.popPose();
                    }
                }
            }
            ps.popPose();
        }
    }
    /** Orient +Z to point out of the face. */
    static void faceRotate(PoseStack ps, Direction f) {
        switch (f) {
            case SOUTH -> {}
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(-90));
            case UP -> ps.mulPose(Axis.XP.rotationDegrees(-90));
            case DOWN -> ps.mulPose(Axis.XP.rotationDegrees(90));
        }
    }
    static void quad(PoseStack.Pose p, com.mojang.blaze3d.vertex.VertexConsumer vc, float s, float u0, float u1, float v0, float v1, int argb) {
        TcBeams.v(p, vc, new Vec3(-s, -s, 0), u0, v1, argb);
        TcBeams.v(p, vc, new Vec3(s, -s, 0), u1, v1, argb);
        TcBeams.v(p, vc, new Vec3(s, s, 0), u1, v0, argb);
        TcBeams.v(p, vc, new Vec3(-s, s, 0), u0, v0, argb);
    }
    private SealRenderClient() {}
}
