package art.arcane.thaumcraft.golems.client;

import art.arcane.thaumcraft.client.render.ObjModel;
import art.arcane.thaumcraft.golems.GolemEntity;
import art.arcane.thaumcraft.golems.GolemProperties;
import art.arcane.thaumcraft.golems.GolemTrait;
import art.arcane.thaumcraft.golems.parts.GolemPart;
import art.arcane.thaumcraft.golems.parts.PartModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * TC6 RenderThaumcraftGolem: the golem_base.obj torso, arms and flag in the material texture, plus the OBJ models of
 * its head, arms, legs and addon attached to the body, head, arm or leg pivots, animated as in TC6.
 */
public final class GolemRenderer extends EntityRenderer<GolemEntity, GolemRenderer.State> {
    private static final Identifier BASE = Identifier.fromNamespaceAndPath("thaumcraft", "models/obj/golem_base.obj");
    public static final class State extends EntityRenderState {
        long props; float limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, swing, speed, yawDelta, bodyRot, wheelRot, prevPitch, pt;
        int color, id, ticks, overlay, deathTime; boolean inCombat, holding;
        final ItemStackRenderState main = new ItemStackRenderState(), off = new ItemStackRenderState();
        boolean mainBlock, offBlock;
        /** Owner sneaking with a seal displayer and the golem out of line of sight: draw it again through walls. */
        boolean xray;
    }
    /** RenderThaumcraftGolem x-ray pass: colour 0.25 x4, blended, no depth test and no depth writes. */
    private static final com.mojang.blaze3d.pipeline.RenderPipeline XRAY_PIPELINE = xrayPipeline();
    private static com.mojang.blaze3d.pipeline.RenderPipeline xrayPipeline() {
        com.mojang.blaze3d.pipeline.RenderPipeline.Snippet snip;
        try {
            var f = net.minecraft.client.renderer.RenderPipelines.class.getDeclaredField("ENTITY_SNIPPET");
            f.setAccessible(true);
            snip = (com.mojang.blaze3d.pipeline.RenderPipeline.Snippet) f.get(null);
        } catch (ReflectiveOperationException ex) { throw new IllegalStateException("ENTITY_SNIPPET", ex); }
        return com.mojang.blaze3d.pipeline.RenderPipeline.builder(snip)
                .withLocation(Identifier.fromNamespaceAndPath("thaumcraft", "pipeline/golem_xray"))
                .withShaderDefine("ALPHA_CUTOUT", 0.003921569f).withShaderDefine("PER_FACE_LIGHTING")
                .withBindGroupLayout(net.minecraft.client.renderer.BindGroupLayouts.SAMPLER1)
                .withColorTargetState(new com.mojang.blaze3d.pipeline.ColorTargetState(com.mojang.blaze3d.pipeline.BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new com.mojang.blaze3d.pipeline.DepthStencilState(com.mojang.blaze3d.platform.CompareOp.ALWAYS_PASS, false))
                .withCull(false).build();
    }
    public static void registerPipelines(net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent e) { e.registerPipeline(XRAY_PIPELINE); }
    private static final Map<Identifier, net.minecraft.client.renderer.rendertype.RenderType> XRAY_TYPES = new java.util.concurrent.ConcurrentHashMap<>();
    private static net.minecraft.client.renderer.rendertype.RenderType xrayType(Identifier tex) {
        return XRAY_TYPES.computeIfAbsent(tex, t -> net.minecraft.client.renderer.rendertype.RenderType.create("thaumcraft_golem_xray",
                net.minecraft.client.renderer.rendertype.RenderSetup.builder(XRAY_PIPELINE).withTexture("Sampler0", t).useLightmap().useOverlay().sortOnUpload().createRenderSetup()));
    }
    private static final int XRAY_COLOR = ARGB.colorFromFloat(0.25f, 0.25f, 0.25f, 0.25f);
    /** True while the x-ray copy is being submitted. */
    private boolean xrayPass;
    private final ItemModelResolver resolver;
    /** PartModelBreakers.ani: per golem grinder speed and angle. */
    private final Map<Integer, float[]> breakerAni = new HashMap<>();

    public GolemRenderer(EntityRendererProvider.Context ctx) { super(ctx); shadowRadius = 0.3f; resolver = ctx.getItemModelResolver(); }
    @Override public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(GolemEntity e, State s, float pt) {
        super.extractRenderState(e, s, pt);
        s.pt = pt;
        s.props = e.getProperties().toLong();
        s.limbSwingAmount = Math.min(1.0f, e.walkAnimation.speed(pt));
        s.limbSwing = e.walkAnimation.position(pt);
        s.ageInTicks = e.tickCount + pt;
        s.bodyRot = Mth.rotLerp(pt, e.yBodyRotO, e.yBodyRot);
        s.netHeadYaw = Mth.rotLerp(pt, e.yHeadRotO, e.yHeadRot) - s.bodyRot;
        s.headPitch = Mth.lerp(pt, e.xRotO, e.getXRot());
        s.prevPitch = e.xRotO;
        s.swing = e.getAttackAnim(pt);
        double dx = e.getX() - e.xo, dz = e.getZ() - e.zo;
        s.speed = (float) (dx * dx + dz * dz);
        s.yawDelta = e.getYRot() - e.yRotO;
        s.color = e.getGolemColor();
        s.id = e.getId();
        s.ticks = e.tickCount;
        s.inCombat = e.isInCombat();
        s.wheelRot = e.wheelRot;
        s.deathTime = e.deathTime;
        s.overlay = OverlayTexture.pack(OverlayTexture.u(0.0f), OverlayTexture.v(e.hurtTime > 0 || e.deathTime > 0));
        ItemStack mh = e.getItemBySlot(EquipmentSlot.MAINHAND), oh = e.getItemBySlot(EquipmentSlot.OFFHAND);
        s.holding = !mh.isEmpty();
        resolver.updateForLiving(s.main, mh, ItemDisplayContext.HEAD, e);
        resolver.updateForLiving(s.off, e.getProperties().hasTrait(GolemTrait.HAULER) ? oh : ItemStack.EMPTY, ItemDisplayContext.HEAD, e);
        s.mainBlock = mh.getItem() instanceof BlockItem;
        s.offBlock = oh.getItem() instanceof BlockItem;
        var player = net.minecraft.client.Minecraft.getInstance().player;
        s.xray = player != null && player.isShiftKeyDown()
                && (player.getMainHandItem().getItem() instanceof art.arcane.thaumcraft.golems.ISealDisplayer || player.getOffhandItem().getItem() instanceof art.arcane.thaumcraft.golems.ISealDisplayer)
                && !player.hasLineOfSight(e);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector c, CameraRenderState cam) {
        model(s, ps, c);
        if (s.xray) {
            xrayPass = true;
            try { model(s, ps, c); } finally { xrayPass = false; }
        }
        super.submit(s, ps, c, cam);
    }

    private void model(State s, PoseStack ps, SubmitNodeCollector c) {
        GolemProperties props = GolemProperties.fromLong(s.props);
        Identifier mat = props.getMaterial().texture;
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180.0f - s.bodyRot));
        if (s.deathTime > 0) {
            float f = (s.deathTime + s.pt - 1.0f) / 20.0f * 1.6f;
            f = Math.min(1.0f, Mth.sqrt(f));
            ps.mulPose(Axis.ZP.rotationDegrees(f * 90.0f));
        }
        EnumMap<PartModel.AttachPoint, List<PartModel>> parts = new EnumMap<>(PartModel.AttachPoint.class);
        for (PartModel.AttachPoint ap : PartModel.AttachPoint.values()) parts.put(ap, new ArrayList<>());
        for (GolemPart p : new GolemPart[]{props.getHead(), props.getArms(), props.getLegs(), props.getAddon()}) if (p.model != null) parts.get(p.model.attachPoint).add(p.model);

