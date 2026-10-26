package art.arcane.thaumcraft.essentia;

import art.arcane.thaumcraft.aspect.AspectRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Immutable single-aspect storage, shared by blocks and their carried items. */
public record EssentiaContents(String aspect, int amount) {
    public static final EssentiaContents EMPTY = new EssentiaContents("", 0);
    public static final Codec<EssentiaContents> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("aspect").forGetter(EssentiaContents::aspect),
        Codec.intRange(0,250).fieldOf("amount").forGetter(EssentiaContents::amount)
    ).apply(i, EssentiaContents::new));
    public EssentiaContents {
        if (amount < 0 || amount > 250 || amount > 0 && AspectRegistry.get(aspect) == null)
            throw new IllegalArgumentException("Invalid essentia contents");
        if (amount == 0) aspect = "";
    }
    public boolean accepts(String tag, int count) {
        return count > 0 && count <= 250 - amount && AspectRegistry.get(tag) != null
            && (amount == 0 || aspect.equals(tag));
    }
    public EssentiaContents insert(String tag, int count) {
        if (!accepts(tag,count)) throw new IllegalArgumentException("Incompatible essentia or insufficient room");
        return new EssentiaContents(tag,amount+count);
    }
    public EssentiaContents extract(int count) {
        if (count <= 0 || count > amount) throw new IllegalArgumentException("Insufficient essentia");
        return new EssentiaContents(aspect,amount-count);
    }
}
