package art.arcane.thaumcraft.client;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
/** Legacy GuiImageButton atlas region and half-size caption. */
final class OriginalImageButton extends Button {
 private final int u,v,color;OriginalImageButton(Builder builder,int u,int v,int color){super(builder);this.u=u;this.v=v;this.color=color;}
 @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float tick){
  boolean hover=mx>=getX()&&my>=getY()&&mx<getX()+getWidth()&&my<getY()+getHeight();
  float brightness=active?(hover?1F:.9F):.5F;
  int tint=(active?0xFF000000:0xE5000000)|((int)(((color>>16)&255)*brightness)<<16)|((int)(((color>>8)&255)*brightness)<<8)|(int)((color&255)*brightness);
  g.blit(RenderPipelines.GUI_TEXTURED,Identifier.parse("thaumcraft:textures/gui/gui_base.png"),getX()-1,getY()-1,u,v,getWidth()+2,getHeight()+2,256,256,tint);
  var font=Minecraft.getInstance().font;var text=ThaumcraftTheme.ink(getMessage());
  g.pose().pushMatrix();g.pose().translate(getX()+getWidth()/2F,getY()+getHeight()/2F);g.pose().scale(.5F,.5F);
  g.text(font,text,-font.width(text)/2,-4,active?(hover?0xFFFFFFA0:0xFFFFFFFF):0xFFA0A0A0,true);g.pose().popMatrix();
 }
}
