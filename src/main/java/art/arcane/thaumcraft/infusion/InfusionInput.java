package art.arcane.thaumcraft.infusion;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** The central pedestal item plus the items on the surrounding pedestals. */
public record InfusionInput(ItemStack central, List<ItemStack> components) implements RecipeInput {
    @Override public ItemStack getItem(int index) { return index == 0 ? central : components.get(index - 1); }
    @Override public int size() { return components.size() + 1; }
}
