package art.arcane.thaumcraft.client;

import net.minecraft.network.chat.Component;
import art.arcane.thaumcraft.research.ResearchCatalog;

/** Human-readable effects and component-aware crystal requirements for the native theory menu. */
public final class TheoryCardText {
    private static final String[] CARDS={"experimentation","analyze","ponder","inspired","rethink","reject","balance","study","notation","concentrate","reactions","synthesis","focus","awareness","spellbinding","tinker","scripting","curio","revelation","truth","realization","celestial"};
    public static Component name(int kind){return kind>=0&&kind<CARDS.length?Component.translatable("screen.thaumcraft.theory.card."+CARDS[kind]):Component.empty();}
    private static Component aspectName(int index){return index>=0&&index<art.arcane.thaumcraft.research.TheorySession.ASPECTS.size()?Component.translatable(art.arcane.thaumcraft.research.TheorySession.ASPECTS.get(index)):Component.empty();}
    public static Component help(art.arcane.thaumcraft.menu.ResearchTableMenu menu,int card){
        int kind=menu.value(13+card*4);if(kind<0||kind>=CARDS.length)return Component.empty();
        String key="screen.thaumcraft.theory.help."+CARDS[kind];
        if(kind==21){
            int first=menu.value(26+card*2),second=menu.value(27+card*2);
            return Component.translatable("screen.thaumcraft.theory.help.celestial",celestialName(first),celestialName(second),Component.translatable("tc.research_category."+ResearchCatalog.CATEGORIES.get(menu.value(14+card*4))));
        }
        if(kind==17){
            int variant=menu.value(26+card*2);var ids=art.arcane.thaumcraft.research.CurioTheory.IDS;
            if(variant<0||variant>=ids.size())return Component.empty();
            var item=new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(ids.get(variant))));
            return Component.translatable("screen.thaumcraft.theory.help.curio",item.getHoverName());
        }
        if(kind==16)return Component.translatable("screen.thaumcraft.theory.help.scripting");
        if(kind<9)return Component.translatable(key,Component.translatable("tc.research_category."+ResearchCatalog.CATEGORIES.get(menu.value(14+card*4))),menu.value(15+card*4));
        int first=menu.value(26+card*2),second=menu.value(27+card*2);
        if(kind==15&&first>=0&&first<art.arcane.thaumcraft.research.TinkerOptions.IDS.size()){var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(art.arcane.thaumcraft.research.TinkerOptions.IDS.get(first)));int minimum=menu.value(15+card*4);return Component.translatable(key,minimum,minimum+10,new net.minecraft.world.item.ItemStack(item).getHoverName());}
        if(kind==9)return Component.translatable(key,aspectName(first));
        if(kind==10)return Component.translatable(key,aspectName(first),aspectName(second));
        if(kind==11&&first>=0&&first<art.arcane.thaumcraft.research.TheorySession.ASPECTS.size()){
            var compound=art.arcane.thaumcraft.aspect.AspectRegistry.get(art.arcane.thaumcraft.research.TheorySession.ASPECTS.get(first));
            if(compound!=null&&!compound.isPrimal())return Component.translatable(key,Component.translatable(compound.parentA().id()),Component.translatable(compound.parentB().id()),aspectName(first));
        }
        return Component.translatable(key);
    }
    private static Component celestialName(int type){return Component.translatable("item.celestial_notes."+(type==0?"sun":type<5?"stars_"+type:"moon_"+(type-4))+".text");}
    private TheoryCardText(){}
}

