package art.arcane.thaumcraft.infusion;

import art.arcane.thaumcraft.registry.ModComponents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.Blocks;

/**
 * TC6 EnumInfusionEnchantment. Enchantments live in the {@code thaumcraft:infusion_enchantments} component (TC6 "infench"
 * NBT list); the id is the lower-case enum name.
 */
public enum InfusionEnchantment {
    COLLECTOR(Set.of("axe", "pickaxe", "shovel", "weapon"), 1, "INFUSIONENCHANTMENT"),
    DESTRUCTIVE(Set.of("axe", "pickaxe", "shovel"), 1, "INFUSIONENCHANTMENT"),
    BURROWING(Set.of("axe", "pickaxe"), 1, "INFUSIONENCHANTMENT"),
    SOUNDING(Set.of("pickaxe"), 4, "INFUSIONENCHANTMENT"),
    REFINING(Set.of("pickaxe"), 4, "INFUSIONENCHANTMENT"),
    ARCING(Set.of("weapon"), 4, "INFUSIONENCHANTMENT"),
    ESSENCE(Set.of("weapon"), 5, "INFUSIONENCHANTMENT"),
    VISBATTERY(Set.of("chargable"), 3, "?"),
    VISCHARGE(Set.of("chargable"), 1, "?"),
    SWIFT(Set.of("boots"), 4, "IEARMOR"),
    AGILE(Set.of("legs"), 1, "IEARMOR"),
    INFESTED(Set.of("chest"), 1, "IETAINT"),
    LAMPLIGHT(Set.of("axe", "pickaxe", "shovel"), 1, "INFUSIONENCHANTMENT");

    public final Set<String> toolClasses;
    public final int maxLevel;
    public final String research;

    InfusionEnchantment(Set<String> toolClasses, int maxLevel, String research) {
        this.toolClasses = toolClasses;
        this.maxLevel = maxLevel;
        this.research = research;
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }
    /** Lang key used by the TC6 tooltip ("enchantment.infusion.COLLECTOR"). */
    public String translationKey() { return "enchantment.infusion." + name(); }

    public static InfusionEnchantment byId(String id) {
        for (var e : values()) if (e.id().equals(id) || e.name().equals(id)) return e;
        return null;
    }

    public record Entry(String id, int lvl) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(Entry::id),
                Codec.intRange(0, 127).fieldOf("lvl").forGetter(Entry::lvl)).apply(i, Entry::new));
    }

    public static List<Entry> entries(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return List.of();
        return stack.getOrDefault(ModComponents.INFUSION_ENCHANTMENTS.get(), List.of());
    }

    /** getInfusionEnchantments. */
    public static List<InfusionEnchantment> getInfusionEnchantments(ItemStack stack) {
        List<InfusionEnchantment> out = new ArrayList<>();
        for (var e : entries(stack)) { var ie = byId(e.id()); if (ie != null) out.add(ie); }
        return out;
    }

    /** getInfusionEnchantmentLevel. */
    public static int getInfusionEnchantmentLevel(ItemStack stack, InfusionEnchantment ench) {
        for (var e : entries(stack)) if (byId(e.id()) == ench) return e.lvl();
        return 0;
    }

    /** addInfusionEnchantment: only raises the level, never above the maximum. */
    public static void addInfusionEnchantment(ItemStack stack, InfusionEnchantment ie, int level) {
        if (stack == null || stack.isEmpty() || level > ie.maxLevel) return;
        List<Entry> list = new ArrayList<>(entries(stack));
        for (int j = 0; j < list.size(); ++j) {
            if (byId(list.get(j).id()) == ie) {
                if (level <= list.get(j).lvl()) return;
                list.set(j, new Entry(ie.id(), level));
                stack.set(ModComponents.INFUSION_ENCHANTMENTS.get(), List.copyOf(list));
                return;
            }
        }
        list.add(new Entry(ie.id(), level));
        stack.set(ModComponents.INFUSION_ENCHANTMENTS.get(), List.copyOf(list));
    }

    /**
     * InfusionEnchantmentRecipe.matches tool-class test. 1.12 tool classes are derived from what the item actually does:
     * attack damage modifiers (weapon), mining speed on stone/logs/dirt (pickaxe/axe/shovel) and the equippable slot (armor).
     */
    public boolean appliesTo(ItemStack central) {
        if (toolClasses.contains("all")) return true;
        if (toolClasses.contains("weapon")) {
            var mods = central.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
            for (var m : mods.modifiers()) if (m.attribute().is(Attributes.ATTACK_DAMAGE) && m.slot().test(EquipmentSlot.MAINHAND)) return true;
        }
        if (toolClasses.contains("pickaxe") && central.getDestroySpeed(Blocks.STONE.defaultBlockState()) > 1.0f) return true;
        if (toolClasses.contains("axe") && central.getDestroySpeed(Blocks.OAK_LOG.defaultBlockState()) > 1.0f) return true;
        if (toolClasses.contains("shovel") && central.getDestroySpeed(Blocks.DIRT.defaultBlockState()) > 1.0f) return true;
        var eq = central.get(DataComponents.EQUIPPABLE);
        if (eq != null) {
            String at = switch (eq.slot()) { case HEAD -> "helm"; case CHEST -> "chest"; case LEGS -> "legs"; case FEET -> "boots"; default -> "none"; };
            if (!at.equals("none") && (toolClasses.contains("armor") || toolClasses.contains(at))) return true;
        }
        return toolClasses.contains("chargable") && central.getItem() instanceof art.arcane.thaumcraft.item.IRechargable;
    }
}
