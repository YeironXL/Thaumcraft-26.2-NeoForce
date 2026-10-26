package art.arcane.thaumcraft.infusion;

import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.registry.ModRecipes;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * TC6 api.crafting.InfusionRecipe: what the runic matrix looks up (ThaumcraftCraftingManager.findMatchingInfusionRecipe).
 * Implementations: {@link StandardInfusionRecipe}, {@link InfusionEnchantmentRecipe}, {@link InfusionRunicAugmentRecipe}.
 */
public interface InfusionRecipe extends Recipe<InfusionInput> {
    /**
     * getRecipeOutput. {@code modifiesInput} is TC6's NBTBase output: the tag is set on the central item in place instead
     * of replacing it (so damage and other data are kept and the damage-ratio transfer is skipped).
     */
    record Output(ItemStack stack, boolean modifiesInput) {}

    String research();
    boolean matches(List<ItemStack> components, ItemStack central, Level level, Player player);
    Output output(Player player, ItemStack central, List<ItemStack> components);
    /** getAspects: aspect id to amount, in recipe order. */
    LinkedHashMap<String, Integer> aspects(Player player, ItemStack central, List<ItemStack> components);
    int instability(Player player, ItemStack central, List<ItemStack> components);

    @Override default boolean matches(InfusionInput input, Level level) { return matches(input.components(), input.central(), level, null); }
    @Override default ItemStack assemble(InfusionInput input) {
        Output out = output(null, input.central(), input.components());
        return out == null ? ItemStack.EMPTY : out.stack();
    }
    @Override default RecipeType<InfusionRecipe> getType() { return ModRecipes.INFUSION_TYPE.get(); }
    @Override default boolean showNotification() { return false; }
    @Override default String group() { return "thaumcraft:infusion"; }
    @Override default PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }
    /** Not a recipe-book recipe: keeps vanilla from logging it as unplaceable and from blocking it under limitedCrafting. */
    @Override default boolean isSpecial() { return true; }
    @Override default RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }

    /** IPlayerKnowledge.isResearchKnown ("KEY" known, "KEY@n" at stage n or later); null player only for research-free checks. */
    static boolean isResearchKnown(Player player, String research) {
        if (research == null) return false;
        if (research.isEmpty()) return true;
        if (!(player instanceof ServerPlayer sp)) return player == null;
        return PlayerProgressManager.get(sp).has(research);
    }

    /** ThaumcraftCapabilities.knowsResearch. */
    static boolean knowsResearch(Player player, String research) {
        if (!(player instanceof ServerPlayer sp)) return player == null;
        return art.arcane.thaumcraft.research.ResearchRules.knows(PlayerProgressManager.get(sp), research);
    }
}
