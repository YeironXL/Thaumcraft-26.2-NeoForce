package art.arcane.thaumcraft.client.render.entity;

import art.arcane.thaumcraft.client.fx.beam.TcBeams;
import art.arcane.thaumcraft.client.render.ObjModel;
import art.arcane.thaumcraft.entity.construct.ArcaneBoreEntity;
import art.arcane.thaumcraft.entity.construct.TurretCrossbowEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.phys.Vec3;

/** TC6 RenderTurretCrossbow, RenderTurretCrossbowAdvanced and RenderArcaneBore. */
public final class TurretRenderers {
    private static void fill(net.minecraft.world.entity.LivingEntity e, TurretModels.State s, float pt) {
        s.headYaw = Mth.rotLerp(pt, e.yHeadRotO, e.yHeadRot);
        s.pitch = Mth.lerp(pt, e.xRotO, e.getXRot());
        s.bodyRot = 0;
        s.attackTime = e.getAttackAnim(pt);
        s.yRot = 0;
        s.minecart = e.getVehicle() instanceof AbstractMinecart;
        if (e instanceof TurretCrossbowEntity t) s.load = t.getLoadProgress(pt);
    }
    public static final class Basic extends MobRenderer<TurretCrossbowEntity, TurretModels.State, TurretModels.Crossbow> {
        private static final Identifier TEX = Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/crossbow.png");
        public Basic(EntityRendererProvider.Context ctx) { super(ctx, new TurretModels.Crossbow(ctx.bakeLayer(TurretModels.CROSSBOW)), 0.5f); }
        @Override public Identifier getTextureLocation(TurretModels.State s) { return TEX; }
        @Override public TurretModels.State createRenderState() { return new TurretModels.State(); }
        @Override public void extractRenderState(TurretCrossbowEntity e, TurretModels.State s, float pt) { super.extractRenderState(e, s, pt); fill(e, s, pt); }
    }
    public static final class Bore extends MobRenderer<ArcaneBoreEntity, Bore.State, TurretModels.Bore> {
        public static final class State extends TurretModels.State { boolean beam; int ticks; float yaw, xr; }
        private static final Identifier TEX = Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/arcanebore.png");
        private static final Identifier BEAM = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/beam1.png");
        public Bore(EntityRendererProvider.Context ctx) { super(ctx, new TurretModels.Bore(ctx.bakeLayer(TurretModels.BORE)), 0.5f); }
        @Override public Identifier getTextureLocation(State s) { return TEX; }
        @Override public State createRenderState() { return new State(); }
        @Override public void extractRenderState(ArcaneBoreEntity e, State s, float pt) {
            super.extractRenderState(e, s, pt); fill(e, s, pt);
            s.beam = e.clientDigging && e.isActive() && e.validInventory();
            s.ticks = e.tickCount; s.yaw = s.headYaw; s.xr = s.pitch;
        }
        @Override public void submit(State s, PoseStack ps, SubmitNodeCollector c, CameraRenderState cam) {
            super.submit(s, ps, c, cam);
            if (!s.beam) return;
            ps.pushPose();
            ps.translate(0, 0.8125, 0);
            ps.mulPose(Axis.YP.rotationDegrees(-s.yaw));
            ps.mulPose(Axis.XP.rotationDegrees(s.xr));
            ps.translate(0, 0.075, 0.5);
            float var11 = (s.ticks) * 0.2f - Mth.floor(s.ticks * 0.1f);
            int col = ARGB.colorFromFloat(0.4f, 0.0f, 1.0f, 0.4f);
            for (int t = 0; t < 3; t++) {
                ps.mulPose(Axis.ZP.rotationDegrees(60.0f + (s.ticks % 72) * 5.0f / 3.0f));
                float v0 = -1.0f + var11 + t / 3.0f, v1 = 5.0f + v0;
                c.submitCustomGeometry(ps, TcBeams.additive(BEAM), (p, vc) -> {
                    TcBeams.v(p, vc, new Vec3(0, 0, 5), 1, v1, col); TcBeams.v(p, vc, new Vec3(-0.15, 0, 0), 1, v0, col);
                    TcBeams.v(p, vc, new Vec3(0.15, 0, 0), 0, v0, col); TcBeams.v(p, vc, new Vec3(0, 0, 5), 0, v1, col);
                });
            }
            ps.popPose();
        }
    }
    /** RenderTurretCrossbowAdvanced: the crossbow_advanced OBJ, legs static, head yawed and pitched, bows flexing on shots. */
    public static final class Advanced extends EntityRenderer<TurretCrossbowEntity, Advanced.State> {
        public static final class State extends EntityRenderState { float yaw, pitch, load, swing; int hurt; boolean minecart; }
        private static final Identifier TEX = Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/crossbow_advanced.png");
        private static final Identifier OBJ = Identifier.fromNamespaceAndPath("thaumcraft", "models/obj/crossbow_advanced.obj");
        public Advanced(EntityRendererProvider.Context ctx) { super(ctx); shadowRadius = 0.5f; }
        @Override public State createRenderState() { return new State(); }
        @Override public void extractRenderState(TurretCrossbowEntity e, State s, float pt) {
            super.extractRenderState(e, s, pt);
            s.yaw = Mth.rotLerp(pt, e.yHeadRotO, e.yHeadRot); s.pitch = Mth.lerp(pt, e.xRotO, e.getXRot());
            s.load = e.getLoadProgress(pt); s.swing = e.getAttackAnim(pt); s.hurt = e.hurtTime; s.minecart = e.getVehicle() instanceof AbstractMinecart;
        }
        @Override public void submit(State s, PoseStack ps, SubmitNodeCollector c, CameraRenderState cam) {
            ObjModel m = ObjModel.get(OBJ);
            int light = s.lightCoords;
            int tint = s.hurt > 0 ? ARGB.colorFromFloat(1, 1, 0.5f, 0.5f) : -1;
            var rt = RenderTypes.entityTranslucent(TEX);
            ps.pushPose();
            ps.translate(0, 0.75, 0);
            ps.pushPose();
            if (s.minecart) ps.scale(0.66f, 0.75f, 0.66f);
            c.submitCustomGeometry(ps, rt, (p, vc) -> m.renderPart("legs", p, vc, -1, light, OverlayTexture.NO_OVERLAY));
            ps.popPose();
            ps.pushPose();
            ps.mulPose(Axis.YN.rotationDegrees(s.yaw));
            ps.mulPose(Axis.XP.rotationDegrees(s.pitch));
            c.submitCustomGeometry(ps, rt, (p, vc) -> { for (String g : new String[]{"mech", "box", "shield", "brain"}) m.renderPart(g, p, vc, tint, light, OverlayTexture.NO_OVERLAY); });
            ps.pushPose();
            ps.translate(0, 0, Mth.sin(Mth.sqrt(s.load) * (float) Math.PI * 2.0f) / 12.0f);
            c.submitCustomGeometry(ps, rt, (p, vc) -> m.renderPart("loader", p, vc, tint, light, OverlayTexture.NO_OVERLAY));
            ps.popPose();
            float sp = Mth.sin(Mth.sqrt(s.swing) * (float) Math.PI * 2.0f) * 20.0f;
            ps.translate(0, 0, 0.375);
            ps.pushPose(); ps.mulPose(Axis.YP.rotationDegrees(sp));
            c.submitCustomGeometry(ps, rt, (p, vc) -> m.renderPart("bow1", p, vc, tint, light, OverlayTexture.NO_OVERLAY)); ps.popPose();
            ps.pushPose(); ps.mulPose(Axis.YN.rotationDegrees(sp));
            c.submitCustomGeometry(ps, rt, (p, vc) -> m.renderPart("bow2", p, vc, tint, light, OverlayTexture.NO_OVERLAY)); ps.popPose();
            ps.popPose();
            ps.popPose();
            super.submit(s, ps, c, cam);
        }
    }
    private TurretRenderers() {}
}
