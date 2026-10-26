package art.arcane.thaumcraft.item.gear;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorType;

/**
 * TC6 ItemFortressArmor: ARMORMAT_FORTRESS helm, cuirass and thigh guards (rare, repaired with thaumium). The helm can
 * carry a "goggles" byte (Helm of Revealing, see Baubles.reveals) and a "mask" int (0 Grinning Devil, 1 Angry Ghost,
 * 2 Sipping Fiend) set by the FORTRESSMASK infusions. Damage goes through SpecialArmor (ISpecialArmor); the set bonus is
 * also mirrored as an attribute modifier so the armour bar shows getArmorDisplay.
 */
public class FortressArmorItem extends Item {
    public static final Identifier SET_BONUS = Identifier.fromNamespaceAndPath("thaumcraft", "fortress_set");
    public final ArmorType type;

    public FortressArmorItem(Properties p, ArmorType type) {
        super(p.humanoidArmor(ElementalGear.FORTRESS, type).rarity(Rarity.RARE));
        this.type = type;
    }

    public static boolean hasGoggles(ItemStack stack) { return tag(stack).contains("goggles"); }
    /** The mask type, or -1 for the plain faceplate. */
    public static int getMask(ItemStack stack) { var t = tag(stack); return t.contains("mask") ? t.getIntOr("mask", -1) : -1; }
    private static net.minecraft.nbt.CompoundTag tag(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        if (hasGoggles(stack)) builder.accept(Component.translatable("item.thaumcraft.goggles").withStyle(ChatFormatting.DARK_PURPLE));
        int mask = getMask(stack);
        if (mask >= 0) builder.accept(Component.translatable("item.fortress_helm.mask." + mask).withStyle(ChatFormatting.GOLD));
    }

    /** getProperties / getArmorDisplay summed over the worn pieces: {armor, toughness}. */
    public static int[] setBonus(LivingEntity wearer) {
        int pieces = 0, masked = 0;
        for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD }) {
            ItemStack piece = wearer.getItemBySlot(slot);
            if (piece.getItem() instanceof FortressArmorItem) { pieces++; if (getMask(piece) >= 0) masked++; }
        }
        return new int[] { pieces * (1 + masked), pieces };
    }

    /** Keeps the transient set-bonus modifiers in step with what the player wears (players only, as in TC6). */
    public static void updateSetBonus(Player player) {
        int[] b = setBonus(player);
        apply(player.getAttribute(Attributes.ARMOR), b[0]);
        apply(player.getAttribute(Attributes.ARMOR_TOUGHNESS), b[1]);
    }
    private static void apply(AttributeInstance inst, int amount) {
        if (inst == null) return;
        AttributeModifier cur = inst.getModifier(SET_BONUS);
        if (cur != null && cur.amount() == amount) return;
        if (cur != null) inst.removeModifier(SET_BONUS);
        if (amount > 0) inst.addTransientModifier(new AttributeModifier(SET_BONUS, amount, AttributeModifier.Operation.ADD_VALUE));
    }
}
