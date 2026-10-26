package art.arcane.thaumcraft.entity.monster;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.item.CrystalEssenceItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * TC6 ConfigEntities.initEntities: EntityPech.tradeInventory, the (value, stack) goods each pech type pays out. Values are
 * 1..5; a trade roll picks entries whose value matches the chunk of the offer being paid out.
 */
public final class PechTrades {
    public record Trade(int value, Function<HolderLookup.Provider, ItemStack> maker) {
        public ItemStack stack(HolderLookup.Provider regs) { return maker.apply(regs).copy(); }
    }

    private static Trade t(int v, Supplier<? extends net.minecraft.world.level.ItemLike> item) { return new Trade(v, r -> new ItemStack(item.get())); }
    private static Trade t(int v, Item item) { return new Trade(v, r -> new ItemStack(item)); }
    private static Trade crystal(int v, art.arcane.thaumcraft.aspect.Aspect a) { return new Trade(v, r -> CrystalEssenceItem.of(a.id(), 1)); }
    private static Trade potion(int v, net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> p) { return new Trade(v, r -> PotionContents.createItemStack(Items.POTION, p)); }
    private static Trade book(int v, ResourceKey<Enchantment> e) {
        return new Trade(v, r -> net.minecraft.world.item.enchantment.EnchantmentHelper.createBook(new EnchantmentInstance(r.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(e), 1)));
    }
    /** ModConfig.foundXIngot: the copper/tin/silver/lead clusters only when such ingots exist. */
    private static boolean hasIngot(String metal) {
        var tag = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots/" + metal));
        return BuiltInRegistries.ITEM.get(tag).map(h -> h.size() > 0).orElse(false);
    }

    /** Pech forager (0), mage (1) and stalker (2) goods. */
    public static List<Trade> forType(int type) {
        List<Trade> forInv = new ArrayList<>(), forMag = new ArrayList<>(), forArc = new ArrayList<>();
        forInv.add(t(1, ModItems.CLUSTER_IRON));
        forInv.add(t(1, ModItems.CLUSTER_GOLD));
        forInv.add(t(1, ModItems.CLUSTER_CINNABAR));
        forInv.add(t(1, ModItems.CLUSTER_QUARTZ));
        if (hasIngot("copper")) forInv.add(t(1, ModItems.CLUSTER_COPPER));
        if (hasIngot("tin")) forInv.add(t(1, ModItems.CLUSTER_TIN));
        if (hasIngot("silver")) forInv.add(t(1, ModItems.CLUSTER_SILVER));
        if (hasIngot("lead")) forInv.add(t(1, ModItems.CLUSTER_LEAD));
        forInv.add(t(2, Items.BLAZE_ROD));
        forInv.add(t(2, ModItems.BLOCK_SAPLING_GREATWOOD));
        forInv.add(t(2, Items.DRAGON_BREATH));
        forInv.add(t(3, Items.EXPERIENCE_BOTTLE));
        forInv.add(t(3, Items.EXPERIENCE_BOTTLE));
        forInv.add(t(3, Items.GOLDEN_APPLE));
        forInv.add(t(4, ModItems.THAUMIUM_PICK));
        forInv.add(t(4, ModItems.THAUMIUM_AXE));
        forInv.add(t(4, ModItems.THAUMIUM_HOE));
        forInv.add(t(5, Items.ENCHANTED_GOLDEN_APPLE));
        forInv.add(t(5, ModItems.BLOCK_SAPLING_SILVERWOOD));
        forInv.add(t(5, ModItems.CURIO_KNOWLEDGE));

        forMag.add(crystal(1, AspectRegistry.AER));
        forMag.add(crystal(1, AspectRegistry.TERRA));
        forMag.add(crystal(1, AspectRegistry.IGNIS));
        forMag.add(crystal(1, AspectRegistry.AQUA));
        forMag.add(crystal(1, AspectRegistry.ORDO));
        forMag.add(crystal(1, AspectRegistry.PERDITIO));
        // 1.12 potion metadata 8193 / 8261: regeneration and instant health
        forMag.add(potion(2, Potions.REGENERATION));
        forMag.add(potion(2, Potions.HEALING));
        forMag.add(crystal(2, AspectRegistry.VITIUM));
        forMag.add(t(3, Items.EXPERIENCE_BOTTLE));
        forMag.add(t(3, Items.EXPERIENCE_BOTTLE));
        forMag.add(crystal(3, AspectRegistry.AURAM));
        forMag.add(t(3, Items.GOLDEN_APPLE));
        forMag.add(t(4, ModItems.CLOTH_BOOTS));
        forMag.add(t(4, ModItems.CLOTH_CHEST));
        forMag.add(t(4, ModItems.CLOTH_LEGS));
        forMag.add(t(5, Items.ENCHANTED_GOLDEN_APPLE));
        forMag.add(t(5, ModItems.PECH_WAND));
        forMag.add(t(5, ModItems.CURIO_KNOWLEDGE));
        forMag.add(t(5, ModItems.AMULET_VIS));
        // (TC6 adds these three to the forager list while building the others)
        forInv.add(t(5, Items.TOTEM_OF_UNDYING));

        List<Supplier<? extends net.minecraft.world.level.ItemLike>> candles = List.of(ModItems.BLOCK_CANDLE_BLACK, ModItems.BLOCK_CANDLE_RED, ModItems.BLOCK_CANDLE_GREEN,
                ModItems.BLOCK_CANDLE_BROWN, ModItems.BLOCK_CANDLE_BLUE, ModItems.BLOCK_CANDLE_PURPLE, ModItems.BLOCK_CANDLE_CYAN, ModItems.BLOCK_CANDLE_SILVER,
                ModItems.BLOCK_CANDLE_GRAY, ModItems.BLOCK_CANDLE_PINK, ModItems.BLOCK_CANDLE_LIME, ModItems.BLOCK_CANDLE_YELLOW, ModItems.BLOCK_CANDLE_LIGHTBLUE,
                ModItems.BLOCK_CANDLE_MAGENTA, ModItems.BLOCK_CANDLE_ORANGE);
        for (var c : candles) forArc.add(t(1, c));
        forArc.add(t(2, Items.GHAST_TEAR));
        forInv.add(t(2, Items.COMPASS));
        forArc.add(book(2, Enchantments.POWER));
        forArc.add(t(3, Items.EXPERIENCE_BOTTLE));
        forArc.add(t(3, Items.EXPERIENCE_BOTTLE));
        forArc.add(t(3, Items.GOLDEN_APPLE));
        // (the eldritch eye, value 4, is not in the port)
        forArc.add(t(4, Items.ENCHANTED_GOLDEN_APPLE));
        forInv.add(t(4, Items.SPECTRAL_ARROW));
        forArc.add(t(5, ModItems.BAUBLES_RING_APPRENTICE));
        forArc.add(book(5, Enchantments.FLAME));
        forArc.add(book(5, Enchantments.INFINITY));
        forArc.add(t(5, ModItems.CURIO_KNOWLEDGE));
        return switch (type) { case 1 -> forMag; case 2 -> forArc; default -> forInv; };
    }

    private PechTrades() {}
}
