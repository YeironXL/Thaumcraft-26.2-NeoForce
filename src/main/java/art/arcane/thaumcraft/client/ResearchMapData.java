package art.arcane.thaumcraft.client;

import art.arcane.thaumcraft.research.ResearchCatalog;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Original presentation metadata, independent of server completion rules. */
public final class ResearchMapData {
    public record Node(int column,int row,List<String> icons) {}
    private static final Map<String,Node> NODES=load();
    private static Map<String,Node> load(){
        Map<String,Node> nodes=new LinkedHashMap<>();
        var files=new java.util.ArrayList<>(ResearchCatalog.CATEGORIES);files.add("SCANS");
        for(String category:files){
            String path="/assets/thaumcraft/research/"+category.toLowerCase(java.util.Locale.ROOT)+".json";
            try(var stream=ResearchMapData.class.getResourceAsStream(path)){
                if(stream==null)throw new IllegalStateException("Missing visual research definition "+path);
                var entries=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("entries");
                for(var element:entries){
                    var value=element.getAsJsonObject();
                    if(!value.has("location"))continue;
                    var location=value.getAsJsonArray("location");
                    var icons=new java.util.ArrayList<String>();
                    if(value.has("icons"))for(var icon:value.getAsJsonArray("icons"))if(icon.isJsonPrimitive())icons.add(icon.getAsString());
                    nodes.put(value.get("key").getAsString(),new Node(location.get(0).getAsInt(),location.get(1).getAsInt(),List.copyOf(icons)));
                }
            }catch(java.io.IOException exception){throw new IllegalStateException("Cannot load research presentation",exception);}
        }
        return Map.copyOf(nodes);
    }
    public static Node node(String key){return NODES.get(key);}
    public static int background(String category){return switch(category){case "AUROMANCY"->2;case "ALCHEMY"->3;case "ARTIFICE"->4;case "GOLEMANCY"->5;case "ELDRITCH"->6;case "INFUSION"->7;default->1;};}
    private ResearchMapData(){}
}
