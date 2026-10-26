package art.arcane.thaumcraft.client.book;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.client.ClientResearch;
import art.arcane.thaumcraft.crafting.multiblock.DustTriggerMultiblock;
import art.arcane.thaumcraft.crafting.multiblock.Multiblocks;
import art.arcane.thaumcraft.crafting.multiblock.Part;
import art.arcane.thaumcraft.research.ResearchCatalog;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Client view of TC6's recipe catalog as the Thaumonomicon sees it (CommonInternals catalog, fake recipes,
 * CraftingManager and ConfigRecipes.recipeGroups, in that lookup order). Recipes are read from the exact JSON
 * shipped in the mod jar so the book works on any server.
 */
public final class BookRecipes {
    public enum Kind { SHAPED, SHAPELESS, ARCANE, ARCANE_SHAPELESS, CRUCIBLE, INFUSION, BLUEPRINT }

    /** One displayable recipe. For BLUEPRINT, json is null and blueprint is set. */
    public record Recipe(Kind kind, JsonObject json, Blueprint blueprint) {
        public String research() {
            if (blueprint != null) return blueprint.research();
            return json != null && json.has("research") ? json.get("research").getAsString() : null;
        }
        public ItemStack output() {
            if (blueprint != null) return blueprint.display() == null ? ItemStack.EMPTY : blueprint.display();
            return stack(json.getAsJsonObject("result"));
        }
    }

    /** ThaumcraftApi.BluePrint. */
    public record Blueprint(String research, ItemStack display, Part[][][] parts, List<ItemStack> ingredients) {}

    private static final Map<String, Object> CACHE = new HashMap<>();
    private static final Object NONE = new Object();
    private static Map<String, List<String>> groups;
    private static Map<String, Blueprint> blueprints;

    /** Normalised catalog key: namespace + lower-cased path ("thaumcraft:StoneArcane" -> "thaumcraft:stonearcane"). */
    public static String norm(String raw) {
        int c = raw.indexOf(':');
        String ns = c < 0 ? "minecraft" : raw.substring(0, c).toLowerCase(Locale.ROOT);
        return ns + ":" + (c < 0 ? raw : raw.substring(c + 1)).toLowerCase(Locale.ROOT);
    }

    /** CommonInternals.getCatalogRecipe / getCatalogRecipeFake / CraftingManager.getRecipe / recipeGroups. Returns Recipe, List&lt;String&gt; (group) or null. */
    public static Object lookup(String raw) {
        String key = norm(raw);
        Object cached = CACHE.get(key);
        if (cached != null) return cached == NONE ? null : cached;
        Object found = resolve(key);
        CACHE.put(key, found == null ? NONE : found);
        return found;
    }

    private static Object resolve(String key) {
        String ns = key.substring(0, key.indexOf(':')), path = key.substring(key.indexOf(':') + 1);
        Blueprint bp = blueprints().get(key);
        if (bp != null) return new Recipe(Kind.BLUEPRINT, null, bp);
        JsonObject data = read("/data/" + ns + "/recipe/" + path + ".json");
        Recipe catalog = data == null ? null : parse(data, true);
        if (catalog != null) return catalog;
        JsonObject fake = read("/assets/" + ns + "/book_recipes/" + path + ".json");
        Recipe fr = fake == null ? null : parse(fake, false);
        if (fr != null) return fr;
        Recipe crafting = data == null ? null : parse(data, false);
        if (crafting != null) return crafting;
        List<String> group = groups().get(path);
        if (group != null && ns.equals("thaumcraft")) {
            List<String> out = new ArrayList<>();
            for (String g : group) out.add(ns + ":" + g);
            return out;
        }
        return null;
    }

    private static Recipe parse(JsonObject json, boolean thaumcraftOnly) {
        String type = json.has("type") ? json.get("type").getAsString() : "";
        Kind kind = switch (type) {
            case "thaumcraft:arcane" -> Kind.ARCANE;
            case "thaumcraft:shapeless_arcane" -> Kind.ARCANE_SHAPELESS;
            case "thaumcraft:crucible" -> Kind.CRUCIBLE;
            case "thaumcraft:infusion" -> Kind.INFUSION;
            case "minecraft:crafting_shaped" -> thaumcraftOnly ? null : Kind.SHAPED;
            case "minecraft:crafting_shapeless" -> thaumcraftOnly ? null : Kind.SHAPELESS;
            default -> null;
        };
        if (kind == null || !json.has("result")) return null;
        return new Recipe(kind, json, null);
    }

