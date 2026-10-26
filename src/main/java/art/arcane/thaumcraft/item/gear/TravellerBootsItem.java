package art.arcane.thaumcraft.item.gear;

import art.arcane.thaumcraft.item.IRechargable;
import art.arcane.thaumcraft.item.RechargeHelper;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.phys.Vec3;

/**
 * TC6 ItemBootsTraveller: ARMORMAT_SPECIAL boots (1 armour, enchantability 25, toughness 1) with 350 durability, repaired
 * with leather, holding 240 vis (one spent each minute through the "energy" counter). While charged they add ground and
 * swim speed, air control, a full block step height (not while sneaking) and a higher jump; fall damage is halved minus one.
 */
public class TravellerBootsItem extends Item implements IRechargable {
    public static final ArmorMaterial SPECIAL = new ArmorMaterial(25,
            Map.of(ArmorType.BOOTS, 1, ArmorType.LEGGINGS, 2, ArmorType.CHESTPLATE, 3, ArmorType.HELMET, 1, ArmorType.BODY, 3),
            25, SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, 0.0F, ItemTags.REPAIRS_LEATHER_ARMOR,
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.parse("thaumcraft:traveller")));
    private static final Identifier STEP = Identifier.fromNamespaceAndPath("thaumcraft", "traveller_step");

    public TravellerBootsItem(Properties p) { super(p.humanoidArmor(SPECIAL, ArmorType.BOOTS).durability(350).rarity(Rarity.RARE)); }

    @Override public int getMaxCharge(ItemStack stack, LivingEntity player) { return 240; }
    @Override public EnumChargeDisplay showInHud(ItemStack stack, LivingEntity player) { return EnumChargeDisplay.PERIODIC; }

    public static boolean worn(Player player) { return player.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof TravellerBootsItem; }

    /** onArmorTick plus PlayerEvents.handleSpeedMods, run on both sides each player tick. */
    public static void tick(Player player) {
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        boolean wearing = boots.getItem() instanceof TravellerBootsItem;
        boolean hasCharge = wearing && RechargeHelper.getCharge(boots) > 0;
        if (wearing && !player.level().isClientSide() && player.tickCount % 20 == 0) {
            int e = boots.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr("energy", 0);
            if (e > 0) --e;
            else if (RechargeHelper.consumeCharge(boots, player, 1)) e = 60;
            int fe = e;
            CustomData.update(DataComponents.CUSTOM_DATA, boots, t -> t.putInt("energy", fe));
        }
        boolean step = false;
        if (hasCharge && !player.getAbilities().flying && player.zza > 0.0f) {
            step = !player.isShiftKeyDown();
            if (player.onGround()) {
                float bonus = 0.05f;
                if (player.isInWater()) bonus /= 4.0f;
                player.moveRelative(bonus, new Vec3(0, 0, 1));
            } else {
                if (player.isInWater()) player.moveRelative(0.025f, new Vec3(0, 0, 1));
                // jumpMovementFactor 0.05 instead of the default 0.02.
                player.moveRelative(0.03f, new Vec3(player.xxa, 0, player.zza));
            }
        }
        AttributeInstance inst = player.getAttribute(Attributes.STEP_HEIGHT);
        if (inst != null) {
            if (step && !inst.hasModifier(STEP)) inst.addTransientModifier(new AttributeModifier(STEP, 0.4, AttributeModifier.Operation.ADD_VALUE));
            else if (!step && inst.hasModifier(STEP)) inst.removeModifier(STEP);
        }
    }
}
