package art.arcane.thaumcraft.crafting;

import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import java.util.Map;

public interface ArcaneCraftingRecipe extends Recipe<CraftingInput> {
    String research();
    int vis();
    Map<String,Integer> crystals();
}
