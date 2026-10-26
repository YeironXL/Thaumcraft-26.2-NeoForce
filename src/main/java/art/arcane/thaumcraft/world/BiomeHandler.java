package art.arcane.thaumcraft.world;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;

/**
 * TC6 BiomeHandler. Forge BiomeDictionary types map to the equivalent {@code c:} biome tags.
 * Entries keep the original registration order (DRY was registered twice; the second entry wins, as in the HashMap).
 */
public final class BiomeHandler {
    public record Info(TagKey<Biome> tag, float aura, String aspect, boolean greatwood, float greatwoodChance) {}
    private static TagKey<Biome> c(String p) { return TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("c", p)); }
    public static final TagKey<Biome> RARE = c("is_rare");
    public static final TagKey<Biome> MAGICAL = c("is_magical");
    public static final List<Info> INFO = List.of(
            new Info(c("is_aquatic"), 0.33f, "aqua", false, 0f),
            new Info(c("is_ocean"), 0.33f, "aqua", false, 0f),
            new Info(c("is_river"), 0.4f, "aqua", false, 0f),
            new Info(c("is_wet"), 0.4f, "aqua", false, 0f),
            new Info(c("is_lush"), 0.5f, "aqua", true, 0.5f),
            new Info(c("is_hot"), 0.33f, "ignis", false, 0f),
            new Info(c("is_nether"), 0.125f, "ignis", false, 0f),
            new Info(c("is_badlands"), 0.33f, "ignis", false, 0f),
            new Info(c("is_spooky"), 0.5f, "ignis", false, 0f),
            new Info(c("is_dense_vegetation"), 0.4f, "ordo", false, 0f),
            new Info(c("is_snowy"), 0.25f, "ordo", false, 0f),
            new Info(c("is_cold"), 0.25f, "ordo", false, 0f),
            new Info(c("is_mushroom"), 0.75f, "ordo", false, 0f),
            new Info(MAGICAL, 0.75f, "ordo", true, 1f),
            new Info(c("is_tree/coniferous"), 0.33f, "terra", true, 0.2f),
            new Info(c("is_forest"), 0.5f, "terra", true, 1f),
            new Info(c("is_sandy"), 0.25f, "terra", false, 0f),
            new Info(c("is_beach"), 0.3f, "terra", false, 0f),
            new Info(c("is_jungle"), 0.6f, "terra", false, 0f),
            new Info(c("is_savanna"), 0.25f, "aer", true, 0.2f),
            new Info(c("is_mountain"), 0.3f, "aer", false, 0f),
            new Info(c("is_hill"), 0.33f, "aer", false, 0f),
            new Info(c("is_plains"), 0.3f, "aer", true, 0.2f),
            new Info(c("is_end"), 0.125f, "aer", false, 0f),
            new Info(c("is_dry"), 0.125f, "perditio", false, 0f),
            new Info(c("is_sparse_vegetation"), 0.2f, "perditio", false, 0f),
            new Info(c("is_swamp"), 0.5f, "perditio", true, 0.2f),
            new Info(c("is_wasteland"), 0.125f, "perditio", false, 0f),
            new Info(c("is_dead"), 0.1f, "perditio", false, 0f));

    private static List<Info> types(Holder<Biome> b) {
        List<Info> l = new ArrayList<>();
        for (Info i : INFO) if (b.is(i.tag)) l.add(i);
        return l;
    }

    /** getBiomeAuraModifier: average of the type values; an unregistered type (RARE/VOID) threw and gave 0.5. */
    public static float auraModifier(Holder<Biome> b) {
        List<Info> l = types(b);
        if (l.isEmpty() || b.is(RARE)) return 0.5f;
        float sum = 0; for (Info i : l) sum += i.aura; return sum / l.size();
    }

    /** getRandomBiomeTag: aspect of a random dictionary type, null when that type has no info. */
    public static String randomTag(Holder<Biome> b, RandomSource r) {
        List<Info> l = types(b);
        int n = l.size() + (b.is(RARE) ? 1 : 0);
        if (n == 0) return null;
        int k = r.nextInt(n);
        return k < l.size() ? l.get(k).aspect : null;
    }

    public static float greatwoodChance(Holder<Biome> b) {
        for (Info i : types(b)) if (i.greatwood) return i.greatwoodChance;
        return 0f;
    }

    private BiomeHandler() {}
}
