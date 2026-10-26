package art.arcane.thaumcraft.network;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
/** Full TC6 object-aspect table (registered tags, before stack bonuses), gzip-compressed JSON. */
public record AspectSnapshotPayload(byte[] data) implements CustomPacketPayload{
    public static final Type<AspectSnapshotPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("thaumcraft","aspect_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AspectSnapshotPayload> CODEC=StreamCodec.composite(ByteBufCodecs.byteArray(4*1024*1024),AspectSnapshotPayload::data,AspectSnapshotPayload::new);
    public Type<AspectSnapshotPayload> type(){return TYPE;}
    public static AspectSnapshotPayload of(Map<String,Map<String,Integer>> values){
        var bytes=new ByteArrayOutputStream();
        try(var gz=new GZIPOutputStream(bytes)){gz.write(new com.google.gson.Gson().toJson(values).getBytes(StandardCharsets.UTF_8));}catch(IOException e){throw new UncheckedIOException(e);}
        return new AspectSnapshotPayload(bytes.toByteArray());
    }
    public Map<String,Map<String,Integer>> values(){
        String text;
        try(var gz=new GZIPInputStream(new ByteArrayInputStream(data))){var raw=gz.readNBytes(32*1024*1024);text=new String(raw,StandardCharsets.UTF_8);}catch(IOException e){throw new UncheckedIOException(e);}
        var result=new LinkedHashMap<String,Map<String,Integer>>();var json=com.google.gson.JsonParser.parseString(text).getAsJsonObject();
        json.entrySet().forEach(entry->{var tags=new LinkedHashMap<String,Integer>();entry.getValue().getAsJsonObject().entrySet().forEach(tag->tags.put(tag.getKey(),tag.getValue().getAsInt()));result.put(entry.getKey(),tags);});
        return result;
    }
}
