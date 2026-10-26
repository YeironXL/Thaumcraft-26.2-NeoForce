package art.arcane.thaumcraft.client.render.entity;

import art.arcane.thaumcraft.entity.monster.InhabitedZombieEntity;
import art.arcane.thaumcraft.item.CultistArmor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;

/**
 * TC6 RenderInhabitedZombie: the zombie with textures/entity/czombie.png. Its crimson plate is drawn with the knight
 * armour models but ItemCultistPlateArmor.getArmorTexture's zombie skin (textures/entity/armor/zombie_plate_armor.png).
 */
public final class InhabitedZombieRenderer extends HumanoidMobRenderer<InhabitedZombieEntity, ZombieRenderState, ZombieModel<ZombieRenderState>> {
    private static final Identifier TEX = Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/czombie.png");
    private static final Identifier PLATE = Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/armor/zombie_plate_armor.png");

    public InhabitedZombieRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new ZombieModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5f);
        addLayer(new PlateLayer(this, ctx));
    }
    @Override public Identifier getTextureLocation(ZombieRenderState s) { return TEX; }
    @Override public ZombieRenderState createRenderState() { return new ZombieRenderState(); }
    @Override
    public void extractRenderState(InhabitedZombieEntity e, ZombieRenderState s, float pt) {
        super.extractRenderState(e, s, pt);
        s.isAggressive = e.isAggressive();
    }

    static final class PlateLayer extends RenderLayer<ZombieRenderState, ZombieModel<ZombieRenderState>> {
        private final CultistArmorModel head, chest, legs;
        PlateLayer(RenderLayerParent<ZombieRenderState, ZombieModel<ZombieRenderState>> parent, EntityRendererProvider.Context ctx) {
            super(parent);
            head = new CultistArmorModel(ctx.bakeLayer(CultistArmorModel.layerFor(CultistArmor.Kind.PLATE, ArmorType.HELMET)), CultistArmor.Kind.PLATE, EquipmentSlot.HEAD);
            chest = new CultistArmorModel(ctx.bakeLayer(CultistArmorModel.layerFor(CultistArmor.Kind.PLATE, ArmorType.CHESTPLATE)), CultistArmor.Kind.PLATE, EquipmentSlot.CHEST);
            legs = new CultistArmorModel(ctx.bakeLayer(CultistArmorModel.layerFor(CultistArmor.Kind.PLATE, ArmorType.LEGGINGS)), CultistArmor.Kind.PLATE, EquipmentSlot.LEGS);
        }
        private static boolean plate(ItemStack s) { return s.getItem() instanceof CultistArmor a && a.kind == CultistArmor.Kind.PLATE; }
        @Override
        public void submit(PoseStack ps, SubmitNodeCollector c, int light, ZombieRenderState s, float yRot, float xRot) {
            if (plate(s.chestEquipment)) coloredCutoutModelCopyLayerRender(chest, PLATE, ps, c, light, s, -1, 1);
            if (plate(s.legsEquipment)) coloredCutoutModelCopyLayerRender(legs, PLATE, ps, c, light, s, -1, 1);
            if (plate(s.headEquipment)) coloredCutoutModelCopyLayerRender(head, PLATE, ps, c, light, s, -1, 1);
        }
    }
}
