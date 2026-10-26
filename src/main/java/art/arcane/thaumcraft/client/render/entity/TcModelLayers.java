package art.arcane.thaumcraft.client.render.entity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/** Ports of the TC6 ModelBase classes as layer definitions. */
public final class TcModelLayers {
    public static final ModelLayerLocation GRAPPLER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("thaumcraft", "grappler"), "main");
    public static final ModelLayerLocation CUBE = new ModelLayerLocation(Identifier.fromNamespaceAndPath("thaumcraft", "cube"), "main");
    public static final ModelLayerLocation CUBE_OVER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("thaumcraft", "cube"), "over");
    /** ModelCube(shift): one 16px cube centred on the origin, 64x64 texture. */
    public static LayerDefinition cube(int shift) {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("cube", CubeListBuilder.create().texOffs(0, shift).mirror().addBox(-8, -8, -8, 16, 16, 16), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
    public static final ModelLayerLocation JAR_BRACE = new ModelLayerLocation(Identifier.fromNamespaceAndPath("thaumcraft", "jar"), "brace");
    public static final ModelLayerLocation JAR_LID_EXTENSION = new ModelLayerLocation(Identifier.fromNamespaceAndPath("thaumcraft", "jar"), "lid_extension");
    /** ModelJar.Lid (the brace) and ModelJar.LidExtension, 64x32. */
    public static final ModelLayerLocation BRAIN = new ModelLayerLocation(Identifier.fromNamespaceAndPath("thaumcraft", "brain"), "main");
    public static final ModelLayerLocation BANNER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("thaumcraft", "banner"), "main");
    /** ModelBanner: pole, beam, two dyed tabs (B1/B2) and the cloth. */
    public static LayerDefinition banner() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("tabs", CubeListBuilder.create().texOffs(0, 29).mirror().addBox(-5, -7.5f, -1.5f, 2, 3, 3).addBox(3, -7.5f, -1.5f, 2, 3, 3), PartPose.ZERO);
        root.addOrReplaceChild("beam", CubeListBuilder.create().texOffs(30, 0).mirror().addBox(-7, -7, -1, 14, 2, 2), PartPose.ZERO);
        root.addOrReplaceChild("banner", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-7, 0, -0.5f, 14, 28, 1), PartPose.offset(0, -5, 0));
        root.addOrReplaceChild("pole", CubeListBuilder.create().texOffs(62, 0).mirror().addBox(0, 0, -1, 2, 31, 2), PartPose.offset(-1, -7, -2));
        return LayerDefinition.create(mesh, 128, 64);
    }
    public static final ModelLayerLocation JAR_BRINE = new ModelLayerLocation(Identifier.fromNamespaceAndPath("thaumcraft", "jar"), "brine");
    /** ModelBrain. */
    public static LayerDefinition brain() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("s1", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0, 0, 0, 12, 10, 16), PartPose.offset(-6, 8, -8));
        root.addOrReplaceChild("s2", CubeListBuilder.create().texOffs(64, 0).mirror().addBox(0, 0, 0, 8, 3, 7), PartPose.offset(-4, 18, 0));
        root.addOrReplaceChild("s3", CubeListBuilder.create().texOffs(0, 32).mirror().addBox(0, 0, 0, 2, 6, 2), PartPose.offsetAndRotation(-1, 18, -2, 0.4089647f, 0, 0));
        return LayerDefinition.create(mesh, 128, 64);
    }
    /** ModelJar.Brine. */
    public static LayerDefinition jarBrine() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("brine", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-4, -11, -4, 8, 10, 8), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }
    public static LayerDefinition jarBrace() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("lid", CubeListBuilder.create().texOffs(32, 24).mirror().addBox(-3, 0, -3, 6, 2, 6), PartPose.offset(0, -14, 0));
        return LayerDefinition.create(mesh, 64, 32);
    }
    public static LayerDefinition jarLidExtension() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("ext", CubeListBuilder.create().texOffs(0, 23).mirror().addBox(-2, -16, -2, 4, 2, 4), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }
    public static final ModelLayerLocation FIRE_BAT = new ModelLayerLocation(Identifier.fromNamespaceAndPath("thaumcraft", "fire_bat"), "main");

    /** ModelGrappler: core cube and three prongs along x, y and z. */
    public static LayerDefinition grappler() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5f, -1.5f, -1.5f, 3, 3, 3), PartPose.ZERO);
        root.addOrReplaceChild("prong1", CubeListBuilder.create().texOffs(0, 10).addBox(-0.5f, -0.5f, -2.5f, 1, 1, 5), PartPose.ZERO);
        root.addOrReplaceChild("prong2", CubeListBuilder.create().texOffs(0, 10).addBox(-0.5f, -0.5f, -2.5f, 1, 1, 5), PartPose.rotation(0.0f, 1.5707964f, 0.0f));
        root.addOrReplaceChild("prong3", CubeListBuilder.create().texOffs(0, 10).addBox(-0.5f, -0.5f, -2.5f, 1, 1, 5), PartPose.rotation(1.5707964f, 1.5707964f, 0.0f));
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** ModelFireBat (the 1.7 bat model). */
    public static LayerDefinition fireBat() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -3, -3, 6, 6, 6), PartPose.ZERO);
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(24, 0).addBox(-4, -6, -2, 3, 4, 1), PartPose.ZERO);
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(24, 0).mirror().addBox(1, -6, -2, 3, 4, 1), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-3, 4, -3, 6, 12, 6).texOffs(0, 34).addBox(-5, 16, 0, 10, 6, 1), PartPose.ZERO);
        PartDefinition rw = body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(42, 0).addBox(-12, 1, 1.5f, 10, 16, 1), PartPose.ZERO);
        rw.addOrReplaceChild("right_wing_tip", CubeListBuilder.create().texOffs(24, 16).addBox(-8, 1, 0, 8, 12, 1), PartPose.offset(-12, 1, 1.5f));
        PartDefinition lw = body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(42, 0).mirror().addBox(2, 1, 1.5f, 10, 16, 1), PartPose.ZERO);
        lw.addOrReplaceChild("left_wing_tip", CubeListBuilder.create().texOffs(24, 16).mirror().addBox(0, 1, 0, 8, 12, 1), PartPose.offset(12, 1, 1.5f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static void register(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(GRAPPLER, TcModelLayers::grappler);
        e.registerLayerDefinition(TurretModels.CROSSBOW, TurretModels::crossbow);
        e.registerLayerDefinition(TurretModels.BORE, TurretModels::bore);
        e.registerLayerDefinition(FIRE_BAT, TcModelLayers::fireBat);
        e.registerLayerDefinition(art.arcane.thaumcraft.golems.client.TravellingTrunkModel.LAYER, art.arcane.thaumcraft.golems.client.TravellingTrunkModel::create);
        e.registerLayerDefinition(TaintSeedModel.LAYER, TaintSeedModel::create);
        e.registerLayerDefinition(TaintacleModel.LAYER, () -> TaintacleModel.create(10));
        e.registerLayerDefinition(TaintacleModel.LAYER_SMALL, () -> TaintacleModel.create(6));
        e.registerLayerDefinition(EldritchCrabModel.LAYER, EldritchCrabModel::create);
        e.registerLayerDefinition(PechModel.LAYER, PechModel::create);
        e.registerLayerDefinition(CUBE, () -> cube(0));
        e.registerLayerDefinition(JAR_BRACE, TcModelLayers::jarBrace);
        e.registerLayerDefinition(BRAIN, TcModelLayers::brain);
        e.registerLayerDefinition(BANNER, TcModelLayers::banner);
        e.registerLayerDefinition(JAR_BRINE, TcModelLayers::jarBrine);
        e.registerLayerDefinition(JAR_LID_EXTENSION, TcModelLayers::jarLidExtension);
        e.registerLayerDefinition(CUBE_OVER, () -> cube(32));
        e.registerLayerDefinition(EldritchGuardianModel.LAYER, EldritchGuardianModel::create);
        e.registerLayerDefinition(EldritchWardenRenderer.EYE, EldritchWardenRenderer::createEye);
        e.registerLayerDefinition(EldritchGolemModel.LAYER, EldritchGolemModel::create);
        e.registerLayerDefinition(EldritchGolemModel.CLOTH, EldritchGolemModel::createCloth);
        e.registerLayerDefinition(TaintacleModel.LAYER_GIANT, () -> TaintacleModel.create(14));
        e.registerLayerDefinition(CultistRenderer.LAYER, CultistRenderer::create);
    }
    private TcModelLayers() {}
}
