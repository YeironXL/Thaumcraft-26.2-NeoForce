package art.arcane.thaumcraft.worldgen;

import art.arcane.thaumcraft.Thaumcraft;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * ConfigEntities.postInitEntitySpawns: TC6 only added its spawns to biomes that already had monster spawns
 * ({@code bgb.getSpawnableList(MONSTER).size() > 0}), e.g. angry zombies across the overworld or pech in magical biomes.
 * EntityRegistry.addSpawn updated an existing entry of the same entity (weight and group size) instead of adding a
 * second one, e.g. the magical forest's own pech 20 (1-2) became 10 (1-1).
 */
public record MonsterBiomeSpawnsBiomeModifier(HolderSet<Biome> biomes, Weighted<SpawnerData> spawner) implements BiomeModifier {
    private static final DeferredRegister<MapCodec<? extends BiomeModifier>> SERIALIZERS = DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, Thaumcraft.MOD_ID);
    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<MonsterBiomeSpawnsBiomeModifier>> CODEC = SERIALIZERS.register("monster_biome_spawns", () -> RecordCodecBuilder.mapCodec(b -> b.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(MonsterBiomeSpawnsBiomeModifier::biomes),
            Weighted.codec(SpawnerData.CODEC).fieldOf("spawners").forGetter(MonsterBiomeSpawnsBiomeModifier::spawner)
    ).apply(b, MonsterBiomeSpawnsBiomeModifier::new)));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD || !biomes.contains(biome)) return;
        if (builder.getMobSpawnSettings().getSpawner(MobCategory.MONSTER).getList().isEmpty()) return;
        var type = spawner.value().type();
        builder.getMobSpawnSettings().getSpawner(type.getCategory()).removeIf(w -> w.value().type() == type);
        builder.getMobSpawnSettings().addSpawn(spawner.value().type().getCategory(), spawner.weight(), spawner.value());
    }

    @Override public MapCodec<? extends BiomeModifier> codec() { return CODEC.get(); }

    public static void register(IEventBus bus) { SERIALIZERS.register(bus); }
}
