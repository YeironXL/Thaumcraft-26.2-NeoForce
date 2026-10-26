package art.arcane.thaumcraft.research;

import art.arcane.thaumcraft.item.CrystalEssenceItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import java.util.LinkedHashMap;
import java.util.Map;

/** Inventory/experience effects are performed only after the complete card cost is validated. */
public interface TheoryResources {
    default boolean consumeCelestialNotes(int first,int second){return false;}
    default boolean consumeScribingSupplies(){return false;}
    default java.util.List<Integer> curioCandidates(){return java.util.stream.IntStream.range(0,CurioTheory.IDS.size()).filter(n->hasItem(CurioTheory.IDS.get(n))).boxed().toList();}
    default boolean consumeItem(String id){return false;}
    default boolean hasItem(String id){return false;}
    int experienceLevels();
    boolean crystals(Map<String,Integer> aspects,boolean consume);
    void removeLevels(int amount);
    void giveCrystal(String aspect);
    RandomSource random();

    static TheoryResources none(){return new TheoryResources(){
        public int experienceLevels(){return 0;}
        public boolean crystals(Map<String,Integer> aspects,boolean consume){return false;}
        public void removeLevels(int amount){}public void giveCrystal(String aspect){}
        public RandomSource random(){return RandomSource.create();}
    };}
    static TheoryResources player(ServerPlayer player){return player(player,()->false);}
    static TheoryResources player(ServerPlayer player,java.util.function.BooleanSupplier scribing){return new TheoryResources(){
        public boolean consumeCelestialNotes(int first,int second){
            int a=-1,b=-1;
            for(int slot=0;slot<36;slot++){
                var stack=player.getInventory().getItem(slot);
                if(!stack.is(art.arcane.thaumcraft.ModItems.CELESTIAL_NOTES.get()))continue;
                int variant=stack.getOrDefault(art.arcane.thaumcraft.registry.ModComponents.CELESTIAL_NOTE.get(),0);
                if(variant==first&&a<0)a=slot;if(variant==second&&b<0)b=slot;
            }
            if(first==second||a<0||b<0)return false;
            player.getInventory().getItem(a).shrink(1);player.getInventory().getItem(b).shrink(1);return true;
        }
        public boolean consumeScribingSupplies(){return scribing.getAsBoolean();}
        public java.util.List<Integer> curioCandidates(){
            var candidates=new java.util.ArrayList<Integer>();
            for(int slot=0;slot<36;slot++){
                var stack=player.getInventory().getItem(slot);if(stack.isEmpty())continue;
                int variant=CurioTheory.IDS.indexOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                if(variant>=0)candidates.add(variant);
            }
            return candidates;
        }
        public boolean consumeItem(String id){return ResearchItemCosts.consume(player.getInventory(),Map.of(id,1));}
        public boolean hasItem(String id){return ResearchItemCosts.has(player.getInventory(),Map.of(id,1));}
        public int experienceLevels(){return player.experienceLevel;}
        public boolean crystals(Map<String,Integer> aspects,boolean consume){
            var demands=new LinkedHashMap<String,Integer>();aspects.forEach((tag,n)->demands.put("thaumcraft:crystal_essence@"+tag,n));
            return consume?ResearchItemCosts.consume(player.getInventory(),demands):ResearchItemCosts.has(player.getInventory(),demands);
        }
        public void removeLevels(int amount){player.giveExperienceLevels(-amount);}
        public void giveCrystal(String aspect){var stack=CrystalEssenceItem.of(aspect,1);if(!player.getInventory().add(stack))player.drop(stack,false);}
        public RandomSource random(){return player.getRandom();}
    };}
}
