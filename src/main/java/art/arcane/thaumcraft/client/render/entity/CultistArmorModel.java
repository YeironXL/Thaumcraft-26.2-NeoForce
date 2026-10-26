package art.arcane.thaumcraft.client.render.entity;

import art.arcane.thaumcraft.item.CultistArmor;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.equipment.ArmorType;

/**
 * TC6 ModelRobe, ModelKnightArmor and ModelLeaderArmor (ModelCustomArmor subclasses, 128x64 texture space). Each comes
 * in an outer (f = 1.0, used by chest and boots items) and inner (f = 0.5, helm and legs items) variant; the biped cubes are
 * cleared and the armour pieces are children of the biped parts. Part visibility follows CustomArmorHelper: head for
 * the head slot, body for chest and legs, arms for chest, legs for legs. Cloths swing with the walk cycle as in render().
 */
public final class CultistArmorModel extends HumanoidModel<HumanoidRenderState> {
    public static final ModelLayerLocation ROBE_OUTER = layer("robe_outer"), ROBE_INNER = layer("robe_inner"),
            KNIGHT_OUTER = layer("knight_outer"), KNIGHT_INNER = layer("knight_inner"),
            LEADER_OUTER = layer("leader_outer"), LEADER_INNER = layer("leader_inner");
    private static ModelLayerLocation layer(String n) { return new ModelLayerLocation(Identifier.fromNamespaceAndPath("thaumcraft", "cultist_armor"), n); }

    private final CultistArmor.Kind kind;
    private final EquipmentSlot slot;
    private final ModelPart[] parts;

    public CultistArmorModel(ModelPart root, CultistArmor.Kind kind, EquipmentSlot slot) {
        super(root);
        this.kind = kind;
        this.slot = slot;
        String[] names = switch (kind) {
            case ROBE -> new String[] {"frontclothr1", "frontclothl1", "frontclothr2", "frontclothl2", "clothbackr1", "clothbackl1", "clothbackr2", "clothbackl2", "clothbackr3", "clothbackl3"};
            case PLATE -> new String[] {"frontcloth1", "frontcloth2", "cloak1", "cloak2", "cloak3"};
            default -> new String[] {"legclothr", "legclothl", "cloak1", "cloak2", "cloak3"};
        };
        parts = new ModelPart[names.length];
        for (int i = 0; i < names.length; i++) parts[i] = body.hasChild(names[i]) ? body.getChild(names[i]) : null;
    }

    /** CustomArmorHelper: model1 (outer) for chest and boots items, model2 (inner) for helm and legs. */
    public static ModelLayerLocation layerFor(CultistArmor.Kind kind, ArmorType type) {
        boolean outer = type == ArmorType.CHESTPLATE || type == ArmorType.BOOTS;
        return switch (kind) {
            case ROBE -> outer ? ROBE_OUTER : ROBE_INNER;
            case LEADER -> outer ? LEADER_OUTER : LEADER_INNER;
            default -> outer ? KNIGHT_OUTER : KNIGHT_INNER;
        };
    }

    @Override
    public void setupAnim(HumanoidRenderState s) {
        super.setupAnim(s);
        head.visible = hat.visible = slot == EquipmentSlot.HEAD;
        body.visible = slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS;
        rightArm.visible = leftArm.visible = slot == EquipmentSlot.CHEST;
        rightLeg.visible = leftLeg.visible = slot == EquipmentSlot.LEGS;
        if (!s.isBaby) { head.xScale *= 1.01f; head.yScale *= 1.01f; head.zScale *= 1.01f; }
        float f = s.walkAnimationPos, f1 = s.walkAnimationSpeed;
        float a = Mth.cos(f * 0.6662f) * 1.4f * f1, b = Mth.cos(f * 0.6662f + (float) Math.PI) * 1.4f * f1, c = Math.min(a, b);
        switch (kind) {
            case ROBE -> {
                rot(0, c - 0.1047198f); rot(1, c - 0.1047198f);
                rot(2, c - 0.3316126f); rot(3, c - 0.3316126f);
                rot(4, -c + 0.1047198f); rot(5, -c + 0.1047198f);
                for (int i = 6; i < 10; i++) rot(i, -c + 0.2268928f);
            }
            case PLATE -> {
                rot(0, c - 0.1047198f); rot(1, c - 0.3316126f);
                rot(2, -c / 2.0f + 0.1396263f); rot(3, -c / 2.0f + 0.3069452f); rot(4, -c / 2.0f + 0.4465716f);
            }
            default -> {
                rot(0, a - 0.1047198f); rot(1, b - 0.1047198f);
                rot(2, -c / 2.0f + 0.1396263f); rot(3, -c / 2.0f + 0.3069452f); rot(4, -c / 2.0f + 0.4465716f);
            }
        }
    }
    private void rot(int i, float x) { if (parts[i] != null) parts[i].xRot = x; }

