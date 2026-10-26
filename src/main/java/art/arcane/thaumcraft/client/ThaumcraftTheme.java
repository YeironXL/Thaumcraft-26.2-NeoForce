package art.arcane.thaumcraft.client;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
public final class ThaumcraftTheme {
    private static final Identifier PAPER=Identifier.parse("thaumcraft:textures/gui/paper.png");
    private static final Identifier BOOK=Identifier.parse("thaumcraft:textures/gui/gui_researchbook.png");
    private static final Identifier SLOTS=Identifier.parse("thaumcraft:textures/gui/gui_smelter.png");
    public static void panel(GuiGraphicsExtractor g,int x,int y,int width,int height){g.fill(x-2,y-2,x+width+2,y+height+2,0xFF302016);g.fill(x,y,x+width,y+height,0xFF6C5030);g.blit(RenderPipelines.GUI_TEXTURED,PAPER,x+4,y+4,32,20,width-8,height-8,192,216,256,256);g.fill(x+8,y+21,x+width-8,y+22,0xFF9C8059);}
    public static void book(GuiGraphicsExtractor g,int x,int y,int width,int height){g.blit(RenderPipelines.GUI_TEXTURED,BOOK,x,y,0,0,width,height,512,362,512,512);}
    public static void slot(GuiGraphicsExtractor g,int x,int y){g.blit(RenderPipelines.GUI_TEXTURED,SLOTS,x-1,y-1,7,82,18,18,256,256);}
    /** UtilsFX.drawTag: 16px aspect icon tinted with the aspect colour, amount at half scale in the corner with a black outline. */
    public static void drawTag(GuiGraphicsExtractor g,net.minecraft.client.gui.Font font,String aspect,int x,int y,float amount){
        g.blit(RenderPipelines.GUI_TEXTURED,Identifier.parse("thaumcraft:textures/aspects/"+aspect+".png"),x,y,0,0,16,16,32,32,32,32,0xFF000000|art.arcane.thaumcraft.aspect.AspectRegistry.color(aspect));
        if(amount>0){
            String am=new java.text.DecimalFormat("#######.##").format(amount);int sw=font.width(am);
            g.pose().pushMatrix();g.pose().scale(0.5f,0.5f);
            int tx=32-sw+x*2,ty=32-font.lineHeight+y*2;
            for(var e:new int[][]{{0,1},{-1,0},{0,-1},{1,0}})g.text(font,am,tx+e[0],ty+e[1],0xFF000000,false);
            g.text(font,am,tx,ty,0xFFFFFFFF,false);
            g.pose().popMatrix();
        }
    }
    public static void aspect(GuiGraphicsExtractor g,String aspect,int x,int y){g.blit(RenderPipelines.GUI_TEXTURED,Identifier.parse("thaumcraft:textures/aspects/"+aspect+".png"),x,y,0,0,12,12,32,32,32,32);}
    public static void button(GuiGraphicsExtractor g,int x,int y,int width,int height,boolean hover,boolean active){g.fill(x-1,y-1,x+width+1,y+height+1,hover&&active?0xFFB78945:0xFF6B5439);g.blit(RenderPipelines.GUI_TEXTURED,BOOK,x,y,168,432,width,height,128,22,512,512,active?0xFFFFFFFF:0xFF99938D);}
    public static void workstation(GuiGraphicsExtractor g,int x,int y,int width,int inventoryX,boolean metal) {
        Identifier texture=Identifier.parse("thaumcraft:textures/gui/"+(metal?"gui_wandtable2.png":"gui_research_table.png"));
        g.blit(RenderPipelines.GUI_TEXTURED,texture,x,y,0,0,width,136,metal?224:255,metal?224:181,256,256);
        if(metal)g.blit(RenderPipelines.GUI_TEXTURED,Identifier.parse("thaumcraft:textures/gui/gui_wandtable.png"),x,y,0,0,width,136,224,224,256,256);
        Identifier inventory=Identifier.parse("thaumcraft:textures/gui/gui_base.png");
        g.blit(RenderPipelines.GUI_TEXTURED,inventory,x+inventoryX-8,y+142,0,168,176,88,256,256);
    }
    public static void card(GuiGraphicsExtractor g,int x,int y,int width,int height) {
        g.blit(RenderPipelines.GUI_TEXTURED,PAPER,x,y,32,20,width,height,192,216,256,256);
    }
    public static net.minecraft.network.chat.Component ink(net.minecraft.network.chat.Component text) {
        return text.copy().withStyle(style -> style.withFont(new net.minecraft.network.chat.FontDescription.Resource(Identifier.parse("minecraft:default"))).withBold(false));
    }
    public static net.minecraft.network.chat.Component ink(String text) { return ink(net.minecraft.network.chat.Component.literal(text)); }
    public static void text(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font, net.minecraft.network.chat.Component text, int x, int y, int color, boolean shadow) { g.text(font, ink(text), x, y, color, false); }
    public static void text(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font, String text, int x, int y, int color, boolean shadow) { g.text(font, ink(text), x, y, color, false); }
    public static void text(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font, net.minecraft.util.FormattedCharSequence text, int x, int y, int color, boolean shadow) { g.text(font, text, x, y, color, false); }
    private ThaumcraftTheme(){}
}
