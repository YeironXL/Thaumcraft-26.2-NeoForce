package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.aspect.ObjectAspects;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.Map;

/** TC6 object aspects: ConfigAspects replay plus recipe generation, checked against original values. */
final class ObjectAspectsVerification {
    private static void check(boolean c, String m) { if (!c) throw new AssertionError(m); }
    private static Map<String, Integer> tags(net.minecraft.world.item.Item item) {
        var out = new java.util.LinkedHashMap<String, Integer>();
        ObjectAspects.get(new ItemStack(item)).forEach((a, n) -> out.put(a.id(), n));
        return out;
    }
    private static final java.util.Set<String> TECHNICAL = java.util.Set.of("air", "barrier", "light", "structure_void", "structure_block", "jigsaw",
            "command_block", "chain_command_block", "repeating_command_block", "command_block_minecart", "debug_stick", "test_block", "test_instance_block", "knowledge_book");
    private static Map<String, Integer> mc(String path) { return tags(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(path))); }
    static void test(GameTestHelper h) {
        ObjectAspects.rebuild(h.getLevel().getServer());
        try (var w = new java.io.FileWriter("aspect_table.json")) {
            var all = new java.util.TreeMap<String, Map<String, Integer>>();
            for (var id : net.minecraft.core.registries.BuiltInRegistries.ITEM.keySet()) {
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id);
                var t = tags(item); if (!t.isEmpty()) all.put(id.toString(), t);
            }
            try (var mw = new java.io.FileWriter("aspect_missing.txt")) {
                for (var id : new java.util.TreeSet<>(net.minecraft.core.registries.BuiltInRegistries.ITEM.keySet()))
                    if (id.getNamespace().equals("minecraft") && !all.containsKey(id.toString())) mw.write(id + "\n");
            }
            w.write(new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(all));
        } catch (java.io.IOException e) { throw new RuntimeException(e); }
        check(tags(Items.STONE).equals(Map.of("terra", 5)), "stone " + tags(Items.STONE));
        check(tags(Items.COBBLESTONE).equals(Map.of("terra", 5, "perditio", 1)), "cobble " + tags(Items.COBBLESTONE));
        check(tags(Items.OAK_PLANKS).equals(Map.of("herba", 3)), "planks " + tags(Items.OAK_PLANKS));
        check(tags(Items.STICK).equals(Map.of("herba", 1)), "stick " + tags(Items.STICK));
        check(tags(Items.CRAFTING_TABLE).equals(Map.of("herba", 9, "fabrico", 20)), "table " + tags(Items.CRAFTING_TABLE));
        check(tags(Items.IRON_INGOT).equals(Map.of("metallum", 15)), "iron " + tags(Items.IRON_INGOT));
        check(tags(Items.WOODEN_SWORD).getOrDefault("aversio", 0) == 4, "wood sword " + tags(Items.WOODEN_SWORD));
        check(tags(Items.DIAMOND_PICKAXE).getOrDefault("instrumentum", 0) == 16, "diamond pick " + tags(Items.DIAMOND_PICKAXE));
        // Every survival vanilla item has aspects (TC6 gave none to spawn eggs or operator/technical items).
        var missing = new java.util.TreeSet<String>();
        for (var id : net.minecraft.core.registries.BuiltInRegistries.ITEM.keySet()) {
            if (!id.getNamespace().equals("minecraft") || TECHNICAL.contains(id.getPath()) || id.getPath().endsWith("_spawn_egg")) continue;
            if (tags(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id)).isEmpty()) missing.add(id.getPath());
        }
        check(missing.isEmpty(), missing.size() + " vanilla items without aspects: " + missing);
        check(tags(Items.DEEPSLATE).equals(Map.of("terra", 5)), "deepslate " + tags(Items.DEEPSLATE));
        check(tags(Items.AMETHYST_SHARD).equals(Map.of("vitreus", 5, "praecantatio", 2)), "amethyst " + tags(Items.AMETHYST_SHARD));
        check(tags(Items.RAW_COPPER).equals(Map.of("metallum", 7, "permutatio", 3, "terra", 2)), "raw copper " + tags(Items.RAW_COPPER));
        check(tags(Items.NETHERITE_INGOT).equals(Map.of("metallum", 30, "praemunio", 15, "desiderium", 15, "ignis", 10)), "netherite " + tags(Items.NETHERITE_INGOT));
        // smithing as a recipe: (diamond sword vitreus 22 desiderium 22 + ingot) x0.75, plus the sword bonus aversio 20
        check(tags(Items.NETHERITE_SWORD).equals(Map.of("vitreus", 16, "desiderium", 27, "metallum", 22, "praemunio", 11, "ignis", 7, "aversio", 20)), "netherite sword " + tags(Items.NETHERITE_SWORD));
        check(tags(Items.SCULK_SENSOR).equals(Map.of("sensus", 15, "tenebrae", 5, "exanimis", 5, "potentia", 5)), "sculk sensor " + tags(Items.SCULK_SENSOR));
        check(tags(Items.CHERRY_LOG).equals(Map.of("herba", 20)), "cherry log " + tags(Items.CHERRY_LOG));
        check(tags(Items.MUSIC_DISC_PIGSTEP).equals(Map.of("sensus", 15, "aer", 5, "bestia", 5, "desiderium", 10)), "pigstep " + tags(Items.MUSIC_DISC_PIGSTEP));
        var copper = new java.util.HashMap<>(mc("copper_block")); copper.merge("perditio", 3, Integer::sum);
        check(mc("oxidized_copper").equals(copper), "oxidized copper " + mc("oxidized_copper") + " vs " + copper);
        var crystal = art.arcane.thaumcraft.item.CrystalEssenceItem.of("ignis", 1);
        check(ObjectAspects.get(crystal).equals(Map.of(AspectRegistry.IGNIS, 1)), "crystal " + ObjectAspects.get(crystal));
        h.succeed();
        System.out.println("Object aspects passed: " + ObjectAspects.table().size() + " registered entries; sample book=" + tags(Items.BOOK) + " bucket=" + tags(Items.BUCKET));
    }
}
