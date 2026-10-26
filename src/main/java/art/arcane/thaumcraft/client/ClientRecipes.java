package art.arcane.thaumcraft.client;

import net.minecraft.world.item.crafting.RecipeMap;

/** The recipes the server synced to this client (OnDatapackSyncEvent.sendRecipes), for recipe viewers such as JEI. */
public final class ClientRecipes {
    private static volatile RecipeMap map = RecipeMap.EMPTY;
    public static RecipeMap get() { return map; }
    static void set(RecipeMap recipes) { map = recipes == null ? RecipeMap.EMPTY : recipes; }
    private ClientRecipes() {}
}
