package art.arcane.thaumcraft.client.render.entity;

import art.arcane.thaumcraft.item.gear.FortressArmorItem;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * TC6 ModelFortressArmor (ModelCustomArmor, 128x64): outer variant (f = 1.0, cuirass) carries the chest/back plates, book
 * and scroll; inner variant (f = 0.5, helm and thigh guards) the lower belt. Ornaments, flaps, shoulder plates, side panels,
 * book and scroll appear as more pieces of the set are worn (checkSet), and the helm shows its goggles and mask.
 */
public final class FortressArmorModel extends HumanoidModel<HumanoidRenderState> {
    public static final ModelLayerLocation OUTER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("thaumcraft", "fortress_armor"), "outer");
    public static final ModelLayerLocation INNER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("thaumcraft", "fortress_armor"), "inner");

    private final EquipmentSlot slot;
    private final ModelPart goggles;
    private final ModelPart[] mask = new ModelPart[3];

    public FortressArmorModel(ModelPart root, EquipmentSlot slot) {
        super(root);
        this.slot = slot;
        goggles = head.getChild("goggles");
        for (int a = 0; a < 3; a++) mask[a] = head.getChild("mask" + a);
    }

    private static void hide(ModelPart parent, String name, boolean hidden) { if (parent.hasChild(name)) parent.getChild(name).visible = !hidden; }

    @Override
    public void setupAnim(HumanoidRenderState s) {
        super.setupAnim(s);
        head.visible = hat.visible = slot == EquipmentSlot.HEAD;
        body.visible = slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS;
        rightArm.visible = leftArm.visible = slot == EquipmentSlot.CHEST;
        rightLeg.visible = leftLeg.visible = slot == EquipmentSlot.LEGS;
        if (!s.isBaby) { head.xScale *= 1.01f; head.yScale *= 1.01f; head.zScale *= 1.01f; }
        // checkSet
        int set = 0, m = -1;
        boolean g = false;
        for (ItemStack piece : new ItemStack[] { s.legsEquipment, s.chestEquipment, s.headEquipment })
            if (piece.getItem() instanceof FortressArmorItem) set++;
        if (s.headEquipment.getItem() instanceof FortressArmorItem) { m = FortressArmorItem.getMask(s.headEquipment); g = FortressArmorItem.hasGoggles(s.headEquipment); }
        if (set == 0) set = -1;
        goggles.visible = g;
        for (int a = 0; a < 3; a++) mask[a].visible = m == a;
        hide(body, "scroll", set < 3);
        hide(body, "book", set < 2);
        for (String n : new String[] { "ornamentl", "ornamentl2", "ornamentr", "ornamentr2", "gemornament", "gem" }) hide(head, n, set < 3);
        hide(head, "flapl", set < 2);
        hide(head, "flapr", set < 2);
        hide(leftArm, "shoulderplateltop", set < 2); hide(leftArm, "shoulderplatel1", set < 2);
        hide(leftArm, "shoulderplatel2", set < 3); hide(leftArm, "shoulderplatel3", set < 3);
        hide(rightArm, "shoulderplatertop", set < 2); hide(rightArm, "shoulderplater1", set < 2);
        hide(rightArm, "shoulderplater2", set < 3); hide(rightArm, "shoulderplater3", set < 3);
        hide(rightLeg, "sidepanelr2", set < 2); hide(leftLeg, "sidepanell2", set < 2);
        hide(rightLeg, "sidepanelr3", set < 3); hide(leftLeg, "sidepanell3", set < 3);
    }

    private static LayerDefinition create(boolean outer) {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0f, 2.0f, 0.0f));
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0f, 2.0f, 0.0f));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9f, 12.0f, 0.0f));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9f, 12.0f, 0.0f));
        // Generated 1:1 from the TC6 constructor.
        head.addOrReplaceChild("ornamentl", CubeListBuilder.create().texOffs(78, 8).mirror().addBox(1.5F, -9.0F, -6.5F, 2F, 2F, 1F), PartPose.rotation(-0.1396263F, 0.0F, 0.0F));
        head.addOrReplaceChild("ornamentl2", CubeListBuilder.create().texOffs(78, 8).mirror().addBox(3.5F, -10.0F, -6.5F, 1F, 2F, 1F), PartPose.rotation(-0.1396263F, 0.0F, 0.0F));
        head.addOrReplaceChild("ornamentr", CubeListBuilder.create().texOffs(78, 8).addBox(-3.5F, -9.0F, -6.5F, 2F, 2F, 1F), PartPose.rotation(-0.1396263F, 0.0F, 0.0F));
        head.addOrReplaceChild("ornamentr2", CubeListBuilder.create().texOffs(78, 8).addBox(-4.5F, -10.0F, -6.5F, 1F, 2F, 1F), PartPose.rotation(-0.1396263F, 0.0F, 0.0F));
        head.addOrReplaceChild("helmet", CubeListBuilder.create().texOffs(41, 8).addBox(-4.5F, -9.0F, -4.5F, 9F, 4F, 9F), PartPose.rotation(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("helmetr", CubeListBuilder.create().texOffs(21, 13).addBox(-6.5F, -3.0F, -4.5F, 1F, 5F, 9F), PartPose.rotation(0.0F, 0.0F, 0.5235988F));
        head.addOrReplaceChild("helmetl", CubeListBuilder.create().texOffs(21, 13).mirror().addBox(5.5F, -3.0F, -4.5F, 1F, 5F, 9F), PartPose.rotation(0.0F, 0.0F, -0.5235988F));
        head.addOrReplaceChild("helmetb", CubeListBuilder.create().texOffs(41, 21).addBox(-4.5F, -3.0F, 5.5F, 9F, 5F, 1F), PartPose.rotation(0.5235988F, 0.0F, 0.0F));
        head.addOrReplaceChild("capsthingy", CubeListBuilder.create().texOffs(21, 0).addBox(-4.5F, -6.0F, -6.5F, 9F, 1F, 2F), PartPose.rotation(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("flapr", CubeListBuilder.create().texOffs(59, 10).addBox(-10.0F, -2.0F, -1.0F, 3F, 3F, 1F), PartPose.rotation(0.0F, -0.5235988F, 0.5235988F));
        head.addOrReplaceChild("flapl", CubeListBuilder.create().texOffs(59, 10).mirror().addBox(7.0F, -2.0F, -1.0F, 3F, 3F, 1F), PartPose.rotation(0.0F, 0.5235988F, -0.5235988F));
        head.addOrReplaceChild("gemornament", CubeListBuilder.create().texOffs(68, 11).addBox(-1.5F, -9.0F, -7.0F, 3F, 3F, 2F), PartPose.rotation(-0.1396263F, 0.0F, 0.0F));
        head.addOrReplaceChild("gem", CubeListBuilder.create().texOffs(72, 8).addBox(-1.0F, -8.5F, -7.5F, 2F, 2F, 1F), PartPose.rotation(-0.1396263F, 0.0F, 0.0F));
        head.addOrReplaceChild("goggles", CubeListBuilder.create().texOffs(100, 18).addBox(-4.5F, -5.0F, -4.25F, 9F, 5F, 1F), PartPose.rotation(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("mask0", CubeListBuilder.create().texOffs(52, 2).addBox(-4.5F, -5.0F, -4.6F, 9F, 5F, 1F), PartPose.rotation(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("mask1", CubeListBuilder.create().texOffs(76, 2).addBox(-4.5F, -5.0F, -4.6F, 9F, 5F, 1F), PartPose.rotation(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("mask2", CubeListBuilder.create().texOffs(100, 2).addBox(-4.5F, -5.0F, -4.6F, 9F, 5F, 1F), PartPose.rotation(0.0F, 0.0F, 0.0F));
        if (!outer) body.addOrReplaceChild("mbelt", CubeListBuilder.create().texOffs(56, 55).addBox(-4.0F, 8.0F, -3.0F, 8F, 4F, 1F), PartPose.rotation(0.0F, 0.0F, 0.0F));
        if (!outer) body.addOrReplaceChild("mbeltl", CubeListBuilder.create().texOffs(76, 44).addBox(4.0F, 8.0F, -3.0F, 1F, 3F, 6F), PartPose.rotation(0.0F, 0.0F, 0.0F));
        if (!outer) body.addOrReplaceChild("mbeltr", CubeListBuilder.create().texOffs(76, 44).addBox(-5.0F, 8.0F, -3.0F, 1F, 3F, 6F), PartPose.rotation(0.0F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("beltr", CubeListBuilder.create().texOffs(76, 44).addBox(-5.0F, 4.0F, -3.0F, 1F, 3F, 6F), PartPose.rotation(0.0F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("beltl", CubeListBuilder.create().texOffs(76, 44).addBox(4.0F, 4.0F, -3.0F, 1F, 3F, 6F), PartPose.rotation(0.0F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("chestplate", CubeListBuilder.create().texOffs(56, 45).addBox(-4.0F, 1.0F, -4.0F, 8F, 7F, 2F), PartPose.rotation(0.0F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("scroll", CubeListBuilder.create().texOffs(34, 27).addBox(-2.0F, 9.5F, 4.0F, 8F, 3F, 3F), PartPose.rotation(0.0F, 0.0F, 0.1919862F));
        if (outer) body.addOrReplaceChild("backplate", CubeListBuilder.create().texOffs(36, 45).addBox(-4.0F, 1.0F, 2.0F, 8F, 11F, 2F), PartPose.rotation(0.0F, 0.0F, 0.0F));
        if (outer) body.addOrReplaceChild("book", CubeListBuilder.create().texOffs(100, 8).addBox(1.0F, -0.3F, 4.0F, 5F, 7F, 2F), PartPose.rotation(0.0F, 0.0F, 0.7679449F));
        rightArm.addOrReplaceChild("shoulderr", CubeListBuilder.create().texOffs(56, 35).addBox(-3.5F, -2.5F, -2.5F, 5F, 5F, 5F), PartPose.rotation(0.0F, 0.0F, 0.0F));
        rightArm.addOrReplaceChild("gauntletr", CubeListBuilder.create().texOffs(100, 26).addBox(-3.5F, 3.5F, -2.5F, 2F, 6F, 5F), PartPose.rotation(0.0F, 0.0F, 0.0F));
        rightArm.addOrReplaceChild("gauntletstrapr1", CubeListBuilder.create().texOffs(84, 31).addBox(-1.5F, 3.5F, -2.5F, 3F, 1F, 5F), PartPose.rotation(0.0F, 0.0F, 0.0F));
        rightArm.addOrReplaceChild("gauntletstrapr2", CubeListBuilder.create().texOffs(84, 31).addBox(-1.5F, 6.5F, -2.5F, 3F, 1F, 5F), PartPose.rotation(0.0F, 0.0F, 0.0F));
        rightArm.addOrReplaceChild("shoulderplatertop", CubeListBuilder.create().texOffs(110, 37).addBox(-5.5F, -2.5F, -3.5F, 2F, 1F, 7F), PartPose.rotation(0.0F, 0.0F, 0.4363323F));
        rightArm.addOrReplaceChild("shoulderplater1", CubeListBuilder.create().texOffs(110, 45).addBox(-4.5F, -1.5F, -3.5F, 1F, 4F, 7F), PartPose.rotation(0.0F, 0.0F, 0.4363323F));
        rightArm.addOrReplaceChild("shoulderplater2", CubeListBuilder.create().texOffs(94, 45).addBox(-3.5F, 1.5F, -3.5F, 1F, 3F, 7F), PartPose.rotation(0.0F, 0.0F, 0.4363323F));
        rightArm.addOrReplaceChild("shoulderplater3", CubeListBuilder.create().texOffs(94, 45).addBox(-2.5F, 3.5F, -3.5F, 1F, 3F, 7F), PartPose.rotation(0.0F, 0.0F, 0.4363323F));
        leftArm.addOrReplaceChild("shoulderl", CubeListBuilder.create().texOffs(56, 35).mirror().addBox(-1.5F, -2.5F, -2.5F, 5F, 5F, 5F), PartPose.rotation(0.0F, 0.0F, 0.0F));
        leftArm.addOrReplaceChild("gauntletl", CubeListBuilder.create().texOffs(114, 26).addBox(1.5F, 3.5F, -2.5F, 2F, 6F, 5F), PartPose.rotation(0.0F, 0.0F, 0.0F));
        leftArm.addOrReplaceChild("gauntletstrapl1", CubeListBuilder.create().texOffs(84, 31).mirror().addBox(-1.5F, 3.5F, -2.5F, 3F, 1F, 5F), PartPose.rotation(0.0F, 0.0F, 0.0F));
        leftArm.addOrReplaceChild("gauntletstrapl2", CubeListBuilder.create().texOffs(84, 31).mirror().addBox(-1.5F, 6.5F, -2.5F, 3F, 1F, 5F), PartPose.rotation(0.0F, 0.0F, 0.0F));
        leftArm.addOrReplaceChild("shoulderplateltop", CubeListBuilder.create().texOffs(110, 37).mirror().addBox(3.5F, -2.5F, -3.5F, 2F, 1F, 7F), PartPose.rotation(0.0F, 0.0F, -0.4363323F));
        leftArm.addOrReplaceChild("shoulderplatel1", CubeListBuilder.create().texOffs(110, 45).mirror().addBox(3.5F, -1.5F, -3.5F, 1F, 4F, 7F), PartPose.rotation(0.0F, 0.0F, -0.4363323F));
        leftArm.addOrReplaceChild("shoulderplatel2", CubeListBuilder.create().texOffs(94, 45).mirror().addBox(2.5F, 1.5F, -3.5F, 1F, 3F, 7F), PartPose.rotation(0.0F, 0.0F, -0.4363323F));
        leftArm.addOrReplaceChild("shoulderplatel3", CubeListBuilder.create().texOffs(94, 45).mirror().addBox(1.5F, 3.5F, -3.5F, 1F, 3F, 7F), PartPose.rotation(0.0F, 0.0F, -0.4363323F));
        rightLeg.addOrReplaceChild("legpanelr1", CubeListBuilder.create().texOffs(0, 51).addBox(-1.0F, 0.5F, -3.5F, 3F, 4F, 1F), PartPose.rotation(-0.4363323F, 0.0F, 0.0F));
        rightLeg.addOrReplaceChild("legpanelr2", CubeListBuilder.create().texOffs(8, 51).addBox(-1.0F, 3.5F, -2.5F, 3F, 4F, 1F), PartPose.rotation(-0.4363323F, 0.0F, 0.0F));
        rightLeg.addOrReplaceChild("legpanelr3", CubeListBuilder.create().texOffs(0, 56).addBox(-1.0F, 6.5F, -1.5F, 3F, 3F, 1F), PartPose.rotation(-0.4363323F, 0.0F, 0.0F));
        rightLeg.addOrReplaceChild("legpanelr4", CubeListBuilder.create().texOffs(0, 43).addBox(-3.0F, 0.5F, -3.5F, 2F, 3F, 1F), PartPose.rotation(-0.4363323F, 0.0F, 0.0F));
        rightLeg.addOrReplaceChild("legpanelr5", CubeListBuilder.create().texOffs(0, 47).addBox(-3.0F, 2.5F, -2.5F, 2F, 3F, 1F), PartPose.rotation(-0.4363323F, 0.0F, 0.0F));
        rightLeg.addOrReplaceChild("legpanelr6", CubeListBuilder.create().texOffs(6, 43).addBox(-3.0F, 4.5F, -1.5F, 2F, 3F, 1F), PartPose.rotation(-0.4363323F, 0.0F, 0.0F));
        rightLeg.addOrReplaceChild("sidepanelr1", CubeListBuilder.create().texOffs(0, 22).addBox(-2.5F, 0.5F, -2.5F, 1F, 4F, 5F), PartPose.rotation(0.0F, 0.0F, 0.4363323F));
        rightLeg.addOrReplaceChild("sidepanelr2", CubeListBuilder.create().texOffs(0, 31).addBox(-1.5F, 3.5F, -2.5F, 1F, 3F, 5F), PartPose.rotation(0.0F, 0.0F, 0.4363323F));
        rightLeg.addOrReplaceChild("sidepanelr3", CubeListBuilder.create().texOffs(12, 31).addBox(-0.5F, 5.5F, -2.5F, 1F, 3F, 5F), PartPose.rotation(0.0F, 0.0F, 0.4363323F));
        rightLeg.addOrReplaceChild("backpanelr1", CubeListBuilder.create().texOffs(0, 18).addBox(-3.0F, 0.5F, 2.5F, 5F, 3F, 1F), PartPose.rotation(0.4363323F, 0.0F, 0.0F));
        rightLeg.addOrReplaceChild("backpanelr2", CubeListBuilder.create().texOffs(0, 18).addBox(-3.0F, 2.5F, 1.5F, 5F, 3F, 1F), PartPose.rotation(0.4363323F, 0.0F, 0.0F));
        rightLeg.addOrReplaceChild("backpanelr3", CubeListBuilder.create().texOffs(0, 18).addBox(-3.0F, 4.5F, 0.5F, 5F, 3F, 1F), PartPose.rotation(0.4363323F, 0.0F, 0.0F));
        leftLeg.addOrReplaceChild("backpanell3", CubeListBuilder.create().texOffs(0, 18).mirror().addBox(-2.0F, 4.5F, 0.5F, 5F, 3F, 1F), PartPose.rotation(0.4363323F, 0.0F, 0.0F));
        leftLeg.addOrReplaceChild("legpanell1", CubeListBuilder.create().texOffs(0, 51).mirror().addBox(-2.0F, 0.5F, -3.5F, 3F, 4F, 1F), PartPose.rotation(-0.4363323F, 0.0F, 0.0F));
        leftLeg.addOrReplaceChild("legpanell2", CubeListBuilder.create().texOffs(8, 51).mirror().addBox(-2.0F, 3.5F, -2.5F, 3F, 4F, 1F), PartPose.rotation(-0.4363323F, 0.0F, 0.0F));
        leftLeg.addOrReplaceChild("legpanell3", CubeListBuilder.create().texOffs(0, 56).mirror().addBox(-2.0F, 6.5F, -1.5F, 3F, 3F, 1F), PartPose.rotation(-0.4363323F, 0.0F, 0.0F));
        leftLeg.addOrReplaceChild("legpanell4", CubeListBuilder.create().texOffs(0, 43).mirror().addBox(1.0F, 0.5F, -3.5F, 2F, 3F, 1F), PartPose.rotation(-0.4363323F, 0.0F, 0.0F));
        leftLeg.addOrReplaceChild("legpanell5", CubeListBuilder.create().texOffs(0, 47).mirror().addBox(1.0F, 2.5F, -2.5F, 2F, 3F, 1F), PartPose.rotation(-0.4363323F, 0.0F, 0.0F));
        leftLeg.addOrReplaceChild("legpanell6", CubeListBuilder.create().texOffs(6, 43).mirror().addBox(1.0F, 4.5F, -1.5F, 2F, 3F, 1F), PartPose.rotation(-0.4363323F, 0.0F, 0.0F));
        leftLeg.addOrReplaceChild("sidepanell1", CubeListBuilder.create().texOffs(0, 22).mirror().addBox(1.5F, 0.5F, -2.5F, 1F, 4F, 5F), PartPose.rotation(0.0F, 0.0F, -0.4363323F));
        leftLeg.addOrReplaceChild("sidepanell2", CubeListBuilder.create().texOffs(0, 31).mirror().addBox(0.5F, 3.5F, -2.5F, 1F, 3F, 5F), PartPose.rotation(0.0F, 0.0F, -0.4363323F));
        leftLeg.addOrReplaceChild("sidepanell3", CubeListBuilder.create().texOffs(12, 31).mirror().addBox(-0.5F, 5.5F, -2.5F, 1F, 3F, 5F), PartPose.rotation(0.0F, 0.0F, -0.4363323F));
        leftLeg.addOrReplaceChild("backpanell1", CubeListBuilder.create().texOffs(0, 18).mirror().addBox(-2.0F, 0.5F, 2.5F, 5F, 3F, 1F), PartPose.rotation(0.4363323F, 0.0F, 0.0F));
        leftLeg.addOrReplaceChild("backpanell2", CubeListBuilder.create().texOffs(0, 18).mirror().addBox(-2.0F, 2.5F, 1.5F, 5F, 3F, 1F), PartPose.rotation(0.4363323F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    public static void register(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(OUTER, () -> create(true));
        e.registerLayerDefinition(INNER, () -> create(false));
    }
}
