package art.arcane.thaumcraft.item;

import art.arcane.thaumcraft.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * TC6 Utils.generateLoot / genGear / getGearItemForSlot and the loot bag tables filled by ModConfig through
 * ThaumcraftApi.addLootBagItem (WeightedRandomLoot.lootBagCommon / Uncommon / Rare). Rarity 0 common, 1 uncommon, 2 rare.
 */
public final class TcLoot {
    private TcLoot() {}

    private record Entry(Supplier<ItemStack> stack, int weight) {}
    @SuppressWarnings("unchecked")
    private static final List<Entry>[] BAGS = new List[] { new ArrayList<>(), new ArrayList<>(), new ArrayList<>() };
    private static boolean built;

    private static void add(Supplier<ItemStack> s, int weight, int... bags) {
        if (bags.length == 0) bags = new int[] { 0 };
        for (int b : bags) BAGS[b].add(new Entry(s, weight));
    }

    private static Supplier<ItemStack> of(Supplier<? extends Item> item, int count) { return () -> new ItemStack(item.get(), count); }
    private static Supplier<ItemStack> of(Item item, int count) { return () -> new ItemStack(item, count); }
    private static Supplier<ItemStack> pearl(int damage) {
        return () -> { ItemStack s = new ItemStack(ModItems.PRIMORDIAL_PEARL.get()); s.setDamageValue(damage); return s; };
    }

    /** ModConfig.postInitLoot. */
    private static synchronized void build() {
        if (built) return;
        add(of(Items.GOLD_NUGGET, 1), 2500, 0);
        add(of(Items.GOLD_NUGGET, 2), 2250, 1);
        add(of(Items.GOLD_NUGGET, 3), 2000, 2);
        add(of(ModItems.SALIS_MUNDUS, 1), 3, 0);
        add(of(ModItems.SALIS_MUNDUS, 1), 6, 1);
        add(of(ModItems.SALIS_MUNDUS, 1), 9, 2);
        add(of(Items.CHORUS_FRUIT, 1), 5, 0, 1, 2);
        add(of(Items.COMPASS, 1), 5, 0, 1, 2);
        add(of(Items.COOKIE, 1), 5, 0, 1, 2);
        add(pearl(7), 1, 0);
        add(pearl(7), 3, 1);
        add(pearl(6), 1, 1);
        add(pearl(5), 9, 2);
        add(pearl(3), 3, 2);
        add(pearl(0), 1, 2);
        add(of(Items.NETHER_STAR, 1), 1, 2);
        add(of(Items.DIAMOND, 1), 10, 0);
        add(of(Items.DIAMOND, 1), 50, 1, 2);
        add(of(Items.EMERALD, 1), 15, 0);
        add(of(Items.EMERALD, 1), 75, 1, 2);
        add(of(Items.GOLD_INGOT, 1), 100, 0, 1, 2);
        add(of(Items.ENDER_PEARL, 1), 100, 0, 1, 2);
        add(of(ModItems.AMULET_VIS, 1), 6, 1, 2);
        add(of(ModItems.BAUBLES_AMULET_MUNDANE, 1), 10, 0);
        add(of(ModItems.BAUBLES_RING_MUNDANE, 1), 10, 0);
        add(of(ModItems.BAUBLES_GIRDLE_MUNDANE, 1), 10, 0);
        add(of(ModItems.BAUBLES_RING_APPRENTICE, 1), 5, 2);
        add(of(ModItems.BAUBLES_AMULET_FANCY, 1), 5, 1);
        add(of(ModItems.BAUBLES_RING_FANCY, 1), 5, 1);
        add(of(ModItems.BAUBLES_GIRDLE_FANCY, 1), 5, 1);
        add(of(Items.EXPERIENCE_BOTTLE, 1), 5, 0);
        add(of(Items.EXPERIENCE_BOTTLE, 1), 10, 1);
        add(of(Items.EXPERIENCE_BOTTLE, 1), 20, 2);
        add(of(Items.ENCHANTED_GOLDEN_APPLE, 1), 1, 0);
        add(of(Items.ENCHANTED_GOLDEN_APPLE, 1), 2, 1);
        add(of(Items.ENCHANTED_GOLDEN_APPLE, 1), 3, 2);
        add(of(Items.GOLDEN_APPLE, 1), 3, 0);
        add(of(Items.GOLDEN_APPLE, 1), 6, 1);
        add(of(Items.GOLDEN_APPLE, 1), 9, 2);
        add(of(Items.BOOK, 1), 10, 0, 1, 2);
        // for (PotionType pt : PotionType.REGISTRY): drinkable and splash in every bag, lingering in uncommon and rare.
        for (var potion : BuiltInRegistries.POTION.listElements().toList()) {
            add(() -> PotionContents.createItemStack(Items.POTION, potion), 2, 0, 1, 2);
            add(() -> PotionContents.createItemStack(Items.SPLASH_POTION, potion), 2, 0, 1, 2);
            add(() -> PotionContents.createItemStack(Items.LINGERING_POTION, potion), 2, 1, 2);
        }
        built = true;
    }