    private interface Builder { void build(PartDefinition head, PartDefinition body, PartDefinition rightArm, PartDefinition leftArm, PartDefinition rightLeg, PartDefinition leftLeg, boolean outer); }

    private static LayerDefinition create(Builder b, boolean outer) {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition ra = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0f, 2.0f, 0.0f));
        PartDefinition la = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0f, 2.0f, 0.0f));
        PartDefinition rl = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9f, 12.0f, 0.0f));
        PartDefinition ll = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9f, 12.0f, 0.0f));
        b.build(head, body, ra, la, rl, ll, outer);
        return LayerDefinition.create(mesh, 128, 64);
    }

    public static void register(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(ROBE_OUTER, () -> create(CultistArmorModel::robe, true));
        e.registerLayerDefinition(ROBE_INNER, () -> create(CultistArmorModel::robe, false));
        e.registerLayerDefinition(KNIGHT_OUTER, () -> create(CultistArmorModel::knight, true));
        e.registerLayerDefinition(KNIGHT_INNER, () -> create(CultistArmorModel::knight, false));
        e.registerLayerDefinition(LEADER_OUTER, () -> create(CultistArmorModel::leader, true));
        e.registerLayerDefinition(LEADER_INNER, () -> create(CultistArmorModel::leader, false));
    }

    // Generated 1:1 from the TC6 model constructors.
    static void robe(PartDefinition head, PartDefinition body, PartDefinition rightArm, PartDefinition leftArm, PartDefinition rightLeg, PartDefinition leftLeg, boolean outer) {
        head.addOrReplaceChild("hood1", CubeListBuilder.create().texOffs(16, 7).mirror(false).addBox(-4.5F, -9.0F, -4.6F, 9.0F, 9.0F, 9.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("hood2", CubeListBuilder.create().texOffs(52, 13).mirror(false).addBox(-4.0F, -9.7F, 2.0F, 8.0F, 9.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.2268928F, 0.0F, 0.0F));
        head.addOrReplaceChild("hood3", CubeListBuilder.create().texOffs(52, 14).mirror(false).addBox(-3.5F, -10.0F, 3.5F, 7.0F, 8.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3490659F, 0.0F, 0.0F));
        head.addOrReplaceChild("hood4", CubeListBuilder.create().texOffs(53, 15).mirror(false).addBox(-3.0F, -10.7F, 3.5F, 6.0F, 7.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5759587F, 0.0F, 0.0F));
        body.addOrReplaceChild("mbelt", CubeListBuilder.create().texOffs(16, 55).mirror(false).addBox(-4.0F, 7.0F, -3.0F, 8.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("mbeltb", CubeListBuilder.create().texOffs(16, 55).mirror(false).addBox(-4.0F, 7.0F, -4.0F, 8.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 3.141593F, 0.0F));
        body.addOrReplaceChild("mbeltl", CubeListBuilder.create().texOffs(16, 36).mirror(false).addBox(4.0F, 8.0F, -3.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("mbeltr", CubeListBuilder.create().texOffs(16, 36).mirror(false).addBox(-5.0F, 8.0F, -3.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        if (!outer) leftLeg.addOrReplaceChild("focipouch", CubeListBuilder.create().texOffs(100, 20).mirror(false).addBox(3.5F, 0.5F, -2.5F, 3.0F, 6.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.122173F));
        if (!outer) body.addOrReplaceChild("frontclothr1", CubeListBuilder.create().texOffs(108, 38).mirror(false).addBox(0.0F, 0.0F, 0.0F, 3.0F, 8.0F, 1.0F), PartPose.offsetAndRotation(-3.0F, 11.0F, -2.9F, -0.1047198F, 0.0F, 0.0F));
        if (!outer) body.addOrReplaceChild("frontclothr2", CubeListBuilder.create().texOffs(108, 47).mirror(false).addBox(0.0F, 7.5F, 1.7F, 3.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(-3.0F, 11.0F, -2.9F, -0.3316126F, 0.0F, 0.0F));
        if (!outer) body.addOrReplaceChild("frontclothl1", CubeListBuilder.create().texOffs(108, 38).mirror().addBox(0.0F, 0.0F, 0.0F, 3.0F, 8.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 11.0F, -2.9F, -0.1047198F, 0.0F, 0.0F));
        if (!outer) body.addOrReplaceChild("frontclothl2", CubeListBuilder.create().texOffs(108, 47).mirror().addBox(0.0F, 7.5F, 1.7F, 3.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 11.0F, -2.9F, -0.3316126F, 0.0F, 0.0F));
        if (!outer) body.addOrReplaceChild("clothbackr1", CubeListBuilder.create().texOffs(118, 16).mirror().addBox(0.0F, 0.0F, 0.0F, 4.0F, 8.0F, 1.0F), PartPose.offsetAndRotation(-4.0F, 11.5F, 2.9F, 0.1047198F, 0.0F, 0.0F));
        if (!outer) body.addOrReplaceChild("clothbackr2", CubeListBuilder.create().texOffs(123, 9).mirror(false).addBox(0.0F, 7.8F, -0.9F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(-4.0F, 11.5F, 2.9F, 0.2268928F, 0.0F, 0.0F));
        if (!outer) body.addOrReplaceChild("clothbackr3", CubeListBuilder.create().texOffs(120, 12).mirror().addBox(1.0F, 7.8F, -0.9F, 3.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(-4.0F, 11.5F, 2.9F, 0.2268928F, 0.0F, 0.0F));
        if (!outer) body.addOrReplaceChild("clothbackl1", CubeListBuilder.create().texOffs(118, 16).mirror(false).addBox(0.0F, 0.0F, 0.0F, 4.0F, 8.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 11.5F, 2.9F, 0.1047198F, 0.0F, 0.0F));
        if (!outer) body.addOrReplaceChild("clothbackl2", CubeListBuilder.create().texOffs(123, 9).mirror().addBox(3.0F, 7.8F, -0.9F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 11.5F, 2.9F, 0.2268928F, 0.0F, 0.0F));
        if (!outer) body.addOrReplaceChild("clothbackl3", CubeListBuilder.create().texOffs(120, 12).mirror(false).addBox(0.0F, 7.8F, -0.9F, 3.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 11.5F, 2.9F, 0.2268928F, 0.0F, 0.0F));
        if (!outer) body.addOrReplaceChild("chestplate", CubeListBuilder.create().texOffs(16, 25).mirror(false).addBox(-4.0F, 1.0F, -3.0F, 8.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        if (!outer) body.addOrReplaceChild("chestthing", CubeListBuilder.create().texOffs(56, 50).mirror(false).addBox(-2.5F, 1.0F, -4.0F, 5.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        if (!outer) body.addOrReplaceChild("scroll", CubeListBuilder.create().texOffs(78, 25).mirror(false).addBox(-2.0F, 9.5F, 4.0F, 8.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1919862F));
        if (!outer) body.addOrReplaceChild("backplate", CubeListBuilder.create().texOffs(36, 45).mirror(false).addBox(-4.0F, 1.0F, 1.9F, 8.0F, 11.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        if (!outer) body.addOrReplaceChild("book", CubeListBuilder.create().texOffs(81, 16).mirror(false).addBox(1.0F, 0.0F, 4.0F, 5.0F, 7.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7679449F));
        if (!outer) body.addOrReplaceChild("clothchestl", CubeListBuilder.create().texOffs(108, 38).mirror().addBox(2.1F, 0.5F, -3.5F, 2.0F, 8.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        if (!outer) body.addOrReplaceChild("clothchestr", CubeListBuilder.create().texOffs(108, 38).mirror(false).addBox(-4.1F, 0.5F, -3.5F, 2.0F, 8.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        rightArm.addOrReplaceChild("shoulderr", CubeListBuilder.create().texOffs(16, 45).mirror().addBox(-3.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        rightArm.addOrReplaceChild("rarm1", CubeListBuilder.create().texOffs(88, 39).mirror(false).addBox(-3.5F, 2.5F, -2.5F, 5.0F, 7.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        rightArm.addOrReplaceChild("rarm2", CubeListBuilder.create().texOffs(76, 32).mirror(false).addBox(-3.0F, 5.5F, 2.5F, 4.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        rightArm.addOrReplaceChild("rarm3", CubeListBuilder.create().texOffs(88, 32).mirror(false).addBox(-2.5F, 3.5F, 2.5F, 3.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        rightArm.addOrReplaceChild("shoulderplatetopr", CubeListBuilder.create().texOffs(56, 25).mirror(false).addBox(-5.5F, -2.5F, -3.5F, 2.0F, 1.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4363323F));
        rightArm.addOrReplaceChild("shoulderplater1", CubeListBuilder.create().texOffs(56, 33).mirror(false).addBox(-4.5F, -1.5F, -3.5F, 1.0F, 4.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4363323F));
        rightArm.addOrReplaceChild("shoulderplater2", CubeListBuilder.create().texOffs(40, 33).mirror(false).addBox(-3.5F, 1.5F, -3.5F, 1.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4363323F));
        rightArm.addOrReplaceChild("shoulderplater3", CubeListBuilder.create().texOffs(40, 33).mirror(false).addBox(-2.5F, 3.5F, -3.5F, 1.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4363323F));
        leftArm.addOrReplaceChild("shoulderl", CubeListBuilder.create().texOffs(16, 45).mirror(false).addBox(-1.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        leftArm.addOrReplaceChild("larm1", CubeListBuilder.create().texOffs(88, 39).mirror().addBox(-1.5F, 2.5F, -2.5F, 5.0F, 7.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        leftArm.addOrReplaceChild("larm2", CubeListBuilder.create().texOffs(76, 32).mirror(false).addBox(-1.0F, 5.5F, 2.5F, 4.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        leftArm.addOrReplaceChild("larm3", CubeListBuilder.create().texOffs(88, 32).mirror(false).addBox(-0.5F, 3.5F, 2.5F, 3.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        leftArm.addOrReplaceChild("shoulderplatetopl", CubeListBuilder.create().texOffs(56, 25).mirror(false).addBox(3.5F, -2.5F, -3.5F, 2.0F, 1.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4363323F));
        leftArm.addOrReplaceChild("shoulderplatel1", CubeListBuilder.create().texOffs(56, 33).mirror(false).addBox(3.5F, -1.5F, -3.5F, 1.0F, 4.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4363323F));
        leftArm.addOrReplaceChild("shoulderplatel2", CubeListBuilder.create().texOffs(40, 33).mirror(false).addBox(2.5F, 1.5F, -3.5F, 1.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4363323F));
        leftArm.addOrReplaceChild("shoulderplatel3", CubeListBuilder.create().texOffs(40, 33).mirror(false).addBox(1.5F, 3.5F, -3.5F, 1.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4363323F));
        rightLeg.addOrReplaceChild("legpanelr4", CubeListBuilder.create().texOffs(76, 38).mirror(false).addBox(-3.0F, 0.5F, -3.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
        rightLeg.addOrReplaceChild("legpanelr5", CubeListBuilder.create().texOffs(76, 42).mirror(false).addBox(-3.0F, 2.5F, -2.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
        rightLeg.addOrReplaceChild("legpanelr6", CubeListBuilder.create().texOffs(82, 38).mirror(false).addBox(-3.0F, 4.5F, -1.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
        rightLeg.addOrReplaceChild("sidepanelr1", CubeListBuilder.create().texOffs(116, 25).mirror(false).addBox(-2.5F, 0.5F, -2.5F, 1.0F, 4.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4363323F));
        rightLeg.addOrReplaceChild("sideclothr1", CubeListBuilder.create().texOffs(116, 42).mirror(false).addBox(-2.5F, 0.5F, -2.5F, 1.0F, 5.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.122173F));
        rightLeg.addOrReplaceChild("sideclothr2", CubeListBuilder.create().texOffs(116, 34).mirror(false).addBox(-1.5F, 5.5F, -2.5F, 1.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.296706F));
        rightLeg.addOrReplaceChild("sideclothr3", CubeListBuilder.create().texOffs(116, 1).mirror(false).addBox(0.4F, 8.4F, -2.5F, 1.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5235988F));
        leftLeg.addOrReplaceChild("legpanell4", CubeListBuilder.create().texOffs(76, 38).mirror().addBox(1.0F, 0.5F, -3.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
        leftLeg.addOrReplaceChild("legpanell5", CubeListBuilder.create().texOffs(76, 42).mirror().addBox(1.0F, 2.5F, -2.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
        leftLeg.addOrReplaceChild("legpanell6", CubeListBuilder.create().texOffs(82, 38).mirror().addBox(1.0F, 4.5F, -1.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
        leftLeg.addOrReplaceChild("sidepanell1", CubeListBuilder.create().texOffs(116, 25).mirror(false).addBox(1.5F, 0.5F, -2.5F, 1.0F, 4.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4363323F));
        leftLeg.addOrReplaceChild("sideclothl1", CubeListBuilder.create().texOffs(116, 42).mirror(false).addBox(1.5F, 0.5F, -2.5F, 1.0F, 5.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.122173F));
        leftLeg.addOrReplaceChild("sideclothl2", CubeListBuilder.create().texOffs(116, 34).mirror(false).addBox(0.5F, 5.5F, -2.5F, 1.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.296706F));
        leftLeg.addOrReplaceChild("sideclothl3", CubeListBuilder.create().texOffs(116, 1).mirror(false).addBox(-1.4F, 8.4F, -2.5F, 1.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5235988F));
    }
    static void knight(PartDefinition head, PartDefinition body, PartDefinition rightArm, PartDefinition leftArm, PartDefinition rightLeg, PartDefinition leftLeg, boolean outer) {
        head.addOrReplaceChild("helmet", CubeListBuilder.create().texOffs(41, 8).mirror(false).addBox(-4.5F, -9.0F, -4.5F, 9.0F, 9.0F, 9.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("mbelt", CubeListBuilder.create().texOffs(56, 55).mirror(false).addBox(-4.0F, 8.0F, -3.0F, 8.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("mbeltl", CubeListBuilder.create().texOffs(76, 44).mirror(false).addBox(4.0F, 8.0F, -3.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("mbeltr", CubeListBuilder.create().texOffs(76, 44).mirror(false).addBox(-5.0F, 8.0F, -3.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("chestplate", CubeListBuilder.create().texOffs(56, 45).mirror(false).addBox(-4.0F, 1.0F, -3.0F, 8.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("frontcloth1", CubeListBuilder.create().texOffs(120, 39).mirror(false).addBox(0.0F, 0.0F, 0.0F, 6.0F, 8.0F, 1.0F), PartPose.offsetAndRotation(-3.0F, 11.0F, -3.5F, -0.1047198F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("frontcloth2", CubeListBuilder.create().texOffs(100, 37).mirror(false).addBox(0.0F, 7.5F, 1.8F, 6.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(-3.0F, 11.0F, -3.5F, -0.3316126F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("tabbard", CubeListBuilder.create().texOffs(114, 52).mirror(false).addBox(-3.0F, 1.2F, -3.5F, 6.0F, 10.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("backplate", CubeListBuilder.create().texOffs(36, 45).mirror(false).addBox(-4.0F, 1.0F, 2.0F, 8.0F, 11.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("cloak1", CubeListBuilder.create().texOffs(0, 47).mirror(false).addBox(0.0F, 0.0F, 0.0F, 9.0F, 12.0F, 1.0F), PartPose.offsetAndRotation(-4.5F, 1.3F, 4.2F, 0.1396263F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("cloak2", CubeListBuilder.create().texOffs(0, 59).mirror(false).addBox(0.0F, 11.7F, -2.0F, 9.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(-4.5F, 1.3F, 4.2F, 0.3069452F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("cloak3", CubeListBuilder.create().texOffs(0, 59).mirror(false).addBox(0.0F, 15.2F, -4.2F, 9.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(-4.5F, 1.3F, 4.2F, 0.4465716F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("cloakatl", CubeListBuilder.create().texOffs(0, 43).mirror(false).addBox(2.5F, 1.0F, 2.0F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1396263F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("cloakatr", CubeListBuilder.create().texOffs(0, 43).mirror(false).addBox(-4.5F, 1.0F, 2.0F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1396263F, 0.0F, 0.0F));
        rightArm.addOrReplaceChild("shoulderr", CubeListBuilder.create().texOffs(56, 35).mirror(false).addBox(-3.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        rightArm.addOrReplaceChild("shoulderr0", CubeListBuilder.create().texOffs(0, 0).mirror(false).addBox(-4.3F, -1.5F, -3.0F, 3.0F, 5.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F));
        rightArm.addOrReplaceChild("shoulderr1", CubeListBuilder.create().texOffs(0, 19).mirror(false).addBox(-3.3F, 3.5F, -2.5F, 1.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F));
        rightArm.addOrReplaceChild("shoulderr2", CubeListBuilder.create().texOffs(0, 11).mirror(false).addBox(-2.3F, 3.5F, -3.0F, 1.0F, 2.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F));
        rightArm.addOrReplaceChild("gauntletr", CubeListBuilder.create().texOffs(100, 26).mirror(false).addBox(-3.5F, 3.5F, -2.5F, 2.0F, 6.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        rightArm.addOrReplaceChild("gauntletstrapr1", CubeListBuilder.create().texOffs(84, 31).mirror(false).addBox(-1.5F, 3.5F, -2.5F, 3.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        rightArm.addOrReplaceChild("gauntletstrapr2", CubeListBuilder.create().texOffs(84, 31).mirror(false).addBox(-1.5F, 6.5F, -2.5F, 3.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        leftArm.addOrReplaceChild("shoulderl", CubeListBuilder.create().texOffs(56, 35).mirror(false).addBox(-1.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        leftArm.addOrReplaceChild("shoulderl0", CubeListBuilder.create().texOffs(0, 0).mirror(false).addBox(1.3F, -1.5F, -3.0F, 3.0F, 5.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.7853982F));
        leftArm.addOrReplaceChild("shoulderl1", CubeListBuilder.create().texOffs(0, 19).mirror().addBox(2.3F, 3.5F, -2.5F, 1.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.7853982F));
        leftArm.addOrReplaceChild("shoulderl2", CubeListBuilder.create().texOffs(0, 11).mirror(false).addBox(1.3F, 3.5F, -3.0F, 1.0F, 2.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.7853982F));
        leftArm.addOrReplaceChild("gauntletl", CubeListBuilder.create().texOffs(114, 26).mirror(false).addBox(1.5F, 3.5F, -2.5F, 2.0F, 6.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        leftArm.addOrReplaceChild("gauntletstrapl1", CubeListBuilder.create().texOffs(84, 31).mirror().addBox(-1.5F, 3.5F, -2.5F, 3.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        leftArm.addOrReplaceChild("gauntletstrapl2", CubeListBuilder.create().texOffs(84, 31).mirror().addBox(-1.5F, 6.5F, -2.5F, 3.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        rightLeg.addOrReplaceChild("sidepanelr0", CubeListBuilder.create().texOffs(96, 14).mirror(false).addBox(-3.0F, -0.5F, -2.5F, 5.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1396263F));
        rightLeg.addOrReplaceChild("sidepanelr1", CubeListBuilder.create().texOffs(96, 7).mirror().addBox(0.0F, 2.5F, -2.5F, 2.0F, 2.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1396263F));
        rightLeg.addOrReplaceChild("sidepanelr2", CubeListBuilder.create().texOffs(114, 5).mirror().addBox(-2.0F, 2.5F, -2.5F, 2.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1396263F));
        rightLeg.addOrReplaceChild("sidepanelr3", CubeListBuilder.create().texOffs(116, 13).mirror(false).addBox(-3.0F, 2.5F, -2.5F, 1.0F, 4.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1396263F));
        leftLeg.addOrReplaceChild("sidepanell0", CubeListBuilder.create().texOffs(96, 14).mirror(false).addBox(-2.0F, -0.5F, -2.5F, 5.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1396263F));
        leftLeg.addOrReplaceChild("sidepanell1", CubeListBuilder.create().texOffs(96, 7).mirror(false).addBox(-2.0F, 2.5F, -2.5F, 2.0F, 2.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1396263F));
        leftLeg.addOrReplaceChild("sidepanell2", CubeListBuilder.create().texOffs(114, 5).mirror(false).addBox(0.0F, 2.5F, -2.5F, 2.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1396263F));
        leftLeg.addOrReplaceChild("sidepanell3", CubeListBuilder.create().texOffs(116, 13).mirror(false).addBox(2.0F, 2.5F, -2.5F, 1.0F, 4.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1396263F));
    }
    static void leader(PartDefinition head, PartDefinition body, PartDefinition rightArm, PartDefinition leftArm, PartDefinition rightLeg, PartDefinition leftLeg, boolean outer) {
        head.addOrReplaceChild("helmet", CubeListBuilder.create().texOffs(41, 8).mirror(false).addBox(-4.5F, -9.0F, -4.5F, 9.0F, 9.0F, 9.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("mbelt", CubeListBuilder.create().texOffs(56, 55).mirror(false).addBox(-4.0F, 8.0F, -3.0F, 8.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("mbeltl", CubeListBuilder.create().texOffs(76, 44).mirror(false).addBox(4.0F, 8.0F, -3.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("mbeltr", CubeListBuilder.create().texOffs(76, 44).mirror(false).addBox(-5.0F, 8.0F, -3.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("beltl", CubeListBuilder.create().texOffs(76, 44).mirror(false).addBox(4.0F, 4.0F, -3.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("beltr", CubeListBuilder.create().texOffs(76, 44).mirror(false).addBox(-5.0F, 4.0F, -3.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("chestplate", CubeListBuilder.create().texOffs(56, 45).mirror(false).addBox(-4.0F, 1.0F, -3.8F, 8.0F, 7.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("chestornament", CubeListBuilder.create().texOffs(76, 53).mirror(false).addBox(-2.5F, 3.0F, -4.8F, 5.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("chestclothr", CubeListBuilder.create().texOffs(20, 47).mirror(false).addBox(-4.5F, 1.2F, -4.5F, 3.0F, 9.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0663225F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("chestclothl", CubeListBuilder.create().texOffs(20, 47).mirror().addBox(1.5F, 1.2F, -4.5F, 3.0F, 9.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0663225F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("legclothr", CubeListBuilder.create().texOffs(20, 55).mirror(false).addBox(0.0F, 0.0F, 0.0F, 3.0F, 8.0F, 1.0F), PartPose.offsetAndRotation(-4.5F, 10.4F, -3.9F, -0.0349066F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("legclothl", CubeListBuilder.create().texOffs(20, 55).mirror().addBox(0.0F, 0.0F, 0.0F, 3.0F, 8.0F, 1.0F), PartPose.offsetAndRotation(1.5F, 10.4F, -3.9F, -0.0349066F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("backplate", CubeListBuilder.create().texOffs(36, 45).mirror(false).addBox(-4.0F, 1.0F, 2.0F, 8.0F, 11.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("collarb", CubeListBuilder.create().texOffs(17, 26).mirror(false).addBox(-4.5F, -1.5F, 7.0F, 9.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.2268928F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("collarr", CubeListBuilder.create().texOffs(17, 11).mirror(false).addBox(-5.5F, -1.5F, -3.0F, 1.0F, 4.0F, 11.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.2268928F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("collarl", CubeListBuilder.create().texOffs(17, 11).mirror(false).addBox(4.5F, -1.5F, -3.0F, 1.0F, 4.0F, 11.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.2268928F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("collarf", CubeListBuilder.create().texOffs(17, 31).mirror(false).addBox(-4.5F, -1.5F, -3.0F, 9.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.2268928F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("cloak1", CubeListBuilder.create().texOffs(0, 47).mirror(false).addBox(-4.5F, 2.0F, 1.0F, 9.0F, 12.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, 0.1396263F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("cloak2", CubeListBuilder.create().texOffs(0, 59).mirror(false).addBox(-4.5F, 14.0F, -1.3F, 9.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, 0.3069452F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("cloak3", CubeListBuilder.create().texOffs(0, 59).mirror(false).addBox(-4.5F, 17.0F, -3.7F, 9.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, 0.4465716F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("cloaktl", CubeListBuilder.create().texOffs(0, 43).mirror(false).addBox(2.5F, 1.0F, -1.0F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, 0.1396263F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("cloaktr", CubeListBuilder.create().texOffs(0, 43).mirror(false).addBox(-4.5F, 1.0F, -1.0F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, 0.1396263F, 0.0F, 0.0F));
        rightArm.addOrReplaceChild("shoulderr", CubeListBuilder.create().texOffs(56, 35).mirror(false).addBox(-3.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        rightArm.addOrReplaceChild("shoulderr1", CubeListBuilder.create().texOffs(0, 0).mirror(false).addBox(-4.3F, -1.5F, -3.0F, 3.0F, 5.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F));
        rightArm.addOrReplaceChild("shoulderr2", CubeListBuilder.create().texOffs(0, 19).mirror(false).addBox(-3.3F, 3.5F, -2.5F, 1.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F));
        rightArm.addOrReplaceChild("shoulderr3", CubeListBuilder.create().texOffs(0, 11).mirror(false).addBox(-2.3F, 3.5F, -3.0F, 1.0F, 2.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F));
        rightArm.addOrReplaceChild("shoulderr4", CubeListBuilder.create().texOffs(18, 4).mirror(false).addBox(-2.3F, -1.5F, -4.0F, 1.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F));
        rightArm.addOrReplaceChild("shoulderr5", CubeListBuilder.create().texOffs(18, 4).mirror(false).addBox(-2.3F, -1.5F, 3.0F, 1.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F));
        rightArm.addOrReplaceChild("gauntletr", CubeListBuilder.create().texOffs(100, 26).mirror(false).addBox(-3.5F, 3.5F, -2.5F, 2.0F, 6.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        rightArm.addOrReplaceChild("gauntletr2", CubeListBuilder.create().texOffs(102, 37).mirror(false).addBox(-5.0F, 3.5F, -2.0F, 1.0F, 5.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1675516F));
        rightArm.addOrReplaceChild("gauntletstrapr1", CubeListBuilder.create().texOffs(84, 31).mirror(false).addBox(-1.5F, 3.5F, -2.5F, 3.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        rightArm.addOrReplaceChild("gauntletstrapr2", CubeListBuilder.create().texOffs(84, 31).mirror(false).addBox(-1.5F, 6.5F, -2.5F, 3.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        leftArm.addOrReplaceChild("shoulderl", CubeListBuilder.create().texOffs(56, 35).mirror(false).addBox(-1.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        leftArm.addOrReplaceChild("shoulderl1", CubeListBuilder.create().texOffs(0, 0).mirror(false).addBox(1.3F, -1.5F, -3.0F, 3.0F, 5.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.7853982F));
        leftArm.addOrReplaceChild("shoulderl2", CubeListBuilder.create().texOffs(0, 19).mirror().addBox(2.3F, 3.5F, -2.5F, 1.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.7853982F));
        leftArm.addOrReplaceChild("shoulderl3", CubeListBuilder.create().texOffs(0, 11).mirror(false).addBox(1.3F, 3.5F, -3.0F, 1.0F, 2.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.7853982F));
        leftArm.addOrReplaceChild("shoulderl4", CubeListBuilder.create().texOffs(18, 4).mirror(false).addBox(1.3F, -1.5F, -4.0F, 1.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.7853982F));
        leftArm.addOrReplaceChild("shoulderl5", CubeListBuilder.create().texOffs(18, 4).mirror(false).addBox(1.3F, -1.5F, 3.0F, 1.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.7853982F));
        leftArm.addOrReplaceChild("gauntletl", CubeListBuilder.create().texOffs(114, 26).mirror(false).addBox(1.5F, 3.5F, -2.5F, 2.0F, 6.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        leftArm.addOrReplaceChild("gauntletl2", CubeListBuilder.create().texOffs(102, 37).mirror(false).addBox(4.0F, 3.5F, -2.0F, 1.0F, 5.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1675516F));
        leftArm.addOrReplaceChild("gauntletstrapl1", CubeListBuilder.create().texOffs(84, 31).mirror().addBox(-1.5F, 3.5F, -2.5F, 3.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        leftArm.addOrReplaceChild("gauntletstrapl2", CubeListBuilder.create().texOffs(84, 31).mirror().addBox(-1.5F, 6.5F, -2.5F, 3.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        rightLeg.addOrReplaceChild("backpanelr1", CubeListBuilder.create().texOffs(0, 25).mirror(false).addBox(-3.0F, -0.5F, 2.5F, 5.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0698132F, 0.0F, 0.0F));
        rightLeg.addOrReplaceChild("backpanelr2", CubeListBuilder.create().texOffs(96, 14).mirror(false).addBox(-3.0F, -0.5F, -2.5F, 5.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1396263F));
        rightLeg.addOrReplaceChild("backpanelr3", CubeListBuilder.create().texOffs(116, 13).mirror(false).addBox(-3.0F, 2.5F, -2.5F, 1.0F, 4.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1396263F));
        rightLeg.addOrReplaceChild("backpanelr4", CubeListBuilder.create().texOffs(0, 25).mirror().addBox(-3.0F, -0.5F, -3.5F, 5.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0349066F, 0.0F, 0.0F));
        leftLeg.addOrReplaceChild("backpanell1", CubeListBuilder.create().texOffs(0, 25).mirror(false).addBox(-2.0F, -0.5F, 2.5F, 5.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0698132F, 0.0F, 0.0F));
        leftLeg.addOrReplaceChild("backpanell2", CubeListBuilder.create().texOffs(96, 14).mirror(false).addBox(-2.0F, -0.5F, -2.5F, 5.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1396263F));
        leftLeg.addOrReplaceChild("backpanell3", CubeListBuilder.create().texOffs(116, 13).mirror(false).addBox(2.0F, 2.5F, -2.5F, 1.0F, 4.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1396263F));
        leftLeg.addOrReplaceChild("backpanell4", CubeListBuilder.create().texOffs(0, 25).mirror(false).addBox(-2.0F, -0.5F, -3.5F, 5.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0349066F, 0.0F, 0.0F));
    }
}
