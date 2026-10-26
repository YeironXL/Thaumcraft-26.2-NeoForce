package art.arcane.thaumcraft.item.gear;

import art.arcane.thaumcraft.item.VisDiscountGear;
import art.arcane.thaumcraft.item.WarpingGear;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import org.jetbrains.annotations.Nullable;

/**
 * TC6 ItemVoidRobeArmor: ARMORMAT_VOIDROBE (18, {helm 4, chest 9, legs 7}, enchantability 10, toughness 2), epic, repaired
 * with void metal and self-repairing (once a second carried, twice while worn). 5% vis discount, warp 3, the hood reveals
 * like goggles (Baubles.reveals), dyeable (default 0x6A3880, washed in a cauldron) and drawn with the ModelRobe.
 */
public class VoidRobeArmorItem extends Item implements VisDiscountGear, WarpingGear {
    public static final int DEFAULT_COLOR = 0x6A3880;
    public static final ArmorMaterial VOIDROBE = new ArmorMaterial(18,
            Map.of(ArmorType.BOOTS, 4, ArmorType.LEGGINGS, 7, ArmorType.CHESTPLATE, 9, ArmorType.HELMET, 4, ArmorType.BODY, 9),
            10, SoundEvents.ARMOR_EQUIP_LEATHER, 2.0F, 0.0F, ElementalGear.VOID_REPAIR,
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.parse("thaumcraft:void_robe")));
    public final ArmorType type;

    public VoidRobeArmorItem(Properties p, ArmorType type) {
        super(p.humanoidArmor(VOIDROBE, type).rarity(Rarity.EPIC));
        this.type = type;
    }

    @Override public int visDiscountPercent() { return 5; }
    @Override public int getWarp(ItemStack stack, Player player) { return 3; }

    public static int getColor(ItemStack stack) { return DyedItemColor.getOrDefault(stack, 0xFF000000 | DEFAULT_COLOR) & 0xFFFFFF; }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, owner, slot);
        if (!(owner instanceof LivingEntity) || owner.tickCount % 20 != 0) return;
        // onUpdate repairs every carried robe; onArmorTick repairs a worn one again.
        int repair = (slot != null && slot.isArmor()) ? 2 : 1;
        if (stack.isDamaged()) stack.setDamageValue(Math.max(0, stack.getDamageValue() - repair));
    }
}
