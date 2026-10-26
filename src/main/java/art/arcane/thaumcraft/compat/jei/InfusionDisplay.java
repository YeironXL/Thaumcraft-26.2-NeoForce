package art.arcane.thaumcraft.compat.jei;

import art.arcane.thaumcraft.infusion.InfusionEnchantmentRecipe;
import art.arcane.thaumcraft.infusion.InfusionRecipe;
import art.arcane.thaumcraft.infusion.InfusionRunicAugmentRecipe;
import art.arcane.thaumcraft.infusion.StandardInfusionRecipe;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * One infusion as JEI shows it. Standard recipes map one to one; infusion enchantments and the runic augment are dynamic
 * (they apply to any suitable item), so they are shown on example items with the cost of a first application.
 */
record InfusionDisplay(Identifier id, List<ItemStack> central, List<List<ItemStack>> components, Map<String, Integer> aspects,
                       int instability, List<ItemStack> outputs, String research, String note) {
    static final int MAX_EXAMPLES = 64;

    static List<InfusionDisplay> of(RecipeHolder<InfusionRecipe> holder) {
        var r = holder.value();
        Identifier id = holder.id().identifier();
        try {
            if (r instanceof StandardInfusionRecipe s) {
                var central = ArcaneCategory.stacks(s.input());
                if (central.isEmpty()) return List.of();
                var comps = s.components().stream().map(ArcaneCategory::stacks).toList();
                List<ItemStack> outs = new ArrayList<>(), ins = new ArrayList<>();
                List<ItemStack> sample = comps.stream().map(l -> l.isEmpty() ? ItemStack.EMPTY : l.get(0)).toList();
                if (s.result().isPresent()) { ins.addAll(central); outs.add(s.result().get().create()); }
                else for (var c : central) { ins.add(c); outs.add(s.output(null, c, sample).stack()); }
                return List.of(new InfusionDisplay(id, ins, comps, s.aspectCost(), s.instability(), outs, s.research(), ""));
            }
            if (r instanceof InfusionEnchantmentRecipe e) {
                List<ItemStack> ins = new ArrayList<>(), outs = new ArrayList<>();
                for (var item : BuiltInRegistries.ITEM) {
                    var st = new ItemStack(item);
                    if (st.isEmpty() || !e.enchantment().appliesTo(st)) continue;
                    var comps = e.components().stream().map(c -> ArcaneCategory.stacks(c).stream().findFirst().orElse(ItemStack.EMPTY)).toList();
                    var out = e.output(null, st, comps);
                    if (out == null) continue;
                    ins.add(st); outs.add(stripWarp(out.stack()));
                    if (ins.size() >= MAX_EXAMPLES) break;
                }
                if (ins.isEmpty()) return List.of();
                var comps = e.components().stream().map(ArcaneCategory::stacks).toList();
                return List.of(new InfusionDisplay(id, ins, comps, e.aspects(null, ins.get(0), List.of()), e.instability(null, ins.get(0), List.of()), outs, e.research(), "jei.thaumcraft.infusion.enchantment"));
            }
            if (r instanceof InfusionRunicAugmentRecipe a) {
                List<ItemStack> ins = new ArrayList<>(), outs = new ArrayList<>();
                for (var item : BuiltInRegistries.ITEM) {
                    var st = new ItemStack(item);
                    if (st.isEmpty() || !InfusionRunicAugmentRecipe.isArmorOrBauble(st)) continue;
                    ins.add(st); outs.add(a.output(null, st, List.of()).stack());
                    if (ins.size() >= MAX_EXAMPLES) break;
                }
                if (ins.isEmpty()) return List.of();
                var comps = InfusionRunicAugmentRecipe.components(ins.get(0)).stream().map(ArcaneCategory::stacks).toList();
                return List.of(new InfusionDisplay(id, ins, comps, a.aspects(null, ins.get(0), List.of()), a.instability(null, ins.get(0), List.of()), outs, a.research(), "jei.thaumcraft.infusion.runic"));
            }
        } catch (RuntimeException ex) {
            com.mojang.logging.LogUtils.getLogger().warn("JEI: skipping infusion recipe {}: {}", id, ex.toString());
        }
        return List.of();
    }

    /** The warping roll of InfusionEnchantmentRecipe.output is random; examples show the plain result. */
    private static ItemStack stripWarp(ItemStack s) {
        var data = s.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data != null && data.copyTag().contains("TC.WARP")) {
            var tag = data.copyTag(); tag.remove("TC.WARP");
            if (tag.isEmpty()) s.remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            else s.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        }
        return s;
    }

    Map<String, Integer> orderedAspects() { return new LinkedHashMap<>(aspects); }
}
