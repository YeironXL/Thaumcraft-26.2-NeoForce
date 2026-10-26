package art.arcane.thaumcraft.scan;

import java.util.Map;
import art.arcane.thaumcraft.aspect.Aspect;
import art.arcane.thaumcraft.aspect.ObjectAspects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Item-id view of the TC6 object aspect table (see {@link ObjectAspects}). */
public final class ScanRegistry {
    public static Map<Aspect, Integer> get(String id) {
        var key=Identifier.tryParse(id.contains("|")?id.substring(0,id.indexOf('|')):id);
        if(key==null)return Map.of();
        var item=BuiltInRegistries.ITEM.getValue(key);
        if(item==null||item==Items.AIR)return Map.of();
        return ObjectAspects.get(new ItemStack(item));
    }
    public static Map<Aspect, Integer> get(ItemStack stack) { return ObjectAspects.get(stack); }
    public static Map<String, Map<Aspect, Integer>> all() { return ObjectAspects.table(); }
    public static void installDerived(Map<String,Map<String,Integer>> values){ ObjectAspects.install(values); }
    public static void clearDerived(){ ObjectAspects.clear(); }
    private ScanRegistry() {}
}
