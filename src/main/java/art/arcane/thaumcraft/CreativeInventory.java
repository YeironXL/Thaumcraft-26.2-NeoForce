package art.arcane.thaumcraft;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

/** Stable thematic order; registry iteration order does not define the creative inventory. */
public final class CreativeInventory {
    private static final List<String> BASICS = List.of(
            "salis_mundus", "thaumonomicon", "thaumometer", "scribing_tools", "research_notes", "celestial_notes");
    private static final Set<String> HIDDEN = Set.of(
            "barrier", "empty", "baubles", "chunks", "curio", "golem_placer", "turret_placer", "turret", "golem_builder", "ingots", "label",
            "mind", "modules", "nuggets", "phial", "plate", "seals", "loot_bag", "primordial_pearl", "gear", "firebat_spawn_egg");
    private static final Set<String> MACHINES = Set.of(
            "activator_rail", "alembic", "arcane_ear", "arcane_ear_toggle", "arcane_workbench",
            "arcane_workbench_charger", "bellows", "brain_box", "centrifuge", "condenser",
            "condenser_lattice", "condenser_lattice_dirty", "crucible", "dioptra", "essentia_input",
            "essentia_output", "everfull_urn", "hungry_chest", "infernal_furnace", "infusion_matrix",
            "jar_brain", "jar_normal", "jar_void", "lamp_arcane", "lamp_fertility", "lamp_growth",
            "levitator", "mirror", "mirror_essentia", "pattern_crafter", "potion_sprayer",
            "recharge_pedestal", "redstone_relay", "research_table", "spa", "thaumatorium",
            "thaumatorium_top", "vis_generator", "void_siphon", "wand_workbench");
    private static final List<String> TOOL_PARTS = List.of(
            "axe", "pick", "shovel", "hoe", "sword", "blade", "caster", "focus", "grapple", "hand_mirror");
    private static final List<String> EQUIPMENT_PARTS = List.of(
            "boots", "chest", "helm", "legs", "goggles", "ring", "amulet", "charm", "band", "belt");

    private CreativeInventory() {}

    public static void populate(CreativeModeTab.Output output) {
        BuiltInRegistries.ITEM.stream()
                .filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(Thaumcraft.MOD_ID))
                .filter(item -> isVisible(path(item)))
                .sorted(Comparator.comparingInt(CreativeInventory::group)
                        .thenComparingInt(item -> BASICS.contains(path(item)) ? BASICS.indexOf(path(item)) : 0)
                        .thenComparing(CreativeInventory::path))
                .forEach(item -> {
                    if (item == ModItems.CRYSTAL_ESSENCE.get()) art.arcane.thaumcraft.aspect.AspectRegistry.all().keySet()
                            .forEach(aspect -> output.accept(art.arcane.thaumcraft.item.CrystalEssenceItem.of(aspect, 1)));
                    else if (item == ModItems.PHIAL_FILLED.get()) art.arcane.thaumcraft.aspect.AspectRegistry.all().keySet()
                            .forEach(aspect -> output.accept(art.arcane.thaumcraft.item.EssentiaPhialItem.filled(aspect)));
                    else if (item == ModItems.MANA_BEAN.get()) java.util.List.of("aer","terra","ignis","aqua","ordo","perditio","herba","praecantatio","victus")
                            .forEach(aspect -> output.accept(art.arcane.thaumcraft.item.ManaBeanItem.of(aspect, 1)));
                    else if (item == ModItems.VERDANT_CHARM.get()) for (int t = 0; t < 3; t++) output.accept(t == 0 ? new net.minecraft.world.item.ItemStack(item) : art.arcane.thaumcraft.item.BaubleItem.withType(new net.minecraft.world.item.ItemStack(item), t));
                    else if (item == ModItems.AMULET_VIS.get()) { output.accept(item); output.accept(art.arcane.thaumcraft.item.BaubleItem.withType(new net.minecraft.world.item.ItemStack(item), 1)); }
                    else if (item == ModItems.GOLEM.get()) art.arcane.thaumcraft.golems.GolemPlacerItem.creativeVariants().forEach(output::accept);
                    else output.accept(item);
                });
    }

    private static String path(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }

    private static boolean isVisible(String id) {
        return !HIDDEN.contains(id) && !id.startsWith("effect_") && !id.startsWith("placeholder_");
    }

    private static int group(Item item) {
        String id = path(item);
        if (BASICS.contains(id)) return 0;
        if (id.startsWith("creative_")) return 9;
        if (id.startsWith("golem_") || id.startsWith("seal_") || id.startsWith("mind_")
                || id.startsWith("module_") || id.startsWith("turret")) return 6;
        if (id.startsWith("loot_") || id.startsWith("curio_")) return 8;
        if (item instanceof BlockItem) {
            if (MACHINES.contains(id) || id.startsWith("tube") || id.startsWith("smelter")
                    || id.startsWith("stabilizer") || id.startsWith("matrix_")
                    || id.startsWith("pedestal_") || id.startsWith("vis_battery")) return 5;
            return 4;
        }
        if (TOOL_PARTS.stream().anyMatch(id::contains)) return 2;
        if (EQUIPMENT_PARTS.stream().anyMatch(id::contains)) return 3;
        if (id.equals("sanity_checker") || id.equals("sanity_soap") || id.equals("bath_salts")
                || id.equals("triple_meat_treat") || id.startsWith("bucket_") || id.equals("bottle_taint")) return 7;
        return 1;
    }
}