    private static ItemStack weighted(List<Entry> list, RandomSource rand) {
        int total = 0;
        for (Entry e : list) total += e.weight();
        int r = rand.nextInt(total);
        for (Entry e : list) { r -= e.weight(); if (r < 0) return e.stack().get(); }
        return ItemStack.EMPTY;
    }

    /** Utils.generateLoot. */
    public static ItemStack generate(int rarity, RandomSource rand, RegistryAccess access) {
        build();
        rarity = Math.clamp(rarity, 0, 2);
        ItemStack is;
        if (rarity > 0 && rand.nextFloat() < 0.025f * rarity) {
            is = gear(rarity, rand, access);
            if (is.isEmpty()) is = generate(rarity, rand, access);
        } else {
            is = weighted(BAGS[rarity], rand);
        }
        if (is.is(Items.BOOK)) is = EnchantmentHelper.enchantItem(rand, is, (int) (5.0f + rarity * 0.75f * rand.nextInt(18)), access, Optional.empty());
        return is.copy();
    }

    /** Utils.genGear: a random armour piece or weapon of rolled quality, worn and maybe enchanted. */
    private static ItemStack gear(int rarity, RandomSource rand, RegistryAccess access) {
        int quality = rand.nextInt(2);
        if (rand.nextFloat() < 0.2f) ++quality;
        if (rand.nextFloat() < 0.15f) ++quality;
        if (rand.nextFloat() < 0.1f) ++quality;
        if (rand.nextFloat() < 0.095f) ++quality;
        if (rand.nextFloat() < 0.095f) ++quality;
        Item item = gearFor(rand.nextInt(5), quality);
        if (item == null) return ItemStack.EMPTY;
        ItemStack is = new ItemStack(item);
        int max = is.getOrDefault(DataComponents.MAX_DAMAGE, 0);
        if (max > 0) is.setDamageValue(rand.nextInt(1 + max / 6));
        if (rand.nextInt(4) < rarity) is = EnchantmentHelper.enchantItem(rand, is, (int) (5.0f + rarity * 0.75f * rand.nextInt(18)), access, Optional.empty());
        return is.copy();
    }

    /** Utils.getGearItemForSlot, including its switch fall-through: an unmatched quality drops to the next slot. */
    private static Item gearFor(int slot, int q) {
        Item[][] table = {
            { Items.IRON_AXE, Items.IRON_SWORD, Items.GOLDEN_AXE, Items.GOLDEN_SWORD, ModItems.THAUMIUM_SWORD.get(), Items.DIAMOND_SWORD, ModItems.VOID_SWORD.get() },
            { Items.LEATHER_BOOTS, Items.GOLDEN_BOOTS, Items.CHAINMAIL_BOOTS, Items.IRON_BOOTS, ModItems.THAUMIUM_BOOTS.get(), Items.DIAMOND_BOOTS, ModItems.VOID_BOOTS.get() },
            { Items.LEATHER_LEGGINGS, Items.GOLDEN_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.IRON_LEGGINGS, ModItems.THAUMIUM_LEGS.get(), Items.DIAMOND_LEGGINGS, ModItems.VOID_LEGS.get() },
            { Items.LEATHER_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE, ModItems.THAUMIUM_CHEST.get(), Items.DIAMOND_CHESTPLATE, ModItems.VOID_CHEST.get() },
            { Items.LEATHER_HELMET, Items.GOLDEN_HELMET, Items.CHAINMAIL_HELMET, Items.IRON_HELMET, ModItems.THAUMIUM_HELM.get(), Items.DIAMOND_HELMET, ModItems.VOID_HELM.get() },
        };
        for (int s = slot; s >= 0; s--) if (q >= 0 && q < 7) return table[s][q];
        return null;
    }
}
