package art.arcane.thaumcraft.item.gear;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Forge 1.12 ISpecialArmor.ArmorProperties.applyArmor for TC6's fortress armour and void robes. The player's armour
 * value (minus the HUD-only set bonus modifier) plus each special piece's Armor/Toughness feed the vanilla formula
 * after the special pieces have absorbed their ratio of the hit; magic and fire/explosion use their own ratios.
 */
public final class SpecialArmor {
    /** ISpecialArmor.ArmorProperties. */
    public static final class Props {
        public int priority; public double ratio, armor, toughness; public int absorbMax; public EquipmentSlot slot;
        Props(int priority, double ratio, int absorbMax) { this.priority = priority; this.ratio = ratio; this.absorbMax = absorbMax; }
    }

    private static final EquipmentSlot[] SLOTS = { EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD };
    /** Entities whose current hit is handled here, so vanilla armour wear for the same tick is skipped. */
    private static final Map<LivingEntity, Integer> HANDLED = new WeakHashMap<>();

    public static boolean isSpecial(ItemStack s) { return s.getItem() instanceof FortressArmorItem || s.getItem() instanceof VoidRobeArmorItem; }
    public static boolean wearsSpecial(LivingEntity e) { for (EquipmentSlot s : SLOTS) if (isSpecial(e.getItemBySlot(s))) return true; return false; }
    public static boolean handledThisTick(LivingEntity e) { Integer t = HANDLED.get(e); return t != null && t == e.tickCount; }

    private static boolean magic(DamageSource src) { return src.is(DamageTypes.MAGIC) || src.is(DamageTypes.INDIRECT_MAGIC) || src.is(DamageTypes.THORNS); }

    /** ItemFortressArmor / ItemVoidRobeArmor.getProperties. */
    public static Props properties(LivingEntity player, ItemStack armor, DamageSource source) {
        int def;
        boolean fortress = armor.getItem() instanceof FortressArmorItem;
        if (fortress) def = ElementalGear.FORTRESS.defense().getOrDefault(((FortressArmorItem) armor.getItem()).type, 0);
        else def = VoidRobeArmorItem.VOIDROBE.defense().getOrDefault(((VoidRobeArmorItem) armor.getItem()).type, 0);
        int priority = 0;
        double ratio = def / 25.0;
        if (magic(source)) { priority = 1; ratio = def / 35.0; }
        else if (fortress && (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_EXPLOSION))) { priority = 1; ratio = def / 20.0; }
        else if (source.is(DamageTypeTags.BYPASSES_ARMOR)) { priority = 0; ratio = 0.0; }
        Props ap = new Props(priority, ratio, armor.getMaxDamage() + 1 - armor.getDamageValue());
        if (fortress && player instanceof Player) {
            int q = 0;
            for (EquipmentSlot s : new EquipmentSlot[] { EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD }) {
                ItemStack piece = player.getItemBySlot(s);
                if (piece.getItem() instanceof FortressArmorItem) {
                    if (FortressArmorItem.getMask(piece) >= 0) ++ap.armor;
                    if (++q <= 1) { ++ap.armor; ++ap.toughness; }
                }
            }
        }
        return ap;
    }

    /** ArmorProperties.StandardizeList: per priority group, ratios summing over 1 are normalised; absorbMax caps each piece. */
    private static void standardize(List<Props> props, double damage) {
        props.sort(Comparator.comparingInt((Props p) -> -p.priority).thenComparingDouble(p -> -p.ratio));
        boolean finished = false;
        int i = 0;
        while (i < props.size()) {
            int j = i, pr = props.get(i).priority;
            while (j < props.size() && props.get(j).priority == pr) j++;
            List<Props> group = props.subList(i, j);
            if (finished) { for (Props p : group) p.ratio = 0; }
            else {
                double total = 0;
                for (Props p : group) total += p.ratio;
                if (total > 1) for (Props p : group) p.ratio /= total;
                total = 0;
                for (Props p : group) {
                    if (damage * p.ratio > p.absorbMax) p.ratio = damage > 0 ? p.absorbMax / damage : 0;
                    total += p.ratio;
                }
                if (total >= 1) finished = true;
            }
            i = j;
        }
    }

    /** Applies the whole armour stage for the hit and returns the damage left; durability is spent as in 1.12. */
    public static float applyArmor(LivingEntity entity, DamageSource source, float amount) {
        HANDLED.put(entity, entity.tickCount);
        double damage = amount;
        double bonusArmor = 0, bonusTough = 0;
        var ai = entity.getAttribute(Attributes.ARMOR);
        var ti = entity.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (ai != null && ai.getModifier(FortressArmorItem.SET_BONUS) != null) bonusArmor = ai.getModifier(FortressArmorItem.SET_BONUS).amount();
        if (ti != null && ti.getModifier(FortressArmorItem.SET_BONUS) != null) bonusTough = ti.getModifier(FortressArmorItem.SET_BONUS).amount();
        double totalArmor = entity.getArmorValue() - bonusArmor;
        double totalToughness = entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS) - bonusTough;
        if (source.is(DamageTypeTags.BYPASSES_ARMOR)) { totalArmor = 0; totalToughness = 0; }
        List<Props> vals = new ArrayList<>();
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!isSpecial(stack)) continue;
            Props p = properties(entity, stack, source);
            totalArmor += p.armor;
            totalToughness += p.toughness;
            p.slot = slot;
            vals.add(p);
        }
        if (!vals.isEmpty()) {
            standardize(vals, damage);
            int level = vals.get(0).priority;
            double ratio = 0;
            for (Props p : vals) {
                if (level != p.priority) { damage -= damage * ratio; ratio = 0; level = p.priority; }
                ratio += p.ratio;
                double absorb = damage * p.ratio;
                if (absorb > 0) {
                    ItemStack stack = entity.getItemBySlot(p.slot);
                    int itemDamage = (int) Math.max(1, absorb);
                    // damageArmor: fall damage never wears these pieces.
                    if (!source.is(DamageTypes.FALL)) stack.hurtAndBreak(itemDamage, entity, p.slot);
                }
            }
            damage -= damage * ratio;
        }
        if (damage > 0 && (totalArmor > 0 || totalToughness > 0)) {
            int armorDamage = (int) Math.max(1.0, damage / 4.0);
            for (EquipmentSlot slot : SLOTS) {
                ItemStack stack = entity.getItemBySlot(slot);
                if (!stack.isEmpty() && stack.has(net.minecraft.core.component.DataComponents.EQUIPPABLE) && stack.isDamageableItem())
                    stack.hurtAndBreak(armorDamage, entity, slot);
            }
            damage = CombatRules.getDamageAfterAbsorb(entity, (float) damage, source, (float) totalArmor, (float) totalToughness);
        }
        return (float) Math.max(0, damage);
    }
    private SpecialArmor() {}
}
