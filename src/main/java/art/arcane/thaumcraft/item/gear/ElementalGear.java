package art.arcane.thaumcraft.item.gear;

import art.arcane.thaumcraft.item.ThaumiumGear;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;

/** TC6 ThaumcraftMaterials TOOLMAT_ELEMENTAL, ARMORMAT_FORTRESS and ItemPrimalCrusher.material (PRIMALVOID). */
public final class ElementalGear {
    private ElementalGear() {}
    public static final TagKey<Item> VOID_REPAIR = TagKey.create(Registries.ITEM, Identifier.parse("thaumcraft:void_repair"));
    public static final TagKey<Block> CRUSHER_MINEABLE = TagKey.create(Registries.BLOCK, Identifier.parse("thaumcraft:mineable/primal_crusher"));
    /** THAUMIUM_ELEMENTAL: harvest 3 (diamond), 1500 uses, speed 9, damage 3, enchantability 18, repaired with thaumium. */
    public static final ToolMaterial TOOL = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1500, 9.0F, 3.0F, 18, ThaumiumGear.REPAIR);
    /** PRIMALVOID: harvest 5 (beyond netherite), 500 uses, speed 8, damage 4, enchantability 20, repaired with void metal. */
    public static final ToolMaterial PRIMAL = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 500, 8.0F, 4.0F, 20, VOID_REPAIR);
    /** FORTRESS: durability factor 40, {boots 3, legs 6, chest 7, helm 3}, enchantability 25, iron sound, toughness 3, repaired with thaumium. */
    public static final ArmorMaterial FORTRESS = new ArmorMaterial(40,
            Map.of(ArmorType.BOOTS, 3, ArmorType.LEGGINGS, 6, ArmorType.CHESTPLATE, 7, ArmorType.HELMET, 3, ArmorType.BODY, 7),
            25, SoundEvents.ARMOR_EQUIP_IRON, 3.0F, 0.0F, ThaumiumGear.REPAIR,
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.parse("thaumcraft:fortress")));
}
