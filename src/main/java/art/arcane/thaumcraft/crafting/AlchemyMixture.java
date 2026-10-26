package art.arcane.thaumcraft.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashMap;
import java.util.Map;

/** Persisted crucible contents; payments validate before any mutation. */
public final class AlchemyMixture {
    private record Entry(String tag, int amount) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(Codec.STRING.fieldOf("tag").forGetter(Entry::tag), Codec.intRange(1,Integer.MAX_VALUE).fieldOf("amount").forGetter(Entry::amount)).apply(i, Entry::new));
    }
    /** TC6 AspectList keeps insertion order (LinkedHashMap); NBT compounds do not, so the aspects persist and sync as a list. */
    private static final Codec<Map<String,Integer>> ORDERED = Entry.CODEC.listOf().xmap(
            l -> { var m = new LinkedHashMap<String,Integer>(); l.forEach(e -> m.merge(e.tag(), e.amount(), Integer::sum)); return m; },
            m -> m.entrySet().stream().map(e -> new Entry(e.getKey(), e.getValue())).toList());
    public static final MapCodec<AlchemyMixture> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(0,1000).optionalFieldOf("water",0).forGetter(AlchemyMixture::water),
            Codec.intRange(0,200).optionalFieldOf("heat",0).forGetter(AlchemyMixture::heat),
            ORDERED.optionalFieldOf("aspect_list").forGetter(m -> java.util.Optional.of(m.aspects())),
            Codec.unboundedMap(Codec.STRING,Codec.intRange(1,Integer.MAX_VALUE)).optionalFieldOf("aspects",Map.of()).forGetter(m -> Map.of()),
            Codec.INT.optionalFieldOf("counter",-100).forGetter(m -> m.counter)
    ).apply(i,(w,h,list,legacy,c) -> new AlchemyMixture(w,h,list.orElse(legacy),c)));
    private int water, heat, counter;
    private final Map<String,Integer> aspects;
    public AlchemyMixture() { this(0,0,Map.of(),-100); }
    private AlchemyMixture(int water,int heat,Map<String,Integer> aspects,int counter) {
        this.water=water; this.heat=heat; this.aspects=new LinkedHashMap<>(aspects); this.counter=counter;
    }
    public int water() { return water; }
    /** Used by the crucible fluid capability (TC6 TileCrucible.fill) and its transaction snapshots. */
    public void setWater(int w) { water = Math.clamp(w, 0, 1000); }
    public int heat() { return heat; }
    public Map<String,Integer> aspects() { return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(aspects)); }
    public long total() { return aspects.values().stream().mapToLong(Integer::longValue).sum(); }
    public boolean boiling() { return water > 0 && heat > 150; }
    public boolean overflowing(){return boiling()&&total()>500;}
    public boolean fill() { if (water == 1000) return false; water=1000; return true; }
    public void heatTick(boolean heated) { heat = Math.clamp(heat + (water > 0 && heated ? 1 : -1),0,200); }
    public boolean dissolve(Map<String,Integer> tags) {
        if (!boiling() || tags.isEmpty() || tags.values().stream().anyMatch(n -> n <= 0)) return false;
        tags.forEach((tag,count) -> aspects.merge(tag,count,(a,b) -> (int)Math.min(Integer.MAX_VALUE,(long)a+b)));
        counter=-150; return true;
    }
    public boolean canPay(Map<String,Integer> cost) {
        return boiling() && cost.entrySet().stream().allMatch(e -> e.getValue() > 0 && aspects.getOrDefault(e.getKey(),0) >= e.getValue());
    }
    public boolean pay(Map<String,Integer> cost) {
        if (!canPay(cost)) return false;
        cost.forEach((tag,count) -> { int left=aspects.get(tag)-count; if (left == 0) aspects.remove(tag); else aspects.put(tag,left); });
        water=Math.max(0,water-50); counter=-250; return true;
    }
    public float tickSpill(int random) {
        float flux=0;
        if (total() > 500) flux+=spillOne(random);
        if (++counter >= 100) { flux+=spillOne(random); counter=0; }
        return flux;
    }
    private float spillOne(int random) {
        if (aspects.isEmpty()) return 0;
        String tag = aspects.keySet().stream().skip(Math.floorMod(random,aspects.size())).findFirst().orElseThrow();
        int left=aspects.get(tag)-1; if (left == 0) aspects.remove(tag); else aspects.put(tag,left);
        return tag.equals("vitium") ? 1F : 0.25F;
    }
    public float empty() {
        float flux=total()*0.25F+aspects.getOrDefault("vitium",0)*0.75F;
        water=0; aspects.clear(); return flux;
    }
}
