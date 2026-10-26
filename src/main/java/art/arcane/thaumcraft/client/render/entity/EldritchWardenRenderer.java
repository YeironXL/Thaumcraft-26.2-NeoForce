package art.arcane.thaumcraft.client.render.entity;

import art.arcane.thaumcraft.entity.monster.boss.EldritchWardenEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * TC6 RenderEldritchGuardian for the warden: eldritch_warden.png, fully opaque alpha blend, sunk into the ground by
 * height x spawnTimer/150 while it rises, plus ModelEldritchGuardian's warden-only HoodEye (x1.01, additive, bright).
 */
public final class EldritchWardenRenderer extends MobRenderer<EldritchWardenEntity, EldritchWardenRenderer.State, EldritchGuardianModel> {
    public static final class State extends EldritchGuardianModel.State { public float sink; }
    private static final Identifier TEX = Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/eldritch_warden.png");
    public static final ModelLayerLocation EYE = new ModelLayerLocation(Identifier.fromNamespaceAndPath("thaumcraft", "eldritch_warden"), "eye");

    /** ModelEldritchGuardian.HoodEye: an 8x8x8 box at texture 0,0 (mirrored) pivoting with the hood. */
    public static LayerDefinition createEye() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("hoodeye", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-4.0f, -8.0f, -4.0f, 8, 8, 8), PartPose.offset(0.0f, -6.0f, 0.0f));
        return LayerDefinition.create(mesh, 128, 64);
    }

    static final class EyeModel extends EntityModel<State> {
        private final ModelPart eye;
        EyeModel(ModelPart root) { super(root, RenderTypes::eyes); eye = root.getChild("hoodeye"); }
        @Override public void setupAnim(State s) { super.setupAnim(s); eye.yRot = s.yRot / 57.295776f; eye.xRot = s.xRot / 57.295776f; }
    }

    public EldritchWardenRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new EldritchGuardianModel(ctx.bakeLayer(EldritchGuardianModel.LAYER)), 0.6f);
        EyeModel eyeModel = new EyeModel(ctx.bakeLayer(EYE));
        addLayer(new RenderLayer<>(this) {
            @Override
            public void submit(PoseStack ps, SubmitNodeCollector c, int light, State s, float yRot, float xRot) {
                if (s.isInvisible) return;
                ps.pushPose();
                ps.scale(1.01f, 1.01f, 1.01f);
                c.order(1).submitModel(eyeModel, s, ps, RenderTypes.eyes(TEX), light, OverlayTexture.NO_OVERLAY, -1, null, s.outlineColor, null);
                ps.popPose();
            }
        });
    }

    @Override public Identifier getTextureLocation(State s) { return TEX; }
    @Override public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(EldritchWardenEntity e, State s, float pt) {
        super.extractRenderState(e, s, pt);
        s.armLiftL = e.armLiftL;
        s.armLiftR = e.armLiftR;
        s.sink = e.getBbHeight() * (e.getSpawnTimer() / 150.0f);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector c, CameraRenderState cam) {
        ps.pushPose();
        ps.translate(0.0f, -s.sink, 0.0f);
        super.submit(s, ps, c, cam);
        ps.popPose();
    }

    @Override protected RenderType getRenderType(State s, boolean visible, boolean transparent, boolean glowing) { return RenderTypes.entityTranslucent(TEX); }
}
