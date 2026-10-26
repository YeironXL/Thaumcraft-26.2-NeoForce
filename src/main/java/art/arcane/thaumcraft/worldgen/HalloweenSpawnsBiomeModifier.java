package art.arcane.thaumcraft.worldgen;

import art.arcane.thaumcraft.Thaumcraft;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.time.LocalDate;
import java.time.Month;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** ConfigEntities: on October 31st (checked when biomes are built, as TC6 checked at startup) fire bats also spawn in the overworld. */
public record HalloweenSpawnsBiomeModifier(HolderSet<Biome> biomes, Weighted<SpawnerData> spawner) implements BiomeModifier {
    private static final DeferredRegister<MapCodec<? extends BiomeModifier>> SERIALIZERS = DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, Thaumcraft.MOD_ID);
    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<HalloweenSpawnsBiomeModifier>> CODEC = SERIALIZERS.register("halloween_spawns", () -> RecordCodecBuilder.mapCodec(b -> b.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(HalloweenSpawnsBiomeModifier::biomes),
            Weighted.codec(SpawnerData.CODEC).fieldOf("spawners").forGetter(HalloweenSpawnsBiomeModifier::spawner)
    ).apply(b, HalloweenSpawnsBiomeModifier::new)));

    public static boolean isHalloween() { LocalDate d = LocalDate.now(); return d.getMonth() == Month.OCTOBER && d.getDayOfMonth() == 31; }

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD || !isHalloween() || !biomes.contains(biome)) return;
        builder.getMobSpawnSettings().addSpawn(spawner.value().type().getCategory(), spawner.weight(), spawner.value());
    }

    @Override public MapCodec<? extends BiomeModifier> codec() { return CODEC.get(); }

    public static void register(IEventBus bus) { SERIALIZERS.register(bus); }
}