        boolean aflag = props.hasTrait(GolemTrait.WHEELED) || props.hasTrait(GolemTrait.FLYER);
        float f1, bry = 0.0f;
        float rx = (float) Math.toDegrees(Mth.sin(s.ageInTicks * 0.067f) * 0.03f);
        float rz = (float) Math.toDegrees(Mth.cos(s.ageInTicks * 0.09f) * 0.05f + 0.05f);
        float rrx = 0, rry = 0, rrz = 0, rlx = 0, rly = 0, rlz = 0;
        if (s.holding) { rrx = 90.0f - rz / 2.0f; rrz = -2.0f; rlx = 90.0f - rz / 2.0f; rlz = 2.0f; }
        else {
            if (aflag) { rrx = rx * 2.0f; rlx = -rx * 2.0f; }
            else {
                f1 = Mth.cos(s.limbSwing * 0.6662f + (float) Math.PI) * 2.0f * s.limbSwingAmount * 0.5f;
                rrx = (float) (Math.toDegrees(f1) + rx);
                f1 = Mth.cos(s.limbSwing * 0.6662f) * 2.0f * s.limbSwingAmount * 0.5f;
                rlx = (float) (Math.toDegrees(f1) - rx);
            }
            rrz += rz + 2.0f;
            rlz -= rz + 2.0f;
        }
        if (s.swing > 0.0f) {
            float wiggle = -Mth.sin(Mth.sqrt(s.swing) * (float) Math.PI * 2.0f) * 0.2f;
            bry = (float) Math.toDegrees(wiggle);
            rrz = -(float) Math.toDegrees(Mth.sin(wiggle) * 3.0f);
            rrx = (float) Math.toDegrees(-Mth.cos(wiggle) * 5.0f);
            rry += bry;
        }
        int light = s.lightCoords, ov = s.overlay;
        ps.mulPose(Axis.YP.rotationDegrees(bry));
        float lean = aflag ? 75.0f : 25.0f;
        ps.mulPose(Axis.XN.rotationDegrees(s.speed * lean));
        ps.mulPose(Axis.ZN.rotationDegrees(s.speed * lean * 0.06f * s.yawDelta));
        ObjModel base = ObjModel.get(BASE);

