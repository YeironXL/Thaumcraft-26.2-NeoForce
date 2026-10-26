package art.arcane.thaumcraft.crafting;

import art.arcane.thaumcraft.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** ThaumcraftApi.addSmeltingBonus / CommonInternals.smeltingBonus with the ConfigRecipes entries (ore dict names → c: tags). */
public final class SmeltingBonus {
    private record Bonus(Predicate<ItemStack> in, Supplier<Item> out, float chance) {}
    private static final List<Bonus> BONUSES = new ArrayList<>();

    private static TagKey<Item> c(String path) { return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path)); }
    private static Predicate<ItemStack> ore(String name) { TagKey<Item> t = c("ores/" + name); return s -> s.is(t); }
    private static Predicate<ItemStack> item(Supplier<Item> i) { return s -> s.is(i.get()); }
    private static void add(Predicate<ItemStack> in, Supplier<Item> out) { add(in, out, 1.0f); }
    private static void add(Predicate<ItemStack> in, Supplier<Item> out, float chance) { BONUSES.add(new Bonus(in, out, chance)); }

    static {
        add(ore("gold"), () -> Items.GOLD_NUGGET);
        add(ore("iron"), () -> Items.IRON_NUGGET);
        add(ore("cinnabar"), ModItems.NUGGET_QUICKSILVER::get);
        add(ore("copper"), ModItems.NUGGET_COPPER::get);
        add(ore("tin"), ModItems.NUGGET_TIN::get);
        add(ore("silver"), ModItems.NUGGET_SILVER::get);
        add(ore("lead"), ModItems.NUGGET_LEAD::get);
        add(ore("quartz"), ModItems.NUGGET_QUARTZ::get);
        add(item(ModItems.CLUSTER_IRON::get), () -> Items.IRON_NUGGET);
        add(item(ModItems.CLUSTER_GOLD::get), () -> Items.GOLD_NUGGET);
        add(item(ModItems.CLUSTER_CINNABAR::get), ModItems.NUGGET_QUICKSILVER::get);
        add(item(ModItems.CLUSTER_COPPER::get), ModItems.NUGGET_COPPER::get);
        add(item(ModItems.CLUSTER_TIN::get), ModItems.NUGGET_TIN::get);
        add(item(ModItems.CLUSTER_SILVER::get), ModItems.NUGGET_SILVER::get);
        add(item(ModItems.CLUSTER_LEAD::get), ModItems.NUGGET_LEAD::get);
        add(item(ModItems.CLUSTER_QUARTZ::get), ModItems.NUGGET_QUARTZ::get);
        add(item(() -> Items.BEEF), ModItems.CHUNK_BEEF::get);
        add(item(() -> Items.CHICKEN), ModItems.CHUNK_CHICKEN::get);
        add(item(() -> Items.PORKCHOP), ModItems.CHUNK_PORK::get);
        add(s -> s.is(Items.COD) || s.is(Items.SALMON) || s.is(Items.TROPICAL_FISH) || s.is(Items.PUFFERFISH), ModItems.CHUNK_FISH::get);
        add(item(() -> Items.RABBIT), ModItems.CHUNK_RABBIT::get);
        add(item(() -> Items.MUTTON), ModItems.CHUNK_MUTTON::get);
        add(ore("diamond"), ModItems.NUGGET_RAREEARTH::get, 0.025f);
        add(ore("redstone"), ModItems.NUGGET_RAREEARTH::get, 0.01f);
        add(ore("lapis"), ModItems.NUGGET_RAREEARTH::get, 0.01f);
        add(ore("emerald"), ModItems.NUGGET_RAREEARTH::get, 0.025f);
        add(ore("gold"), ModItems.NUGGET_RAREEARTH::get, 0.02f);
        add(ore("iron"), ModItems.NUGGET_RAREEARTH::get, 0.01f);
        add(ore("cinnabar"), ModItems.NUGGET_RAREEARTH::get, 0.025f);
        add(ore("copper"), ModItems.NUGGET_RAREEARTH::get, 0.01f);
        add(ore("tin"), ModItems.NUGGET_RAREEARTH::get, 0.01f);
        add(ore("silver"), ModItems.NUGGET_RAREEARTH::get, 0.02f);
        add(ore("lead"), ModItems.NUGGET_RAREEARTH::get, 0.01f);
        add(ore("quartz"), ModItems.NUGGET_RAREEARTH::get, 0.01f);
        add(s -> s.is(ModItems.CLUSTER_IRON.get()) || s.is(ModItems.CLUSTER_GOLD.get()) || s.is(ModItems.CLUSTER_CINNABAR.get()) || s.is(ModItems.CLUSTER_COPPER.get())
                || s.is(ModItems.CLUSTER_TIN.get()) || s.is(ModItems.CLUSTER_SILVER.get()) || s.is(ModItems.CLUSTER_LEAD.get()) || s.is(ModItems.CLUSTER_QUARTZ.get()),
                ModItems.NUGGET_RAREEARTH::get, 0.02f);
    }

    /** TileInfernalFurnace.getSmeltingBonus: every matching bonus rolls its chance independently. */
    public static List<ItemStack> roll(ItemStack in, RandomSource rand) {
        List<ItemStack> out = new ArrayList<>();
        for (Bonus b : BONUSES) if (b.in.test(in) && rand.nextFloat() <= b.chance) out.add(new ItemStack(b.out.get()));
        return out;
    }
    /** One possible extra output and its odds, for recipe viewers. */
    public record Chance(ItemStack stack, float chance) {}
    /** Every bonus the infernal furnace can roll for this input. */
    public static List<Chance> bonuses(ItemStack in) {
        List<Chance> out = new ArrayList<>();
        for (Bonus b : BONUSES) if (b.in.test(in)) out.add(new Chance(new ItemStack(b.out.get()), b.chance));
        return out;
    }
    private SmeltingBonus() {}
}
