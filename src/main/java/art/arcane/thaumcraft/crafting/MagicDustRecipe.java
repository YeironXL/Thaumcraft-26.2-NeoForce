package art.arcane.thaumcraft.crafting;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.item.CrystalEssenceItem;
import art.arcane.thaumcraft.registry.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import java.util.HashSet;

/** Three distinct crystal aspects, redstone, bowl and flint; bowl/flint are returned. */
public final class MagicDustRecipe extends CustomRecipe {
    @Override public boolean matches(CraftingInput input, Level level) {
        boolean bowl = false, flint = false, redstone = false;
        var crystals = new HashSet<String>();
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            if (stack.is(Items.BOWL)) { if (bowl) return false; bowl = true; }
            else if (stack.is(Items.FLINT)) { if (flint) return false; flint = true; }
            else if (stack.is(Items.REDSTONE)) { if (redstone) return false; redstone = true; }
            else { String aspect = CrystalEssenceItem.aspect(stack); if (aspect.isEmpty() || !crystals.add(aspect) || crystals.size() > 3) return false; }
        }
        return bowl && flint && redstone && crystals.size() == 3;
    }
    @Override public ItemStack assemble(CraftingInput input) { return new ItemStack(ModItems.SALIS_MUNDUS.get()); }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        var result = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int slot = 0; slot < input.size(); slot++) {
            var stack = input.getItem(slot);
            if (stack.is(Items.BOWL) || stack.is(Items.FLINT)) result.set(slot, stack.copyWithCount(1));
            else if (stack.getCraftingRemainder() != null) result.set(slot, stack.getCraftingRemainder().create());
        }
        return result;
    }
    @Override public RecipeSerializer<MagicDustRecipe> getSerializer() { return ModRecipes.MAGIC_DUST.get(); }
}
