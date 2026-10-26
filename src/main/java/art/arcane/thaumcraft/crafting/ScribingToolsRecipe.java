package art.arcane.thaumcraft.crafting;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.registry.ModRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** TC6: bottle or empty phial, feather and black dye; refill uses tools and black dye. */
public final class ScribingToolsRecipe extends CustomRecipe {
    private final boolean refill;
    public ScribingToolsRecipe(boolean refill){this.refill=refill;}
    private static boolean ink(ItemStack stack){return stack.is(Items.INK_SAC) || stack.is(Items.DYE.black())
        || stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,net.minecraft.resources.Identifier.fromNamespaceAndPath("c","dyes/black")));}
    @Override public boolean matches(CraftingInput input,Level level){
        boolean container=false,feather=false,dye=false;
        for(int i=0;i<input.size();i++){
            var stack=input.getItem(i);if(stack.isEmpty())continue;
            if(ink(stack)){if(dye)return false;dye=true;}
            else if(refill && stack.is(ModItems.SCRIBING_TOOLS.get()) && stack.isDamaged()){if(container)return false;container=true;}
            else if(!refill && (stack.is(Items.GLASS_BOTTLE) || stack.is(ModItems.PHIAL_EMPTY.get()))){if(container)return false;container=true;}
            else if(!refill && stack.is(Items.FEATHER)){if(feather)return false;feather=true;}
            else return false;
        }
        return container && dye && (refill || feather);
    }
    @Override public ItemStack assemble(CraftingInput input){
        if(refill)for(int i=0;i<input.size();i++)if(input.getItem(i).is(ModItems.SCRIBING_TOOLS.get())){
            var result=input.getItem(i).copyWithCount(1);result.setDamageValue(0);return result;
        }
        return new ItemStack(ModItems.SCRIBING_TOOLS.get());
    }
    @Override public RecipeSerializer<ScribingToolsRecipe> getSerializer(){return refill?ModRecipes.SCRIBING_REFILL.get():ModRecipes.SCRIBING_CRAFT.get();}
}
