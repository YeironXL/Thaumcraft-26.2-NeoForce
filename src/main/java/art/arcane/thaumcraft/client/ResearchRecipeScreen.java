package art.arcane.thaumcraft.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Read-only shaped crafting diagrams from the exact recipe resources shipped by this build. */
public final class ResearchRecipeScreen extends Screen {
    private static final Identifier OVERLAY=Identifier.parse("thaumcraft:textures/gui/gui_researchbook_overlay.png");
    public record Diagram(String id,JsonObject recipe) {}
    private final Screen parent;
    private final List<Diagram> diagrams;
    private int index,left,top,screenMouseX,screenMouseY;
    private float layoutScale=1;
    private Button previous,next;
    public ResearchRecipeScreen(Screen parent,List<Diagram> diagrams){super(Component.translatable("screen.thaumcraft.recipes"));this.parent=parent;this.diagrams=List.copyOf(diagrams);if(diagrams.isEmpty())throw new IllegalArgumentException("Empty recipe book");}
    private static JsonObject groups;
    /** Original book ids ("thaumcraft:EssentiaSmelter") are the lower-cased recipe ids; recipe groups expand to their members. */
    private static List<String> expand(String raw){
        var id=Identifier.tryParse(raw.indexOf(':')<0?raw:raw.substring(0,raw.indexOf(':')+1)+raw.substring(raw.indexOf(':')+1).toLowerCase(java.util.Locale.ROOT));
        if(id==null||!id.getNamespace().equals("thaumcraft"))return List.of();
        var rm=Minecraft.getInstance().getResourceManager();
        if(rm.getResource(Identifier.fromNamespaceAndPath("thaumcraft","book_recipes/"+id.getPath()+".json")).isPresent())return List.of(id.getPath());
        if(groups==null){groups=new JsonObject();var res=rm.getResource(Identifier.fromNamespaceAndPath("thaumcraft","book_groups.json"));
            if(res.isPresent())try(var r=res.get().openAsReader()){groups=JsonParser.parseReader(r).getAsJsonObject();}catch(java.io.IOException|RuntimeException e){com.mojang.logging.LogUtils.getLogger().warn("Cannot read book groups",e);}}
        if(!groups.has(id.getPath()))return List.of();
        var out=new ArrayList<String>();for(var e:groups.getAsJsonArray(id.getPath()))out.add(e.getAsString());return out;
    }
    public static List<Diagram> resolve(List<String> ids){
        var result=new ArrayList<Diagram>();
        for(String raw:ids)for(String path:expand(raw)){
            var resource=Minecraft.getInstance().getResourceManager().getResource(Identifier.fromNamespaceAndPath("thaumcraft","book_recipes/"+path+".json"));
            if(resource.isEmpty())continue;
            try(var reader=resource.get().openAsReader()){
                var json=JsonParser.parseReader(reader).getAsJsonObject();String type=json.has("type")?json.get("type").getAsString():"";
                if(type.equals("thaumcraft:crucible")&&simpleCrucible(json))result.add(new Diagram(raw,json));
                if(java.util.Set.of("thaumcraft:arcane","minecraft:crafting_shaped").contains(type)&&json.has("pattern")&&json.has("key")&&json.has("result")&&simpleIngredients(json))result.add(new Diagram(raw,json));
                if(java.util.Set.of("thaumcraft:shapeless_arcane","minecraft:crafting_shapeless").contains(type)&&json.has("ingredients")&&json.has("result")&&shapelessIngredients(json))result.add(new Diagram(raw,json));
            }catch(java.io.IOException|RuntimeException error){com.mojang.logging.LogUtils.getLogger().warn("Cannot read book recipe {}",raw,error);}
        }
        return List.copyOf(result);
    }
    private static boolean shapelessIngredients(JsonObject json){
        var output=json.getAsJsonObject("result");if(!output.has("id")||stack(output.get("id").getAsString(),1).isEmpty())return false;
        var list=json.getAsJsonArray("ingredients");if(list.isEmpty()||list.size()>9)return false;
        for(var e:list)if(ingredientOptions(e).isEmpty())return false;
        return true;
    }
    private static boolean simpleCrucible(JsonObject json){
        if(!json.has("catalyst")||ingredientOptions(json.get("catalyst")).isEmpty()||!json.has("result")||!json.has("aspects"))return false;
        var output=json.getAsJsonObject("result");if(!output.has("id")||stack(output.get("id").getAsString(),1).isEmpty())return false;
        var aspects=json.getAsJsonObject("aspects");if(aspects.size()==0||aspects.size()>9)return false;
        return aspects.entrySet().stream().allMatch(entry->art.arcane.thaumcraft.aspect.AspectRegistry.get(entry.getKey())!=null&&entry.getValue().getAsInt()>0);
    }
    private static boolean simpleIngredients(JsonObject json){
        var output=json.getAsJsonObject("result");if(!output.has("id")||stack(output.get("id").getAsString(),1).isEmpty())return false;
        var keys=json.getAsJsonObject("key");
        for(var row:json.getAsJsonArray("pattern")){
            String line=row.getAsString();if(line.length()>3)return false;
            for(char symbol:line.toCharArray())if(symbol!=' '){
                var ingredient=keys.get(String.valueOf(symbol));
                if(ingredient==null||ingredientOptions(ingredient).isEmpty())return false;
            }
        }
        return json.getAsJsonArray("pattern").size()>0&&json.getAsJsonArray("pattern").size()<=3;
    }
    @Override protected void init(){
        layoutScale=Math.min(1f,Math.min((width-16)/256f,(height-16)/256f));
        left=Math.round((width-256*layoutScale)/2);top=Math.round((height-256*layoutScale)/2);
        previous=addRenderableWidget(Button.builder(Component.translatable("screen.thaumcraft.previous_page"),b->index=Math.max(0,index-1)).bounds(left+Math.round(40*layoutScale),top+Math.round(232*layoutScale),Math.max(1,Math.round(12*layoutScale)),Math.max(1,Math.round(8*layoutScale))).build(builder->new RecipeArrow(builder,false)));
        next=addRenderableWidget(Button.builder(Component.translatable("screen.thaumcraft.next_page"),b->index=Math.min(diagrams.size()-1,index+1)).bounds(left+Math.round(204*layoutScale),top+Math.round(232*layoutScale),Math.max(1,Math.round(12*layoutScale)),Math.max(1,Math.round(8*layoutScale))).build(builder->new RecipeArrow(builder,true)));
    }
    private static final class RecipeArrow extends Button{
        private final boolean forward;
        RecipeArrow(Builder builder,boolean forward){super(builder);this.forward=forward;setTooltip(net.minecraft.client.gui.components.Tooltip.create(getMessage()));}
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float tick){
            g.blit(RenderPipelines.GUI_TEXTURED,Identifier.parse("thaumcraft:textures/gui/gui_researchbook.png"),getX(),getY(),forward?24:0,368,getWidth(),getHeight(),24,16,512,512);
        }
    }
    private static ItemStack stack(String raw,int count){var id=Identifier.tryParse(raw);if(id==null)return ItemStack.EMPTY;var item=BuiltInRegistries.ITEM.getValue(id);return item==null||item==Items.AIR?ItemStack.EMPTY:new ItemStack(item,count);}
    public static List<ItemStack> ingredientOptions(com.google.gson.JsonElement e){
        if(e.isJsonPrimitive())return ingredientOptions(e.getAsString());
        if(e.isJsonArray()){var out=new ArrayList<ItemStack>();for(var x:e.getAsJsonArray())out.addAll(ingredientOptions(x));return out;}
        var o=e.getAsJsonObject();if(!o.has("items"))return List.of();
        var base=ingredientOptions(o.get("items"));if(!o.has("components"))return base;
        var comps=o.getAsJsonObject("components");var out=new ArrayList<ItemStack>();
        for(var b:base){var c=b.copy();if(comps.has("thaumcraft:aspect"))c.set(art.arcane.thaumcraft.registry.ModComponents.ASPECT.get(),comps.get("thaumcraft:aspect").getAsString());out.add(c);}
        return out;
    }
    public static List<ItemStack> ingredientOptions(String raw){
        if(!raw.startsWith("#")){var value=stack(raw,1);return value.isEmpty()?List.of():List.of(value);}
        var id=Identifier.tryParse(raw.substring(1));if(id==null)return List.of();var result=new ArrayList<ItemStack>();
        for(var holder:BuiltInRegistries.ITEM.getTagOrEmpty(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,id))){
            if(holder.value()!=Items.AIR)result.add(new ItemStack(holder.value()));
        }
        return List.copyOf(result);
    }
    public static ItemStack ingredientAt(com.google.gson.JsonElement raw,int slot,long millis){
        var options=ingredientOptions(raw);return options.isEmpty()?ItemStack.EMPTY:options.get((int)Math.floorMod(millis/1000+slot,options.size()));
    }
    public static ItemStack ingredientAt(String raw,int slot,long millis){
        var options=ingredientOptions(raw);return options.isEmpty()?ItemStack.EMPTY:options.get((int)Math.floorMod(millis/1000+slot,options.size()));
    }
    private void item(GuiGraphicsExtractor g,ItemStack stack,int x,int y,int mx,int my){
        if(stack.isEmpty())return;g.item(stack,x,y);g.itemDecorations(font,stack,x,y);
        if(mx>=x&&mx<x+16&&my>=y&&my<y+16)g.setTooltipForNextFrame(ThaumcraftTheme.ink(stack.getHoverName().copy().append(stack.getCount()>1?" ×"+stack.getCount():"")),screenMouseX,screenMouseY);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float tick){
        screenMouseX=mx;screenMouseY=my;
        g.fillGradient(0,0,width,height,0xEC0D0C16,0xEC241C2D);
        int actualLeft=left,actualTop=top;
        int localX=(int)((mx-left)/layoutScale),localY=(int)((my-top)/layoutScale);
        g.pose().pushMatrix();g.pose().translate(left,top);g.pose().scale(layoutScale,layoutScale);left=0;top=0;
        g.blit(RenderPipelines.GUI_TEXTURED,Identifier.parse("thaumcraft:textures/gui/paper.png"),0,0,0,0,255,255,256,256);g.nextStratum();
        mx=localX;my=localY;
        var diagram=diagrams.get(index);var json=diagram.recipe();int cx=128,cy=128;
        if(json.get("type").getAsString().equals("thaumcraft:crucible")){renderCrucible(g,json,cx,128,mx,my);finish(g,actualLeft,actualTop,tick);return;}
        String type=json.get("type").getAsString();
        boolean arcane=type.startsWith("thaumcraft:");boolean shapeless=type.endsWith("shapeless")||type.equals("thaumcraft:shapeless_arcane");
        ThaumcraftTheme.text(g,font,Component.translatable(arcane?"screen.thaumcraft.arcane_recipe":"screen.thaumcraft.crafting_recipe"),128-font.width(ThaumcraftTheme.ink(Component.translatable(arcane?"screen.thaumcraft.arcane_recipe":"screen.thaumcraft.crafting_recipe")))/2,24,0xFF505050,false);
        g.blit(RenderPipelines.GUI_TEXTURED,OVERLAY,cx-52,cy-52,arcane?112:60,15,arcane?104:102,104,arcane?52:51,52,256,256);
        g.blit(RenderPipelines.GUI_TEXTURED,OVERLAY,cx-16,cy-92,20,3,32,32,16,16,256,256);g.nextStratum();
        var output=json.getAsJsonObject("result");item(g,stack(output.get("id").getAsString(),output.has("count")?output.get("count").getAsInt():1),cx-8,cy-84,mx,my);
        if(shapeless){
            var list=json.getAsJsonArray("ingredients");
            for(int n=0;n<list.size();n++)item(g,ingredientAt(list.get(n),n,net.minecraft.util.Util.getMillis()),cx-40+n%3*32,cy-40+n/3*32,mx,my);
            ThaumcraftTheme.text(g,font,Component.translatable("screen.thaumcraft.shapeless"),cx+28,cy-92,0xFF505050,false);
        }else{
        var pattern=json.getAsJsonArray("pattern");var key=json.getAsJsonObject("key");
        for(int row=0;row<Math.min(3,pattern.size());row++){
            String line=pattern.get(row).getAsString();for(int col=0;col<Math.min(3,line.length());col++){
                var ingredient=key.get(String.valueOf(line.charAt(col)));if(ingredient==null)continue;
                item(g,ingredientAt(ingredient,col+row*3,net.minecraft.util.Util.getMillis()),cx-40+col*32,cy-40+row*32,mx,my);
            }
        }
        }
        if(arcane&&json.has("crystals")){
            var crystals=json.getAsJsonObject("crystals");int n=0;
            for(var entry:crystals.entrySet())item(g,art.arcane.thaumcraft.item.CrystalEssenceItem.of(entry.getKey(),entry.getValue().getAsInt()),cx+4-crystals.size()*10+n++*20,cy+59,mx,my);
        }
        if(arcane&&json.has("vis"))ThaumcraftTheme.text(g,font,json.get("vis").getAsInt()+" vis",cx-20,cy+90,0xFF505050,false);
        finish(g,actualLeft,actualTop,tick);
    }
    private void finish(GuiGraphicsExtractor g,int actualLeft,int actualTop,float tick){
        left=actualLeft;top=actualTop;g.pose().popMatrix();
        previous.visible=index>0;next.visible=index+1<diagrams.size();
        super.extractRenderState(g,screenMouseX,screenMouseY,tick);
    }
    private void renderCrucible(GuiGraphicsExtractor g,JsonObject json,int cx,int cy,int mx,int my){
        ThaumcraftTheme.text(g,font,Component.translatable("screen.thaumcraft.crucible_recipe"),128-font.width(ThaumcraftTheme.ink(Component.translatable("screen.thaumcraft.crucible_recipe")))/2,24,0xFF505050,false);
        g.blit(RenderPipelines.GUI_TEXTURED,OVERLAY,cx-56,cy-58,0,3,112,34,56,17,256,256);
        g.blit(RenderPipelines.GUI_TEXTURED,OVERLAY,cx-56,cy-24,0,20,112,96,56,48,256,256);
        g.blit(RenderPipelines.GUI_TEXTURED,OVERLAY,cx-50,cy-52,100,84,22,26,11,13,256,256);g.nextStratum();
        var output=json.getAsJsonObject("result");item(g,stack(output.get("id").getAsString(),output.has("count")?output.get("count").getAsInt():1),cx-8,cy-50,mx,my);
        var catalyst=ingredientAt(json.get("catalyst"),0,net.minecraft.util.Util.getMillis());
        if(json.has("catalyst_aspect")){catalyst=catalyst.copy();catalyst.set(art.arcane.thaumcraft.registry.ModComponents.ASPECT.get(),json.get("catalyst_aspect").getAsString());}
        item(g,catalyst,cx-64,cy-56,mx,my);
        var aspects=json.getAsJsonObject("aspects");var keys=aspects.keySet().stream().sorted().toList();int rows=(keys.size()-1)/3,shift=(3-keys.size()%3)*10;
        for(int n=0;n<keys.size();n++){
            String aspect=keys.get(n);int m=n/3>=rows&&(rows>1||keys.size()<3)?1:0;
            int x=cx-28+n%3*20+shift*m,y=cy+8-10*rows+n/3*20;
            g.blit(RenderPipelines.GUI_TEXTURED,Identifier.parse("thaumcraft:textures/aspects/"+aspect+".png"),x,y,0,0,16,16,32,32,32,32,0xFF000000|art.arcane.thaumcraft.aspect.AspectRegistry.color(aspect));
            g.nextStratum();g.text(font,Component.literal(aspects.get(aspect).getAsString()),x+10,y+9,0xFFFFFFFF,true);
            if(mx>=x&&mx<x+16&&my>=y&&my<y+16)g.setTooltipForNextFrame(ThaumcraftTheme.ink(Component.translatable(aspect).append(" ×"+aspects.get(aspect).getAsInt())),screenMouseX,screenMouseY);
        }
    }
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
