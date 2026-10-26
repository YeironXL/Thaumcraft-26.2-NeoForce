package art.arcane.thaumcraft.aspect;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.registry.ModComponents;
import art.arcane.thaumcraft.registry.ModRecipes;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Port of TC6 object aspects: ConfigAspects.registerItemAspects (replayed in order from
 * data/thaumcraft/aspects/objects.json), ThaumcraftCraftingManager.getObjectTags/generateTags/getBonusTags and
 * AspectHelper.cullTags. The registered table is built on the server (it needs recipes) and synchronised to clients;
 * the bonus tags (essentia contents, armour, weapons, tools, dyes, enchantments) are applied on both sides.
 */
public final class ObjectAspects {
    public static final int ASPECT_CAP = 500;
    private static volatile Map<String, Map<Aspect, Integer>> registered = Map.of();

    // ---------------------------------------------------------------- queries
    /** TC6 AspectHelper.getObjectAspects / ThaumcraftCraftingManager.getObjectTags (registered table only). */
    public static Map<Aspect, Integer> get(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Map.of();
        ensureBuilt();
        var base = lookup(registered, stack);
        return Collections.unmodifiableMap(cap(bonus(stack, base == null ? null : new LinkedHashMap<>(base)), ASPECT_CAP));
    }
    public static Map<Aspect, Integer> get(Item item) { return get(new ItemStack(item)); }
    /** Registered tags without bonus, by plain item id. */
    public static Map<Aspect, Integer> registered(String key) { return registered.getOrDefault(key, Map.of()); }
    public static Map<String, Map<Aspect, Integer>> table() { ensureBuilt(); return registered; }
    /** The table needs loaded recipes; build it on first server-side use if no lifecycle event did yet. */
    private static boolean building;
    private static void ensureBuilt() {
        if (!registered.isEmpty() || building) return;
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.isSameThread()) rebuild(server);
    }
    public static boolean exists(ItemStack stack) { return lookup(registered, stack) != null; }

    public static String key(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
        if (potion != null && potion.potion().isPresent()) id += "|" + potion.potion().get().getRegisteredName();
        return id;
    }
    private static Map<Aspect, Integer> lookup(Map<String, Map<Aspect, Integer>> table, ItemStack stack) {
        var exact = table.get(key(stack));
        if (exact != null) return exact;
        return table.get(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    // ---------------------------------------------------------------- client sync
    public static void install(Map<String, Map<String, Integer>> values) {
        var copy = new LinkedHashMap<String, Map<Aspect, Integer>>();
        values.forEach((id, tags) -> {
            var mapped = new LinkedHashMap<Aspect, Integer>();
            tags.forEach((tag, amount) -> { var a = AspectRegistry.get(tag); if (a != null && amount > 0) mapped.put(a, amount); });
            copy.put(id, Collections.unmodifiableMap(mapped));
        });
        registered = Collections.unmodifiableMap(copy);
    }
    public static Map<String, Map<String, Integer>> export() {
        var out = new LinkedHashMap<String, Map<String, Integer>>();
        registered.forEach((id, tags) -> { var m = new LinkedHashMap<String, Integer>(); tags.forEach((a, n) -> m.put(a.id(), n)); out.put(id, m); });
        return out;
    }
    public static void clear() { registered = Map.of(); }

    // ---------------------------------------------------------------- server build
    public static synchronized void rebuild(MinecraftServer server) {
        building = true;
        try { build(server); } finally { building = false; }
    }
    private static void build(MinecraftServer server) {
        var engine = new Engine(server.getRecipeManager(), server.potionBrewing());
        engine.replay();
        // TC6 generated lazily on every query; generate now for every item so the client table is complete.
        var ids = new ArrayList<>(BuiltInRegistries.ITEM.keySet());
        ids.sort(Comparator.comparing(Identifier::toString));
        for (var id : ids) {
            var item = BuiltInRegistries.ITEM.getValue(id);
            if (item != null && item != Items.AIR) engine.getObjectTags(new ItemStack(item), new ArrayList<>());
        }
        var frozen = new LinkedHashMap<String, Map<Aspect, Integer>>();
        engine.tags.forEach((k, v) -> frozen.put(k, Collections.unmodifiableMap(new LinkedHashMap<>(v))));
        registered = Collections.unmodifiableMap(frozen);
    }

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    static final class Engine {
        final LinkedHashMap<String, LinkedHashMap<Aspect, Integer>> tags = new LinkedHashMap<>();
        final List<RecipeHolder<?>> crafting = new ArrayList<>();
        final List<art.arcane.thaumcraft.crafting.CrucibleRecipe> crucible = new ArrayList<>();
        final PotionBrewing brewing;
        Engine(RecipeManager manager, PotionBrewing brewing) {
            this.brewing = brewing;
            var map = manager.recipeMap();
            map.byType(RecipeType.CRAFTING).forEach(crafting::add);
            map.byType(ModRecipes.ARCANE_TYPE.get()).forEach(crafting::add);
            crafting.sort(Comparator.comparing(h -> h.id().identifier().toString()));
            var cr = new ArrayList<>(map.byType(ModRecipes.CRUCIBLE_TYPE.get()));
            cr.sort(Comparator.comparing(h -> h.id().identifier().toString()));
            cr.forEach(h -> crucible.add(h.value()));
        }

        /** Items only registered as a side effect of generating another item while ConfigAspects is replayed. */
        final Set<String> provisional = new HashSet<>();
        boolean exists(ItemStack stack) { return tags.containsKey(key(stack)); }
        void register(ItemStack stack, Map<Aspect, Integer> aspects) {
            tags.put(key(stack), aspects == null ? new LinkedHashMap<>() : new LinkedHashMap<>(aspects));
            provisional.remove(key(stack));
        }

        LinkedHashMap<Aspect, Integer> getObjectTags(ItemStack stack, List<String> history) {
            if (stack.isEmpty()) return null;
            var tmp = tags.get(key(stack));
            LinkedHashMap<Aspect, Integer> base = tmp == null ? null : new LinkedHashMap<>(tmp);
            if (tmp == null) base = generateTags(stack, history);
            return cap(bonus(stack, base), ASPECT_CAP);
        }

        LinkedHashMap<Aspect, Integer> generateTags(ItemStack is, List<String> history) {
            ItemStack stack = is.copyWithCount(1);
            if (exists(stack)) return getObjectTags(stack, history);
            String ss = key(stack);
            if (history.contains(ss)) return null;
            if (System.getProperty("thaumcraft.debugAspects") != null && ss.equals(System.getProperty("thaumcraft.debugAspects"))) System.out.println("DEBUGASPECT generating " + ss + " via " + history);
            history.add(ss);
            if (history.size() < 100) {
                var ret = cap(fromRecipes(stack, history), ASPECT_CAP);
                register(is, ret);
                if (history.size() > 1) provisional.add(key(is));
                return ret;
            }
            return null;
        }

        LinkedHashMap<Aspect, Integer> fromRecipes(ItemStack stack, List<String> history) {
            var ret = fromCrucible(stack, history);
            if (ret != null) return ret;
            return fromCrafting(stack, history);
        }

        LinkedHashMap<Aspect, Integer> fromCrucible(ItemStack stack, List<String> history) {
            for (var recipe : crucible) {
                var out = recipe.result().create();
                if (!out.is(stack.getItem())) continue;
                var catalyst = first(recipe.catalyst());
                if (catalyst.isEmpty()) return null;
                if (!recipe.catalystAspect().isEmpty()) catalyst.set(ModComponents.ASPECT.get(), recipe.catalystAspect());
                var ot2 = getObjectTags(catalyst, history);
                var result = new LinkedHashMap<Aspect, Integer>();
                if (ot2 != null) ot2.forEach((a, n) -> add(result, a, n));
                int count = out.getCount();
                costOf(recipe, history).forEach((a, amount) -> add(result, a, (int)(Math.sqrt(amount) / count)));
                result.values().removeIf(v -> v <= 0);
                return result;
            }
            return null;
        }

        /** Crucible cost; postAspects recipes read the table being built. */
        Map<Aspect, Integer> costOf(art.arcane.thaumcraft.crafting.CrucibleRecipe recipe, List<String> history) {
            var out = new LinkedHashMap<Aspect, Integer>();
            if (recipe.aspectsOf().isEmpty()) { recipe.fixedAspects().forEach((tag, n) -> { var a = AspectRegistry.get(tag); if (a != null) out.put(a, n); }); return out; }
            var of = BuiltInRegistries.ITEM.getValue(Identifier.parse(recipe.aspectsOf()));
            var t = of == null ? null : getObjectTags(new ItemStack(of), history);
            if (t != null) t.forEach((a, n) -> add(out, a, n));
            if (!recipe.aspectsMinus().isEmpty()) {
                var minus = BuiltInRegistries.ITEM.getValue(Identifier.parse(recipe.aspectsMinus()));
                var m = minus == null ? null : getObjectTags(new ItemStack(minus), history);
                if (m != null) m.forEach((a, n) -> reduce(out, a, n));
            }
            return out;
        }

        LinkedHashMap<Aspect, Integer> fromCrafting(ItemStack stack, List<String> history) {
            LinkedHashMap<Aspect, Integer> ret = null;
            int value = Integer.MAX_VALUE;
            for (var holder : crafting) {
                var recipe = holder.value();
                Grid grid = grid(recipe);
                if (grid == null) continue;
                ItemStack out;
                try { out = assemble(recipe, grid); } catch (RuntimeException e) { continue; }
                if (out.isEmpty() || !out.is(stack.getItem())) continue;
                var ph = fromIngredients(grid, out, recipe, history);
                if (recipe instanceof art.arcane.thaumcraft.crafting.ArcaneCraftingRecipe arcane && arcane.vis() > 0)
                    add(ph, AspectRegistry.PRAECANTATIO, (int)(Math.sqrt(1 + arcane.vis() / 2) / (float)out.getCount()));
                ph.values().removeIf(v -> v <= 0);
                int size = visSize(ph);
                if (size >= value || size <= 0) continue;
                ret = ph; value = size;
            }
            return ret;
        }

        record Grid(int width, int height, List<ItemStack> stacks) {}
        static Grid grid(Recipe<?> recipe) {
            List<Optional<Ingredient>> ingredients; int width, height;
            if (recipe instanceof ShapedRecipe shaped) { ingredients = shaped.getIngredients(); width = shaped.getWidth(); height = shaped.getHeight(); }
            else if (recipe instanceof art.arcane.thaumcraft.crafting.ArcaneRecipe arcane) { ingredients = arcane.pattern().ingredients(); width = arcane.pattern().width(); height = arcane.pattern().height(); }
            else if (recipe instanceof ShapelessRecipe shapeless) { ingredients = shapeless.placementInfo().ingredients().stream().map(Optional::of).toList(); width = ingredients.size(); height = 1; }
            else if (recipe instanceof art.arcane.thaumcraft.crafting.ShapelessArcaneRecipe sa) { ingredients = sa.ingredients().stream().map(Optional::of).toList(); width = ingredients.size(); height = 1; }
            else return null;
            if (width < 1 || height < 1) return null;
            var stacks = new ArrayList<ItemStack>();
            for (var ing : ingredients) stacks.add(ing.map(Engine::first).orElse(ItemStack.EMPTY));
            return new Grid(width, height, stacks);
        }
        @SuppressWarnings({"unchecked", "rawtypes"})
        static ItemStack assemble(Recipe recipe, Grid grid) {
            if (recipe instanceof art.arcane.thaumcraft.crafting.ArcaneRecipe arcane) return arcane.result().create();
            if (recipe instanceof art.arcane.thaumcraft.crafting.ShapelessArcaneRecipe sa) return sa.result().create();
            return recipe.assemble(CraftingInput.of(grid.width, grid.height, grid.stacks));
        }
        static ItemStack first(Ingredient ingredient) {
            return ingredient.items().findFirst().map(h -> new ItemStack(h.value())).orElse(ItemStack.EMPTY);
        }

        LinkedHashMap<Aspect, Integer> fromIngredients(Grid grid, ItemStack out, Recipe<?> recipe, List<String> history) {
            var mid = new LinkedHashMap<Aspect, Integer>();
            for (var s : grid.stacks) {
                if (s.isEmpty()) continue;
                var obj = getObjectTags(s, history);
                if (obj != null) obj.forEach((a, n) -> add(mid, a, n));
            }
            if (recipe instanceof CraftingRecipe cr) {
                try {
                    for (var rem : cr.getRemainingItems(CraftingInput.of(grid.width, grid.height, grid.stacks))) {
                        if (rem.isEmpty()) continue;
                        var obj = getObjectTags(rem, history);
                        if (obj != null) obj.forEach((a, n) -> reduce(mid, a, n));
                    }
                } catch (RuntimeException ignored) {}
            }
            var result = new LinkedHashMap<Aspect, Integer>();
            mid.forEach((a, n) -> {
                float v = n * 0.75f / out.getCount();
                if (v < 1.0f && v > 0.75) v = 1.0f;
                add(result, a, (int) v);
            });
            result.values().removeIf(v -> v <= 0);
            return result;
        }

        // ------------------------------------------------ replay of ConfigAspects
        void replay() {
            JsonArray ops;
            try (var reader = new InputStreamReader(Objects.requireNonNull(ObjectAspects.class.getResourceAsStream("/data/thaumcraft/aspects/objects.json")), StandardCharsets.UTF_8)) {
                ops = JsonParser.parseReader(reader).getAsJsonArray();
            } catch (Exception e) { throw new IllegalStateException("Cannot read TC6 object aspects", e); }
            apply(ops);
            applyCompat();
        }

        /** Optional files for other mods / common c:* tags. Absent mods, unknown ids and empty tags are skipped silently. */
        static final String[] COMPAT = {"common", "create", "mekanism", "farmersdelight", "verification"};
        void applyCompat() {
            var skipped = new ArrayList<String>();
            for (String name : COMPAT) {
                var in = ObjectAspects.class.getResourceAsStream("/data/thaumcraft/aspects/compat/" + name + ".json");
                if (in == null) continue;
                try (var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    var root = JsonParser.parseReader(reader).getAsJsonObject();
                    if (!modsLoaded(root)) { skipped.add(name); continue; }
                    var entries = new JsonArray();
                    for (var e : root.getAsJsonArray("entries")) if (modsLoaded(e.getAsJsonObject())) entries.add(e);
                    apply(entries);
                } catch (Exception e) { LOGGER.warn("Ignoring malformed aspect compat file {}: {}", name, e.toString()); }
            }
            if (!skipped.isEmpty()) LOGGER.debug("Aspect compat files skipped (mod not loaded): {}", skipped);
        }
        static boolean modsLoaded(com.google.gson.JsonObject o) {
            if (!o.has("requires")) return true;
            for (var m : o.getAsJsonArray("requires")) if (!net.neoforged.fml.ModList.get().isLoaded(m.getAsString())) return false;
            return true;
        }

        void apply(JsonArray ops) {
            for (JsonElement el : ops) {
                var op = el.getAsJsonObject();
                String kind = op.get("op").getAsString();
                if (kind.equals("potions")) { potions(); continue; }
                for (ItemStack target : targets(op.getAsJsonArray("targets"))) {
                    LinkedHashMap<Aspect, Integer> list = new LinkedHashMap<>();
                    if (op.has("base")) {
                        var b = BuiltInRegistries.ITEM.getValue(Identifier.parse(op.get("base").getAsString()));
                        var bt = b == null ? null : getObjectTags(new ItemStack(b), new ArrayList<>());
                        if (bt != null) bt.forEach((a, n) -> add(list, a, n));
                    }
                    for (var o : op.getAsJsonArray("ops")) {
                        var arr = o.getAsJsonArray();
                        var a = AspectRegistry.get(arr.get(1).getAsString());
                        if (a == null) continue;
                        int n = arr.get(2).getAsInt();
                        switch (arr.get(0).getAsString()) {
                            case "add" -> add(list, a, n);
                            case "merge" -> list.merge(a, n, Math::max);
                            case "remove" -> reduce(list, a, n);
                            default -> throw new IllegalStateException("Unknown aspect op");
                        }
                    }
                    if (kind.equals("set")) register(target, list);
                    else complex(target, list);
                }
            }
        }
        /** TC6 AspectEventProxy.registerComplexObjectTag, including its no-op merge for already known items. */
        void complex(ItemStack item, LinkedHashMap<Aspect, Integer> aspects) {
            // Modern recipe chains (e.g. sugar from honey bottles) can generate an item before its ConfigAspects line;
            // 1.12 had no such chain, so treat those side-effect entries as not yet registered.
            if (provisional.remove(key(item))) tags.remove(key(item));
            if (!exists(item)) {
                var tmp = generateTags(item, new ArrayList<>());
                if (tmp != null && !tmp.isEmpty()) tmp.forEach((a, n) -> add(aspects, a, n));
                register(item, aspects);
            } else {
                var tmp = getObjectTags(item, new ArrayList<>());
                register(item, tmp);
            }
        }
        static List<ItemStack> targets(JsonArray targets) {
            var out = new ArrayList<ItemStack>();
            for (var t : targets) {
                String s = t.getAsString();
                if (s.startsWith("#")) {
                    var tag = TagKey.create(Registries.ITEM, Identifier.parse(s.substring(1)));
                    for (var h : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) out.add(new ItemStack(h.value()));
                } else {
                    var id = Identifier.tryParse(s);
                    var item = id == null ? null : BuiltInRegistries.ITEM.getValue(id);
                    if (item != null && item != Items.AIR) out.add(new ItemStack(item));
                }
            }
            return out;
        }

        void potions() {
            Item[] containers = {Items.POTION, Items.TIPPED_ARROW, Items.SPLASH_POTION, Items.LINGERING_POTION};
            Aspect[] extra = {AspectRegistry.AQUA, AspectRegistry.AVERSIO, AspectRegistry.POTENTIA, AspectRegistry.VINCULUM};
            for (var potion : BuiltInRegistries.POTION.listElements().toList()) {
                for (int i = 0; i < containers.length; i++) {
                    var stack = PotionContents.createItemStack(containers[i], potion);
                    var list = potionAspects(potion);
                    add(list, extra[i], 5);
                    register(stack, list);
                }
            }
        }
        LinkedHashMap<Aspect, Integer> potionAspects(Holder<Potion> potion) {
            var tmp = new LinkedHashMap<Aspect, Integer>();
            if (!potion.is(Potions.WATER)) {
                var reagents = new LinkedHashSet<Item>();
                reagents(potion, reagents, 0);
                boolean didit = false;
                for (var r : reagents) {
                    var t2 = getObjectTags(new ItemStack(r), new ArrayList<>());
                    if (t2 != null) t2.forEach((a, n) -> add(tmp, a, n));
                    didit = true;
                    add(tmp, AspectRegistry.ALKIMIA, 3);
                }
                for (var a : new ArrayList<>(tmp.keySet())) reduce(tmp, a, (int)(tmp.get(a) * 0.66));
                if (!didit) { add(tmp, AspectRegistry.PRAECANTATIO, 5); add(tmp, AspectRegistry.ALKIMIA, 5); }
            } else add(tmp, AspectRegistry.AQUA, 5);
            return tmp;
        }
        void reagents(Holder<Potion> potion, Set<Item> out, int depth) {
            if (depth > 16) return;
            for (PotionBrewing.Mix<Potion> mix : brewing.potionMixes) {
                if (!mix.to().is(potion)) continue;
                mix.ingredient().items().findFirst().ifPresent(h -> out.add(h.value()));
                if (!mix.from().is(Potions.WATER)) reagents(mix.from(), out, depth + 1);
                break;
            }
        }
    }

    // ---------------------------------------------------------------- shared helpers
    static void add(Map<Aspect, Integer> list, Aspect aspect, int amount) {
        if (aspect == null) return;
        list.merge(aspect, amount, Integer::sum);
    }
    static void reduce(Map<Aspect, Integer> list, Aspect aspect, int amount) {
        if (aspect == null || !list.containsKey(aspect)) return;
        int n = list.get(aspect) - amount;
        if (n <= 0) list.remove(aspect); else list.put(aspect, n);
    }
    static int visSize(Map<Aspect, Integer> list) { int s = 0; for (int v : list.values()) s += v; return s; }
    static LinkedHashMap<Aspect, Integer> cap(LinkedHashMap<Aspect, Integer> source, int amount) {
        if (source == null) return null;
        var out = new LinkedHashMap<Aspect, Integer>();
        source.forEach((a, n) -> out.merge(a, Math.min(amount, n), Math::max));
        return out;
    }

    private static final TagKey<Item> DYES = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "dyes"));
    /** TC6 ThaumcraftCraftingManager.getBonusTags followed by AspectHelper.cullTags(7). */
    static LinkedHashMap<Aspect, Integer> bonus(ItemStack stack, LinkedHashMap<Aspect, Integer> source) {
        var tmp = new LinkedHashMap<Aspect, Integer>();
        if (stack.isEmpty()) return tmp;
        Item item = stack.getItem();
        int contained = item == ModItems.CRYSTAL_ESSENCE.get() || item == ModItems.MANA_BEAN.get() ? 1 : item == ModItems.PHIAL_FILLED.get() ? 10 : 0;
        if (contained > 0) {
            if (source != null) source.clear();
            var a = AspectRegistry.get(stack.getOrDefault(ModComponents.ASPECT.get(), ""));
            if (a != null) tmp.put(a, contained);
        }
        if (source != null) source.forEach((a, n) -> add(tmp, a, n));
        var mods = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY);
        double armor = 0, attack = 0;
        for (var e : mods.modifiers()) {
            if (e.attribute().is(Attributes.ARMOR)) armor += e.modifier().amount();
            if (e.attribute().is(Attributes.ATTACK_DAMAGE)) attack += e.modifier().amount();
        }
        if (armor > 0 && stack.has(DataComponents.EQUIPPABLE)) {
            tmp.merge(AspectRegistry.PRAEMUNIO, (int) armor * 4, Math::max);
        } else if (stack.is(ItemTags.SWORDS) && attack - 3 + 1 > 0) {
            tmp.merge(AspectRegistry.AVERSIO, (int)(attack - 3 + 1) * 4, Math::max);
        } else if (item instanceof BowItem) {
            tmp.merge(AspectRegistry.AVERSIO, 10, Math::max); tmp.merge(AspectRegistry.VOLATUS, 5, Math::max);
        } else if (stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.AXES) || stack.is(ItemTags.SHOVELS)) {
            tmp.merge(AspectRegistry.INSTRUMENTUM, (harvestLevel(stack) + 1) * 4, Math::max);
        } else if (item instanceof ShearsItem || stack.is(ItemTags.HOES)) {
            int max = stack.getMaxDamage();
            int tool = max <= 59 ? 4 : (max <= 131 || max <= 32) ? 8 : max <= 250 ? 12 : 16;
            tmp.merge(AspectRegistry.INSTRUMENTUM, tool, Math::max);
        }
        if (stack.is(DYES)) tmp.merge(AspectRegistry.SENSUS, 5, Math::max);
        ItemEnchantments ench = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (ench.isEmpty()) ench = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (!ench.isEmpty()) {
            int magic = 0;
            for (var entry : ench.entrySet()) {
                Holder<Enchantment> e = entry.getKey();
                int lvl = entry.getIntValue() * 3;
                enchant(tmp, e, lvl);
                int w = e.value().getWeight();
                if (w == 5) magic += 2; else if (w == 2) magic += 4; else if (w == 1) magic += 6;
                magic += lvl;
            }
            if (magic > 0) tmp.merge(AspectRegistry.PRAECANTATIO, magic, Math::max);
        }
        return cull(tmp, 7);
    }
    private static int harvestLevel(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (id.startsWith("stone_")) return 1;
        if (id.startsWith("iron_")) return 2;
        if (id.startsWith("diamond_") || id.startsWith("thaumium_") || id.startsWith("elemental_")) return 3;
        if (id.startsWith("netherite_") || id.startsWith("void_")) return 4;
        return 0;
    }
    private static void enchant(Map<Aspect, Integer> t, Holder<Enchantment> e, int lvl) {
        record M(net.minecraft.resources.ResourceKey<Enchantment> key, Aspect a, int div, Aspect b) {}
        M[] table = {
            new M(Enchantments.AQUA_AFFINITY, AspectRegistry.AQUA, 1, null), new M(Enchantments.BANE_OF_ARTHROPODS, AspectRegistry.BESTIA, 2, AspectRegistry.AVERSIO),
            new M(Enchantments.BLAST_PROTECTION, AspectRegistry.PRAEMUNIO, 2, AspectRegistry.PERDITIO), new M(Enchantments.EFFICIENCY, AspectRegistry.INSTRUMENTUM, 1, null),
            new M(Enchantments.FEATHER_FALLING, AspectRegistry.VOLATUS, 1, null), new M(Enchantments.FIRE_ASPECT, AspectRegistry.IGNIS, 2, AspectRegistry.AVERSIO),
            new M(Enchantments.FIRE_PROTECTION, AspectRegistry.PRAEMUNIO, 2, AspectRegistry.IGNIS), new M(Enchantments.FLAME, AspectRegistry.IGNIS, 1, null),
            new M(Enchantments.FORTUNE, AspectRegistry.DESIDERIUM, 1, null), new M(Enchantments.INFINITY, AspectRegistry.FABRICO, 1, null),
            new M(Enchantments.KNOCKBACK, AspectRegistry.AER, 1, null), new M(Enchantments.LOOTING, AspectRegistry.DESIDERIUM, 1, null),
            new M(Enchantments.POWER, AspectRegistry.AVERSIO, 1, null), new M(Enchantments.PROJECTILE_PROTECTION, AspectRegistry.PRAEMUNIO, 1, null),
            new M(Enchantments.PROTECTION, AspectRegistry.PRAEMUNIO, 1, null), new M(Enchantments.PUNCH, AspectRegistry.AER, 1, null),
            new M(Enchantments.RESPIRATION, AspectRegistry.AER, 1, null), new M(Enchantments.SHARPNESS, AspectRegistry.AVERSIO, 1, null),
            new M(Enchantments.SILK_TOUCH, AspectRegistry.PERMUTATIO, 1, null), new M(Enchantments.THORNS, AspectRegistry.AVERSIO, 1, null),
            new M(Enchantments.SMITE, AspectRegistry.EXANIMIS, 2, AspectRegistry.AVERSIO), new M(Enchantments.UNBREAKING, AspectRegistry.TERRA, 1, null),
            new M(Enchantments.DEPTH_STRIDER, AspectRegistry.AQUA, 1, null), new M(Enchantments.LUCK_OF_THE_SEA, AspectRegistry.DESIDERIUM, 1, null),
            new M(Enchantments.LURE, AspectRegistry.BESTIA, 1, null), new M(Enchantments.FROST_WALKER, AspectRegistry.GELUM, 1, null),
            new M(Enchantments.MENDING, AspectRegistry.FABRICO, 1, null)};
        for (var m : table) if (e.is(m.key)) {
            t.merge(m.a, lvl / m.div, Math::max);
            if (m.b != null) t.merge(m.b, lvl / m.div, Math::max);
            return;
        }
    }
    /** AspectHelper.cullTags. */
    public static LinkedHashMap<Aspect, Integer> cull(LinkedHashMap<Aspect, Integer> temp, int cap) {
        var t2 = new LinkedHashMap<Aspect, Integer>();
        temp.forEach((a, n) -> { if (a != null) add(t2, a, n); });
        while (t2.size() > cap) {
            Aspect lowest = null; float low = Short.MAX_VALUE;
            for (var e : t2.entrySet()) {
                Aspect tag = e.getKey(); float ta = e.getValue();
                if (tag.isPrimal()) ta *= .9f;
                else {
                    if (!tag.parentA().isPrimal()) {
                        ta *= 1.1f;
                        if (!tag.parentA().parentA().isPrimal()) ta *= 1.05f;
                        if (!tag.parentA().parentB().isPrimal()) ta *= 1.05f;
                    }
                    if (!tag.parentB().isPrimal()) {
                        ta *= 1.1f;
                        if (!tag.parentB().parentA().isPrimal()) ta *= 1.05f;
                        if (!tag.parentB().parentB().isPrimal()) ta *= 1.05f;
                    }
                }
                if (ta < low) { low = ta; lowest = tag; }
            }
            t2.remove(lowest);
        }
        return t2;
    }
    private ObjectAspects() {}
}
