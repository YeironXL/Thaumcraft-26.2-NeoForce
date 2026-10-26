package art.arcane.thaumcraft.client;

import art.arcane.thaumcraft.menu.EssentiaSmelterMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class EssentiaSmelterScreen extends AbstractContainerScreen<EssentiaSmelterMenu> {
    private static final Identifier TEXTURE=Identifier.parse("thaumcraft:textures/gui/gui_smelter.png");
    public EssentiaSmelterScreen(EssentiaSmelterMenu menu,Inventory inventory,Component title){super(menu,inventory,title,176,166);}
    private void sprite(GuiGraphicsExtractor g,int x,int y,int u,int v,int width,int height){
        if(width>0&&height>0)g.blit(RenderPipelines.GUI_TEXTURED,TEXTURE,leftPos+x,topPos+y,u,v,width,height,256,256);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float tick){
        super.extractBackground(g,mouseX,mouseY,tick);
        sprite(g,0,0,0,0,176,166);
        int burn=Math.clamp(menu.fuelFraction()*20/1000,0,20);
        sprite(g,80,46-burn,176,20-burn,16,burn);
        int progress=Math.clamp(menu.progress()*46/1000,0,46);
        sprite(g,106,59-progress,216,46-progress,9,progress);
        int stored=Math.clamp(menu.stored()*48/256,0,48);
        sprite(g,61,60-stored,200,48-stored,8,stored);
        sprite(g,60,8,232,0,10,55);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mouseX,int mouseY){ThaumcraftTheme.text(g,font,title,8,-13,0xFFE5D6B3,false);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float tick){
        super.extractRenderState(g,mouseX,mouseY,tick);
        int x=mouseX-leftPos,y=mouseY-topPos;
        Component tip=null;
        if(x>=60&&x<70&&y>=8&&y<63)tip=Component.translatable("screen.thaumcraft.smelter.storage",menu.stored()).append(" / 256");
        else if(x>=106&&x<115&&y>=13&&y<60)tip=Component.translatable("screen.thaumcraft.smelter.progress",menu.progress()/10);
        else if(x>=80&&x<96&&y>=26&&y<46)tip=Component.translatable("screen.thaumcraft.smelter.burn",menu.fuelSeconds());
        else if(x>=50&&x<124&&y>=0&&y<70)tip=Component.translatable("screen.thaumcraft.smelter.efficiency",menu.efficiency());
        if(tip!=null)g.setTooltipForNextFrame(ThaumcraftTheme.ink(tip),mouseX,mouseY);
    }
}
