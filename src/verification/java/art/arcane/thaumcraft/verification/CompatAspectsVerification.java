package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.aspect.ObjectAspects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.util.Map;

/** Optional aspect data (aspects/compat/*.json): common c:* tags apply to any item, absent mods and unknown ids are skipped. */
final class CompatAspectsVerification {
    private static void check(boolean c, String m) { if (!c) throw new AssertionError(m); }
    private static Map<String, Integer> tags(Item item) {
        var out = new java.util.LinkedHashMap<String, Integer>();
        ObjectAspects.get(new ItemStack(item)).forEach((a, n) -> out.put(a.id(), n));
        return out;
    }
    static void test(GameTestHelper h) {
        ObjectAspects.rebuild(h.getLevel().getServer());
        // Verification datapack puts flower_banner_pattern in c:ingots/tin and creeper_banner_pattern in c:raw_materials/zinc.
        check(new ItemStack(Items.FLOWER_BANNER_PATTERN).is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:ingots/tin"))), "test tag not loaded");
        check(tags(Items.FLOWER_BANNER_PATTERN).equals(Map.of("metallum", 10, "vitreus", 5)), "c:ingots/tin item " + tags(Items.FLOWER_BANNER_PATTERN));
        check(tags(Items.CREEPER_BANNER_PATTERN).equals(Map.of("metallum", 7, "permutatio", 3, "terra", 2)), "c:raw_materials/zinc item " + tags(Items.CREEPER_BANNER_PATTERN));
        // Entries gated on an absent mod are not applied; unknown ids/tags/aspects do not break loading.
        check(tags(Items.STICK).equals(Map.of("herba", 1)), "gated entry applied to stick " + tags(Items.STICK));
        var table = ObjectAspects.table();
        for (var ns : new String[]{"create", "mekanism", "farmersdelight", "nonexistent_mod"})
            check(table.keySet().stream().noneMatch(k -> k.startsWith(ns + ":")), "entries registered for absent mod " + ns);
        check(BuiltInRegistries.ITEM.keySet().size() > 0, "registry");
        System.out.println("Compat aspects passed: c:ingots/tin and c:raw_materials/zinc applied to tagged items, absent-mod files and unknown ids skipped.");
        h.succeed();
    }
}
