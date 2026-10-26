package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.registry.ModEntities;
import art.arcane.thaumcraft.world.BiomeHandler;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;

/** TC6 BiomeHandler / Registrar.registerBiomes / ConfigEntities.postInitEntitySpawns spawn lists. */
public final class BiomeSpawnsVerification {
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    private static boolean has(Biome b, EntityType<?> t, int w, int min, int max) {
        return b.getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream()
                .filter(x -> x.value().type() == t).anyMatch(x -> x.weight() == w && x.value().minCount() == min && x.value().maxCount() == max);
    }
    private static long count(Biome b, EntityType<?> t) {
        return b.getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream().filter(x -> x.value().type() == t).count();
    }

    public static void test(GameTestHelper h) {
        var reg = h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        var mfKey = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("thaumcraft", "magical_forest"));
        var eeKey = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("thaumcraft", "eerie"));
        var elKey = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("thaumcraft", "eldritch"));
        var mfH = reg.getOrThrow(mfKey); var eeH = reg.getOrThrow(eeKey); var elH = reg.getOrThrow(elKey);
        Biome mf = mfH.value(), ee = eeH.value(), el = elH.value();
        // BiomeDictionary types
        check(mfH.is(BiomeHandler.MAGICAL) && eeH.is(BiomeHandler.MAGICAL) && elH.is(BiomeHandler.MAGICAL), "TC biomes must be MAGICAL");
        check(mfH.is(BiomeTags.IS_FOREST) && mfH.is(BiomeTags.IS_OVERWORLD) && !eeH.is(BiomeTags.IS_OVERWORLD), "Magical forest must be an overworld forest, eerie not generated");
        // pech: own 20/1/2 entry updated by addSpawn(MAGICAL) to 10/1/1; eerie and outer lands get 10/1/1
        var pech = ModEntities.PECH.get();
        check(has(mf, pech, 10, 1, 1) && count(mf, pech) == 1, "Magical forest pech must be a single 10/1/1 entry");
        check(has(ee, pech, 10, 1, 1) && has(el, pech, 10, 1, 1), "Eerie / outer lands lack the 10/1/1 pech");
        check(has(mf, ModEntities.WISP.get(), 20, 1, 2), "Magical forest wisp 20/1/2 missing");
        check(has(mf, ModEntities.BRAINY_ZOMBIE.get(), 10, 1, 1), "Magical forest brainy zombie 10/1/1 missing");
        check(has(ee, ModEntities.BRAINY_ZOMBIE.get(), 32, 1, 1) && has(ee, ModEntities.GIANT_BRAINY_ZOMBIE.get(), 8, 1, 1), "Eerie brainy/giant brainy zombies missing");
        check(has(el, ModEntities.INHABITED_ZOMBIE.get(), 1, 1, 1) && has(el, ModEntities.ELDRITCH_GUARDIAN.get(), 1, 1, 1), "Outer lands inhabited zombie / guardian missing");
        // vanilla overworld: brainy zombie everywhere with monsters, no pech outside magical biomes
        var plains = reg.getOrThrow(Biomes.PLAINS).value();
        check(has(plains, ModEntities.BRAINY_ZOMBIE.get(), 10, 1, 1) && count(plains, pech) == 0, "Plains spawn list wrong");
        // the overworld preset contains the magical forest (BiomeManager WARM/COOL)
        var preset = h.getLevel().registryAccess().lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                .getOrThrow(net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists.OVERWORLD).value();
        check(preset.parameters().values().stream().anyMatch(p -> p.getSecond().is(mfKey)), "Overworld biome source lacks the magical forest");
        check(preset.parameters().values().stream().anyMatch(p -> p.getSecond().is(Biomes.FOREST)), "Overworld lost the vanilla forest");
        System.out.println("Biome spawns passed: magical forest/eerie/outer lands lists, pech, brainy, inhabited and overworld injection.");
        h.succeed();
    }
}
