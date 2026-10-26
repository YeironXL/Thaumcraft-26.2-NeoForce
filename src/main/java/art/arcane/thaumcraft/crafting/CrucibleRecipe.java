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

public record CrucibleRecipe(Ingredient catalyst,ItemStackTemplate result,String research,Map<String,Integer> fixedAspects,String catalystAspect,String aspectsOf,String aspectsMinus) implements Recipe<SingleRecipeInput> {
    public static final MapCodec<CrucibleRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("catalyst").forGetter(CrucibleRecipe::catalyst),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(CrucibleRecipe::result),
            Codec.STRING.fieldOf("research").forGetter(CrucibleRecipe::research),
            Codec.unboundedMap(Codec.STRING,Codec.intRange(1,1000000)).optionalFieldOf("aspects",Map.of()).forGetter(CrucibleRecipe::fixedAspects),
            Codec.STRING.optionalFieldOf("catalyst_aspect","").forGetter(CrucibleRecipe::catalystAspect),
            // ConfigRecipes.postAspects: costs equal to an item's object aspects (optionally minus another item's).
            Codec.STRING.optionalFieldOf("aspects_of","").forGetter(CrucibleRecipe::aspectsOf),
            Codec.STRING.optionalFieldOf("aspects_minus","").forGetter(CrucibleRecipe::aspectsMinus)
    ).apply(i,CrucibleRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf,CrucibleRecipe> STREAM_CODEC=StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC,CrucibleRecipe::catalyst,ItemStackTemplate.STREAM_CODEC,CrucibleRecipe::result,
            ByteBufCodecs.STRING_UTF8,CrucibleRecipe::research,
            ByteBufCodecs.map(java.util.HashMap::new,ByteBufCodecs.STRING_UTF8,ByteBufCodecs.VAR_INT),CrucibleRecipe::fixedAspects,ByteBufCodecs.STRING_UTF8,CrucibleRecipe::catalystAspect,
            ByteBufCodecs.STRING_UTF8,CrucibleRecipe::aspectsOf,ByteBufCodecs.STRING_UTF8,CrucibleRecipe::aspectsMinus,CrucibleRecipe::new);
    public CrucibleRecipe { fixedAspects=Map.copyOf(fixedAspects); }
    public CrucibleRecipe(Ingredient catalyst,ItemStackTemplate result,String research,Map<String,Integer> aspects,String catalystAspect){this(catalyst,result,research,aspects,catalystAspect,"","");}
    /** Recipe cost; dynamic costs follow the current object-aspect table (TC6 postAspects). */
    public Map<String,Integer> aspects(){
        if(aspectsOf.isEmpty())return fixedAspects;
        var out=new java.util.LinkedHashMap<String,Integer>();
        art.arcane.thaumcraft.scan.ScanRegistry.get(aspectsOf).forEach((a,n)->out.merge(a.id(),n,Integer::sum));
        if(!aspectsMinus.isEmpty())art.arcane.thaumcraft.scan.ScanRegistry.get(aspectsMinus).forEach((a,n)->{int left=out.getOrDefault(a.id(),0)-n;if(left<=0)out.remove(a.id());else out.put(a.id(),left);});
        return java.util.Collections.unmodifiableMap(out);
    }
    /** TC6 chooses the affordable known recipe with the greatest total aspect cost; ties keep first. */
    public static CrucibleRecipe select(Iterable<CrucibleRecipe> recipes,java.util.function.Predicate<CrucibleRecipe> eligible){
        long highest=0;CrucibleRecipe chosen=null;
        for(var recipe:recipes){if(!eligible.test(recipe))continue;long total=recipe.aspects().values().stream().mapToLong(Integer::longValue).sum();if(total>highest){highest=total;chosen=recipe;}}
        return chosen;
    }
    @Override public boolean matches(SingleRecipeInput input,Level level) { return catalyst.test(input.item()) && (catalystAspect.isEmpty() || catalystAspect.equals(art.arcane.thaumcraft.item.CrystalEssenceItem.aspect(input.item()))); }
    @Override public ItemStack assemble(SingleRecipeInput input) { return result.create(); }
    @Override public RecipeSerializer<CrucibleRecipe> getSerializer() { return ModRecipes.CRUCIBLE.get(); }
    @Override public RecipeType<CrucibleRecipe> getType() { return ModRecipes.CRUCIBLE_TYPE.get(); }
    @Override public boolean showNotification() { return false; }
    @Override public String group() { return "thaumcraft:crucible"; }
    @Override public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }
    /** Not a recipe-book recipe: keeps vanilla from logging it as unplaceable and from blocking it under limitedCrafting. */
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
}
