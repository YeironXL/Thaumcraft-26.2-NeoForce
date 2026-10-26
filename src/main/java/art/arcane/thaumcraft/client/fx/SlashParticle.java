package art.arcane.thaumcraft.client.fx;

import art.arcane.thaumcraft.client.fx.beam.TcBeams;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * TC6 FXPlane (FXDispatcher.drawSlash, PacketFXSlash): a flat quad flying from one point to another over its life,
 * oriented along the flight with a random roll, cycling the blue crescent frames 22-30 of row 10 on the 32-grid
 * particle sheet, growing from 0.5 to 1.5 and fading in over the first fifth then out; drawn on ParticleEngine layer 0
 * (additive, no depth writes) at half alpha, double sided.
 */
public final class SlashParticle extends TcBeams.BeamParticle {
    private static final Identifier SHEET = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png");
    private final float angle, angleYaw, anglePitch;
    private float alpha;

    public SlashParticle(ClientLevel level, double x, double y, double z, double tx, double ty, double tz, int life) {
        super(level, x, y, z);
        gravity = 0.0f;
        hasPhysics = false;
        lifetime = life;
        xo = x; yo = y; zo = z;
        alpha = 0.0f;
        double dx = tx - x, dy = ty - y, dz = tz - z;
        xd = dx / lifetime; yd = dy / lifetime; zd = dz / lifetime;
        double d3 = Math.sqrt(dx * dx + dz * dz);
        float yw = 0.0f, pt = 0.0f;
        if (d3 >= 1.0E-7) {
            yw = (float) (Mth.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
            pt = (float) (-(Mth.atan2(dy, d3) * 180.0 / Math.PI));
        }
        angleYaw = yw; anglePitch = pt;
        angle = (float) (random.nextGaussian() * 20.0);
    }

    @Override
    public void tick() {
        xo = x; yo = y; zo = z;
        float threshold = lifetime / 5.0f;
        alpha = age <= threshold ? age / threshold : (lifetime - age) / (float) lifetime;
        if (age++ >= lifetime) remove();
        x += xd; y += yd; z += zd;
    }

    @Override public RenderType renderType() { return TcBeams.additive(SHEET); }

    @Override
    public SubmitNodeCollector.CustomGeometryRenderer geometry(Vec3 cam, float f) {
        PoseStack ps = new PoseStack();
        ps.translate(xo + (x - xo) * f - cam.x, yo + (y - yo) * f - cam.y, zo + (z - zo) * f - cam.z);
        ps.mulPose(Axis.YP.rotationDegrees(-angleYaw + 90.0f));
        ps.mulPose(Axis.ZP.rotationDegrees(anglePitch + 90.0f));
        ps.mulPose(Axis.YP.rotationDegrees(angle));
        int tx = 22 + Math.round((age + f) / lifetime * 8.0f);
        float u0 = tx / 32.0f, u1 = u0 + 0.03125f, v0 = 10 / 32.0f, v1 = v0 + 0.03125f;
        float s = 0.5f + (age + f) / lifetime;
        int argb = ARGB.colorFromFloat(Math.max(0.0f, Math.min(1.0f, alpha / 2.0f)), 1.0f, 1.0f, 1.0f);
        PoseStack.Pose local = ps.last().copy();
        return (pose, vc) -> {
            PoseStack.Pose p = pose.copy();
            p.mulPose(local.pose());
            float h = 0.5f * s;
            vc.addVertex(p, -h, h, 0).setColor(argb).setUv(u1, v1).setLight(0xF000F0).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(p, 0f, 1f, 0f);
            vc.addVertex(p, h, h, 0).setColor(argb).setUv(u1, v0).setLight(0xF000F0).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(p, 0f, 1f, 0f);
            vc.addVertex(p, h, -h, 0).setColor(argb).setUv(u0, v0).setLight(0xF000F0).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(p, 0f, 1f, 0f);
            vc.addVertex(p, -h, -h, 0).setColor(argb).setUv(u0, v1).setLight(0xF000F0).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(p, 0f, 1f, 0f);
        };
    }
}
