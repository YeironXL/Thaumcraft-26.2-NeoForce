package art.arcane.thaumcraft.aura;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** TC6 aura is scalar vis/flux, not a separate pool for every aspect. */
public final class ChunkAura {
    public static final MapCodec<ChunkAura> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(-1,500).optionalFieldOf("base", -1).forGetter(ChunkAura::base),
            Codec.floatRange(0,Float.MAX_VALUE).optionalFieldOf("vis", 0F).forGetter(ChunkAura::vis),
            Codec.floatRange(0,Float.MAX_VALUE).optionalFieldOf("flux", 0F).forGetter(ChunkAura::flux)
    ).apply(instance, ChunkAura::new));
    private int base;
    private float vis, flux;
    public ChunkAura() { this(-1,0,0); }
    private ChunkAura(int base, float vis, float flux) { this.base = base; this.vis = vis; this.flux = flux; }
    public int base() { return base; }
    public float vis() { return vis; }
    public float flux() { return flux; }
    public void initialize(int value) { if (base < 0) { base = Math.clamp(value,0,500); vis = base; } }
    public boolean spend(float amount) { if (!Float.isFinite(amount) || amount < 0 || vis < amount) return false; vis -= amount; return true; }
    public boolean spend(int amount) { if (amount < 0 || vis < amount) return false; vis -= amount; return true; }
    public boolean spendFlux(int amount) { if (amount < 0 || flux < amount) return false; flux -= amount; return true; }
    public void addVis(float amount) { if (Float.isFinite(amount)) vis = (float)Math.clamp((double)vis + amount,0D,(double)Float.MAX_VALUE); }
    public void addFlux(float amount) { if (Float.isFinite(amount)) flux = (float)Math.clamp((double)flux + amount,0D,(double)Float.MAX_VALUE); }
    public void setVis(float v) { vis = Float.isFinite(v) ? Math.max(0F, v) : vis; }
    public void setFlux(float v) { flux = Float.isFinite(v) ? Math.max(0F, v) : flux; }
}