        ps.pushPose();
        ps.translate(0.0, 0.5, 0.0);
        draw(c, ps, mat, base, "chest", -1, light, ov);
        draw(c, ps, mat, base, "waist", -1, light, ov);
        if (s.color > 0) draw(c, ps, mat, base, "flag", 0xFF000000 | DyeColor.byId(s.color - 1).getTextureDiffuseColor(), light, ov);
        for (PartModel p : parts.get(PartModel.AttachPoint.BODY)) renderPart(c, ps, s, p, mat, PartModel.LimbSide.MIDDLE, light, ov);
        ps.popPose();

        ps.pushPose();
        ps.translate(0.0, 0.75, -0.03125);
        ps.mulPose(Axis.YN.rotationDegrees(s.netHeadYaw));
        ps.mulPose(Axis.XN.rotationDegrees(s.headPitch));
        for (PartModel p : parts.get(PartModel.AttachPoint.HEAD)) renderPart(c, ps, s, p, mat, PartModel.LimbSide.MIDDLE, light, ov);
        draw(c, ps, mat, base, "head", -1, light, ov);
        ps.popPose();

        List<PartModel> arms = parts.get(PartModel.AttachPoint.ARMS);
        ps.pushPose();
        ps.translate(0.20625, 0.6875, 0.0);
        if (!arms.isEmpty() && arms.get(0).special == PartModel.Special.DARTS && s.inCombat) { rrx = 90.0f - s.prevPitch + rrx / 10.0f; rry /= 10.0f; rrz /= 10.0f; }
        ps.mulPose(Axis.XP.rotationDegrees(rrx));
        ps.mulPose(Axis.YP.rotationDegrees(rry));
        ps.mulPose(Axis.ZP.rotationDegrees(rrz));
        draw(c, ps, mat, base, "arm", -1, light, ov);
        for (PartModel p : arms) renderPart(c, ps, s, p, mat, PartModel.LimbSide.RIGHT, light, ov);
        ps.popPose();

        ps.pushPose();
        ps.translate(-0.20625, 0.6875, 0.0);
        if (!arms.isEmpty() && arms.get(0).special == PartModel.Special.DARTS && s.inCombat) { rlx = 90.0f - s.prevPitch + rlx / 10.0f; rly /= 10.0f; rlz /= 10.0f; }
        ps.mulPose(Axis.XP.rotationDegrees(rlx));
        ps.mulPose(Axis.YP.rotationDegrees(rly + 180.0f));
        ps.mulPose(Axis.ZN.rotationDegrees(rlz));
        draw(c, ps, mat, base, "arm", -1, light, ov);
        for (PartModel p : arms) renderPart(c, ps, s, p, mat, PartModel.LimbSide.LEFT, light, ov);
        ps.popPose();

