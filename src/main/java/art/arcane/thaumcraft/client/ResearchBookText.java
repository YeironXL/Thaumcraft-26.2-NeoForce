package art.arcane.thaumcraft.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Original research paragraph/page markers, without leaking image descriptors into prose. */
public final class ResearchBookText {
    public static String readable(String value){
        return value.replaceAll("(?is)<IMG>.*?</IMG>", "\n\n")
            .replaceAll("(?i)<br\\s*/?>", "\n\n")
            .replaceAll("(?i)<page\\s*/?>", "\f")
            .replaceAll("(?i)<line\\s*/?>", "\n")
            .replaceAll("(?i)<div\\s*/?>", "\n\n")
            .replaceAll("<[^>]+>", "")
            .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">");
    }
    public static List<List<FormattedCharSequence>> pages(Font font,String text,int width,int capacity){
        if(width<1||capacity<1)throw new IllegalArgumentException("Invalid book page dimensions");
        var pages=new ArrayList<List<FormattedCharSequence>>();
        for(String section:text.split("\f",-1)){
            var lines=font.split(ThaumcraftTheme.ink(Component.literal(section)),width);
            if(lines.isEmpty())pages.add(List.of());
            else for(int start=0;start<lines.size();start+=capacity)
                pages.add(List.copyOf(lines.subList(start,Math.min(lines.size(),start+capacity))));
        }
        return List.copyOf(pages);
    }
    public sealed interface Element permits Text,Image { int y(); }
    public record Text(int y,FormattedCharSequence line) implements Element {}
    public record Image(int y,net.minecraft.resources.Identifier texture,int u,int v,int sourceWidth,int sourceHeight,int width,int height) implements Element {}
    private static Image image(String descriptor,int pageWidth,int pageHeight){
        try{
            String[] fields=descriptor.split(":");if(fields.length!=7)return null;
            var texture=net.minecraft.resources.Identifier.tryParse(fields[0]+":"+fields[1].replace("textures/items/","textures/item/").replace("textures/blocks/","textures/block/"));
            int u=Integer.parseInt(fields[2]),v=Integer.parseInt(fields[3]),w=Integer.parseInt(fields[4]),h=Integer.parseInt(fields[5]);double scale=Double.parseDouble(fields[6]);
            if(texture==null||u<0||v<0||w<=0||h<=0||w>256||h>256||u>256-w||v>256-h||!Double.isFinite(scale)||scale<=0)return null;
            scale=Math.min(scale,Math.min(pageWidth/(double)w,(pageHeight-4)/(double)h));
            return new Image(0,texture,u,v,w,h,Math.max(1,(int)(w*scale)),Math.max(1,(int)(h*scale)));
        }catch(IllegalArgumentException exception){return null;}
    }
    /** Text and illustrations share page height; original UVs are normalized to a 256 atlas. */
    public static List<List<Element>> illustratedPages(Font font,String raw,int width,int height){
        if(width<1||height<11)throw new IllegalArgumentException("Invalid illustrated page dimensions");
        var pages=new ArrayList<List<Element>>();var current=new ArrayList<Element>();int y=0,cursor=0;
        var matcher=java.util.regex.Pattern.compile("(?is)<IMG>(.*?)</IMG>|<(PAGE|LINE|DIV)\\s*/?>").matcher(raw);
        var parts=new ArrayList<Object>();
        while(matcher.find()){
            if(matcher.start()>cursor)parts.add(raw.substring(cursor,matcher.start()));
            if(matcher.group(1)!=null){var img=image(matcher.group(1).trim(),width,height);if(img!=null)parts.add(img);}
            else if(matcher.group(2).equalsIgnoreCase("PAGE"))parts.add(Boolean.TRUE);
            else {var img=image("thaumcraft:textures/gui/gui_researchbook.png:"+(matcher.group(2).equalsIgnoreCase("LINE")?"24:184:95:6:1":"28:192:140:6:1"),width,height);parts.add(img);}
            cursor=matcher.end();
        }
        if(cursor<raw.length())parts.add(raw.substring(cursor));
        for(Object part:parts){
            if(part instanceof Boolean){pages.add(List.copyOf(current));current.clear();y=0;}
            else if(part instanceof Image img){
                if(y+img.height()+4>height&&!current.isEmpty()){pages.add(List.copyOf(current));current.clear();y=0;}
                current.add(new Image(y,img.texture(),img.u(),img.v(),img.sourceWidth(),img.sourceHeight(),img.width(),img.height()));y+=img.height()+4;
            }else for(var line:font.split(ThaumcraftTheme.ink(Component.literal(readable((String)part))),width)){
                if(y+11>height){pages.add(List.copyOf(current));current.clear();y=0;}
                current.add(new Text(y,line));y+=11;
            }
        }
        pages.add(List.copyOf(current));return List.copyOf(pages);
    }
    private ResearchBookText(){}
}
