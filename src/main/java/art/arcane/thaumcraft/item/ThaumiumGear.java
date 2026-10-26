package art.arcane.thaumcraft.item;

import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.*;

/** TC6 ThaumcraftMaterials TOOLMAT_THAUMIUM / ARMORMAT_THAUMIUM. */
public final class ThaumiumGear {
    private ThaumiumGear() {}
    public static final TagKey<Item> REPAIR = TagKey.create(Registries.ITEM, Identifier.parse("thaumcraft:thaumium_repair"));
    // harvest 3 (diamond tier), 500 uses, speed 7, damage 2.5, enchantability 22
    public static final ToolMaterial TOOL = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 500, 7.0F, 2.5F, 22, REPAIR);
    // durability factor 25, {boots 2, legs 5, chest 6, helm 2}, enchantability 25, iron sound, toughness 1
    public static final ArmorMaterial ARMOR = new ArmorMaterial(25,
            Map.of(ArmorType.BOOTS, 2, ArmorType.LEGGINGS, 5, ArmorType.CHESTPLATE, 6, ArmorType.HELMET, 2, ArmorType.BODY, 6),
            25, SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.0F, REPAIR,
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.parse("thaumcraft:thaumium")));

    public static Item.Properties rare(Item.Properties p) { return p.rarity(Rarity.UNCOMMON); }
}
