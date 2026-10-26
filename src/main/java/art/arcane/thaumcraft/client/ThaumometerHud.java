package art.arcane.thaumcraft.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import art.arcane.thaumcraft.network.ThaumometerAuraPayload;
/** Original HUD atlas/525-unit scale. Values are supplied only by the server. */
public final class ThaumometerHud {
 private static final Identifier HUD=Identifier.parse("thaumcraft:textures/gui/hud.png");
 private static ThaumometerAuraPayload aura;
 public static void receive(ThaumometerAuraPayload value){if(value.base()>=0&&Float.isFinite(value.vis())&&Float.isFinite(value.flux())&&value.vis()>=0&&value.flux()>=0)aura=value;}
 public static void clear(){aura=null;}
 public static ThaumometerAuraPayload current(){return aura;}
 public static void render(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event){
  var mc=Minecraft.getInstance();if(mc.player==null||aura==null||mc.gui.hud.isHidden())return;
  if(!(mc.player.getMainHandItem().getItem() instanceof art.arcane.thaumcraft.item.ThaumometerItem)&&!(mc.player.getOffhandItem().getItem() instanceof art.arcane.thaumcraft.item.ThaumometerItem))return;
  var g=event.getGuiGraphics();float base=Math.clamp(aura.base()/525F,0,1),vis=Math.clamp(aura.vis()/525F,0,1),flux=Math.clamp(aura.flux()/525F,0,1);
  if(vis+flux>1){float scale=1/(vis+flux);base*=scale;vis*=scale;flux*=scale;}
  int vh=Math.round(vis*64),fh=Math.round(flux*64);
  if(vh>0)g.blit(RenderPipelines.GUI_TEXTURED,HUD,7,74-vh,88,56,8,vh,8,64,256,256,0xFFB366E6);
  if(fh>0)g.blit(RenderPipelines.GUI_TEXTURED,HUD,7,74-vh-fh,88,56,8,fh,8,64,256,256,0xFF401A4D);
  g.blit(RenderPipelines.GUI_TEXTURED,HUD,3,1,72,48,16,80,256,256);
  g.blit(RenderPipelines.GUI_TEXTURED,HUD,4,8+Math.round((1-base)*64),117,61,14,5,256,256);
  if(mc.player.isShiftKeyDown()){
   g.pose().pushMatrix();g.pose().scale(.5F,.5F);
   g.text(mc.font,String.format(java.util.Locale.ROOT,"%.1f",aura.vis()),36,(74-vh)*2,0xFFEEABFF,false);
   if(fh>0)g.text(mc.font,String.format(java.util.Locale.ROOT,"%.1f",aura.flux()),36,(70-vh-fh)*2,0xFFAA11BB,false);
   g.pose().popMatrix();
  }
 }
 private ThaumometerHud(){}
}
