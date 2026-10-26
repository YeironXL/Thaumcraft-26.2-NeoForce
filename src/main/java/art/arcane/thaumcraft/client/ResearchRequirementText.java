package art.arcane.thaumcraft.client;

import art.arcane.thaumcraft.item.CrystalEssenceItem;
import art.arcane.thaumcraft.item.EssentiaPhialItem;
import art.arcane.thaumcraft.research.LegacyItemReferences;
import art.arcane.thaumcraft.research.ResearchCatalog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Show authored costs as localized game objects instead of registry names and legacy NBT. */
public final class ResearchRequirementText {
    public static Component format(String type,String value){
        if(type.equals("required_item") || type.equals("required_craft")){
            String key=type.equals("required_item")?LegacyItemReferences.requirementKey(value):LegacyItemReferences.item(value);
            if(key.isEmpty())return Component.translatable("screen.thaumcraft.unavailable_item");
            String[] descriptor=key.split("@",2);
            var item=BuiltInRegistries.ITEM.getValue(Identifier.parse(descriptor[0]));
            if(item==null || item==Items.AIR)return Component.translatable("screen.thaumcraft.unavailable_item");
            Component name=descriptor.length==2?Component.translatable(descriptor[0].equals("thaumcraft:crystal_essence")?"item.thaumcraft.crystal_essence.aspect":"item.thaumcraft.phial_filled.aspect",Component.translatable(descriptor[1])):Component.translatable(item.getDescriptionId());
            int amount=1;String[] original=value.split(";");
            try{if(original.length>1)amount=Integer.parseInt(original[1]);}catch(NumberFormatException ignored){}
            return Component.translatable("screen.thaumcraft.item_cost",amount,name);
        }
        if(type.equals("required_knowledge")){
            String[] parts=value.split(";");
            if(parts.length==3)return Component.translatable("screen.thaumcraft.knowledge_cost",parts[2],
                Component.translatable(parts[0].equals("THEORY")?"screen.thaumcraft.theory_points":"screen.thaumcraft.observation_points"),
                Component.translatable("tc.research_category."+parts[1]));
        }
        if(type.equals("required_research")){
            String[] parts=value.split("@",2);var entry=ResearchCatalog.entries().get(parts[0]);
            Component name=entry==null?Component.literal(parts[0]):Component.translatable(entry.name());
            if(parts[0].startsWith("!") && art.arcane.thaumcraft.aspect.AspectRegistry.get(parts[0].substring(1))!=null)
                name=Component.translatable("screen.thaumcraft.aspect_discovery",Component.translatable(parts[0].substring(1)));
            return parts.length==2?Component.translatable("screen.thaumcraft.research_stage",name,parts[1]):name;
        }
        return Component.literal(value);
    }
    private ResearchRequirementText(){}
}
