package art.arcane.thaumcraft.item;

import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;

/**
 * TC6 ItemCultistRobeArmor, ItemCultistPlateArmor, ItemCultistLeaderArmor and ItemCultistBoots with the
 * ThaumcraftMaterials ARMORMAT_CULTIST_* materials. All of them repair with iron ingots (getIsRepairable).
 * The custom ModelRobe / ModelKnightArmor / ModelLeaderArmor models are supplied client side.
 */
public class CultistArmor extends Item {
    public enum Kind { ROBE, PLATE, LEADER, BOOTS }

    private static ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> asset(String n) {
        return ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath("thaumcraft", n));
    }
    // durability factor, {boots, legs, chest, helm}, enchantability, sound, toughness
    public static final ArmorMaterial PLATE = new ArmorMaterial(18,
            Map.of(ArmorType.BOOTS, 2, ArmorType.LEGGINGS, 5, ArmorType.CHESTPLATE, 6, ArmorType.HELMET, 2, ArmorType.BODY, 6),
            13, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F, ItemTags.REPAIRS_IRON_ARMOR, asset("cultist_plate"));
    public static final ArmorMaterial ROBE = new ArmorMaterial(17,
            Map.of(ArmorType.BOOTS, 2, ArmorType.LEGGINGS, 4, ArmorType.CHESTPLATE, 5, ArmorType.HELMET, 2, ArmorType.BODY, 5),
            13, SoundEvents.ARMOR_EQUIP_CHAIN, 0.0F, 0.0F, ItemTags.REPAIRS_IRON_ARMOR, asset("cultist_robe"));
    public static final ArmorMaterial LEADER = new ArmorMaterial(30,
            Map.of(ArmorType.BOOTS, 3, ArmorType.LEGGINGS, 6, ArmorType.CHESTPLATE, 7, ArmorType.HELMET, 3, ArmorType.BODY, 7),
            20, SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.0F, ItemTags.REPAIRS_IRON_ARMOR, asset("cultist_leader"));
    /** ItemCultistBoots: vanilla IRON material (15, {2,5,6,2}, 9, toughness 0) with the cultistboots texture. */
    public static final ArmorMaterial BOOTS = new ArmorMaterial(15,
            Map.of(ArmorType.BOOTS, 2, ArmorType.LEGGINGS, 5, ArmorType.CHESTPLATE, 6, ArmorType.HELMET, 2, ArmorType.BODY, 5),
            9, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F, ItemTags.REPAIRS_IRON_ARMOR, asset("cultist_boots"));

    public final Kind kind;
    public final ArmorType type;

    public CultistArmor(Properties p, Kind kind, ArmorType type) {
        super(p.humanoidArmor(switch (kind) { case ROBE -> ROBE; case PLATE -> PLATE; case LEADER -> LEADER; case BOOTS -> BOOTS; }, type)
                .rarity(kind == Kind.LEADER ? Rarity.RARE : Rarity.UNCOMMON));
        this.kind = kind;
        this.type = type;
    }

    /** Robes and boots: IVisDiscountGear 1% and IWarpingGear 1. */
    public static final class Warping extends CultistArmor implements VisDiscountGear, WarpingGear {
        public Warping(Properties p, Kind kind, ArmorType type) { super(p, kind, type); }
        @Override public int visDiscountPercent() { return 1; }
        @Override public int getWarp(ItemStack stack, Player player) { return 1; }
    }
}
