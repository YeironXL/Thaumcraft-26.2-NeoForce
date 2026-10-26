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
import java.util.Map;

public record ArcaneRecipe(ShapedRecipePattern pattern, ItemStackTemplate result, String research, int vis, Map<String, Integer> crystals, boolean keepComponents)
        implements ArcaneCraftingRecipe {
    public static final MapCodec<ArcaneRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ShapedRecipePattern.MAP_CODEC.forGetter(ArcaneRecipe::pattern), ItemStackTemplate.CODEC.fieldOf("result").forGetter(ArcaneRecipe::result),
            Codec.STRING.fieldOf("research").forGetter(ArcaneRecipe::research), Codec.intRange(0, 1000000).fieldOf("vis").forGetter(ArcaneRecipe::vis),
            Codec.unboundedMap(Codec.STRING, Codec.intRange(1, 64)).optionalFieldOf("crystals", Map.of()).forGetter(ArcaneRecipe::crystals),
            // TC6 ShapedArcaneVoidJar: the result keeps the data of the (single) non-empty input, e.g. jar contents.
            Codec.BOOL.optionalFieldOf("keep_components", false).forGetter(ArcaneRecipe::keepComponents)
    ).apply(instance, ArcaneRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ArcaneRecipe> STREAM_CODEC = StreamCodec.composite(
            ShapedRecipePattern.STREAM_CODEC, ArcaneRecipe::pattern, ItemStackTemplate.STREAM_CODEC, ArcaneRecipe::result,
            ByteBufCodecs.STRING_UTF8, ArcaneRecipe::research, ByteBufCodecs.VAR_INT, ArcaneRecipe::vis,
            ByteBufCodecs.map(java.util.HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT), ArcaneRecipe::crystals,
            ByteBufCodecs.BOOL, ArcaneRecipe::keepComponents, ArcaneRecipe::new);
    public ArcaneRecipe {
        crystals = Map.copyOf(crystals);
        if (!java.util.Set.of("aer", "terra", "ignis", "aqua", "ordo", "perditio").containsAll(crystals.keySet()))
            throw new IllegalArgumentException("Arcane crafting accepts only primal crystals");
    }
    @Override public boolean matches(CraftingInput input, Level level) { return pattern.matches(input); }
    @Override public ItemStack assemble(CraftingInput input) {
        ItemStack out = result.create();
        if (keepComponents) for (int i = 0; i < input.size(); i++) {
            ItemStack in = input.getItem(i);
            if (!in.isEmpty()) { out.applyComponents(in.getComponentsPatch()); break; }
        }
        return out;
    }
    @Override public RecipeSerializer<ArcaneRecipe> getSerializer() { return ModRecipes.ARCANE.get(); }
    @Override public RecipeType<ArcaneCraftingRecipe> getType() { return ModRecipes.ARCANE_TYPE.get(); }
    @Override public boolean showNotification() { return false; }
    @Override public String group() { return "thaumcraft:arcane"; }
    @Override public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }
    /** Not a recipe-book recipe: keeps vanilla from logging it as unplaceable and from blocking it under limitedCrafting. */
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
}
