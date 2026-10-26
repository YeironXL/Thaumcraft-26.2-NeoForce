package art.arcane.thaumcraft.block.world;

import art.arcane.thaumcraft.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** TC6 BlockStonePorous: (rand(15)+fortune) > 13 gives a weighted treasure, otherwise gravel. */
public final class StonePorousBlock extends Block {
    public StonePorousBlock(Properties p) { super(p); }

    private record W(ItemStack stack, int weight) {}

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        var level = params.getLevel();
        var r = level.getRandom();
        int fortune = 0;
        net.minecraft.world.item.ItemInstance tool = params.getOptionalParameter(LootContextParams.TOOL);
        if (tool != null) fortune = EnchantmentHelper.getItemEnchantmentLevel(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), tool);
        List<ItemStack> ret = new ArrayList<>();
        if (r.nextInt(15) + fortune > 13) {
            List<W> drops = new ArrayList<>();
            for (var a : art.arcane.thaumcraft.aspect.AspectRegistry.all().values()) {
                if (a.parentA() == null) continue; // Aspect.getCompoundAspects()
                drops.add(new W(art.arcane.thaumcraft.item.CrystalEssenceItem.of(a.id(), 1), 1));
            }
            drops.add(new W(new ItemStack(ModItems.AMBER.get()), 20));
            drops.add(new W(new ItemStack(ModItems.CLUSTER_IRON.get()), 20));
            drops.add(new W(new ItemStack(ModItems.CLUSTER_GOLD.get()), 10));
            drops.add(new W(new ItemStack(ModItems.CLUSTER_CINNABAR.get()), 10));
            drops.add(new W(new ItemStack(ModItems.CLUSTER_COPPER.get()), 10)); // vanilla copper ingot always exists
            drops.add(new W(new ItemStack(Items.DIAMOND), 2));
            drops.add(new W(new ItemStack(Items.EMERALD), 4));
            drops.add(new W(new ItemStack(Items.REDSTONE), 8));
            drops.add(new W(new ItemStack(Items.PRISMARINE_CRYSTALS), 3));
            drops.add(new W(new ItemStack(Items.PRISMARINE_SHARD), 3));
            drops.add(new W(new ItemStack(Items.CLAY_BALL), 30));
            drops.add(new W(new ItemStack(Items.QUARTZ), 15));
            int total = drops.stream().mapToInt(W::weight).sum(), pick = r.nextInt(total);
            for (W w : drops) { pick -= w.weight; if (pick < 0) { ret.add(w.stack.copy()); break; } }
        } else ret.add(new ItemStack(Items.GRAVEL));
        return ret;
    }
}
