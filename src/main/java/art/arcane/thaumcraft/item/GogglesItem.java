package art.arcane.thaumcraft.item;
import java.util.Map;
import net.minecraft.resources.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.*;
public final class GogglesItem extends Item implements VisDiscountGear {
    private static final ArmorMaterial MATERIAL=new ArmorMaterial(25,Map.of(ArmorType.HELMET,1),25,SoundEvents.ARMOR_EQUIP_LEATHER,1F,0F,TagKey.create(Registries.ITEM,Identifier.parse("thaumcraft:goggles_repair")),ResourceKey.create(EquipmentAssets.ROOT_ID,Identifier.parse("thaumcraft:goggles")));
    public GogglesItem(Properties properties){super(properties.humanoidArmor(MATERIAL,ArmorType.HELMET).durability(350).rarity(Rarity.RARE));}
    public int visDiscountPercent(){return 5;}
}
