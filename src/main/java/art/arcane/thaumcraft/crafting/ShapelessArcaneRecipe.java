package art.arcane.thaumcraft.crafting;

import art.arcane.thaumcraft.registry.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.List;
import java.util.Map;

public record ShapelessArcaneRecipe(List<Ingredient> ingredients, ItemStackTemplate result, String research, int vis, Map<String,Integer> crystals)
        implements ArcaneCraftingRecipe {
    public static final MapCodec<ShapelessArcaneRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.listOf(1,9).fieldOf("ingredients").forGetter(ShapelessArcaneRecipe::ingredients),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(ShapelessArcaneRecipe::result),
            Codec.STRING.fieldOf("research").forGetter(ShapelessArcaneRecipe::research),
            Codec.intRange(0,1000000).fieldOf("vis").forGetter(ShapelessArcaneRecipe::vis),
            Codec.unboundedMap(Codec.STRING,Codec.intRange(1,64)).optionalFieldOf("crystals",Map.of()).forGetter(ShapelessArcaneRecipe::crystals)
    ).apply(i,ShapelessArcaneRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf,ShapelessArcaneRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()),ShapelessArcaneRecipe::ingredients,
            ItemStackTemplate.STREAM_CODEC,ShapelessArcaneRecipe::result,ByteBufCodecs.STRING_UTF8,ShapelessArcaneRecipe::research,
            ByteBufCodecs.VAR_INT,ShapelessArcaneRecipe::vis,
            ByteBufCodecs.map(java.util.HashMap::new,ByteBufCodecs.STRING_UTF8,ByteBufCodecs.VAR_INT),ShapelessArcaneRecipe::crystals,ShapelessArcaneRecipe::new);
    public ShapelessArcaneRecipe {
        ingredients = List.copyOf(ingredients); crystals = Map.copyOf(crystals);
        if (!java.util.Set.of("aer","terra","ignis","aqua","ordo","perditio").containsAll(crystals.keySet()))
            throw new IllegalArgumentException("Arcane crafting accepts only primal crystals");
    }
    @Override public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() != ingredients.size()) return false;
        return net.neoforged.neoforge.common.util.RecipeMatcher.findMatches(input.items().stream().filter(s -> !s.isEmpty()).toList(),ingredients) != null;
    }
    @Override public ItemStack assemble(CraftingInput input) { return result.create(); }
    @Override public RecipeSerializer<ShapelessArcaneRecipe> getSerializer() { return ModRecipes.SHAPELESS_ARCANE.get(); }
    @Override public RecipeType<ArcaneCraftingRecipe> getType() { return ModRecipes.ARCANE_TYPE.get(); }
    @Override public boolean showNotification() { return false; }
    @Override public String group() { return "thaumcraft:arcane"; }
    @Override public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }
    /** Not a recipe-book recipe: keeps vanilla from logging it as unplaceable and from blocking it under limitedCrafting. */
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
}
