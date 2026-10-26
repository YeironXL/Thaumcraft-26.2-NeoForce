package art.arcane.thaumcraft.casters.ward;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * TC4 Focus of Warding data, kept per chunk (chunk attachment): packed block position to the warder's UUID.
 * TC4 stored warded blocks as a separate "warded" block + tile entity holding the owner; here the original block
 * stays in place and the ward lives in the chunk.
 */
public final class WardData {
    private final Map<Long, UUID> wards = new HashMap<>();

    public WardData() {}
    private WardData(Map<Long, UUID> m) { wards.putAll(m); }

    private record Entry(long pos, UUID owner) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("pos").forGetter(Entry::pos), UUIDUtil.CODEC.fieldOf("owner").forGetter(Entry::owner)).apply(i, Entry::new));
    }

    public static final MapCodec<WardData> CODEC = Entry.CODEC.listOf().fieldOf("wards").xmap(
            l -> { var d = new WardData(); for (var e : l) d.wards.put(e.pos(), e.owner()); return d; },
            d -> d.wards.entrySet().stream().map(e -> new Entry(e.getKey(), e.getValue())).toList());

    public static final StreamCodec<ByteBuf, WardData> STREAM_CODEC = ByteBufCodecs.map(HashMap::new, ByteBufCodecs.VAR_LONG, UUIDUtil.STREAM_CODEC)
            .map(WardData::new, d -> new HashMap<>(d.wards));

    public UUID owner(long pos) { return wards.get(pos); }
    public void put(long pos, UUID owner) { wards.put(pos, owner); }
    public UUID remove(long pos) { return wards.remove(pos); }
    public boolean isEmpty() { return wards.isEmpty(); }
    public java.util.Set<Long> positions() { return wards.keySet(); }
    public List<Long> list() { return List.copyOf(wards.keySet()); }
}