        ps.pushPose();
        ps.translate(0.09375, 0.375, 0.0);
        f1 = Mth.cos(s.limbSwing * 0.6662f) * s.limbSwingAmount;
        ps.mulPose(Axis.XP.rotationDegrees((float) Math.toDegrees(f1)));
        for (PartModel p : parts.get(PartModel.AttachPoint.LEGS)) renderPart(c, ps, s, p, mat, PartModel.LimbSide.RIGHT, light, ov);
        ps.popPose();
        ps.pushPose();
        ps.translate(-0.09375, 0.375, 0.0);
        f1 = Mth.cos(s.limbSwing * 0.6662f + (float) Math.PI) * s.limbSwingAmount;
        ps.mulPose(Axis.XP.rotationDegrees((float) Math.toDegrees(f1)));
        for (PartModel p : parts.get(PartModel.AttachPoint.LEGS)) renderPart(c, ps, s, p, mat, PartModel.LimbSide.LEFT, light, ov);
        ps.popPose();

        if (!s.main.isEmpty() && !xrayPass) {
            ps.pushPose();
            ps.translate(0.0, 0.625, 0.0);
            ps.mulPose(Axis.XP.rotationDegrees(90.0f - rz * 0.5f));
            ps.mulPose(Axis.XN.rotationDegrees(90.0f));
            ps.scale(0.375f, 0.375f, 0.375f);
            ps.translate(0.0f, 0.25f, -1.5f);
            if (!s.mainBlock) ps.translate(0.0f, -0.6f, 0.0f);
            s.main.submit(ps, c, light, OverlayTexture.NO_OVERLAY, s.outlineColor);
            ps.popPose();
        }
        ps.popPose();
    }

    private void draw(SubmitNodeCollector c, PoseStack ps, Identifier tex, ObjModel m, String part, int argb, int light, int ov) {
        if (!m.has(part)) return;
        if (xrayPass) c.submitCustomGeometry(ps, xrayType(tex), (pose, vc) -> m.renderPart(part, pose, vc, XRAY_COLOR, light, ov));
        else c.submitCustomGeometry(ps, RenderTypes.entityTranslucent(tex), (pose, vc) -> m.renderPart(part, pose, vc, argb, light, ov));
    }

    /** RenderThaumcraftGolem.renderPart with the PartModel subclass hooks. */
    private void renderPart(SubmitNodeCollector c, PoseStack ps, State s, PartModel part, Identifier mat, PartModel.LimbSide side, int light, int ov) {
        ObjModel model = ObjModel.get(part.objModel);
        for (String op : model.partNames()) {
            ps.pushPose();
            Identifier tex = PartModel.useMaterialTextureForObjectPart(op) || part.texture == null ? mat : part.texture;
            switch (part.special) {
                case CLAWS -> {
                    if (op.startsWith("claw")) {
                        float f = s.swing * 4.1f;
                        f *= f;
                        ps.translate(0.0, -0.2, 0.0);
                        ps.mulPose((op.endsWith("1") ? Axis.XP : Axis.XN).rotationDegrees(f));
                    }
                }
                case BREAKERS -> {
                    if (op.equals("grinder")) {
                        float[] a = breakerAni.getOrDefault(s.id, new float[2]);
                        float f = Math.max(a[0], s.swing * 20.0f);
                        float rot = a[1] + f;
                        breakerAni.put(s.id, new float[]{f * 0.99f, rot});
                        ps.translate(0.0, -0.34, 0.0);
                        ps.mulPose((side == PartModel.LimbSide.LEFT ? Axis.XN : Axis.XP).rotationDegrees((s.ticks + s.pt) / 2.0f + rot + (side == PartModel.LimbSide.LEFT ? 22 : 0)));
                    }
                }
                case WHEEL -> {
                    if (op.equals("wheel")) { ps.translate(0.0, -0.375, 0.0); ps.mulPose(Axis.XN.rotationDegrees(s.wheelRot)); }
                }
                default -> {}
            }
            draw(c, ps, tex, model, op, -1, light, ov);
            if (part.special == PartModel.Special.HAULER && !s.off.isEmpty() && !xrayPass) {
                ps.pushPose();
                ps.scale(0.375f, 0.375f, 0.375f);
                ps.translate(0.0f, 0.33f, 0.825f);
                if (!s.offBlock) ps.translate(0.0f, 0.0f, -0.25f);
                s.off.submit(ps, c, light, OverlayTexture.NO_OVERLAY, s.outlineColor);
                ps.popPose();
            }
            ps.popPose();
        }
    }
}
