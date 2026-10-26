package art.arcane.thaumcraft.client;

import art.arcane.thaumcraft.casters.FocusEngine;
import art.arcane.thaumcraft.casters.FocusNode;
import art.arcane.thaumcraft.research.LegacyItemReferences;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** TC6 GuiResearchBrowser.drawResearchIcon, shared by the research browser and the research toast. */
public final class ResearchIcons {
    private static final java.util.Map<Identifier,int[]> SIZES=new java.util.concurrent.ConcurrentHashMap<>();

    /** Legacy entry point (toast): full-bright icon. */
    public static void draw(GuiGraphicsExtractor g,String key,int x,int y,int tint){draw(g,key,x,y,(tint&0xFFFFFF)!=0xFFFFFF);}

    public static void draw(GuiGraphicsExtractor g,String key,int iconX,int iconY,boolean bw){
        var node=ResearchMapData.node(key);
        if(node==null||node.icons().isEmpty())return;
        String raw=node.icons().get((int)(System.currentTimeMillis()/1000L%node.icons().size()));
        if(raw.startsWith("focus")){
            String k=raw.replaceAll("focus:","");
            var fp=FocusEngine.getElement(k.trim());
            if(fp instanceof FocusNode fn)FocalManipulatorScreen.drawPart(g,fn,iconX+8,iconY+8,24.0f,false);
            return;
        }
        if(raw.contains("textures")||raw.endsWith(".png")){
            Identifier texture=Identifier.tryParse(raw.replace("textures/items/","textures/item/").replace("textures/blocks/","textures/block/"));
            if(texture==null||Minecraft.getInstance().getResourceManager().getResource(texture).isEmpty())return;
            int color=bw?0xFF333333:0xFFFFFFFF;
            int[] size=size(texture);int w=size[0],h=size[1];
            if(h>w&&h%w==0){
                int m=h/w;int frame=(int)(System.currentTimeMillis()/150L%m);
                g.blit(RenderPipelines.GUI_TEXTURED,texture,iconX,iconY,0,frame*16,16,16,16,16*m,color);
            }else if(w>h&&w%h==0){
                int m=w/h;int frame=(int)(System.currentTimeMillis()/150L%m);
                g.blit(RenderPipelines.GUI_TEXTURED,texture,iconX,iconY,frame*16,0,16,16,16*m,16,color);
            }else g.blit(RenderPipelines.GUI_TEXTURED,texture,iconX,iconY,0,0,16,16,16,16,color);
            return;
        }
        // ItemStack icons (InventoryUtils.cycleItemStack): item rendering ignores the GL colour in TC6.
        Identifier id=Identifier.tryParse(LegacyItemReferences.item(raw));
        if(id==null)return;
        var item=BuiltInRegistries.ITEM.getValue(id);
        if(item!=null&&item!=Items.AIR)g.item(new ItemStack(item),iconX,iconY);
    }
    private static int[] size(Identifier id){
        return SIZES.computeIfAbsent(id,key->{
            try(var stream=Minecraft.getInstance().getResourceManager().open(key);var input=new java.io.DataInputStream(stream)){
                input.skipNBytes(16);return new int[]{input.readInt(),input.readInt()};
            }catch(java.io.IOException exception){return new int[]{16,16};}
        });
    }
    private ResearchIcons(){}
}
