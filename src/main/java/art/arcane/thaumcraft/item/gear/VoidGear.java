package art.arcane.thaumcraft.item.gear;

import art.arcane.thaumcraft.item.VisDiscountGear;
import art.arcane.thaumcraft.item.WarpingGear;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import org.jetbrains.annotations.Nullable;

/**
 * TC6 void metal gear (ItemVoidSword/Pickaxe/Axe/Shovel/Hoe, ItemVoidArmor), ItemCrimsonBlade and the cloth ItemRobeArmor.
 * Void gear repairs itself one point a second while carried (armour once more while worn) and carries warp 1; the sword
 * applies Weakness (Lesser Sapping), the crimson blade Weakness and Hunger (Greater Sapping) with warp 2.
 */
public final class VoidGear {
    private VoidGear() {}

    /** TOOLMAT_VOID: harvest 4, 150 uses, speed 8, damage 3, enchantability 10, repaired with void metal. */
    public static final ToolMaterial TOOL = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 150, 8.0F, 3.0F, 10, ElementalGear.VOID_REPAIR);
    /** ItemCrimsonBlade.toolMatCrimsonVoid "CVOID": harvest 4, 200 uses, speed 8, damage 3.5, enchantability 20. */
    public static final ToolMaterial CRIMSON = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 200, 8.0F, 3.5F, 20, ElementalGear.VOID_REPAIR);
    /** ARMORMAT_VOID: durability 10, {boots 3, legs 6, chest 8, helm 3}, enchantability 10, chain sound, toughness 1. */
    public static final ArmorMaterial ARMOR = new ArmorMaterial(10,
            Map.of(ArmorType.BOOTS, 3, ArmorType.LEGGINGS, 6, ArmorType.CHESTPLATE, 8, ArmorType.HELMET, 3, ArmorType.BODY, 8),
            10, SoundEvents.ARMOR_EQUIP_CHAIN, 1.0F, 0.0F, ElementalGear.VOID_REPAIR,
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.parse("thaumcraft:void")));
    public static final TagKey<Item> FABRIC_REPAIR = TagKey.create(Registries.ITEM, Identifier.parse("thaumcraft:fabric_repair"));
    /** ARMORMAT_SPECIAL: durability 25, {boots 1, legs 2, chest 3, helm 1}, enchantability 25, leather sound, toughness 1. */
    public static final ArmorMaterial CLOTH = new ArmorMaterial(25,
            Map.of(ArmorType.BOOTS, 1, ArmorType.LEGGINGS, 2, ArmorType.CHESTPLATE, 3, ArmorType.HELMET, 1, ArmorType.BODY, 3),
            25, SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, 0.0F, FABRIC_REPAIR,
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.parse("thaumcraft:cloth")));

    /** onUpdate: one point of durability back every second while in an inventory (server side). */
    static void selfRepair(ItemStack stack, Entity owner, int amount) {
        if (stack.isDamaged() && owner instanceof LivingEntity && owner.tickCount % 20 == 0)
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - amount));
    }

    /** hitEntity: sapping effects, skipped on players when PvP is off. */
    static void sap(LivingEntity target, LivingEntity attacker, boolean greater) {
        if (!(target.level() instanceof ServerLevel sl)) return;
        if (target instanceof Player && attacker instanceof Player && !sl.isPvpAllowed()) return;
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60));
        if (greater) target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120));
    }

    static void sapTooltip(Consumer<Component> builder, boolean greater) {
        builder.accept(Component.translatable(greater ? "enchantment.special.sapgreat" : "enchantment.special.sapless").withStyle(ChatFormatting.GOLD));
    }

    /** ItemVoidSword and ItemCrimsonBlade. */
    public static class Sword extends Item implements WarpingGear {
        private final boolean crimson;
        public Sword(Properties p, boolean crimson) {
            super(crimson ? p.sword(CRIMSON, 3.0F, -2.4F).rarity(Rarity.EPIC) : p.sword(TOOL, 3.0F, -2.4F));
            this.crimson = crimson;
        }
        @Override public int getWarp(ItemStack stack, Player player) { return crimson ? 2 : 1; }
        @Override public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) { super.inventoryTick(stack, level, owner, slot); selfRepair(stack, owner, 1); }
        @Override public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) { super.hurtEnemy(stack, target, attacker); sap(target, attacker, crimson); }
        @Override public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) { sapTooltip(builder, crimson); super.appendHoverText(stack, ctx, display, builder, flag); }
    }

    public static class Pickaxe extends Item implements WarpingGear {
        public Pickaxe(Properties p) { super(p.pickaxe(TOOL, 1.0F, -2.8F)); }
        @Override public int getWarp(ItemStack stack, Player player) { return 1; }
        @Override public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) { super.inventoryTick(stack, level, owner, slot); selfRepair(stack, owner, 1); }
    }

    public static class Axe extends AxeItem implements WarpingGear {
        public Axe(Properties p) { super(TOOL, 8.0F, -3.0F, p); } // ItemVoidAxe: super(material, 8, -3)
        @Override public int getWarp(ItemStack stack, Player player) { return 1; }
        @Override public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) { super.inventoryTick(stack, level, owner, slot); selfRepair(stack, owner, 1); }
    }

    public static class Shovel extends ShovelItem implements WarpingGear {
        public Shovel(Properties p) { super(TOOL, 1.5F, -3.0F, p); }
        @Override public int getWarp(ItemStack stack, Player player) { return 1; }
        @Override public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) { super.inventoryTick(stack, level, owner, slot); selfRepair(stack, owner, 1); }
    }

    public static class Hoe extends HoeItem implements WarpingGear {
        public Hoe(Properties p) { super(TOOL, -3.0F, 0.0F, p); }
        @Override public int getWarp(ItemStack stack, Player player) { return 1; }
        @Override public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) { super.inventoryTick(stack, level, owner, slot); selfRepair(stack, owner, 1); }
    }

    /** ItemVoidArmor: uncommon, warp 1, repairs once a second carried and once more while worn. */
    public static class Armor extends Item implements WarpingGear {
        public Armor(Properties p, ArmorType type) { super(p.humanoidArmor(ARMOR, type).rarity(Rarity.UNCOMMON)); }
        @Override public int getWarp(ItemStack stack, Player player) { return 1; }
        @Override public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
            super.inventoryTick(stack, level, owner, slot);
            selfRepair(stack, owner, slot != null && slot.isArmor() ? 2 : 1);
        }
    }

    /** ItemRobeArmor: dyeable (default 0x6A3880, washed in a cauldron), uncommon, vis discount 3% (boots 2%), repaired with fabric. */
    public static class Robe extends Item implements VisDiscountGear {
        public static final int DEFAULT_COLOR = 0x6A3880;
        private final ArmorType type;
        public Robe(Properties p, ArmorType type) { super(p.humanoidArmor(CLOTH, type).rarity(Rarity.UNCOMMON)); this.type = type; }
        @Override public int visDiscountPercent() { return type == ArmorType.BOOTS ? 2 : 3; }
    }
}
