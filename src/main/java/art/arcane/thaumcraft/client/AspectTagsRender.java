package art.arcane.thaumcraft.client;

import art.arcane.thaumcraft.aspect.Aspect;
import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.client.fx.beam.TcBeams;
import com.mojang.math.Axis;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/** TC6 RenderEventHandler.drawTagsOnContainer / drawTextInAir and the shared static tagscale. */
public final class AspectTagsRender {
    /** RenderEventHandler.tagscale: grown by whoever draws tags, decayed 0.005 per frame in renderLast. */
    public static float tagscale;

    /** tagscale += 0.031 - tagscale/10 while below max. */
    public static void grow(float max) { if (tagscale < max) tagscale += 0.031f - tagscale / 10.0f; }

    /**
     * drawTagsOnContainer(x, y, z, tags, 220, dir, pt): x/y/z are the block-corner coordinates TC6 passes; with a
     * direction the icons are pushed tagscale*2 along it, without one x/z are shifted -0.5 (an entity's centre).
     */
    public static void drawTagsOnContainer(SubmitCustomGeometryEvent e, double x, double y, double z, Map<Aspect, Integer> tags, Direction dir) {
        if (tags == null || tags.isEmpty()) return;
        int fox = 0, foy = 0, foz = 0;
        if (dir != null) { fox = dir.getStepX(); foy = dir.getStepY(); foz = dir.getStepZ(); }
        else { x -= 0.5; z -= 0.5; }
        var mc = Minecraft.getInstance();
        var cam = e.getLevelRenderState().cameraRenderState.pos;
        var ps = e.getPoseStack();
        var collector = e.getSubmitNodeCollector();
        int rowsize = 5, current = 0, left = tags.size();
        float shifty = 0;
        for (var entry : tags.entrySet()) {
            Aspect tag = entry.getKey();
            int div = Math.min(left, rowsize);
            if (current >= rowsize) {
                current = 0;
                shifty -= tagscale * 1.05f;
                left -= rowsize;
                if (left < rowsize) div = left % rowsize;
            }
            float shift = (current - div / 2.0f + 0.5f) * tagscale * 4.0f;
            shift *= tagscale;
            int argb = (0xBF << 24) | (AspectRegistry.color(tag.id()) & 0xFFFFFF);
            ps.pushPose();
            ps.translate(x + 0.5 + tagscale * 2.0f * fox - cam.x, y - shifty + 0.5 + tagscale * 2.0f * foy - cam.y, z + 0.5 + tagscale * 2.0f * foz - cam.z);
            float rotYaw = (float) (Math.atan2(cam.x - (x + 0.5), cam.z - (z + 0.5)) * 180.0 / Math.PI);
            ps.mulPose(Axis.YP.rotationDegrees(rotYaw + 180.0f));
            ps.translate(shift, 0, 0);
            ps.scale(tagscale, tagscale, tagscale);
            var tex = Identifier.parse("thaumcraft:textures/aspects/" + tag.id() + ".png");
            collector.submitCustomGeometry(ps, TcBeams.overlay(tex), (pose, vc) -> {
                TcBeams.v(pose, vc, new Vec3(0.5, 0.5, 0), 0, 0, argb);
                TcBeams.v(pose, vc, new Vec3(-0.5, 0.5, 0), 1, 0, argb);
                TcBeams.v(pose, vc, new Vec3(-0.5, -0.5, 0), 1, 1, argb);
                TcBeams.v(pose, vc, new Vec3(0.5, -0.5, 0), 0, 1, argb);
            });
            int amount = entry.getValue();
            if (amount >= 0) {
                ps.mulPose(Axis.ZP.rotationDegrees(180.0f));
                ps.scale(0.04f, 0.04f, 0.04f);
                ps.translate(0, 6.0, -0.1);
                var seq = Component.literal(Integer.toString(amount)).getVisualOrderText();
                int sw = mc.font.width(seq);
                // TC6: 0x111111 shadow at (14,1), then white at (13,0); submit orders keep them above the icons and in that order
                collector.order(1).submitText(ps, 14 - sw, 1, seq, false, Font.DisplayMode.SEE_THROUGH, 0xF000F0, 0xFF111111, 0, 0);
                collector.order(2).submitText(ps, 13 - sw, 0, seq, false, Font.DisplayMode.SEE_THROUGH, 0xF000F0, 0xFFFFFFFF, 0, 0);
            }
            ps.popPose();
            ++current;
        }
    }

    /** drawTextInAir(x, y, z, pt, text): white text with shadow at the block-corner point + 0.5, facing the camera. */
    public static void drawTextInAir(SubmitCustomGeometryEvent e, double x, double y, double z, Component text) {
        var mc = Minecraft.getInstance();
        var cam = e.getLevelRenderState().cameraRenderState.pos;
        var ps = e.getPoseStack();
        x += 0.5; y += 0.5; z += 0.5;
        ps.pushPose();
        ps.translate(x - cam.x, y - cam.y, z - cam.z);
        float rotYaw = (float) (Math.atan2(cam.x - x, cam.z - z) * 180.0 / Math.PI);
        ps.mulPose(Axis.YP.rotationDegrees(rotYaw + 180.0f));
        ps.mulPose(Axis.ZP.rotationDegrees(180.0f));
        ps.scale(0.0125f, 0.0125f, 0.0125f);
        var seq = text.getVisualOrderText();
        int sw = mc.font.width(seq);
        e.getSubmitNodeCollector().submitText(ps, 1 - sw / 2f, 1.0f, seq, true, Font.DisplayMode.SEE_THROUGH, 0xF000F0, 0xFFFFFFFF, 0, 0);
        ps.popPose();
    }

    private AspectTagsRender() {}
}