    private static JsonObject read(String path) {
        InputStream in = BookRecipes.class.getResourceAsStream(path);
        if (in == null) in = net.minecraft.client.Minecraft.class.getResourceAsStream(path);
        if (in == null) return null;
        try (var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) { return null; }
    }

    private static Map<String, List<String>> groups() {
        if (groups == null) {
            groups = new HashMap<>();
            JsonObject json = read("/assets/thaumcraft/book_groups.json");
            if (json != null) for (var e : json.entrySet()) {
                List<String> l = new ArrayList<>();
                for (JsonElement x : e.getValue().getAsJsonArray()) l.add(x.getAsString().toLowerCase(Locale.ROOT));
                groups.put(e.getKey().toLowerCase(Locale.ROOT), l);
            }
        }
        return groups;
    }

    /** ConfigRecipes.initializeCompoundRecipes catalog entries. */
    private static Map<String, Blueprint> blueprints() {
        if (blueprints == null) {
            Map<String, Blueprint> m = new LinkedHashMap<>();
            Multiblocks.init();
            Map<String, Part[][][]> byResearch = new HashMap<>();
            for (DustTriggerMultiblock t : DustTriggerMultiblock.TRIGGERS) byResearch.putIfAbsent(t.research(), t.blueprint());
            put(m, "thaumcraft:infernalfurnace", "INFERNALFURNACE", null, byResearch, st(Blocks.NETHER_BRICKS, 12), st(Blocks.OBSIDIAN, 12), st(Blocks.IRON_BARS, 1), new ItemStack(Items.LAVA_BUCKET));
            put(m, "thaumcraft:infusionaltar", "INFUSION", null, byResearch, st(ModBlocks.STONE_ARCANE.get(), 8), st(ModBlocks.PEDESTAL_ARCANE.get(), 1), st(ModBlocks.INFUSION_MATRIX.get(), 1));
            put(m, "thaumcraft:infusionaltarancient", "INFUSIONANCIENT", null, byResearch, st(ModBlocks.STONE_ANCIENT.get(), 8), st(ModBlocks.PEDESTAL_ANCIENT.get(), 1), st(ModBlocks.INFUSION_MATRIX.get(), 1));
            put(m, "thaumcraft:infusionaltareldritch", "INFUSIONELDRITCH", null, byResearch, st(ModBlocks.STONE_ELDRITCH_TILE.get(), 8), st(ModBlocks.PEDESTAL_ELDRITCH.get(), 1), st(ModBlocks.INFUSION_MATRIX.get(), 1));
            put(m, "thaumcraft:thaumatorium", "THAUMATORIUM", null, byResearch, st(ModBlocks.METAL_ALCHEMICAL.get(), 2), st(ModBlocks.CRUCIBLE.get(), 1));
            put(m, "thaumcraft:golempress", "MINDCLOCKWORK", st(ModBlocks.GOLEM_BUILDER.get(), 1), byResearch, st(Blocks.IRON_BARS, 1), new ItemStack(Items.CAULDRON), st(Blocks.PISTON, 1), st(Blocks.ANVIL, 1), st(ModBlocks.TABLE_STONE.get(), 1));
            blueprints = m;
        }
        return blueprints;
    }

    private static void put(Map<String, Blueprint> m, String id, String research, ItemStack display, Map<String, Part[][][]> parts, ItemStack... ingredients) {
        Part[][][] p = parts.get(research);
        if (p != null) m.put(id, new Blueprint(research, display, p, List.of(ingredients)));
    }

    private static ItemStack st(Block b, int n) { return new ItemStack(b.asItem(), n); }

    // ---------------------------------------------------------------- ingredients

