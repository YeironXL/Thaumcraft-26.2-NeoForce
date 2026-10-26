package art.arcane.thaumcraft.client.fx;

import art.arcane.thaumcraft.client.fx.beam.TcBeams;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;

/**
 * TC6 FXShieldRunes: the hemisphere OBJ (models/obj/hemis.obj) with the 15 animated "hemis" rune frames (the "ripple"
 * frames for hostile mobs other than cultists, at half brightness), additive (GL 770/1), no depth writes, no culling,
 * full brightness. It follows the target's centre, faces the hit direction (yaw/pitch), scales to 0.2 x the target's
 * height and fades over the last third of its 8-12 tick life. With no target (pedestal shield) it is tinted purple.
 */
public final class ShieldRunesParticle extends TcBeams.BeamParticle {
    private final Entity target;
    private final float yaw, pitch;
    private float lastPt;

    public ShieldRunesParticle(ClientLevel level, double x, double y, double z, Entity target, int age, float yaw, float pitch) {
        super(level, x, y, z);
        this.gravity = 0.0f;
        this.xd = this.yd = this.zd = 0.0;
        this.hasPhysics = false;
        this.lifetime = age + random.nextInt(age / 2);
        this.target = target;
        this.yaw = yaw;
        this.pitch = pitch;
        if (target != null) {
            this.x = target.getX();
            this.y = (target.getBoundingBox().minY + target.getBoundingBox().maxY) / 2.0;
            this.z = target.getZ();
        }
        this.xo = this.x; this.yo = this.y; this.zo = this.z;
    }

    @Override
    public void tick() {
        xo = x; yo = y; zo = z;
        if (age++ >= lifetime) remove();
        if (target != null) {
            x = target.getX();
            y = (target.getBoundingBox().minY + target.getBoundingBox().maxY) / 2.0;
            z = target.getZ();
        }
    }

    private boolean mob() { return target instanceof Monster && !(target instanceof art.arcane.thaumcraft.entity.monster.CultistEntity); }

    private int frame(float pt) {
        float fade = (age + pt) / lifetime;
        return Math.max(1, Math.min(15, (int) (14.0f * fade) + 1));
    }

    @Override public RenderType renderType() {
        return TcBeams.additive(Identifier.fromNamespaceAndPath("thaumcraft", "textures/models/" + (mob() ? "ripple" : "hemis") + frame(lastPt) + ".png"));
    }

    @Override
    public SubmitNodeCollector.CustomGeometryRenderer geometry(Vec3 cam, float f) {
        lastPt = f;
        float[] m = SonicParticle.mesh();
        if (m.length == 0) return (pose, vc) -> {};
        float fade = (age + f) / lifetime;
        PoseStack ps = new PoseStack();
        ps.translate(xo + (x - xo) * f - cam.x, yo + (y - yo) * f - cam.y, zo + (z - zo) * f - cam.z);
        ps.mulPose(Axis.YP.rotationDegrees(180.0f - yaw));
        ps.mulPose(Axis.XP.rotationDegrees(-pitch));
        float th = target == null ? 2.0f : target.getBbHeight();
        ps.scale(0.2f * th, 0.2f * th, 0.2f * th);
        float alpha = Math.max(0.0f, Math.min(1.0f, (1.0f - fade) * 3.0f));
        float b = mob() ? 0.5f : 1.0f;
        int argb = target == null ? ARGB.colorFromFloat(alpha, 0.65f, 0.1f, 0.5f) : ARGB.colorFromFloat(alpha, b, b, b);
        PoseStack.Pose local = ps.last().copy();
        return (pose, vc) -> {
            PoseStack.Pose p = pose.copy();
            p.mulPose(local.pose());
            p.normal().mul(local.normal());
            for (int i = 0; i < m.length; i += 15) {
                for (int k = 0; k < 4; ++k) {
                    int o = i + Math.min(k, 2) * 5;
                    vc.addVertex(p, m[o], m[o + 1], m[o + 2]).setColor(argb).setUv(m[o + 3], m[o + 4]).setLight(0xF000F0).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(p, 0f, 1f, 0f);
                }
            }
        };
    }
}
