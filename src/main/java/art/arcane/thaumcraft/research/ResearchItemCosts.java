package art.arcane.thaumcraft.research;

import art.arcane.thaumcraft.registry.ModComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.Map;

/** Plan the complete removal by slot before changing anything; specific aspects take priority. */
public final class ResearchItemCosts {
    public static boolean matches(ItemStack stack,String key){
        String[] parts=key.split("@",-1);
        if(parts.length>2)return false;
        var id=Identifier.tryParse(parts[0]);if(id==null)return false;
        var item=BuiltInRegistries.ITEM.getValue(id);
        if(item==null || item==Items.AIR || !stack.is(item))return false;
        if(parts.length==1)return true;
        if(!parts[0].equals("thaumcraft:crystal_essence") && !parts[0].equals("thaumcraft:phial_filled"))return false;
        return art.arcane.thaumcraft.aspect.AspectRegistry.get(parts[1])!=null && parts[1].equals(stack.get(ModComponents.ASPECT.get()));
    }
    public static boolean consume(Container inventory,Map<String,Integer> demands){return apply(inventory,demands,true);}
    public static boolean has(Container inventory,Map<String,Integer> demands){return apply(inventory,demands,false);}
    private static boolean apply(Container inventory,Map<String,Integer> demands,boolean remove){
        int[] removals=new int[inventory.getContainerSize()];
        var ordered=demands.entrySet().stream().sorted(java.util.Comparator.comparingInt(e->e.getKey().contains("@")?0:1)).toList();
        for(var demand:ordered){
            if(demand.getValue()==null || demand.getValue()<1)return false;
            int remaining=demand.getValue();
            for(int slot=0;slot<removals.length && remaining>0;slot++){
                var stack=inventory.getItem(slot);
                if(!matches(stack,demand.getKey()))continue;
                int moved=Math.min(remaining,stack.getCount()-removals[slot]);
                removals[slot]+=moved;remaining-=moved;
            }
            if(remaining>0)return false;
        }
        if(!remove)return true;
        for(int slot=0;slot<removals.length;slot++)if(removals[slot]>0)inventory.removeItem(slot,removals[slot]);
        inventory.setChanged();return true;
    }
    private ResearchItemCosts(){}
}
