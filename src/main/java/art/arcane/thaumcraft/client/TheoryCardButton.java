package art.arcane.thaumcraft.client;

import art.arcane.thaumcraft.menu.ResearchTableMenu;
import art.arcane.thaumcraft.research.ResearchCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** A clickable paper choice; the menu still sends the original server action. */
final class TheoryCardButton extends Button {
    private final ResearchTableMenu menu;
    private final int choice;
    TheoryCardButton(Builder builder, ResearchTableMenu menu, int choice) {
        super(builder); this.menu=menu; this.choice=choice;
    }
    static Identifier categoryIcon(int category) {
        String name=ResearchCatalog.CATEGORIES.get(Math.clamp(category,0,ResearchCatalog.CATEGORIES.size()-1));
        if(name.equals("BASICS"))return Identifier.parse("thaumcraft:textures/item/thaumonomicon_cheat.png");
        return Identifier.parse("thaumcraft:textures/research/cat_"+name.toLowerCase(java.util.Locale.ROOT)+".png");
    }
    @Override protected void extractContents(GuiGraphicsExtractor g,int mouseX,int mouseY,float tick) {
        var font=Minecraft.getInstance().font;
        int cx=getX()+50,cy=getY()+60;
        float scale=(isHoveredOrFocused()?6.3F:6F);
        g.pose().pushMatrix();g.pose().translate(cx,cy);g.pose().scale(scale,scale);
        g.blit(RenderPipelines.GUI_TEXTURED,Identifier.parse("thaumcraft:textures/gui/paper.png"),-8,-8,0,0,16,16,256,256,256,256);
        int category=menu.value(14+choice*4),size=category==0?32:256;
        g.blit(RenderPipelines.GUI_TEXTURED,categoryIcon(category),-4,-4,0,0,8,8,size,size,size,size,0x2AFFFFFF);
        g.pose().scale(.0625F,.0625F);
        var name=ThaumcraftTheme.ink(getMessage()).copy().withStyle(net.minecraft.ChatFormatting.BOLD);
        g.text(font,name,-font.width(name)/2,-65,0xFF000000,false);
        var description=font.split(ThaumcraftTheme.ink(TheoryCardText.help(menu,choice)),140);
        for(int i=0;i<Math.min(8,description.size());i++)g.text(font,description.get(i),-70,-48+i*9,0xFF000000,false);
        int cost=menu.value(16+choice*4);
        for(int i=0;i<Math.abs(cost);i++)g.blit(RenderPipelines.GUI_TEXTURED,Identifier.parse("thaumcraft:textures/gui/gui_base.png"),-10*Math.abs(cost)+20*i,-95,32,96,16,16,256,256);
        g.pose().popMatrix();
    }
}