    public static ItemStack stack(JsonObject result) {
        if (result == null || !result.has("id")) return ItemStack.EMPTY;
        var id = Identifier.tryParse(result.get("id").getAsString());
        if (id == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.getValue(id);
        if (item == null || item == Items.AIR) return ItemStack.EMPTY;
        ItemStack s = new ItemStack(item, result.has("count") ? result.get("count").getAsInt() : 1);
        if (result.has("components")) applyComponents(s, result.getAsJsonObject("components"));
        return s;
    }

    /** Result / ingredient component patch (crystal aspect, infusion enchantments, runic charge, variant, custom data...). */
    private static void applyComponents(ItemStack s, JsonObject comps) {
        if (comps.has("thaumcraft:aspect")) s.set(art.arcane.thaumcraft.registry.ModComponents.ASPECT.get(), comps.get("thaumcraft:aspect").getAsString());
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null) return;
        net.minecraft.core.component.DataComponentPatch.CODEC.parse(net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, level.registryAccess()), comps)
                .result().ifPresent(s::applyComponents);
    }

    /** Ingredient.getMatchingStacks. */
    public static List<ItemStack> options(JsonElement e) {
        if (e == null || e.isJsonNull()) return List.of();
        if (e.isJsonPrimitive()) return options(e.getAsString());
        if (e.isJsonArray()) { var out = new ArrayList<ItemStack>(); for (var x : e.getAsJsonArray()) out.addAll(options(x)); return out; }
        var o = e.getAsJsonObject();
        if (o.has("id") && !o.has("items")) { ItemStack s = stack(o); return s.isEmpty() ? List.of() : List.of(s); }
        if (!o.has("items")) return List.of();
        var base = options(o.get("items"));
        if (!o.has("components")) return base;
        var comps = o.getAsJsonObject("components");
        var out = new ArrayList<ItemStack>();
        for (var b : base) {
            var c = b.copy();
            applyComponents(c, comps);
            out.add(c);
        }
        return out;
    }

    public static List<ItemStack> options(String raw) {
        if (!raw.startsWith("#")) {
            var id = Identifier.tryParse(raw);
            if (id == null) return List.of();
            Item item = BuiltInRegistries.ITEM.getValue(id);
            return item == null || item == Items.AIR ? List.of() : List.of(new ItemStack(item));
        }
        var id = Identifier.tryParse(raw.substring(1));
        if (id == null) return List.of();
        var result = new ArrayList<ItemStack>();
        for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, id)))
            if (holder.value() != Items.AIR) result.add(new ItemStack(holder.value()));
        return result;
    }

    /** InventoryUtils.cycleItemStack(input, counter). */
    public static ItemStack cycle(List<ItemStack> options, int counter) {
        if (options.isEmpty()) return ItemStack.EMPTY;
        int idx = (int) ((counter + System.currentTimeMillis() / 1000L) % options.size());
        return options.get(idx);
    }

    public static ItemStack cycle(JsonElement ingredient, int counter) { return cycle(options(ingredient), counter); }

    // ---------------------------------------------------------------- clickthrough

    private static final Map<String, String> KEY_CACHE = new HashMap<>();
    public static void clearKeyCache() { KEY_CACHE.clear(); }

    private static boolean itemEqual(ItemStack a, ItemStack b) { return !a.isEmpty() && !b.isEmpty() && a.getItem() == b.getItem(); }

    /** GuiResearchPage.findRecipePage. */
    static int findRecipePage(String rk, ItemStack stack, int start) {
        Object recipe = lookup(rk);
        if (recipe == null) return -1;
        if (recipe instanceof List<?> list) {
            int g = 0;
            for (Object rl : list) {
                int q = findRecipePage((String) rl, stack, g);
                if (q >= 0) return q;
                ++g;
            }
            return -1;
        }
        Recipe r = (Recipe) recipe;
        if (r.kind() == Kind.BLUEPRINT) return -1;
        if (!itemEqual(r.output(), stack)) return -1;
        if (r.kind() == Kind.CRUCIBLE || r.kind() == Kind.INFUSION || r.kind() == Kind.ARCANE || r.kind() == Kind.ARCANE_SHAPELESS) {
            if (!ClientResearch.knows(r.research())) return -99;
        }
        return start;
    }

    /** GuiResearchPage.getCraftingRecipeKey: "recipe;page", "minecraft:UNKNOWN", or null. */
    public static String craftingRecipeKey(ItemStack stack) {
        String key = BuiltInRegistries.ITEM.getKey(stack.getItem()) + "|" + stack.getComponentsPatch();
        if (KEY_CACHE.containsKey(key)) return KEY_CACHE.get(key);
        for (var entry : ResearchCatalog.entries().values()) {
            for (var stage : entry.stages()) {
                for (String rec : stage.recipes()) {
                    int result = findRecipePage(rec, stack, 0);
                    if (result != -1) {
                        String s = result == -99 ? "minecraft:UNKNOWN" : rec + ";" + result;
                        KEY_CACHE.put(key, s);
                        return s;
                    }
                }
            }
        }
        KEY_CACHE.put(key, null);
        return null;
    }

    private BookRecipes() {}
}
